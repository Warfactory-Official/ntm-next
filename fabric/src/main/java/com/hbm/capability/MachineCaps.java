// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.capability;

import com.hbm.api.fluidmk2.FluidFace;
import com.hbm.registration.RegistryHandle;
import java.util.function.BiPredicate;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.jspecify.annotations.Nullable;

public final class MachineCaps {

    private static final BiPredicate<BlockEntity, Direction> ALL = (be, side) -> true;
    private static final BiPredicate<BlockEntity, FluidFace> ALL_FLUID = (be, face) -> true;

    public static final MachineCaps NONE = new MachineCaps(null);
    public static final int POWER_IN = 1;
    public static final int POWER_OUT = 1 << 1;
    public static final int FLUID_IN = 1 << 2;
    public static final int FLUID_OUT = 1 << 3;
    public static final int ITEMS = 1 << 4;
    public static final int ITEMS_AT_CELLS = 1 << 5;
    public static final int FE = 1 << 6;
    final boolean powerIn, powerOut, fluidIn, fluidOut, items, itemsAtCells, fe;
    final boolean selfProvided;
    private final BiPredicate<BlockEntity, Direction> faces;
    private final BiPredicate<BlockEntity, FluidFace> fluidFaces;
    private final @Nullable RegistryHandle<? extends BlockEntityType<?>> beType;

    private MachineCaps(@Nullable RegistryHandle<? extends BlockEntityType<?>> beType) {
        this(beType, false, false, false, false, false, false, false, false, ALL, ALL_FLUID);
    }

    private MachineCaps(
            @Nullable RegistryHandle<? extends BlockEntityType<?>> beType,
            boolean powerIn,
            boolean powerOut,
            boolean fluidIn,
            boolean fluidOut,
            boolean items,
            boolean itemsAtCells,
            boolean fe,
            boolean selfProvided,
            BiPredicate<BlockEntity, Direction> faces,
            BiPredicate<BlockEntity, FluidFace> fluidFaces) {
        this.beType = beType;
        this.powerIn = powerIn;
        this.powerOut = powerOut;
        this.fluidIn = fluidIn;
        this.fluidOut = fluidOut;
        this.items = items;
        this.itemsAtCells = itemsAtCells;
        this.fe = fe;
        this.selfProvided = selfProvided;
        this.faces = faces;
        this.fluidFaces = fluidFaces;
    }

    public static MachineCaps of(RegistryHandle<? extends BlockEntityType<?>> beType) {
        return new MachineCaps(beType);
    }

    public static MachineCaps blockKeyed() {
        return new MachineCaps(null);
    }

    public MachineCaps powerIn() {
        return new MachineCaps(
                beType,
                true,
                powerOut,
                fluidIn,
                fluidOut,
                items,
                itemsAtCells,
                fe,
                selfProvided,
                faces,
                fluidFaces);
    }

    public MachineCaps powerOut() {
        return new MachineCaps(
                beType,
                powerIn,
                true,
                fluidIn,
                fluidOut,
                items,
                itemsAtCells,
                fe,
                selfProvided,
                faces,
                fluidFaces);
    }

    public MachineCaps fluidIn() {
        return new MachineCaps(
                beType,
                powerIn,
                powerOut,
                true,
                fluidOut,
                items,
                itemsAtCells,
                fe,
                selfProvided,
                faces,
                fluidFaces);
    }

    public MachineCaps fluidOut() {
        return new MachineCaps(
                beType,
                powerIn,
                powerOut,
                fluidIn,
                true,
                items,
                itemsAtCells,
                fe,
                selfProvided,
                faces,
                fluidFaces);
    }

    public MachineCaps items() {
        return new MachineCaps(
                beType,
                powerIn,
                powerOut,
                fluidIn,
                fluidOut,
                true,
                itemsAtCells,
                fe,
                selfProvided,
                faces,
                fluidFaces);
    }

    public MachineCaps itemsAtCells() {
        return new MachineCaps(
                beType,
                powerIn,
                powerOut,
                fluidIn,
                fluidOut,
                items,
                true,
                fe,
                selfProvided,
                faces,
                fluidFaces);
    }

    public MachineCaps fe() {
        return new MachineCaps(
                beType,
                powerIn,
                powerOut,
                fluidIn,
                fluidOut,
                items,
                itemsAtCells,
                true,
                selfProvided,
                faces,
                fluidFaces);
    }

    public MachineCaps selfProvided() {
        return new MachineCaps(
                beType,
                powerIn,
                powerOut,
                fluidIn,
                fluidOut,
                items,
                itemsAtCells,
                fe,
                true,
                faces,
                fluidFaces);
    }

    public <BE extends BlockEntity> MachineCaps faces(
            Class<BE> type, BiPredicate<BE, Direction> gate) {
        return new MachineCaps(
                beType,
                powerIn,
                powerOut,
                fluidIn,
                fluidOut,
                items,
                itemsAtCells,
                fe,
                selfProvided,
                (be, side) -> gate.test(type.cast(be), side),
                fluidFaces);
    }

    public <BE extends BlockEntity> MachineCaps fluidFaces(
            Class<BE> type, BiPredicate<BE, FluidFace> gate) {
        return new MachineCaps(
                beType,
                powerIn,
                powerOut,
                fluidIn,
                fluidOut,
                items,
                itemsAtCells,
                fe,
                selfProvided,
                faces,
                (be, face) -> gate.test(type.cast(be), face));
    }

    BiPredicate<BlockEntity, Direction> faceGate() {
        return faces;
    }

    BiPredicate<BlockEntity, FluidFace> fluidGate() {
        return fluidFaces;
    }

    public static boolean acceptsFace(BlockEntity owner, Direction side) {
        return !(owner.getBlockState().getBlock() instanceof ICapabilityBlock declaring)
                || declaring.caps().faces.test(owner, side);
    }

    public static int declaredDomains(@Nullable BlockEntity owner) {
        if (owner == null) return 0;
        return owner.getBlockState().getBlock() instanceof ICapabilityBlock declaring
                ? declaring.caps().declaredBits()
                : 0;
    }

    public int declaredBits() {
        return (powerIn ? POWER_IN : 0)
                | (powerOut ? POWER_OUT : 0)
                | (fluidIn ? FLUID_IN : 0)
                | (fluidOut ? FLUID_OUT : 0)
                | (items ? ITEMS : 0)
                | (itemsAtCells ? ITEMS_AT_CELLS : 0)
                | (fe ? FE : 0);
    }

    RegistryHandle<? extends BlockEntityType<?>> beType() {
        if (beType == null) {
            throw new IllegalStateException(
                    "a cap keyed on the block entity type was declared without one");
        }
        return beType;
    }

    @Nullable RegistryHandle<? extends BlockEntityType<?>> beTypeOrNull() {
        return beType;
    }
}
