package com.chunktracer;

import com.chunktracer.network.ChunkHeatSyncPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

/**
 * Chunk Tracer
 * <p>
 * Tiene traccia di quali chunk vengono visitati più spesso dai giocatori
 * (camminando o volando) e invia periodicamente ai client uno snapshot
 * della zona circostante, così da poterla visualizzare come heatmap dall'alto.
 */
public class ChunkTracer implements ModInitializer {

    public static final String MOD_ID = "chunktracer";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    /** Raggio (in chunk) dell'area sincronizzata attorno a ogni giocatore. */
    public static final int SYNC_RADIUS = 6;

    /** Ogni quanti tick di server si registra la posizione e si sincronizza (20 tick = 1 secondo). */
    private static final int TRACK_INTERVAL_TICKS = 20;

    /** Ogni quanti tick di server si salva su disco (6000 tick = 5 minuti). */
    private static final int SAVE_INTERVAL_TICKS = 6000;

    public static final ChunkHeatData HEAT_DATA = new ChunkHeatData();

    private static Path saveFile;
    private int tickCounter = 0;

    @Override
    public void onInitialize() {
        saveFile = FabricLoader.getInstance().getConfigDir().resolve("chunktracer").resolve("heatmap.txt");

        PayloadTypeRegistry.clientboundPlay().register(ChunkHeatSyncPayload.TYPE, ChunkHeatSyncPayload.STREAM_CODEC);

        ServerLifecycleEvents.SERVER_STARTED.register(server -> HEAT_DATA.load(saveFile));
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> HEAT_DATA.save(saveFile));

        ServerTickEvents.END_SERVER_TICK.register(this::onServerTick);

        LOGGER.info("[ChunkTracer] Inizializzato.");
    }

    private void onServerTick(MinecraftServer server) {
        tickCounter++;

        if (tickCounter % TRACK_INTERVAL_TICKS == 0) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                Vec3 pos = player.position();
                int chunkX = (int) Math.floor(pos.x() / 16.0);
                int chunkZ = (int) Math.floor(pos.z() / 16.0);
                String dimensionId = player.level().dimension().toString();

                HEAT_DATA.recordVisit(dimensionId, chunkX, chunkZ);

                int[] counts = HEAT_DATA.snapshot(dimensionId, chunkX, chunkZ, SYNC_RADIUS);
                ChunkHeatSyncPayload payload =
                        new ChunkHeatSyncPayload(dimensionId, chunkX, chunkZ, SYNC_RADIUS, counts);
                ServerPlayNetworking.send(player, payload);
            }
        }

        if (tickCounter % SAVE_INTERVAL_TICKS == 0) {
            HEAT_DATA.save(saveFile);
        }
    }
}
