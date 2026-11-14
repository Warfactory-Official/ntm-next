/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.blockentity.machine.rbmk;

import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.packet.SyncBindings;
import com.hbm.packet.SyncSource;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

public abstract sealed class ColumnSnapshot implements SyncSource
        permits ColumnSnapshot.Plain,
                ColumnSnapshot.Fuel,
                ColumnSnapshot.Boiler,
                ColumnSnapshot.Control,
                ColumnSnapshot.Heater {

    private static final RBMKColumnType[] TYPES = RBMKColumnType.values();
    private static final int EMPTY = -1;
    private static final String CELSIUS = "°C";

    private final RBMKColumnType type;
    public double heat;
    public double heatCeiling;
    public boolean moderated;
    public int reasimWater;
    public int reasimSteam;
    public int craneHighlight;

    private ColumnSnapshot(RBMKColumnType type) {
        this.type = type;
    }

    public static ColumnSnapshot create(RBMKColumnType type) {
        return switch (type) {
            case FUEL, FUEL_SIM, BREEDER -> new Fuel(type);
            case BOILER -> new Boiler(type);
            case CONTROL, CONTROL_AUTO -> new Control(type);
            case HEATEX -> new Heater(type);
            case BLANK, MODERATOR, ABSORBER, REFLECTOR, OUTGASSER, STORAGE, COOLER ->
                    new Plain(type);
        };
    }

    public static ColumnSnapshot reuse(@Nullable ColumnSnapshot previous, RBMKColumnType type) {
        return previous != null && previous.type == type ? previous : create(type);
    }

    public final RBMKColumnType type() {
        return type;
    }

    public static int fluidId(@Nullable Fluid fluid) {
        return fluid == null ? EMPTY : BuiltInRegistries.FLUID.getId(fluid);
    }

    protected static String fluidName(int id) {
        return id == EMPTY ? "" : NTMFluidProperties.clientName(BuiltInRegistries.FLUID.byId(id));
    }

    public static void write(ByteBuf output, @Nullable ColumnSnapshot snapshot) {
        if (snapshot == null) {
            output.writeByte(EMPTY);
            return;
        }
        output.writeByte(snapshot.type.ordinal());
        snapshot.serialize(output);
    }

    public static @Nullable ColumnSnapshot read(ByteBuf input) {
        int ordinal = input.readByte();
        if (ordinal == EMPTY) return null;
        if (ordinal < 0 || ordinal >= TYPES.length)
            throw new DecoderException("Invalid RBMK column type " + ordinal);
        ColumnSnapshot snapshot = create(TYPES[ordinal]);
        snapshot.deserialize(input);
        return snapshot;
    }

    protected void serialize(ByteBuf output) {
        output.writeDouble(heat);
        output.writeDouble(heatCeiling);
        output.writeBoolean(moderated);
        output.writeInt(reasimWater);
        output.writeInt(reasimSteam);
        output.writeByte(craneHighlight);
    }

    protected void deserialize(ByteBuf input) {
        hbm$syncSet$ColumnSnapshot$heat(input.readDouble());
        hbm$syncSet$ColumnSnapshot$heatCeiling(input.readDouble());
        hbm$syncSet$ColumnSnapshot$moderated(input.readBoolean());
        hbm$syncSet$ColumnSnapshot$reasimWater(input.readInt());
        hbm$syncSet$ColumnSnapshot$reasimSteam(input.readInt());
        hbm$syncSet$ColumnSnapshot$craneHighlight(input.readByte());
    }

    public final List<Component> tooltip() {
        var lines = new ArrayList<Component>();
        lines.add(
                Component.translatable("rbmk.heat", tenths(heat) + CELSIUS)
                        .withStyle(ChatFormatting.YELLOW));
        kindLines(lines);
        if (moderated)
            lines.add(Component.translatable("rbmk.moderated").withStyle(ChatFormatting.YELLOW));
        return lines;
    }

    protected void kindLines(List<Component> lines) {}

    private static double tenths(double value) {
        return (int) (value * 10) / 10D;
    }

    private static double thousandths(double value) {
        return (int) (value * 1000) / 1000D;
    }

    public static final class Plain extends ColumnSnapshot {
        public Plain(RBMKColumnType type) {
            super(type);
        }
    }

    public static final class Fuel extends ColumnSnapshot {
        public double enrichment;
        public double xenon;
        public double coreHeat;
        public double hullHeat;
        public double hullMeltingPoint;

        public Fuel(RBMKColumnType type) {
            super(type);
        }

        @Override
        protected void serialize(ByteBuf output) {
            super.serialize(output);
            output.writeDouble(enrichment);
            output.writeDouble(xenon);
            output.writeDouble(coreHeat);
            output.writeDouble(hullHeat);
            output.writeDouble(hullMeltingPoint);
        }

        @Override
        protected void deserialize(ByteBuf input) {
            super.deserialize(input);
            hbm$syncSet$Fuel$enrichment(input.readDouble());
            hbm$syncSet$Fuel$xenon(input.readDouble());
            hbm$syncSet$Fuel$coreHeat(input.readDouble());
            hbm$syncSet$Fuel$hullHeat(input.readDouble());
            hbm$syncSet$Fuel$hullMeltingPoint(input.readDouble());
        }

        @Override
        protected void kindLines(List<Component> lines) {
            lines.add(
                    Component.translatable(
                                    "rbmk.rod.depletion",
                                    (int) ((1 - enrichment) * 100000) / 1000D + "%")
                            .withStyle(ChatFormatting.GREEN));
            lines.add(
                    Component.translatable("rbmk.rod.xenon", thousandths(xenon) + "%")
                            .withStyle(ChatFormatting.DARK_PURPLE));
            lines.add(
                    Component.translatable("rbmk.rod.coreTemp", tenths(coreHeat) + CELSIUS)
                            .withStyle(ChatFormatting.DARK_RED));
            lines.add(
                    Component.translatable(
                                    "rbmk.rod.skinTemp",
                                    tenths(hullHeat) + CELSIUS,
                                    tenths(hullMeltingPoint) + CELSIUS)
                            .withStyle(ChatFormatting.RED));
        }

        public final void hbm$syncSet$Fuel$enrichment(double next) {
            if (Double.doubleToRawLongBits(this.enrichment) != Double.doubleToRawLongBits(next)) {
                this.enrichment = next;
                if ((0L) == 0L) syncChanged(3);
                else syncUnitsChanged(3, 0L);
            }
        }

        public final void hbm$syncSet$Fuel$xenon(double next) {
            if (Double.doubleToRawLongBits(this.xenon) != Double.doubleToRawLongBits(next)) {
                this.xenon = next;
                if ((0L) == 0L) syncChanged(3);
                else syncUnitsChanged(3, 0L);
            }
        }

        public final void hbm$syncSet$Fuel$coreHeat(double next) {
            if (Double.doubleToRawLongBits(this.coreHeat) != Double.doubleToRawLongBits(next)) {
                this.coreHeat = next;
                if ((0L) == 0L) syncChanged(3);
                else syncUnitsChanged(3, 0L);
            }
        }

        public final void hbm$syncSet$Fuel$hullHeat(double next) {
            if (Double.doubleToRawLongBits(this.hullHeat) != Double.doubleToRawLongBits(next)) {
                this.hullHeat = next;
                if ((0L) == 0L) syncChanged(3);
                else syncUnitsChanged(3, 0L);
            }
        }

        public final void hbm$syncSet$Fuel$hullMeltingPoint(double next) {
            if (Double.doubleToRawLongBits(this.hullMeltingPoint)
                    != Double.doubleToRawLongBits(next)) {
                this.hullMeltingPoint = next;
                if ((0L) == 0L) syncChanged(3);
                else syncUnitsChanged(3, 0L);
            }
        }
    }

    public static final class Boiler extends ColumnSnapshot {
        public int waterFill;
        public int waterCapacity;
        public int steamFill;
        public int steamCapacity;
        public int steamFluid;

        {
            hbm$syncSet$Boiler$steamFluid(EMPTY);
        }

        public Boiler(RBMKColumnType type) {
            super(type);
        }

        @Override
        protected void serialize(ByteBuf output) {
            super.serialize(output);
            output.writeInt(waterFill);
            output.writeInt(waterCapacity);
            output.writeInt(steamFill);
            output.writeInt(steamCapacity);
            output.writeInt(steamFluid);
        }

        @Override
        protected void deserialize(ByteBuf input) {
            super.deserialize(input);
            hbm$syncSet$Boiler$waterFill(input.readInt());
            hbm$syncSet$Boiler$waterCapacity(input.readInt());
            hbm$syncSet$Boiler$steamFill(input.readInt());
            hbm$syncSet$Boiler$steamCapacity(input.readInt());
            hbm$syncSet$Boiler$steamFluid(input.readInt());
        }

        @Override
        protected void kindLines(List<Component> lines) {
            lines.add(
                    Component.translatable("rbmk.boiler.water", waterFill, waterCapacity)
                            .withStyle(ChatFormatting.BLUE));
            lines.add(
                    Component.translatable("rbmk.boiler.steam", steamFill, steamCapacity)
                            .withStyle(ChatFormatting.WHITE));
            lines.add(
                    Component.translatable("rbmk.boiler.type", fluidName(steamFluid))
                            .withStyle(ChatFormatting.YELLOW));
        }

        public final void hbm$syncSet$Boiler$waterFill(int next) {
            if (this.waterFill != next) {
                this.waterFill = next;
                if ((0L) == 0L) syncChanged(3);
                else syncUnitsChanged(3, 0L);
            }
        }

        public final void hbm$syncSet$Boiler$waterCapacity(int next) {
            if (this.waterCapacity != next) {
                this.waterCapacity = next;
                if ((0L) == 0L) syncChanged(3);
                else syncUnitsChanged(3, 0L);
            }
        }

        public final void hbm$syncSet$Boiler$steamFill(int next) {
            if (this.steamFill != next) {
                this.steamFill = next;
                if ((0L) == 0L) syncChanged(3);
                else syncUnitsChanged(3, 0L);
            }
        }

        public final void hbm$syncSet$Boiler$steamCapacity(int next) {
            if (this.steamCapacity != next) {
                this.steamCapacity = next;
                if ((0L) == 0L) syncChanged(3);
                else syncUnitsChanged(3, 0L);
            }
        }

        public final void hbm$syncSet$Boiler$steamFluid(int next) {
            if (this.steamFluid != next) {
                this.steamFluid = next;
                if ((0L) == 0L) syncChanged(3);
                else syncUnitsChanged(3, 0L);
            }
        }
    }

    public static final class Control extends ColumnSnapshot {
        public double extraction;
        public short dye;

        {
            hbm$syncSet$Control$dye((short) (EMPTY));
        }

        public Control(RBMKColumnType type) {
            super(type);
        }

        @Override
        protected void serialize(ByteBuf output) {
            super.serialize(output);
            output.writeDouble(extraction);
            output.writeShort(dye);
        }

        @Override
        protected void deserialize(ByteBuf input) {
            super.deserialize(input);
            hbm$syncSet$Control$extraction(input.readDouble());
            short group = input.readShort();
            if (group < EMPTY || group >= RodGroup.VALUES.length) {
                throw new DecoderException("Invalid RBMK rod group " + group);
            }
            hbm$syncSet$Control$dye(group);
        }

        @Override
        protected void kindLines(List<Component> lines) {
            if (dye >= 0)
                lines.add(
                        Component.translatable(RodGroup.VALUES[dye].langKey())
                                .withStyle(ChatFormatting.YELLOW));
            lines.add(
                    Component.translatable("rbmk.control.level", (int) (extraction * 100) + "%")
                            .withStyle(ChatFormatting.YELLOW));
        }

        public final void hbm$syncSet$Control$extraction(double next) {
            if (Double.doubleToRawLongBits(this.extraction) != Double.doubleToRawLongBits(next)) {
                this.extraction = next;
                if ((0L) == 0L) syncChanged(3);
                else syncUnitsChanged(3, 0L);
            }
        }

        public final void hbm$syncSet$Control$dye(short next) {
            if (this.dye != next) {
                this.dye = next;
                if ((0L) == 0L) syncChanged(3);
                else syncUnitsChanged(3, 0L);
            }
        }
    }

    public static final class Heater extends ColumnSnapshot {
        public int feedFill;
        public int feedCapacity;
        public int productFill;
        public int productCapacity;
        public int feedFluid;

        {
            hbm$syncSet$Heater$feedFluid(EMPTY);
        }

        public int productFluid;

        {
            hbm$syncSet$Heater$productFluid(EMPTY);
        }

        public Heater(RBMKColumnType type) {
            super(type);
        }

        @Override
        protected void serialize(ByteBuf output) {
            super.serialize(output);
            output.writeInt(feedFill);
            output.writeInt(feedCapacity);
            output.writeInt(productFill);
            output.writeInt(productCapacity);
            output.writeInt(feedFluid);
            output.writeInt(productFluid);
        }

        @Override
        protected void deserialize(ByteBuf input) {
            super.deserialize(input);
            hbm$syncSet$Heater$feedFill(input.readInt());
            hbm$syncSet$Heater$feedCapacity(input.readInt());
            hbm$syncSet$Heater$productFill(input.readInt());
            hbm$syncSet$Heater$productCapacity(input.readInt());
            hbm$syncSet$Heater$feedFluid(input.readInt());
            hbm$syncSet$Heater$productFluid(input.readInt());
        }

        @Override
        protected void kindLines(List<Component> lines) {
            lines.add(
                    Component.literal(
                                    fluidName(feedFluid)
                                            + " "
                                            + feedFill
                                            + "/"
                                            + feedCapacity
                                            + "mB")
                            .withStyle(ChatFormatting.BLUE));
            lines.add(
                    Component.literal(
                                    fluidName(productFluid)
                                            + " "
                                            + productFill
                                            + "/"
                                            + productCapacity
                                            + "mB")
                            .withStyle(ChatFormatting.RED));
        }

        public final void hbm$syncSet$Heater$feedFill(int next) {
            if (this.feedFill != next) {
                this.feedFill = next;
                if ((0L) == 0L) syncChanged(3);
                else syncUnitsChanged(3, 0L);
            }
        }

        public final void hbm$syncSet$Heater$feedCapacity(int next) {
            if (this.feedCapacity != next) {
                this.feedCapacity = next;
                if ((0L) == 0L) syncChanged(3);
                else syncUnitsChanged(3, 0L);
            }
        }

        public final void hbm$syncSet$Heater$productFill(int next) {
            if (this.productFill != next) {
                this.productFill = next;
                if ((0L) == 0L) syncChanged(3);
                else syncUnitsChanged(3, 0L);
            }
        }

        public final void hbm$syncSet$Heater$productCapacity(int next) {
            if (this.productCapacity != next) {
                this.productCapacity = next;
                if ((0L) == 0L) syncChanged(3);
                else syncUnitsChanged(3, 0L);
            }
        }

        public final void hbm$syncSet$Heater$feedFluid(int next) {
            if (this.feedFluid != next) {
                this.feedFluid = next;
                if ((0L) == 0L) syncChanged(3);
                else syncUnitsChanged(3, 0L);
            }
        }

        public final void hbm$syncSet$Heater$productFluid(int next) {
            if (this.productFluid != next) {
                this.productFluid = next;
                if ((0L) == 0L) syncChanged(3);
                else syncUnitsChanged(3, 0L);
            }
        }
    }

    public final void hbm$syncSet$ColumnSnapshot$heat(double next) {
        if (Double.doubleToRawLongBits(this.heat) != Double.doubleToRawLongBits(next)) {
            this.heat = next;
            if ((0L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 0L);
        }
    }

    public final void hbm$syncSet$ColumnSnapshot$heatCeiling(double next) {
        if (Double.doubleToRawLongBits(this.heatCeiling) != Double.doubleToRawLongBits(next)) {
            this.heatCeiling = next;
            if ((0L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 0L);
        }
    }

    public final void hbm$syncSet$ColumnSnapshot$moderated(boolean next) {
        if (this.moderated != next) {
            this.moderated = next;
            if ((0L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 0L);
        }
    }

    public final void hbm$syncSet$ColumnSnapshot$reasimWater(int next) {
        if (this.reasimWater != next) {
            this.reasimWater = next;
            if ((0L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 0L);
        }
    }

    public final void hbm$syncSet$ColumnSnapshot$reasimSteam(int next) {
        if (this.reasimSteam != next) {
            this.reasimSteam = next;
            if ((0L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 0L);
        }
    }

    public final void hbm$syncSet$ColumnSnapshot$craneHighlight(int next) {
        if (this.craneHighlight != next) {
            this.craneHighlight = next;
            if ((0L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 0L);
        }
    }

    private SyncSource hbm$syncOwner;

    private int hbm$syncMask;

    private int hbm$syncBindings;

    private long hbm$syncUnits;

    public final boolean syncBound() {
        return hbm$syncOwner != null;
    }

    public final void syncChanged(int mask) {
        if (hbm$syncOwner == null) return;
        if (hbm$syncUnits == 0) hbm$syncOwner.syncChanged(hbm$syncMask);
        else hbm$syncOwner.syncUnitsChanged(hbm$syncMask, hbm$syncUnits);
    }

    public final void syncUnitsChanged(int mask, long units) {
        syncChanged(mask);
    }

    public final void bindSync(SyncSource owner, int mask) {
        bindSyncUnits(owner, mask, 0);
    }

    public final void bindSyncUnits(SyncSource owner, int mask, long units) {
        if (hbm$syncOwner != null && hbm$syncOwner != owner) {
            throw new IllegalStateException("Mutable sync state has two owners");
        }
        hbm$syncOwner = owner;
        hbm$syncMask |= mask;
        hbm$syncUnits |= units;
        if (hbm$syncBindings++ == 0) SyncBindings.bindFields(this, owner);
    }

    public final void unbindSync(SyncSource owner) {
        assert hbm$syncOwner == owner;
        if (--hbm$syncBindings != 0) return;
        SyncBindings.unbindFields(this, owner);
        hbm$syncOwner = null;
        hbm$syncMask = 0;
        hbm$syncUnits = 0;
    }
}
