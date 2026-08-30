package com.chunktracer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ChunkHeatData {

    private final Map<String, Map<Long, Integer>> data = new ConcurrentHashMap<>();

    public static long chunkKey(int chunkX, int chunkZ) {
        return (((long) chunkX) << 32) ^ (chunkZ & 0xffffffffL);
    }

    public static int keyToX(long key) {
        return (int) (key >> 32);
    }

    public static int keyToZ(long key) {
        return (int) key;
    }

    public void recordVisit(String dimensionId, int chunkX, int chunkZ) {
        Map<Long, Integer> dimData = data.computeIfAbsent(dimensionId, d -> new ConcurrentHashMap<>());
        dimData.merge(chunkKey(chunkX, chunkZ), 1, Integer::sum);
    }

    public int getCount(String dimensionId, int chunkX, int chunkZ) {
        Map<Long, Integer> dimData = data.get(dimensionId);
        if (dimData == null) return 0;
        Integer v = dimData.get(chunkKey(chunkX, chunkZ));
        return v == null ? 0 : v;
    }

    public int[] snapshot(String dimensionId, int centerChunkX, int centerChunkZ, int radius) {
        int side = radius * 2 + 1;
        int[] result = new int[side * side];
        Map<Long, Integer> dimData = data.get(dimensionId);
        if (dimData == null) return result;

        int i = 0;
        for (int dz = -radius; dz <= radius; dz++) {
            for (int dx = -radius; dx <= radius; dx++) {
                Integer v = dimData.get(chunkKey(centerChunkX + dx, centerChunkZ + dz));
                result[i++] = v == null ? 0 : v;
            }
        }
        return result;
    }

    public void save(Path file) {
        try {
            Files.createDirectories(file.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(file)) {
                for (Map.Entry<String, Map<Long, Integer>> dimEntry : data.entrySet()) {
                    String dimension = dimEntry.getKey();
                    for (Map.Entry<Long, Integer> chunkEntry : dimEntry.getValue().entrySet()) {
                        long key = chunkEntry.getKey();
                        writer.write(dimension + ";" + keyToX(key) + ";" + keyToZ(key) + ";" + chunkEntry.getValue());
                        writer.newLine();
                    }
                }
            }
        } catch (IOException e) {
            ChunkTracer.LOGGER.warn("[ChunkTracer] Impossibile salvare i dati dei chunk: {}", e.getMessage());
        }
    }

    public void load(Path file) {
        if (!Files.exists(file)) return;
        Map<String, Map<Long, Integer>> loaded = new HashMap<>();
        try (BufferedReader reader = Files.newBufferedReader(file)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(";");
                if (parts.length != 4) continue;
                String dimension = parts[0];
                int x = Integer.parseInt(parts[1]);
                int z = Integer.parseInt(parts[2]);
                int count = Integer.parseInt(parts[3]);
                loaded.computeIfAbsent(dimension, d -> new ConcurrentHashMap<>()).put(chunkKey(x, z), count);
            }
            data.clear();
            data.putAll(loaded);
            ChunkTracer.LOGGER.info("[ChunkTracer] Dati dei chunk caricati da {}", file);
        } catch (IOException e) {
            ChunkTracer.LOGGER.warn("[ChunkTracer] Impossibile caricare i dati dei chunk: {}", e.getMessage());
        }
    }
}
