/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: HbmMods (The Bobcat)
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) HbmMods (The Bobcat). All rights reserved.
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

package com.hbm.blockentity.machine.oil;

import com.google.common.base.Suppliers;
import com.hbm.api.energymk2.EnergyHandler;
import com.hbm.api.energymk2.EnergyItem;
import com.hbm.api.energymk2.ItemEnergyTransfer;
import com.hbm.api.environment.Environment;
import com.hbm.api.fluidmk2.FluidFlushOutputs;
import com.hbm.api.fluidmk2.FluidTankHandler;
import com.hbm.api.fluidmk2.FlushFaces;
import com.hbm.api.fluidmk2.FlushLanes;
import com.hbm.blockentity.AudioLoop;
import com.hbm.blockentity.FluidSettingsHandler;
import com.hbm.blockentity.MachineBlockEntity;
import com.hbm.blockentity.PersistentDrop;
import com.hbm.blockentity.SoundEmitter;
import com.hbm.blockentity.Tiltable;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.machine.RefineryBlock;
import com.hbm.config.BalanceConfig;
import com.hbm.data.MachineData;
import com.hbm.handler.pollution.PollutionHandler;
import com.hbm.handler.pollution.PollutionType;
import com.hbm.handler.threading.TargetPoint;
import com.hbm.interfaces.Extinguishable;
import com.hbm.interfaces.OverpressureHandler;
import com.hbm.interfaces.RepairHandler;
import com.hbm.inventory.container.RefineryMenu;
import com.hbm.inventory.fluid.FluidStackNTM;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankAccess;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.material.MaterialTagRegistry;
import com.hbm.inventory.recipes.RefineryRecipe;
import com.hbm.inventory.recipes.RefineryRecipes;
import com.hbm.inventory.recipes.ingredient.CountIngredient;
import com.hbm.inventory.recipes.loader.Outputs;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModItems;
import com.hbm.items.machine.FluidIdentifierItem;
import com.hbm.packet.PacketWire;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.packet.layout.EffectAnnihilatorFlameLayout;
import com.hbm.sound.AudioSystem;
import com.hbm.sound.ModSounds;
import com.hbm.sound.SoundHandle;
import com.hbm.util.TickPhase;
import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

public class RefineryBlockEntity extends MachineBlockEntity
        implements FluidTankAccess,
                EnergyHandler,
                FluidTankHandler,
                MenuProvider,
                PersistentDrop,
                FluidSettingsHandler,
                Tiltable,
                SoundEmitter,
                AudioLoop,
                OverpressureHandler,
                RepairHandler,
                Extinguishable,
                SyncUnitSchema {

    public static final int SLOT_BATTERY = 0;
    public static final int SLOT_BYPRODUCT = 11;
    public static final int SLOT_FLUID_ID = 12;
    public static final int SLOT_COUNT = 13;

    public static final long MAX_POWER = 1_000L;
    public static final int TANK_CAPACITY_INPUT = FractionColumn.FEED_CAPACITY;
    public static final int TANK_CAPACITY_OUTPUT = FractionColumn.FRACTION_CAPACITY;

    private static final long BATCH_POWER = 5L;
    private static final int BATCHES_PER_SOLID = 10;
    private static final int FIRE_DRAIN = 10;
    private static final int LINGER = 20;
    private static final int[] FACE_SLOTS = {SLOT_BYPRODUCT};
    private static final String[] TANK_KEYS = {"tank0", "tank1", "tank2", "tank3", "tank4"};
    private static final String[] PERSISTENT_KEYS = {
        "onFire", "tank0", "tank1", "tank2", "tank3", "tank4"
    };
    private static final Supplier<List<CountIngredient>> REPAIR_COST =
            Suppliers.memoize(
                    () ->
                            List.of(
                                    CountIngredient.of(MaterialTagRegistry.STEEL.plate(), 8),
                                    CountIngredient.of(ModItems.DUCTTAPE.get(), 4)));

    public static Consumer<RefineryBlockEntity> CLIENT_SOUND = _ -> {};

    public long power;
    public final FluidTankNTM[] tanks;
    public boolean onFire;
    public boolean running;

    private final FractionColumn column;
    private final FluidFlushOutputs flush = new FluidFlushOutputs();
    private int batches;
    private int linger;
    private @Nullable Explosion lastExplosion;

    public RefineryBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.REFINERY.get(), pos, state, SLOT_COUNT);
        column =
                new FractionColumn(
                        new FluidTankNTM(NTMFluids.HOTOIL, FractionColumn.FEED_CAPACITY),
                        NTMFluids.HEAVYOIL,
                        NTMFluids.NAPHTHA,
                        NTMFluids.LIGHTOIL,
                        NTMFluids.PETROLEUM);
        tanks = column.tanks;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.machineRefinery");
    }

    @Override
    public void tickServer() {
        tickTilt(Tiltable.Policy.CONFIGURED, false);
        boolean ran = false;
        if (!hasExploded()) ran = operate();
        else if (onFire) burn();
        if (running != ran) hbm$syncSet$RefineryBlockEntity$running(ran);
        networkPackNT(150);
    }

    private boolean operate() {
        long charged = ItemEnergyTransfer.extract(this, SLOT_BATTERY, MAX_POWER - power, false);
        if (charged != 0L) hbm$syncSet$RefineryBlockEntity$power((long) (power + (charged)));
        boolean changed = column.fillFeed(inventory, SLOT_FLUID_ID);
        boolean ran = refine();
        changed |= column.drainFractions(inventory);
        if (changed) markChanged();
        flush.provide((ServerLevel) level, this);
        return ran;
    }

    private boolean refine() {
        RefineryRecipe recipe = RefineryRecipes.INSTANCE.getRefinery(tanks[0].getTankType());
        if (recipe == null) return column.refine(null, false);
        if (!column.refine(recipe.outputFluid, power >= BATCH_POWER)) return false;
        hbm$syncSet$RefineryBlockEntity$power((long) (power - (BATCH_POWER)));
        if (++batches >= BATCHES_PER_SOLID) {
            batches -= BATCHES_PER_SOLID;
            var solids = recipe.outputItems();
            if (Outputs.fitsFixed(inventory, SLOT_BYPRODUCT, solids)) {
                Outputs.insertFixed(inventory, SLOT_BYPRODUCT, solids);
            }
        }
        markChanged();
        if (TickPhase.every(this, SharedConstants.TICKS_PER_SECOND)) {
            PollutionHandler.add(
                    level, worldPosition, PollutionType.SOOT, 5 * PollutionHandler.SOOT_PER_SECOND);
        }
        return true;
    }

    private void burn() {
        if (!column.holdsFluid()) return;
        for (FluidTankNTM tank : tanks) tank.setFill(tank.getFill() - FIRE_DRAIN);
        markChanged();

        int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
        for (Entity entity :
                level.getEntitiesOfClass(
                        Entity.class, new AABB(x - 1.5, y, z - 1.5, x + 2.5, y + 8, z + 2.5))) {
            entity.igniteForSeconds(5F);
        }
        RandomSource random = level.getRandom();
        double px = x + random.nextDouble();
        double py = y + 1.5 + random.nextDouble() * 3;
        double pz = z + random.nextDouble();
        PacketWire.sendToAllAround(
                EffectAnnihilatorFlameLayout.encode(px, py, pz),
                new TargetPoint((ServerLevel) level, px, py, pz, 150));
        if (TickPhase.every(this, SharedConstants.TICKS_PER_SECOND)) {
            PollutionHandler.add(
                    level,
                    worldPosition,
                    PollutionType.SOOT,
                    70 * PollutionHandler.SOOT_PER_SECOND);
        }
    }

    @Override
    public void tickClient() {
        CLIENT_SOUND.accept(this);
    }

    @Override
    public boolean tickSound() {
        if (running) linger = LINGER;
        if (linger <= 0) return false;
        linger--;
        return true;
    }

    @Override
    public @Nullable SoundHandle createAudioLoop() {
        return AudioSystem.getLoopedSound(
                ModSounds.BOILER_LOOP.get(),
                SoundSource.BLOCKS,
                worldPosition.getX(),
                worldPosition.getY(),
                worldPosition.getZ(),
                0.25F,
                15F,
                1.0F,
                20);
    }

    public boolean hasExploded() {
        return getBlockState().getValue(RefineryBlock.EXPLODED);
    }

    public boolean firstHitBy(Explosion explosion) {
        if (lastExplosion == explosion) return false;
        lastExplosion = explosion;
        return true;
    }

    private void setExploded(boolean exploded) {
        BlockState state = getBlockState();
        if (state.getValue(RefineryBlock.EXPLODED) == exploded) return;
        level.setBlock(
                worldPosition,
                state.setValue(RefineryBlock.EXPLODED, exploded),
                Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SKIP_ON_PLACE);
    }

    @Override
    public void explode(Level level, BlockPos pos) {
        if (hasExploded()) return;
        setExploded(true);
        hbm$syncSet$RefineryBlockEntity$onFire(true);
        markChanged();
    }

    @Override
    public boolean isDamaged() {
        return hasExploded();
    }

    @Override
    public List<CountIngredient> getRepairMaterials() {
        return REPAIR_COST.get();
    }

    @Override
    public void repair(Player player) {
        setExploded(false);
        markChanged();
    }

    @Override
    public void tryExtinguish(Level level, BlockPos pos, Extinguishable.Agent agent) {
        if (!onFire || !hasExploded()) return;
        switch (agent) {
            case FOAM, CO2 -> {
                hbm$syncSet$RefineryBlockEntity$onFire(false);
                markChanged();
            }
            case WATER -> {
                if (column.holdsFluid()) {
                    level.explode(
                            null,
                            worldPosition.getX() + 0.5,
                            worldPosition.getY() + 1.5,
                            worldPosition.getZ() + 0.5,
                            5F,
                            true,
                            Level.ExplosionInteraction.BLOCK);
                }
            }
        }
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_BATTERY -> EnergyItem.matches(stack);
            case 1, 3, 5, 7, 9 -> FluidTankNTM.isFluidContainer(stack);
            case SLOT_FLUID_ID -> stack.getItem() instanceof FluidIdentifierItem;
            default -> false;
        };
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return FACE_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == SLOT_BYPRODUCT;
    }

    @Override
    public int getSupportCheckCount() {
        return 4;
    }

    @Override
    public BlockPos getSupportCheckPosition(int index) {
        return floorGrid3(index);
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public void setPower(long stored) {
        hbm$syncSet$RefineryBlockEntity$power(Math.clamp(stored, 0L, MAX_POWER));
    }

    @Override
    public long getMaxPower() {
        return MAX_POWER;
    }

    @Override
    public FluidTankNTM[] getReceivingTanks() {
        return column.feed;
    }

    @Override
    public FluidTankNTM[] getSendingTanks() {
        return column.fractions;
    }

    @Override
    public void declareFlush(FlushLanes out) {
        for (FluidTankNTM tank : column.fractions) out.add(tank, FlushFaces.own());
    }

    @Override
    public int tankCount() {
        return tanks.length;
    }

    @Override
    public FluidTankNTM tank(int index) {
        return tanks[index];
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new RefineryMenu(containerId, playerInventory, this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        hbm$syncSet$RefineryBlockEntity$power(input.getLongOr("power", 0L));
        batches = input.getIntOr("sulfur", 0);
        hbm$syncSet$RefineryBlockEntity$onFire(input.getBooleanOr("onFire", false));
        for (int i = 0; i < tanks.length; i++)
            input.child(TANK_KEYS[i]).ifPresent(tanks[i]::deserialize);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("power", power);
        output.putInt("sulfur", batches);
        output.putBoolean("onFire", onFire);
        for (int i = 0; i < tanks.length; i++) tanks[i].serialize(output.child(TANK_KEYS[i]));
    }

    @Override
    public void writePersistent(DataComponentMap.Builder components) {
        boolean exploded = hasExploded();
        if (!exploded && !column.holdsFluid()) return;
        components.set(
                ModDataComponents.REFINERY_CONTENTS.get(),
                new RefineryContents(FluidStackNTM.snapshot(tanks), exploded, onFire));
    }

    @Override
    public void readPersistent(DataComponentGetter components) {
        RefineryContents contents = components.get(ModDataComponents.REFINERY_CONTENTS.get());
        if (contents == null) return;
        FluidStackNTM.restore(contents.tanks(), tanks);
        hbm$syncSet$RefineryBlockEntity$onFire(contents.onFire());
        if (contents.hasExploded()) setExploded(true);
    }

    @Override
    public String[] persistentKeys() {
        return PERSISTENT_KEYS;
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 0x1bL;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeLong(power);
            case 1 -> {
                for (FluidTankNTM tank : tanks) tank.packetSerialize(output);
            }
            case 3 -> output.writeBoolean(onFire);
            case 4 -> output.writeBoolean(running);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> hbm$syncSet$RefineryBlockEntity$power(input.readLong());
            case 1 -> {
                for (FluidTankNTM tank : tanks) tank.packetDeserialize(input);
            }
            case 3 -> hbm$syncSet$RefineryBlockEntity$onFire(input.readBoolean());
            case 4 -> hbm$syncSet$RefineryBlockEntity$running(input.readBoolean());
            default -> super.readSyncUnit(unit, input);
        }
    }

    public final void hbm$syncSet$RefineryBlockEntity$power(long next) {
        if (this.power != next) {
            this.power = next;
            if ((1L << 0) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 0);
        }
    }

    public final FluidTankNTM hbm$syncArraySet$RefineryBlockEntity$tanks(
            FluidTankNTM[] array, int index, FluidTankNTM next) {
        if (array[index] != next) {
            if (array == this.tanks && syncBound())
                com.hbm.packet.SyncBindings.unbind(this, array[index]);
            array[index] = next;
            if (array == this.tanks) {
                syncArrayChanged(array, index, 3, 1L << 1);
            }
        }
        return next;
    }

    public final void hbm$syncSet$RefineryBlockEntity$onFire(boolean next) {
        if (this.onFire != next) {
            this.onFire = next;
            if ((1L << 3) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 3);
        }
    }

    public final void hbm$syncSet$RefineryBlockEntity$running(boolean next) {
        if (this.running != next) {
            this.running = next;
            if ((1L << 4) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 4);
        }
    }

    static {
        com.hbm.packet.SyncBindings.field(RefineryBlockEntity.class, "tanks", 3, 1L << 1);
        com.hbm.inventory.container.SyncedData.fields(RefineryBlockEntity.class, "power");
    }

    public void stripPersistent(ValueOutput output) {
        for (String key : persistentKeys()) output.discard(key);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        writePersistent(components);
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        readPersistent(components);
    }

    @Override
    public void removeComponentsFromTag(ValueOutput output) {
        super.removeComponentsFromTag(output);
        stripPersistent(output);
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

    private boolean hbm$soundActive;

    public boolean soundStarted(boolean active) {
        boolean started = active && !hbm$soundActive;
        hbm$soundActive = active;
        return started;
    }

    private SoundHandle hbm$audio;

    public void audioLoop(boolean running, float volume) {
        hbm$audio = AudioLoop.loop(this, hbm$audio, running, volume, 0F, false);
    }

    public void audioLoop(boolean running, float volume, float pitch) {
        hbm$audio = AudioLoop.loop(this, hbm$audio, running, volume, pitch, true);
    }

    private void hbm$audioRemove() {
        audioLoop(false, 0F);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        hbm$audioRemove();
    }
}
