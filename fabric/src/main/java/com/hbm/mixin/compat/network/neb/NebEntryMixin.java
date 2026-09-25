// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin.compat.network.neb;

import com.hbm.packet.compat.NebEntry;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.ProtocolInfo;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.*;

@Pseudo
@Mixin(
        targets = "cn.ussshenzhou.notenoughbandwidth.aggregation.AggregatedEncodePacket",
        remap = false)
public abstract class NebEntryMixin implements NebEntry {
    @Shadow @Final public Identifier type;
    @Shadow @Final private CustomPacketPayload payload;

    @Shadow
    public abstract void encode(ByteBuf output, ProtocolInfo<?> protocol, PacketFlow flow);

    @Override
    public Identifier hbm$type() {
        return type;
    }

    @Override
    public CustomPacketPayload hbm$payload() {
        return payload;
    }

    @Override
    public void hbm$encode(ByteBuf output, ProtocolInfo<?> protocol, PacketFlow flow) {
        encode(output, protocol, flow);
    }
}
