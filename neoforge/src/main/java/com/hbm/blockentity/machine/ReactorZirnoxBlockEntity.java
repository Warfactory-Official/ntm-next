/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) Contributors to Hbm's Nuclear Tech Mod. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only,
 * as published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this code. If not, see <https://www.gnu.org/licenses/>.
 *
 * Under GPLv3 §7(c) and §7(e), these additional terms apply to LGPL-covered
 * material authored by movblock:
 *
 * - the license grants no rights to the name "NTM NEXT";
 * - a modified version must not be named NTM NEXT or presented as the
 *   official release, and must be marked as different from it.
 *
 * Accurately identifying a work as derived from NTM NEXT does not violate
 * these additional terms, provided it is clearly distinguished from the
 * official release and does not imply endorsement. Ports are encouraged to
 * acknowledge NTM NEXT as their upstream project and provide a link to it.
 */

package com.hbm.blockentity.machine;

import com.hbm.advancement.AwardRegions;
import com.hbm.advancement.DetonationTrigger;
import com.hbm.advancement.HbmCriteria;
import com.hbm.api.environment.Environment;
import com.hbm.api.fluidmk2.FluidFlushOutputs;
import com.hbm.api.fluidmk2.FluidTankHandler;
import com.hbm.api.fluidmk2.FlushFaces;
import com.hbm.api.fluidmk2.FlushLanes;
import com.hbm.api.redstoneoverradio.RadioControlHandler;
import com.hbm.api.redstoneoverradio.RadioValueProvider;
import com.hbm.blockentity.MachineBlockEntity;
import com.hbm.blockentity.Tiltable;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.machine.DestroyedZirnoxBlock;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.config.BalanceConfig;
import com.hbm.data.MachineData;
import com.hbm.entity.projectile.ZirnoxDebrisEntity.DebrisType;
import com.hbm.entity.projectile.ZirnoxDebrisEntity;
import com.hbm.explosion.BlockConversionTable;
import com.hbm.handler.BossSpawnHandler;
import com.hbm.inventory.container.ReactorZirnoxMenu;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ZirnoxRodItem.EnumZirnoxType;
import com.hbm.items.machine.ZirnoxRodItem;
import com.hbm.items.tool.SwordMeteoriteItem;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.particle.helper.ExplosionCreator;
import com.hbm.saveddata.satellites.SatelliteRayEvents;
import com.hbm.sound.ModSounds;
import com.hbm.sound.WorldSounds;
import com.hbm.util.TickPhase;
import io.netty.buffer.ByteBuf;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import static com.hbm.api.redstoneoverradio.RadioCommandParser.FUN;
import static com.hbm.api.redstoneoverradio.RadioCommandParser.NAME_SEP;
import static com.hbm.api.redstoneoverradio.RadioCommandParser.VAL;

public class ReactorZirnoxBlockEntity extends MachineBlockEntity
        implements FluidTankHandler,
                MenuProvider,
                RadioValueProvider,
                RadioControlHandler,
                SyncUnitSchema,
                Tiltable {

    public static final int maxHeat = 100000;
    public static final int maxPressure = 100000;
    public static final String[] ROR =
            new String[] {
                VAL + "heat",
                VAL + "pressure",
                VAL + "water",
                VAL + "steam",
                VAL + "co2",
                VAL + "state",
                VAL + "slottype",
                VAL + "slotdep",
                VAL + "slotmaxdep",
                FUN + "setslot" + NAME_SEP + "id (0 to 23)",
                FUN + "setstate" + NAME_SEP + "active (0 or 1)",
                FUN + "ventco2"
            };
    private static final int[][] NEIGHBOURS = {
        {1, 7},
        {0, 2, 8},
        {1, 9},
        {4, 10},
        {3, 5, 11},
        {4, 6, 12},
        {5, 13},
        {0, 8, 14},
        {1, 7, 9, 15},
        {2, 8, 16},
        {3, 11, 17},
        {4, 10, 12, 18},
        {5, 11, 13, 19},
        {6, 12, 20},
        {7, 15, 21},
        {8, 14, 16, 22},
        {9, 15, 23},
        {10, 18},
        {11, 17, 19},
        {12, 18, 20},
        {13, 19},
        {14, 22},
        {15, 21, 23},
        {16, 22}
    };
    private static final int[] ACCESSIBLE_SLOTS = {
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23
    };
    public final FluidTankNTM steam = new FluidTankNTM(NTMFluids.SUPERHOTSTEAM, 8000);
    private final FluidTankNTM[] receiving;
    private final FluidTankNTM[] sending;
    public final FluidTankNTM carbonDioxide = new FluidTankNTM(NTMFluids.CARBONDIOXIDE, 16000);
    public final FluidTankNTM water = new FluidTankNTM(NTMFluids.WATER, 32000);
    public int heat;
    public int pressure;
    public boolean isOn;

    {
        hbm$syncSet$ReactorZirnoxBlockEntity$isOn(false);
    }

    protected int output;
    private boolean redstonePowered;

    {
        hbm$syncSet$ReactorZirnoxBlockEntity$redstonePowered(false);
    }

    private int rorSlot;

    private final FluidFlushOutputs flush = new FluidFlushOutputs();

    public ReactorZirnoxBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ZIRNOX.get(), pos, state, 28);
        receiving = new FluidTankNTM[] {water, carbonDioxide};
        sending = new FluidTankNTM[] {steam};
    }

    public void setRedstonePowered(boolean powered) {
        if (powered != this.redstonePowered) markChanged();
        if (!powered && this.redstonePowered) hbm$syncSet$ReactorZirnoxBlockEntity$isOn(false);
        this.hbm$syncSet$ReactorZirnoxBlockEntity$redstonePowered(powered);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.zirnox");
    }

    @Override
    public void tickServer() {
        tickTilt(Tiltable.Policy.CONFIGURED, true);

        if (redstonePowered) hbm$syncSet$ReactorZirnoxBlockEntity$isOn(true);
        this.output = 0;

        boolean loaded = carbonDioxide.loadTank(24, 26, inventory);
        loaded |= water.loadTank(25, 27, inventory);
        if (loaded) setChanged();

        if (isOn) {
            for (int i = 0; i < 24; i++) {
                if (inventory.get(i).getItem() instanceof ZirnoxRodItem) decay(i);
                else
                    SwordMeteoriteItem.upgrade(
                            inventory,
                            i,
                            ModItems.METEORITE_SWORD_BRED,
                            ModItems.METEORITE_SWORD_IRRADIATED);
            }
        }

        this.hbm$syncSet$ReactorZirnoxBlockEntity$pressure(
                (carbonDioxide.getFill() * 2)
                        + (int)
                                ((float) heat
                                        * ((float) carbonDioxide.getFill()
                                                / (float) carbonDioxide.getMaxFill())));

        if (heat > 0 && heat < maxHeat) {
            int lastHeat = heat;
            if (water.getFill() > 0
                    && carbonDioxide.getFill() > 0
                    && steam.getFill() < steam.getMaxFill()) {
                generateSteam();
                hbm$syncSet$ReactorZirnoxBlockEntity$heat(
                        (int) (heat - ((int) ((float) heat * (float) pressure / 1000000F))));
            } else {
                hbm$syncSet$ReactorZirnoxBlockEntity$heat((int) (heat - (10)));
            }
            if (heat != lastHeat) markChanged();
            if (level.getGameTime() % 100 == 0) {
                SatelliteRayEvents.report(
                        (ServerLevel) level,
                        worldPosition,
                        SatelliteRayEvents.NEUTRON_EMISSION,
                        200);
            }
        }

        if (pressure > maxPressure || heat > maxHeat) meltdown();

        if (!isTilted()) flush.provide((ServerLevel) level, this);

        networkPackNT(150);
    }

    @Override
    public int getSupportCheckCount() {
        return 3 * 3;
    }

    @Override
    public BlockPos getSupportCheckPosition(int index) {
        return floorGrid5(index);
    }

    private void generateSteam() {
        if (heat > 10256) {
            int cycle =
                    (int)
                            ((((float) heat - 10256F) / (float) maxHeat)
                                    * Math.min((float) carbonDioxide.getFill() / 14000F, 1F)
                                    * 25F
                                    * 5F);
            this.output = cycle;
            if (cycle != 0) markChanged();
            water.setFill(water.getFill() - cycle);
            steam.setFill(steam.getFill() + cycle);
            if (water.getFill() < 0) water.setFill(0);
            if (steam.getFill() > steam.getMaxFill()) steam.setFill(steam.getMaxFill());
        }
    }

    private boolean hasFuelRod(int id) {
        if (inventory.get(id).getItem() instanceof ZirnoxRodItem rod) return !rod.type.breeding;
        return false;
    }

    private int getNeighbourCount(int id) {
        int count = 0;
        for (int neighbour : NEIGHBOURS[id]) if (hasFuelRod(neighbour)) count++;
        return count;
    }

    private void decay(int id) {
        int decay = getNeighbourCount(id);
        ItemStack stack = inventory.get(id);
        EnumZirnoxType num = ((ZirnoxRodItem) stack.getItem()).type;

        if (!num.breeding) decay++;

        for (int i = 0; i < decay; i++) {
            this.hbm$syncSet$ReactorZirnoxBlockEntity$heat((int) (this.heat + (num.heat)));
            ZirnoxRodItem.incrementLifeTime(stack);

            if (ZirnoxRodItem.getLifeTime(stack) > num.maxLife) {
                ItemLike out = ModItems.spentZirnoxFuel(stack.getItem());
                inventory.set(id, out != null ? new ItemStack(out) : ItemStack.EMPTY);
                break;
            }
        }
        setChanged();
    }

    private void meltdown() {
        inventory.clear();

        Direction dir = getBlockState().getValue(MultiblockCoreBlock.FACING);

        MultiblockCoreBlock.withoutTeardown(
                () -> {
                    for (int ox = -2; ox <= 2; ox++)
                        for (int oz = -2; oz <= 2; oz++)
                            for (int oy = 2; oy <= 5; oy++)
                                level.setBlock(
                                        worldPosition.offset(ox, oy, oz),
                                        Blocks.AIR.defaultBlockState(),
                                        3);

                    DestroyedZirnoxBlock shell = ModBlocks.ZIRNOX_DESTROYED.get();
                    level.setBlock(
                            worldPosition,
                            shell.defaultBlockState().setValue(MultiblockCoreBlock.FACING, dir),
                            3);
                    shell.fillSpace(level, worldPosition, dir);
                });

        double cx = worldPosition.getX() + 0.5,
                cy = worldPosition.getY() + 0.5,
                cz = worldPosition.getZ() + 0.5;
        ExplosionCreator.composeEffectRBMKMush(level, cx, cy, cz, 9F);
        WorldSounds.machine(
                level,
                null,
                worldPosition.getX(),
                worldPosition.getY() + 2,
                worldPosition.getZ(),
                ModSounds.RBMK_EXPLOSION.get(),
                SoundSource.BLOCKS,
                10.0F,
                1.0F);

        if (level instanceof ServerLevel serverLevel) {
            level.explode(
                    null,
                    worldPosition.getX(),
                    worldPosition.getY() + 3,
                    worldPosition.getZ(),
                    12.0F,
                    false,
                    Level.ExplosionInteraction.BLOCK);
            zirnoxDebris();
            BlockConversionTable.FALLOUT.sphere(level, worldPosition, 35, serverLevel.getRandom());
            AwardRegions.within(
                    serverLevel,
                    cx,
                    cy,
                    cz,
                    100,
                    p -> HbmCriteria.detonation(p, DetonationTrigger.Kind.ZIRNOX));
            BossSpawnHandler.markRad(serverLevel, cx, cy, cz);
        }
    }

    private void zirnoxDebris() {
        for (int i = 0; i < 2; i++) spawnDebris(DebrisType.EXCHANGER);
        for (int i = 0; i < 20; i++) {
            spawnDebris(DebrisType.CONCRETE);
            spawnDebris(DebrisType.BLANK);
        }
        for (int i = 0; i < 10; i++) {
            spawnDebris(DebrisType.ELEMENT);
            spawnDebris(DebrisType.GRAPHITE);
            spawnDebris(DebrisType.SHRAPNEL);
        }
    }

    private void spawnDebris(DebrisType type) {
        ZirnoxDebrisEntity debris =
                new ZirnoxDebrisEntity(
                        level,
                        worldPosition.getX() + 0.5D,
                        worldPosition.getY() + 4D,
                        worldPosition.getZ() + 0.5D,
                        type);
        RandomSource rand = level.getRandom();
        double mx = rand.nextGaussian() * 0.75D;
        double mz = rand.nextGaussian() * 0.75D;
        double my = 0.01D + rand.nextDouble() * 1.25D;

        if (type == DebrisType.CONCRETE) {
            mx *= 0.25D;
            my += rand.nextDouble();
            mz *= 0.25D;
        }
        if (type == DebrisType.EXCHANGER) {
            mx += 0.5D;
            my *= 0.1D;
            mz += 0.5D;
        }

        debris.setDeltaMovement(mx, my, mz);
        level.addFreshEntity(debris);
    }

    public int getGaugeScaled(int i, int type) {
        return switch (type) {
            case 0 -> (steam.getFill() * i) / steam.getMaxFill();
            case 1 -> (carbonDioxide.getFill() * i) / carbonDioxide.getMaxFill();
            case 2 -> (water.getFill() * i) / water.getMaxFill();
            case 3 -> (heat * i) / maxHeat;
            case 4 -> (pressure * i) / maxPressure;
            default -> 1;
        };
    }

    public void toggleReactor() {
        if (!redstonePowered) hbm$syncSet$ReactorZirnoxBlockEntity$isOn(!isOn);
        setChanged();
    }

    public void ventCarbonDioxide() {
        carbonDioxide.setFill(Math.max(carbonDioxide.getFill() - 1000, 0));
        setChanged();
    }

    @Override
    public int tankCount() {
        return 3;
    }

    @Override
    public FluidTankNTM tank(int index) {
        return switch (index) {
            case 0 -> water;
            case 1 -> steam;
            case 2 -> carbonDioxide;
            default -> throw new IndexOutOfBoundsException(index);
        };
    }

    @Override
    public FluidTankNTM[] getReceivingTanks() {
        return receiving;
    }

    @Override
    public FluidTankNTM[] getSendingTanks() {
        return sending;
    }

    @Override
    public void declareFlush(FlushLanes out) {
        for (FluidTankNTM tank : getSendingTanks()) out.add(tank, FlushFaces.own());
    }

    @Override
    public long getProviderSpeed(Fluid type, int pressure) {
        return isTilted() ? 0L : FluidTankHandler.super.getProviderSpeed(type, pressure);
    }

    @Override
    public boolean netSubscribed() {
        return !isTilted();
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ACCESSIBLE_SLOTS;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot < 24 && stack.getItem() instanceof ZirnoxRodItem;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot < 24 && !(stack.getItem() instanceof ZirnoxRodItem);
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new ReactorZirnoxMenu(containerId, playerInventory, this);
    }

    @Override
    public String[] getFunctionInfo() {
        return ROR;
    }

    @Override
    public @Nullable String getRadioValue(String name) {
        if ((VAL + "heat").equals(name))
            return "" + (int) Math.round(heat * 1.0E-5D * 780.0D + 20.0D);
        if ((VAL + "pressure").equals(name))
            return "" + (int) Math.round(pressure * 1.0E-5D * 30.0D);
        if ((VAL + "water").equals(name)) return "" + water.getFill();
        if ((VAL + "steam").equals(name)) return "" + steam.getFill();
        if ((VAL + "co2").equals(name)) return "" + carbonDioxide.getFill();
        if ((VAL + "state").equals(name)) return "" + (isOn ? 1 : 0);
        if ((VAL + "slottype").equals(name)) {
            return hasFuelRod(rorSlot)
                    ? ((ZirnoxRodItem) inventory.get(rorSlot).getItem()).type.legacyName()
                    : "";
        }
        if ((VAL + "slotdep").equals(name)) {
            return hasFuelRod(rorSlot)
                    ? "" + ZirnoxRodItem.getLifeTime(inventory.get(rorSlot))
                    : null;
        }
        if ((VAL + "slotmaxdep").equals(name)) {
            return hasFuelRod(rorSlot)
                    ? "" + ((ZirnoxRodItem) inventory.get(rorSlot).getItem()).type.maxLife
                    : null;
        }
        return null;
    }

    @Override
    public void runRadioFunction(String name, String[] params) {
        if ((FUN + "setslot").equals(name) && params.length > 0) {
            if (redstonePowered) return;
            try {
                int slot = Integer.parseInt(params[0]);
                if (slot >= 0 && slot < 24) rorSlot = slot;
            } catch (NumberFormatException ignored) {
            }
            return;
        }
        if ((FUN + "setstate").equals(name) && params.length > 0) {
            if (redstonePowered) return;
            try {
                hbm$syncSet$ReactorZirnoxBlockEntity$isOn(Integer.parseInt(params[0]) == 1);
                setChanged();
            } catch (NumberFormatException ignored) {
            }
            return;
        }
        if ((FUN + "ventco2").equals(name)) {
            carbonDioxide.setFill(Math.max(carbonDioxide.getFill() - 1000, 0));
            setChanged();
            return;
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        hbm$syncSet$ReactorZirnoxBlockEntity$heat(input.getIntOr("heat", 0));
        hbm$syncSet$ReactorZirnoxBlockEntity$pressure(input.getIntOr("pressure", 0));
        hbm$syncSet$ReactorZirnoxBlockEntity$isOn(input.getBooleanOr("isOn", false));
        hbm$syncSet$ReactorZirnoxBlockEntity$redstonePowered(
                input.getBooleanOr("redstonePowered", false));
        input.child("steam").ifPresent(steam::deserialize);
        input.child("carbondioxide").ifPresent(carbonDioxide::deserialize);
        input.child("water").ifPresent(water::deserialize);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("heat", heat);
        output.putInt("pressure", pressure);
        output.putBoolean("isOn", isOn);
        output.putBoolean("redstonePowered", redstonePowered);
        steam.serialize(output.child("steam"));
        carbonDioxide.serialize(output.child("carbondioxide"));
        water.serialize(output.child("water"));
    }

    @Override
    public long syncUnitMask() {
        return 0x7fL;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeInt(this.heat);
            case 1 -> output.writeInt(this.pressure);
            case 2 -> output.writeBoolean(this.isOn);
            case 3 -> output.writeBoolean(this.redstonePowered);
            case 4 -> this.steam.packetSerialize(output);
            case 5 -> this.carbonDioxide.packetSerialize(output);
            case 6 -> this.water.packetSerialize(output);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.hbm$syncSet$ReactorZirnoxBlockEntity$heat(input.readInt());
            case 1 -> this.hbm$syncSet$ReactorZirnoxBlockEntity$pressure(input.readInt());
            case 2 -> this.hbm$syncSet$ReactorZirnoxBlockEntity$isOn(input.readBoolean());
            case 3 ->
                    this.hbm$syncSet$ReactorZirnoxBlockEntity$redstonePowered(input.readBoolean());
            case 4 -> this.steam.packetDeserialize(input);
            case 5 -> this.carbonDioxide.packetDeserialize(input);
            case 6 -> this.water.packetDeserialize(input);
            default -> throw new IllegalArgumentException();
        }
    }

    public final void hbm$syncSet$ReactorZirnoxBlockEntity$heat(int next) {
        if (this.heat != next) {
            this.heat = next;
            if ((1L << 0) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 0);
        }
    }

    public final void hbm$syncSet$ReactorZirnoxBlockEntity$pressure(int next) {
        if (this.pressure != next) {
            this.pressure = next;
            if ((1L << 1) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 1);
        }
    }

    public final void hbm$syncSet$ReactorZirnoxBlockEntity$isOn(boolean next) {
        if (this.isOn != next) {
            this.isOn = next;
            if ((1L << 2) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 2);
        }
    }

    public final void hbm$syncSet$ReactorZirnoxBlockEntity$redstonePowered(boolean next) {
        if (this.redstonePowered != next) {
            this.redstonePowered = next;
            if ((1L << 3) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 3);
        }
    }

    static {
        com.hbm.packet.SyncBindings.field(ReactorZirnoxBlockEntity.class, "steam", 3, 1L << 4);
        com.hbm.packet.SyncBindings.field(
                ReactorZirnoxBlockEntity.class, "carbonDioxide", 3, 1L << 5);
        com.hbm.packet.SyncBindings.field(ReactorZirnoxBlockEntity.class, "water", 3, 1L << 6);
    }

    private int hbm$tiltChecked;

    private int hbm$tiltValid;

    public boolean isTilted() {
        return getBlockState().getValue(Tiltable.TILT);
    }

    public void tickTilt(Tiltable.Policy policy, boolean extraHeavy) {
        Level level = getLevel();
        boolean doesTilt =
                (policy == Tiltable.Policy.UNAVOIDABLE
                                ? Environment.unavoidableGravity(level)
                                : MachineData.ENABLE_MACHINE_GRAVITY.get()
                                        || BalanceConfig.enable528MachineGravity)
                        && !Environment.orbital(level);

        if (!doesTilt || getSupportCheckCount() <= 0) {
            hbm$settle(false);
            return;
        }
        if (!TickPhase.every(this, SharedConstants.TICKS_PER_SECOND)) return;

        if (hbm$tiltChecked >= getSupportCheckCount()) {
            boolean tilted = hbm$tiltValid < hbm$tiltChecked * 0.95;
            if (tilted && !isTilted()) {
                SoundEvent impact = ModSounds.METAL_IMPACT.get();
                level.playSound(null, getBlockPos(), impact, SoundSource.BLOCKS, 3F, 1F);
            }
            hbm$settle(tilted);
            hbm$tiltChecked = 0;
            hbm$tiltValid = 0;
        }

        BlockPos pos = getSupportCheckPosition(hbm$tiltChecked);
        if (pos == null) return;

        BlockState ground = level.getBlockState(pos);
        hbm$tiltChecked++;

        if (extraHeavy) {
            if (!ground.isSolidRender() || ground.isSignalSource()) return;
            if (ground.getBlock() instanceof FallingBlock
                    || ground.is(BlockTags.MINEABLE_WITH_SHOVEL)
                    || ground.is(BlockTags.WOOL)) return;
            if (ground.getBlock().getExplosionResistance() < Blocks.STONE.getExplosionResistance())
                return;
            hbm$tiltValid++;
            return;
        }
        if (!ground.isFaceSturdy(level, pos, Direction.UP)) return;
        if (ground.getBlock() instanceof FallingBlock) return;
        if (ground.is(ModBlocks.DIRT_DEAD.get())
                || ground.is(ModBlocks.DIRT_OILY.get())
                || ground.is(ModBlocks.STONE_CRACKED.get())) return;
        hbm$tiltValid++;
    }

    private void hbm$settle(boolean tilted) {
        BlockState state = getBlockState();
        if (state.getValue(Tiltable.TILT) == tilted) return;
        BlockState settled = state.setValue(Tiltable.TILT, tilted);
        getLevel()
                .setBlock(
                        getBlockPos(),
                        settled,
                        Block.UPDATE_CLIENTS
                                | Block.UPDATE_KNOWN_SHAPE
                                | Block.UPDATE_SKIP_ON_PLACE);
    }
}
