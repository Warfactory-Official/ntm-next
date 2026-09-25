// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.packet.toclient;

import com.hbm.lib.Library;
import com.hbm.packet.IPayloadHandlerContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.chunk.CompiledSectionMesh;
import net.minecraft.client.renderer.chunk.SectionMesh;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.jetbrains.annotations.NotNull;

public record OcclusionRefreshPayload(long chunkPos) implements CustomPacketPayload {

    public static final Type<OcclusionRefreshPayload> TYPE =
            new Type<>(Library.id("occlusion_refresh"));

    public static final StreamCodec<ByteBuf, OcclusionRefreshPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> buf.writeLong(payload.chunkPos),
                    buf -> new OcclusionRefreshPayload(buf.readLong()));

    public static void handle(OcclusionRefreshPayload payload, IPayloadHandlerContext ctx) {
        Minecraft mc = Minecraft.getInstance();
        LevelRenderer lr = mc.levelRenderer;
        ClientLevel level = mc.level;
        if (level == null) return;
        ViewArea viewArea = lr.viewArea;
        if (viewArea == null) return;
        int cx = ChunkPos.getX(payload.chunkPos);
        int cz = ChunkPos.getZ(payload.chunkPos);
        LevelChunk chunk = level.getChunkSource().getChunk(cx, cz, ChunkStatus.FULL, false);
        if (chunk == null) return;

        LevelChunkSection[] sections = chunk.getSections();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int i = 0; i < sections.length; i++) {
            pos.set(cx << 4, chunk.getSectionYFromSectionIndex(i) << 4, cz << 4);
            SectionRenderDispatcher.RenderSection rs = viewArea.getRenderSectionAt(pos);
            if (rs == null) continue;

            SectionMesh mesh = rs.getSectionMesh();
            if (sections[i].hasOnlyAir()
                    && mesh != CompiledSectionMesh.UNCOMPILED
                    && mesh != CompiledSectionMesh.EMPTY) {
                rs.reset();
            }
            lr.sectionOcclusionGraph.schedulePropagationFrom(rs);
        }
    }

    @Override
    public @NotNull Type<OcclusionRefreshPayload> type() {
        return TYPE;
    }
}
