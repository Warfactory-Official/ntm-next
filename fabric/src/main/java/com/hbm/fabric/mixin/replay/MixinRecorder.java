// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.fabric.mixin.replay;

import com.hbm.packet.toclient.BlobPayload;
import com.hbm.packet.toclient.UnitPayload;
import com.moulberry.flashback.record.Recorder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Recorder.class, remap = false)
public abstract class MixinRecorder {
    @Shadow
    public abstract void writePacketAsync(Packet<?> packet, ConnectionProtocol phase);

    @Inject(method = "writePacketAsync", at = @At("HEAD"), cancellable = true)
    private void hbm$recordMachineFrame(
            Packet<?> packet, ConnectionProtocol phase, CallbackInfo ci) {
        if (!(packet instanceof ClientboundCustomPayloadPacket custom)) return;
        BlockPos pos;
        int type;
        CompoundTag tag;
        if (custom.payload() instanceof UnitPayload unit) {
            pos = unit.pos();
            type = unit.blockEntityType();
            tag = unit.recordedTag();
        } else if (custom.payload() instanceof BlobPayload blob) {
            pos = blob.pos();
            type = blob.blockEntityType();
            tag = blob.recordedTag();
        } else {
            return;
        }
        ci.cancel();
        BlockEntityType<?> blockEntityType = BuiltInRegistries.BLOCK_ENTITY_TYPE.byId(type);
        if (blockEntityType != null) {
            writePacketAsync(
                    new ClientboundBlockEntityDataPacket(pos, blockEntityType, tag), phase);
        }
    }
}
