package com.chunktracer.client;

import com.chunktracer.ChunkTracer;
import com.chunktracer.network.ChunkHeatSyncPayload;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

/**
 * Disegna, in alto a destra dello schermo, una griglia che rappresenta
 * i chunk attorno al giocatore, colorata in base a quante volte quei
 * chunk sono stati visitati (dal giocatore stesso o da altri, se su un
 * server condiviso). Più il colore tende al rosso, più il chunk è
 * "battuto" di frequente. Il piccolo quadrato bianco al centro è il
 * giocatore stesso, sempre visto dall'alto.
 */
public final class ChunkHeatOverlay {

    private static final int CELL_SIZE = 8; // pixel per chunk
    private static final int MARGIN = 8;

    private ChunkHeatOverlay() {
    }

    public static void register() {
        HudElementRegistry.attachElementBefore(
                VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath(ChunkTracer.MOD_ID, "chunk_heat_overlay"),
                ChunkHeatOverlay::render
        );
    }

    private static void render(GuiGraphicsExtractor graphics, DeltaTracker tickCounter) {
        if (!ChunkTracerClient.overlayEnabled) return;

        ChunkHeatSyncPayload snapshot = ChunkTracerClient.latestSnapshot;
        if (snapshot == null) return;

        Minecraft client = Minecraft.getInstance();
        Player player = client.player;
        if (player == null) return;

        int radius = snapshot.radius();
        int side = radius * 2 + 1;
        int[] counts = snapshot.counts();
        if (counts.length != side * side) return;

        int gridSize = side * CELL_SIZE;
        int screenWidth = client.getWindow().getGuiScaledWidth();
        int startX = screenWidth - gridSize - MARGIN;
        int startY = MARGIN;

        // Troviamo il massimo per normalizzare i colori (con un minimo per evitare divisioni brutte).
        int max = 1;
        for (int value : counts) {
            if (value > max) max = value;
        }

        // Sfondo semi-trasparente dietro la griglia.
        graphics.fill(startX - 2, startY - 2, startX + gridSize + 2, startY + gridSize + 2, 0x80000000);

        int i = 0;
        for (int dz = -radius; dz <= radius; dz++) {
            for (int dx = -radius; dx <= radius; dx++) {
                int value = counts[i++];
                int color = colorForValue(value, max);

                int cellX = startX + (dx + radius) * CELL_SIZE;
                int cellY = startY + (dz + radius) * CELL_SIZE;

                if (dx == 0 && dz == 0) {
                    // Il chunk in cui si trova il giocatore ora: bordo bianco per farlo risaltare.
                    graphics.fill(cellX, cellY, cellX + CELL_SIZE, cellY + CELL_SIZE, color);
                    graphics.outline(cellX, cellY, CELL_SIZE, CELL_SIZE, 0xFFFFFFFF);
                } else {
                    graphics.fill(cellX + 1, cellY + 1, cellX + CELL_SIZE - 1, cellY + CELL_SIZE - 1, color);
                }
            }
        }

        graphics.drawString(client.font, "Chunk Tracer (K)", startX, startY + gridSize + 4, 0xFFFFFFFF);
    }

    /**
     * Restituisce un colore ARGB che va dal blu (poco visitato) al rosso (molto visitato),
     * passando per il giallo, in base al rapporto value/max.
     */
    private static int colorForValue(int value, int max) {
        if (value <= 0) {
            return 0x50104060; // quasi trasparente, blu scuro: "mai visitato"
        }

        float t = Math.min(1.0f, value / (float) max);
        int r, g, b;
        if (t < 0.5f) {
            // blu -> giallo
            float localT = t / 0.5f;
            r = (int) (localT * 255);
            g = (int) (localT * 255);
            b = (int) (255 * (1 - localT));
        } else {
            // giallo -> rosso
            float localT = (t - 0.5f) / 0.5f;
            r = 255;
            g = (int) (255 * (1 - localT));
            b = 0;
        }

        int alpha = 0xE0;
        return (alpha << 24) | (r << 16) | (g << 8) | b;
    }
}
