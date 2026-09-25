// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.packet.toclient;

import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluidProperty;
import com.hbm.inventory.fluid.NTMFluidPropertyCodec;
import com.hbm.lib.Library;
import com.hbm.packet.PacketWire;
import com.hbm.packet.threading.ThreadedPayload;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.NotNull;

public final class FluidPropertySyncPayload extends ThreadedPayload {

    public static final Type<FluidPropertySyncPayload> TYPE =
            new Type<>(Library.id("fluid_property_sync"));
    public static final StreamCodec<ByteBuf, FluidPropertySyncPayload> STREAM_CODEC =
            streamCodec(FluidPropertySyncPayload::decode);

    private static final StreamCodec<ByteBuf, NTMFluidProperty> PROPERTY =
            ByteBufCodecs.fromCodec(NTMFluidPropertyCodec.CODEC);

    private final Map<Fluid, NTMFluidProperty> properties;

    private FluidPropertySyncPayload(Map<Fluid, NTMFluidProperty> properties) {
        this.properties = properties;
    }

    public static FluidPropertySyncPayload of() {
        return new FluidPropertySyncPayload(NTMFluidProperties.snapshot());
    }

    private static boolean sharesTheServerMap(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        return server != null && server.isSingleplayerOwner(player.nameAndId());
    }

    public static void resendIfPending(MinecraftServer server) {
        if (!NTMFluidProperties.takeResyncPending()) return;
        List<ServerPlayer> remote = new ArrayList<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!sharesTheServerMap(player)) remote.add(player);
        }
        if (!remote.isEmpty()) PacketWire.sendToPlayers(of(), remote);
    }

    private static FluidPropertySyncPayload decode(ByteBuf buf) {
        int count = buf.readInt();
        Map<Fluid, NTMFluidProperty> properties = new Reference2ObjectOpenHashMap<>(count);
        for (int i = 0; i < count; i++) {
            Fluid fluid = BuiltInRegistries.FLUID.byId(buf.readInt());
            NTMFluidProperty property = PROPERTY.decode(buf);
            if (fluid != null && fluid != Fluids.EMPTY) properties.put(fluid, property);
        }
        return new FluidPropertySyncPayload(properties);
    }

    public static void apply(FluidPropertySyncPayload payload) {
        NTMFluidProperties.acceptSync(payload.properties);
    }

    public Map<Fluid, NTMFluidProperty> properties() {
        return properties;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(properties.size());
        for (Map.Entry<Fluid, NTMFluidProperty> entry : properties.entrySet()) {
            buf.writeInt(BuiltInRegistries.FLUID.getId(entry.getKey()));
            PROPERTY.encode(buf, entry.getValue());
        }
    }

    @Override
    public @NotNull Type<FluidPropertySyncPayload> type() {
        return TYPE;
    }
}
