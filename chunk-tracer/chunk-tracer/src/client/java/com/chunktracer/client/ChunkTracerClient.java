package com.chunktracer.client;

import com.chunktracer.ChunkTracer;
import com.chunktracer.network.ChunkHeatSyncPayload;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public class ChunkTracerClient implements ClientModInitializer {

    private static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath(ChunkTracer.MOD_ID, "main"));

    private static KeyMapping toggleOverlayKey;

    /** Ultimo snapshot ricevuto dal server, letto dall'overlay durante il render. */
    public static volatile ChunkHeatSyncPayload latestSnapshot;

    /** Se l'overlay è visibile o meno (attivabile/disattivabile con il tasto). */
    public static volatile boolean overlayEnabled = true;

    @Override
    public void onInitializeClient() {
        // Registriamo il tipo di payload e chi lo gestisce lato client.
        ClientPlayNetworking.registerGlobalReceiver(ChunkHeatSyncPayload.TYPE, (payload, context) -> {
            latestSnapshot = payload;
        });

        toggleOverlayKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.chunktracer.toggle_overlay",
                InputConstants.Type.KEYSYM,
                InputConstants.KEY_K,
                CATEGORY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleOverlayKey.consumeClick()) {
                overlayEnabled = !overlayEnabled;
            }
        });

        ChunkHeatOverlay.register();

        ChunkTracer.LOGGER.info("[ChunkTracer] Client inizializzato. Premi K per mostrare/nascondere la heatmap.");
    }
}
