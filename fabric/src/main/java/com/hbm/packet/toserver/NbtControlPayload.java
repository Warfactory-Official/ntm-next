// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.packet.toserver;

import com.hbm.api.control.IControlReceiver;
import com.hbm.lib.Library;
import com.hbm.packet.IPayloadHandlerContext;
import com.hbm.packet.threading.ThreadedPayload;
import com.hbm.tileentity.machine.BlockEntityLaunchpadSoyuz;
import com.hbm.tileentity.machine.BlockEntityMachineRadar;
import com.hbm.util.ChunkUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public final class NbtControlPayload extends ThreadedPayload {

    public static final Type<NbtControlPayload> TYPE = new Type<>(Library.id("nbt_control"));
    public static final StreamCodec<ByteBuf, NbtControlPayload> STREAM_CODEC =
            streamCodec(NbtControlPayload::decode);

    private static final double MAX_DIST_SQ = 24.0D * 24.0D;
    private final BlockPos pos;
    private final CompoundTag data;

    public NbtControlPayload(BlockPos pos, CompoundTag data) {
        this.pos = pos;
        this.data = data;
    }

    private static NbtControlPayload decode(ByteBuf buf) {
        BlockPos pos = BlockPos.STREAM_CODEC.decode(buf);
        CompoundTag data = ByteBufCodecs.COMPOUND_TAG.decode(buf);
        return new NbtControlPayload(pos, data);
    }

    public static void handleServer(NbtControlPayload payload, IPayloadHandlerContext ctx) {
        if (!(ctx.player() instanceof ServerPlayer sp)) return;
        BlockEntity be = ChunkUtil.blockEntityIfLoaded(sp.level(), payload.pos);
        if (!(be instanceof IControlReceiver r) || !r.hasPermission(sp)) return;
        boolean remoteRadar =
                be instanceof BlockEntityMachineRadar radar && radar.isRadarMenuValid(sp);

        boolean launchpad = be instanceof BlockEntityLaunchpadSoyuz;
        if (!remoteRadar
                && !launchpad
                && sp.getEyePosition().distanceToSqr(Vec3.atCenterOf(payload.pos)) > MAX_DIST_SQ)
            return;

        r.receiveControl(sp, payload.data);

        be.setChanged();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        BlockPos.STREAM_CODEC.encode(buf, pos);
        ByteBufCodecs.COMPOUND_TAG.encode(buf, data);
    }

    @Override
    public @NotNull Type<NbtControlPayload> type() {
        return TYPE;
    }
}
