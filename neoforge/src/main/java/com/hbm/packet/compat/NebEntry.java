// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.packet.compat;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.ProtocolInfo;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public interface NebEntry {
    Identifier hbm$type();

    CustomPacketPayload hbm$payload();

    void hbm$encode(ByteBuf output, ProtocolInfo<?> protocol, PacketFlow flow);
}
