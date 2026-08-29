package com.chunktracer.network;

import com.chunktracer.ChunkTracer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Pacchetto server -> client contenente lo snapshot delle visite ai chunk
 * in una griglia quadrata (2*radius+1) x (2*radius+1) centrata sul giocatore.
 */
public record ChunkHeatSyncPayload(String dimensionId, int centerChunkX, int centerChunkZ, int radius, int[] counts)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ChunkHeatSyncPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(ChunkTracer.MOD_ID, "chunk_heat_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ChunkHeatSyncPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeUtf(payload.dimensionId());
                buf.writeVarInt(payload.centerChunkX());
                buf.writeVarInt(payload.centerChunkZ());
                buf.writeVarInt(payload.radius());
                buf.writeVarInt(payload.counts().length);
                for (int value : payload.counts()) {
                    buf.writeVarInt(value);
                }
            },
            buf -> {
                String dimensionId = buf.readUtf();
                int centerChunkX = buf.readVarInt();
                int centerChunkZ = buf.readVarInt();
                int radius = buf.readVarInt();
                int length = buf.readVarInt();
                int[] counts = new int[length];
                for (int i = 0; i < length; i++) {
                    counts[i] = buf.readVarInt();
                }
                return new ChunkHeatSyncPayload(dimensionId, centerChunkX, centerChunkZ, radius, counts);
            }
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
