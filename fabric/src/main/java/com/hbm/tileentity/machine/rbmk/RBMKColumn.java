// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine.rbmk;

import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncSource;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.material.Fluid;

public abstract class RBMKColumn implements SyncSource {

    public final RBMKColumnType type;
    @SyncField public double heat;
    @SyncField public double maxHeat;
    @SyncField public boolean moderated;
    @SyncField public int reasimWater;
    @SyncField public int reasimSteam;
    @SyncField public int indicator;

    protected RBMKColumn(RBMKColumnType type) {
        this.type = type;
    }

    protected static Component fluidLine(ChatFormatting color, int fluidId, int fill, int max) {
        return Component.literal(fluidName(fluidId) + " " + fill + "/" + max + "mB")
                .withStyle(color);
    }

    protected static String fluidName(int fluidId) {
        if (fluidId < 0) return "";
        Fluid fluid = BuiltInRegistries.FLUID.byId(fluidId);
        return NTMFluidProperties.clientName(fluid);
    }

    public static int fluidId(Fluid fluid) {
        return fluid == null ? -1 : BuiltInRegistries.FLUID.getId(fluid);
    }

    public static RBMKColumn readFromBuf(ByteBuf buf) {
        byte ordinal = buf.readByte();
        if (ordinal == -1) return null;
        RBMKColumn column = createForType(RBMKColumnType.values()[ordinal]);
        column.deserialize(buf);
        return column;
    }

    public static void writeToBuf(ByteBuf buf, RBMKColumn column) {
        if (column == null) {
            buf.writeByte(-1);
        } else {
            buf.writeByte((byte) column.type.ordinal());
            column.serialize(buf);
        }
    }

    public static RBMKColumn createForType(RBMKColumnType type) {
        return switch (type) {
            case FUEL, FUEL_SIM, BREEDER -> new FuelColumn(type);
            case BOILER -> new BoilerColumn();
            case CONTROL, CONTROL_AUTO -> new ControlColumn(type);
            case HEATEX -> new HeaterColumn();
            default -> new StandardColumn(type);
        };
    }

    protected void serialize(ByteBuf buf) {
        buf.writeDouble(heat);
        buf.writeDouble(maxHeat);
        buf.writeBoolean(moderated);
        buf.writeInt(reasimWater);
        buf.writeInt(reasimSteam);
        buf.writeByte(indicator);
    }

    protected void deserialize(ByteBuf buf) {
        heat = buf.readDouble();
        maxHeat = buf.readDouble();
        moderated = buf.readBoolean();
        reasimWater = buf.readInt();
        reasimSteam = buf.readInt();
        indicator = buf.readByte();
    }

    public final List<Component> getFancyStats() {
        List<Component> stats = new ArrayList<>();
        stats.add(
                Component.translatable("rbmk.heat", ((int) (heat * 10D)) / 10D + "°C")
                        .withStyle(ChatFormatting.YELLOW));
        typeStats(stats);
        if (moderated)
            stats.add(Component.translatable("rbmk.moderated").withStyle(ChatFormatting.YELLOW));
        return stats;
    }

    protected void typeStats(List<Component> stats) {}

    public static class StandardColumn extends RBMKColumn {
        public StandardColumn(RBMKColumnType type) {
            super(type);
        }
    }

    public static class FuelColumn extends RBMKColumn {
        @SyncField public double enrichment;
        @SyncField public double xenon;
        @SyncField public double c_coreHeat;
        @SyncField public double c_heat;
        @SyncField public double c_maxHeat;

        public FuelColumn(RBMKColumnType type) {
            super(type);
        }

        @Override
        protected void serialize(ByteBuf buf) {
            super.serialize(buf);
            buf.writeDouble(enrichment);
            buf.writeDouble(xenon);
            buf.writeDouble(c_coreHeat);
            buf.writeDouble(c_heat);
            buf.writeDouble(c_maxHeat);
        }

        @Override
        protected void deserialize(ByteBuf buf) {
            super.deserialize(buf);
            enrichment = buf.readDouble();
            xenon = buf.readDouble();
            c_coreHeat = buf.readDouble();
            c_heat = buf.readDouble();
            c_maxHeat = buf.readDouble();
        }

        @Override
        protected void typeStats(List<Component> stats) {
            stats.add(
                    Component.translatable(
                                    "rbmk.rod.depletion",
                                    ((int) (((1D - enrichment) * 100000)) / 1000D) + "%")
                            .withStyle(ChatFormatting.GREEN));
            stats.add(
                    Component.translatable(
                                    "rbmk.rod.xenon", ((int) ((xenon * 1000D)) / 1000D) + "%")
                            .withStyle(ChatFormatting.DARK_PURPLE));
            stats.add(
                    Component.translatable(
                                    "rbmk.rod.coreTemp", ((int) (c_coreHeat * 10D)) / 10D + "°C")
                            .withStyle(ChatFormatting.DARK_RED));
            stats.add(
                    Component.translatable(
                                    "rbmk.rod.skinTemp",
                                    ((int) (c_heat * 10D)) / 10D + "°C",
                                    ((int) (c_maxHeat * 10D)) / 10D + "°C")
                            .withStyle(ChatFormatting.RED));
        }
    }

    public static class BoilerColumn extends RBMKColumn {
        @SyncField public int water;
        @SyncField public int maxWater;
        @SyncField public int steam;
        @SyncField public int maxSteam;
        @SyncField public int steamType = -1;

        public BoilerColumn() {
            super(RBMKColumnType.BOILER);
        }

        @Override
        protected void serialize(ByteBuf buf) {
            super.serialize(buf);
            buf.writeInt(water);
            buf.writeInt(maxWater);
            buf.writeInt(steam);
            buf.writeInt(maxSteam);
            buf.writeInt(steamType);
        }

        @Override
        protected void deserialize(ByteBuf buf) {
            super.deserialize(buf);
            water = buf.readInt();
            maxWater = buf.readInt();
            steam = buf.readInt();
            maxSteam = buf.readInt();
            steamType = buf.readInt();
        }

        @Override
        protected void typeStats(List<Component> stats) {
            stats.add(
                    Component.translatable("rbmk.boiler.water", water, maxWater)
                            .withStyle(ChatFormatting.BLUE));
            stats.add(
                    Component.translatable("rbmk.boiler.steam", steam, maxSteam)
                            .withStyle(ChatFormatting.WHITE));
            stats.add(
                    Component.translatable("rbmk.boiler.type", fluidName(steamType))
                            .withStyle(ChatFormatting.YELLOW));
        }
    }

    public static class ControlColumn extends RBMKColumn {
        @SyncField public double level;
        @SyncField public short color = -1;

        public ControlColumn(RBMKColumnType type) {
            super(type);
        }

        @Override
        protected void serialize(ByteBuf buf) {
            super.serialize(buf);
            buf.writeDouble(level);
            buf.writeShort(color);
        }

        @Override
        protected void deserialize(ByteBuf buf) {
            super.deserialize(buf);
            level = buf.readDouble();
            color = buf.readShort();
        }

        @Override
        protected void typeStats(List<Component> stats) {
            if (color >= 0 && color < RBMKColor.VALUES.length) {
                stats.add(
                        Component.translatable(
                                        "rbmk.control."
                                                + RBMKColor.VALUES[color]
                                                        .name()
                                                        .toLowerCase(Locale.US))
                                .withStyle(ChatFormatting.YELLOW));
            }
            stats.add(
                    Component.translatable("rbmk.control.level", ((int) (level * 100D)) + "%")
                            .withStyle(ChatFormatting.YELLOW));
        }
    }

    public static class HeaterColumn extends RBMKColumn {
        @SyncField public int water;
        @SyncField public int maxWater;
        @SyncField public int steam;
        @SyncField public int maxSteam;
        @SyncField public int coldType = -1;
        @SyncField public int hotType = -1;

        public HeaterColumn() {
            super(RBMKColumnType.HEATEX);
        }

        @Override
        protected void serialize(ByteBuf buf) {
            super.serialize(buf);
            buf.writeInt(water);
            buf.writeInt(maxWater);
            buf.writeInt(steam);
            buf.writeInt(maxSteam);
            buf.writeInt(coldType);
            buf.writeInt(hotType);
        }

        @Override
        protected void deserialize(ByteBuf buf) {
            super.deserialize(buf);
            water = buf.readInt();
            maxWater = buf.readInt();
            steam = buf.readInt();
            maxSteam = buf.readInt();
            coldType = buf.readInt();
            hotType = buf.readInt();
        }

        @Override
        protected void typeStats(List<Component> stats) {
            stats.add(fluidLine(ChatFormatting.BLUE, coldType, water, maxWater));
            stats.add(fluidLine(ChatFormatting.RED, hotType, steam, maxSteam));
        }
    }
}
