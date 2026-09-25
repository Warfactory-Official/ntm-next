// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.api.control.IControlReceiver;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.handler.MissileStruct;
import com.hbm.inventory.IGUIProvider;
import com.hbm.inventory.container.MenuMachineMissileAssembly;
import com.hbm.items.weapon.ItemCustomMissile;
import com.hbm.items.weapon.ItemCustomMissilePart.PartType;
import com.hbm.items.weapon.ItemCustomMissilePart;
import com.hbm.packet.SyncSlots;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.BlockEntityMachineBase;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

@SyncSlots(
        value = {
            BlockEntityMachineMissileAssembly.SLOT_WARHEAD,
            BlockEntityMachineMissileAssembly.SLOT_FUSELAGE,
            BlockEntityMachineMissileAssembly.SLOT_FINS,
            BlockEntityMachineMissileAssembly.SLOT_THRUSTER
        },
        units = 1L << 0)
public class BlockEntityMachineMissileAssembly extends BlockEntityMachineBase
        implements IGUIProvider, IControlReceiver, SyncUnitSchema {

    public static final int SLOT_CHIP = 0;
    public static final int SLOT_WARHEAD = 1;
    public static final int SLOT_FUSELAGE = 2;
    public static final int SLOT_FINS = 3;
    public static final int SLOT_THRUSTER = 4;
    public static final int SLOT_OUTPUT = 5;
    public static final int SLOT_COUNT = 6;

    private static final int[] ACCESSIBLE_SLOTS = {SLOT_CHIP};

    public MissileStruct loadedMissile = MissileStruct.EMPTY;

    public BlockEntityMachineMissileAssembly(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MISSILE_ASSEMBLY.get(), pos, state, SLOT_COUNT);
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (level != null && !level.isClientSide()) networkPackNTTracking();
    }

    private void writeMissile(ByteBuf output) {
        MissileStruct.STREAM_CODEC.encode(
                registryBuf(output),
                new MissileStruct(
                        getItem(SLOT_WARHEAD),
                        getItem(SLOT_FUSELAGE),
                        getItem(SLOT_FINS),
                        getItem(SLOT_THRUSTER)));
    }

    private void readMissile(ByteBuf input) {
        loadedMissile = MissileStruct.STREAM_CODEC.decode(registryBuf(input)).sanitised();
    }

    private RegistryFriendlyByteBuf registryBuf(ByteBuf buf) {
        return new RegistryFriendlyByteBuf(buf, level.registryAccess());
    }

    public int fuselageState() {
        ItemCustomMissilePart fuselage = ItemCustomMissilePart.part(getItem(SLOT_FUSELAGE));
        return fuselage != null && fuselage.type == PartType.FUSELAGE ? 1 : 0;
    }

    public int chipState() {
        ItemCustomMissilePart chip = ItemCustomMissilePart.part(getItem(SLOT_CHIP));
        return chip != null && chip.type == PartType.CHIP ? 1 : 0;
    }

    public int warheadState() {
        ItemCustomMissilePart warhead = ItemCustomMissilePart.part(getItem(SLOT_WARHEAD));
        ItemCustomMissilePart fuselage = ItemCustomMissilePart.part(getItem(SLOT_FUSELAGE));
        ItemCustomMissilePart thruster = ItemCustomMissilePart.part(getItem(SLOT_THRUSTER));
        if (warhead == null || fuselage == null || thruster == null) return 0;

        if (warhead.type != PartType.WARHEAD
                || fuselage.type != PartType.FUSELAGE
                || thruster.type != PartType.THRUSTER) {
            return 0;
        }
        float weight = (Float) warhead.attributes[2];
        float thrust = (Float) thruster.attributes[2];
        return warhead.bottom == fuselage.top && weight <= thrust ? 1 : 0;
    }

    public int stabilityState() {
        if (getItem(SLOT_FINS).isEmpty()) return -1;

        ItemCustomMissilePart fins = ItemCustomMissilePart.part(getItem(SLOT_FINS));
        ItemCustomMissilePart fuselage = ItemCustomMissilePart.part(getItem(SLOT_FUSELAGE));
        if (fins == null || fuselage == null) return 0;
        return fins.top == fuselage.bottom && fins.type == PartType.FINS ? 1 : 0;
    }

    public int thrusterState() {
        ItemCustomMissilePart thruster = ItemCustomMissilePart.part(getItem(SLOT_THRUSTER));
        ItemCustomMissilePart fuselage = ItemCustomMissilePart.part(getItem(SLOT_FUSELAGE));
        if (thruster == null || fuselage == null) return 0;

        return thruster.type == PartType.THRUSTER
                        && fuselage.type == PartType.FUSELAGE
                        && thruster.top == fuselage.bottom
                        && thruster.fuelType() == fuselage.fuelType()
                ? 1
                : 0;
    }

    public boolean canBuild() {
        if (!getItem(SLOT_OUTPUT).isEmpty()) return false;
        if (chipState() != 1 || warheadState() != 1 || fuselageState() != 1 || thrusterState() != 1)
            return false;
        return stabilityState() != 0;
    }

    public void construct() {
        if (!canBuild()) return;

        ItemStack missile =
                ItemCustomMissile.buildMissile(
                        getItem(SLOT_CHIP),
                        getItem(SLOT_WARHEAD),
                        getItem(SLOT_FUSELAGE),
                        getItem(SLOT_FINS),
                        getItem(SLOT_THRUSTER));

        if (stabilityState() == 1) setItem(SLOT_FINS, ItemStack.EMPTY);
        setItem(SLOT_CHIP, ItemStack.EMPTY);
        setItem(SLOT_WARHEAD, ItemStack.EMPTY);
        setItem(SLOT_FUSELAGE, ItemStack.EMPTY);
        setItem(SLOT_THRUSTER, ItemStack.EMPTY);
        setItem(SLOT_OUTPUT, missile);

        level.playSound(
                null, worldPosition, ModSounds.MISSILE_ASSEMBLY2.get(), SoundSource.BLOCKS, 1F, 1F);
    }

    @Override
    public boolean hasPermission(Player player) {
        return true;
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("build")) construct();
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ACCESSIBLE_SLOTS;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.missileAssembly");
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new MenuMachineMissileAssembly(containerId, playerInventory, this);
    }

    @Override
    public long syncUnitMask() {
        return 1L << 0;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> writeMissile(output);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> readMissile(input);
            default -> throw new IllegalArgumentException();
        }
    }
}
