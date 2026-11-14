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

import com.hbm.api.energymk2.EnergyHandler;
import com.hbm.api.energymk2.ItemEnergyTransfer;
import com.hbm.api.fluidmk2.FluidFlushOutputs;
import com.hbm.api.fluidmk2.FluidTankHandler;
import com.hbm.api.fluidmk2.FlushFaces;
import com.hbm.api.fluidmk2.FlushLanes;
import com.hbm.blockentity.ChunkPublication;
import com.hbm.blockentity.MachineBlockEntity;
import com.hbm.blockentity.UpgradeInfoProvider;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.gas.GasBlock;
import com.hbm.blocks.generic.FluidReserve;
import com.hbm.inventory.container.MiningLaserMenu;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.CentrifugeRecipes;
import com.hbm.inventory.recipes.CrystallizerRecipes;
import com.hbm.inventory.recipes.ShredderRecipes;
import com.hbm.inventory.recipes.ingredient.RecipeInputState;
import com.hbm.items.ModItems;
import com.hbm.items.machine.upgrade.MachineUpgradeItem;
import com.hbm.items.machine.upgrade.UpgradeInfo;
import com.hbm.items.machine.upgrade.UpgradeLimits;
import com.hbm.items.machine.upgrade.UpgradeManager;
import com.hbm.items.machine.upgrade.UpgradeType;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.platform.Caps;
import com.hbm.sound.ModSounds;
import com.hbm.sound.WorldSounds;
import com.hbm.util.ChunkUtil;
import com.hbm.util.InventoryUtil;
import com.hbm.util.RegistryUtil;
import io.netty.buffer.ByteBuf;
import java.util.Arrays;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

public final class MiningLaserBlockEntity extends MachineBlockEntity
        implements EnergyHandler,
                FluidTankHandler,
                MenuProvider,
                UpgradeInfoProvider,
                SyncUnitSchema {
    public static final int SLOT_COUNT = 30;
    public static final int SLOT_BATTERY = 0;
    public static final int SLOT_UPGRADE_START = 1;
    public static final int SLOT_UPGRADE_END = 8;
    public static final int SLOT_OUTPUT_START = 9;
    public static final int SLOT_OUTPUT_END = 29;
    public static final long MAX_POWER = 100_000_000L;
    public static final int CONSUMPTION = 10_000;
    public static final int TANK_CAPACITY = 64_000;
    public static final int OIL_PER_ORE = 500;

    private static final int[] OUTPUT_SLOTS = {
        9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29
    };
    private static final Direction[] CONTACT_DIRECTIONS = {
        Direction.EAST, Direction.WEST, Direction.SOUTH, Direction.NORTH
    };
    private static final UpgradeLimits UPGRADE_CAPS =
            UpgradeLimits.of(UpgradeType.SPEED, 12)
                    .and(UpgradeType.POWER, 12)
                    .and(UpgradeType.EFFECT, 12)
                    .and(UpgradeType.FORTUNE, 3)
                    .and(UpgradeType.OVERDRIVE, 9);
    private static final TagKey<Item> WORTHLESS =
            TagKey.create(Registries.ITEM, RegistryUtil.id("mining_laser_worthless"));
    private static final int NULLIFIER = 1;
    private static final int SCREAM = 2;

    public long power;
    public int lastTargetX;
    public int lastTargetY;
    public int lastTargetZ;
    public int targetX;
    public int targetY;
    public int targetZ;
    public boolean beam;
    public boolean isOn;
    public double clientBreakProgress;
    public final FluidTankNTM tank = new FluidTankNTM(NTMFluids.OIL, TANK_CAPACITY);
    public boolean redstonePower;

    private final FluidTankNTM[] sendingTanks = {tank};
    private final FluidFlushOutputs fluidOutputs = new FluidFlushOutputs();
    private final UpgradeManager upgrades = new UpgradeManager(this);
    private final boolean[] unloadTargets = new boolean[CONTACT_DIRECTIONS.length];
    private final BlockPos.MutableBlockPos scanPosition = new BlockPos.MutableBlockPos();
    private final LevelChunkSection[] scanSections = new LevelChunkSection[5];
    private final RecipeManager.CachedCheck<SingleRecipeInput, SmeltingRecipe> smelting =
            RecipeManager.createCheck(RecipeType.SMELTING);
    private double breakProgress;
    private boolean layerArmed;
    private RefiningStep refining = RefiningStep.NONE;
    private int specialEffects;
    private @Nullable Object capabilityListener;
    private boolean unloadRefreshPending;

    private enum RefiningStep {
        NONE,
        CRYSTALLIZE,
        CENTRIFUGE,
        SHRED,
        SMELT
    }

    public MiningLaserBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MINING_LASER.get(), pos, state, SLOT_COUNT);
        var initialTargetX = pos.getX();
        hbm$syncSet$MiningLaserBlockEntity$lastTargetX(initialTargetX);
        hbm$syncSet$MiningLaserBlockEntity$targetX(initialTargetX);
        var initialTargetY = pos.getY() - 2;
        hbm$syncSet$MiningLaserBlockEntity$lastTargetY(initialTargetY);
        hbm$syncSet$MiningLaserBlockEntity$targetY(initialTargetY);
        var initialTargetZ = pos.getZ();
        hbm$syncSet$MiningLaserBlockEntity$lastTargetZ(initialTargetZ);
        hbm$syncSet$MiningLaserBlockEntity$targetZ(initialTargetZ);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        super.setItem(slot, stack);
        if (level instanceof ServerLevel
                && slot >= SLOT_UPGRADE_START
                && slot <= SLOT_UPGRADE_END
                && stack.getItem() instanceof MachineUpgradeItem) {
            WorldSounds.machine(
                    level,
                    null,
                    worldPosition,
                    ModSounds.UPGRADE_PLUG.get(),
                    SoundSource.BLOCKS,
                    1F,
                    1F);
        }
    }

    @Override
    public void onGraphLoad(ServerLevel server) {
        tank.bindOwner(this);
        updateRedstonePower();
        refreshUnloadTargets();
        if (capabilityListener == null) {
            var contacts =
                    List.of(
                            worldPosition.east(2),
                            worldPosition.west(2),
                            worldPosition.south(2),
                            worldPosition.north(2));
            capabilityListener =
                    Caps.listenForCapChanges(
                            server,
                            contacts,
                            () -> {
                                unloadRefreshPending = true;
                                return !isRemoved();
                            });
        }
    }

    @Override
    public void onGraphDetach(ServerLevel server) {
        capabilityListener = null;
    }

    public void updateRedstonePower() {
        var signals = ChunkPublication.signals((ServerLevel) level, worldPosition);
        hbm$syncSet$MiningLaserBlockEntity$redstonePower(false);
        for (var direction : CONTACT_DIRECTIONS) {
            var ring = worldPosition.relative(direction);
            if (signals.getBestNeighborSignal(ring) > 0) {
                hbm$syncSet$MiningLaserBlockEntity$redstonePower(true);
                return;
            }
        }
    }

    public void refreshUnloadTargets() {
        unloadRefreshPending = false;
        for (int contact = 0; contact < CONTACT_DIRECTIONS.length; contact++) {
            var direction = CONTACT_DIRECTIONS[contact];
            var destination = worldPosition.relative(direction, 2);
            unloadTargets[contact] =
                    ChunkUtil.chunkIfLoaded(level, destination) != null
                            && Caps.findItemStore(level, destination, direction.getOpposite())
                                    != null;
        }
    }

    @Override
    public void tickServer() {
        var server = (ServerLevel) level;
        fluidOutputs.provide(server, this);
        hbm$syncSet$MiningLaserBlockEntity$power(
                (long)
                        (power
                                + (ItemEnergyTransfer.extract(
                                        this, SLOT_BATTERY, MAX_POWER - power, false))));
        if (targetX != lastTargetX || targetY != lastTargetY || targetZ != lastTargetZ)
            breakProgress = 0D;
        hbm$syncSet$MiningLaserBlockEntity$lastTargetX(targetX);
        hbm$syncSet$MiningLaserBlockEntity$lastTargetY(targetY);
        hbm$syncSet$MiningLaserBlockEntity$lastTargetZ(targetZ);

        if (isOn && !redstonePower) {
            upgrades.scan(this, SLOT_UPGRADE_START, SLOT_UPGRADE_END);
            projectSpecialUpgrades();
            int speed = upgrades.getLevel(UpgradeType.SPEED);
            int draw =
                    CONSUMPTION
                            - CONSUMPTION * upgrades.getLevel(UpgradeType.POWER) / 16
                            + CONSUMPTION * speed / 16;
            int pulses = 1 + upgrades.getLevel(UpgradeType.OVERDRIVE);
            int radius = 1 + 2 * upgrades.getLevel(UpgradeType.EFFECT);
            for (int pulse = 0; pulse < pulses; pulse++) {
                if (power < draw) {
                    hbm$syncSet$MiningLaserBlockEntity$beam(false);
                    break;
                }
                usePower(draw);
                if (!layerArmed || targetY <= level.getMinY()) {
                    hbm$syncSet$MiningLaserBlockEntity$targetY(worldPosition.getY() - 2);
                    layerArmed = true;
                }
                if (selectFirstBlock(radius)) cutSelectedBlock(1 + speed);
                else {
                    hbm$syncSet$MiningLaserBlockEntity$beam(false);
                    hbm$syncSet$MiningLaserBlockEntity$targetY((int) (targetY - 1));
                    clearLiquidAndWall(scanPosition.set(targetX, targetY, targetZ));
                }
            }
        } else {
            hbm$syncSet$MiningLaserBlockEntity$targetY(worldPosition.getY() - 2);
            layerArmed = false;
            hbm$syncSet$MiningLaserBlockEntity$beam(false);
        }
        if (unloadRefreshPending) refreshUnloadTargets();
        unloadItems();
        networkPackNT(250);
    }

    private void projectSpecialUpgrades() {
        boolean crystallizer = false;
        boolean centrifuge = false;
        boolean shredder = false;
        boolean smelter = false;
        specialEffects = 0;
        for (int slot = SLOT_UPGRADE_START; slot <= SLOT_UPGRADE_END; slot++) {
            var stack = inventory.get(slot);
            if (stack.is(ModItems.UPGRADE_CRYSTALLIZER.get())) crystallizer = true;
            else if (stack.is(ModItems.UPGRADE_CENTRIFUGE.get())) centrifuge = true;
            else if (stack.is(ModItems.UPGRADE_SHREDDER.get())) shredder = true;
            else if (stack.is(ModItems.UPGRADE_SMELTER.get())) smelter = true;
            else if (stack.is(ModItems.UPGRADE_NULLIFIER.get())) specialEffects |= NULLIFIER;
            else if (stack.is(ModItems.UPGRADE_SCREM.get())) specialEffects |= SCREAM;
        }
        if (crystallizer) refining = RefiningStep.CRYSTALLIZE;
        else if (centrifuge) refining = RefiningStep.CENTRIFUGE;
        else if (shredder) refining = RefiningStep.SHRED;
        else if (smelter) refining = RefiningStep.SMELT;
        else refining = RefiningStep.NONE;
    }

    private boolean selectFirstBlock(int radius) {
        if (level.isOutsideBuildHeight(targetY)) return false;
        int sectionIndex = level.getSectionIndex(targetY);
        int previousChunkX = Integer.MIN_VALUE;
        int minimumZ = worldPosition.getZ() - radius;
        int maximumZ = worldPosition.getZ() + radius;
        int minimumChunkZ = minimumZ >> 4;
        try {
            for (int x = worldPosition.getX() - radius; x <= worldPosition.getX() + radius; x++) {
                int chunkX = x >> 4;
                if (chunkX != previousChunkX) {
                    Arrays.fill(scanSections, null);
                    previousChunkX = chunkX;
                }
                for (int z = minimumZ; z <= maximumZ; z++) {
                    scanPosition.set(x, targetY, z);
                    if (!level.isInValidBounds(scanPosition)) continue;
                    int chunkZ = z >> 4;
                    int sectionSlot = chunkZ - minimumChunkZ;
                    var section = scanSections[sectionSlot];
                    if (section == null) {
                        section = level.getChunk(chunkX, chunkZ).getSection(sectionIndex);
                        scanSections[sectionSlot] = section;
                    }
                    if (section.hasOnlyAir()) {
                        z = Math.min(maximumZ, (chunkZ << 4) + 15);
                        continue;
                    }
                    var state = section.getBlockState(x & 15, targetY & 15, z & 15);
                    if (!isCuttable(state, scanPosition)) continue;
                    hbm$syncSet$MiningLaserBlockEntity$targetX(x);
                    hbm$syncSet$MiningLaserBlockEntity$targetZ(z);
                    hbm$syncSet$MiningLaserBlockEntity$beam(true);
                    return true;
                }
            }
            return false;
        } finally {
            Arrays.fill(scanSections, null);
        }
    }

    private boolean isCuttable(BlockState state, BlockPos pos) {
        return !state.isAir()
                && !(state.getBlock() instanceof GasBlock)
                && !state.is(Blocks.BEDROCK)
                && !state.liquid()
                && state.getDestroySpeed(level, pos) >= 0F;
    }

    private void cutSelectedBlock(int speed) {
        var pos = scanPosition.set(targetX, targetY, targetZ);
        var state = level.getBlockState(pos);
        float effort = state.getDestroySpeed(level, pos) * 15F / speed;
        float contribution = effort == 0F ? 1F : 1F / effort;
        breakProgress += contribution;
        hbm$syncSet$MiningLaserBlockEntity$clientBreakProgress(Math.min(breakProgress, 1D));
        if (breakProgress < 1D) {
            level.destroyBlockProgress(-1, pos, (int) (breakProgress * 10D));
            return;
        }
        harvest(pos, state);
        wallAdjacentLiquids(pos);
        breakProgress = 0D;
    }

    private void harvest(BlockPos pos, BlockState state) {
        var server = (ServerLevel) level;
        var input = new ItemStack(state.getBlock());
        var refined = refinedProducts(input, server);
        if (refined != null) {
            for (var product : refined) spawnProduct(pos, product);
        } else {
            var tool = new ItemStack(Items.DIAMOND_PICKAXE);
            int fortune = upgrades.getLevel(UpgradeType.FORTUNE);
            if (fortune > 0)
                tool.enchant(
                        server.registryAccess()
                                .lookupOrThrow(Registries.ENCHANTMENT)
                                .getOrThrow(Enchantments.FORTUNE),
                        fortune);
            var products =
                    Block.getDrops(state, server, pos, level.getBlockEntity(pos), null, tool);
            for (var product : products) spawnProduct(pos, product);
        }
        level.destroyBlock(pos, false);
        collectAt(pos);
        if ((specialEffects & SCREAM) != 0) {
            WorldSounds.machine(
                    level, null, pos, ModSounds.BLOCK_SCREM.get(), SoundSource.BLOCKS, 2000F, 1F);
        }
    }

    private ItemStack[] refinedProducts(ItemStack input, ServerLevel server) {
        if (input.isEmpty()) return null;
        return switch (refining) {
            case NONE -> null;
            case CRYSTALLIZE -> {
                var recipe = CrystallizerRecipes.INSTANCE.getOutput(input, NTMFluids.PEROXIDE);
                if (recipe == null)
                    recipe = CrystallizerRecipes.INSTANCE.getOutput(input, NTMFluids.SULFURIC_ACID);
                var output = recipe == null ? ItemStack.EMPTY : recipe.output().copy();
                yield output.isEmpty() ? null : new ItemStack[] {output};
            }
            case CENTRIFUGE -> {
                var outputs = CentrifugeRecipes.INSTANCE.getOutputs(input, level);
                boolean hasProduct = false;
                if (outputs != null) {
                    for (var output : outputs) {
                        if (!output.isEmpty()) {
                            hasProduct = true;
                            break;
                        }
                    }
                }
                yield hasProduct ? outputs : null;
            }
            case SHRED -> {
                var output = ShredderRecipes.shred(input);
                yield output.isEmpty() || output.is(ModItems.SCRAP.get())
                        ? null
                        : new ItemStack[] {output};
            }
            case SMELT -> {
                var recipeInput = new SingleRecipeInput(input);
                var recipe = smelting.getRecipeFor(recipeInput, server).orElse(null);
                if (recipe == null || !RecipeInputState.accepts(recipe.value().input(), input))
                    yield null;
                var output = recipe.value().assemble(recipeInput);
                yield output.isEmpty() ? null : new ItemStack[] {output.copy()};
            }
        };
    }

    private void spawnProduct(BlockPos pos, ItemStack product) {
        if (product.isEmpty()) return;
        level.addFreshEntity(
                new ItemEntity(
                        level,
                        pos.getX() + 0.5D,
                        pos.getY() + 0.5D,
                        pos.getZ() + 0.5D,
                        product.copy()));
    }

    private void collectAt(BlockPos cut) {
        double x = cut.getX() + 0.5D;
        double y = cut.getY() + 0.5D;
        double z = cut.getZ() + 0.5D;
        var items =
                level.getEntitiesOfClass(
                        ItemEntity.class, new AABB(x - 3D, y - 1D, z - 3D, x + 3D, y + 1D, z + 3D));
        for (var entity : items) {
            if (entity.isRemoved()) continue;
            var stack = entity.getItem();
            if ((specialEffects & NULLIFIER) != 0 && stack.is(WORTHLESS)) {
                entity.discard();
                continue;
            }
            var block = Block.byItem(stack.getItem());
            var reserve = FluidReserve.of(block);
            if (reserve != null) {
                int amount = reserve.blockFluidAmount();
                if (amount > 0) collectFluid(reserve.primaryFluid(), amount);
                entity.discard();
                continue;
            }
            if (block == ModBlocks.ORE_OIL.get()) {
                collectFluid(NTMFluids.OIL, OIL_PER_ORE);
                entity.discard();
                continue;
            }
            int before = stack.getCount();
            var remainder =
                    InventoryUtil.tryAddItemToInventory(
                            inventory, SLOT_OUTPUT_START, SLOT_OUTPUT_END, stack.copy());
            if (remainder.getCount() != before) setChanged();
            if (remainder.isEmpty()) entity.discard();
            else if (remainder.getCount() != before) entity.setItem(remainder);
        }
        for (var entity :
                level.getEntitiesOfClass(
                        LivingEntity.class,
                        new AABB(x - 1D, y - 1D, z - 1D, x + 1D, y + 1D, z + 1D),
                        EntitySelector.NO_SPECTATORS)) {
            entity.igniteForSeconds(5F);
        }
    }

    private void collectFluid(Fluid fluid, int amount) {
        tank.setTankType(fluid);
        tank.receive(fluid, amount);
        setChanged();
    }

    private void clearLiquidAndWall(BlockPos pos) {
        if (!level.getBlockState(pos).liquid()) return;
        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        wallAdjacentLiquids(pos);
    }

    private void wallAdjacentLiquids(BlockPos pos) {
        for (var direction : Direction.VALUES) {
            var neighbor = pos.relative(direction);
            if (level.getBlockState(neighbor).liquid()) {
                level.setBlockAndUpdate(neighbor, ModBlocks.BARRICADE.get().defaultBlockState());
            }
        }
    }

    private void unloadItems() {
        for (int contact = 0; contact < CONTACT_DIRECTIONS.length; contact++) {
            if (!unloadTargets[contact]) continue;
            var direction = CONTACT_DIRECTIONS[contact];
            var destination = worldPosition.relative(direction, 2);
            if (ChunkUtil.chunkIfLoaded(level, destination) == null) continue;
            var store = Caps.findItemStore(level, destination, direction.getOpposite());
            if (store == null) continue;
            for (int slot = SLOT_OUTPUT_START; slot <= SLOT_OUTPUT_END; slot++) {
                var stack = inventory.get(slot);
                if (stack.isEmpty()) continue;
                var remainder = Caps.insert(store, stack);
                if (remainder.getCount() == stack.getCount()) continue;
                inventory.set(slot, remainder);
                setChanged();
                break;
            }
        }
    }

    public void setOn(boolean enabled) {
        if (isOn == enabled) return;
        hbm$syncSet$MiningLaserBlockEntity$isOn(enabled);
        setChanged();
    }

    public void toggleFromControl() {
        setOn(!isOn);
    }

    public int getWidth() {
        int radius = 1;
        for (int slot = SLOT_UPGRADE_START; slot <= SLOT_UPGRADE_END; slot++) {
            radius += 2 * MachineUpgradeItem.getLevel(inventory.get(slot), UpgradeType.EFFECT);
        }
        return 1 + 2 * Math.min(radius, 25);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return OUTPUT_SLOTS;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot >= SLOT_OUTPUT_START && slot <= SLOT_OUTPUT_END;
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public long getMaxPower() {
        return MAX_POWER;
    }

    @Override
    public void setPower(long stored) {
        hbm$syncSet$MiningLaserBlockEntity$power(Math.clamp(stored, 0L, MAX_POWER));
    }

    @Override
    public int tankCount() {
        return 1;
    }

    @Override
    public FluidTankNTM tank(int index) {
        return sendingTanks[index];
    }

    @Override
    public FluidTankNTM[] getSendingTanks() {
        return sendingTanks;
    }

    @Override
    public void declareFlush(FlushLanes out) {
        out.add(tank, FlushFaces.own());
    }

    @Override
    public UpgradeLimits upgradeCaps() {
        return UPGRADE_CAPS;
    }

    @Override
    public void appendUpgradeInfo(UpgradeType type, int tier, List<Component> tooltip) {
        switch (type) {
            case SPEED ->
                    UpgradeInfo.speedAndPower(tooltip, 100 / (tier + 1) - 100, 100 * tier / 16);
            case POWER ->
                    UpgradeInfo.percent(tooltip, "upgrade.consumption", -100 * tier / 16, true);
            case EFFECT -> UpgradeInfo.range(tooltip, 2 * tier);
            case FORTUNE ->
                    tooltip.add(
                            Component.translatable("upgrade.fortune", "+" + tier)
                                    .withStyle(ChatFormatting.GREEN));
            default -> {}
        }
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.miningLaser");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new MiningLaserMenu(id, playerInventory, this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        hbm$syncSet$MiningLaserBlockEntity$power(input.getLongOr("power", power));
        hbm$syncSet$MiningLaserBlockEntity$isOn(input.getBooleanOr("isOn", false));
        input.child("oil").ifPresent(tank::deserialize);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("power", power);
        output.putBoolean("isOn", isOn);
        tank.serialize(output.child("oil"));
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 4095L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeLong(power);
            case 1 -> output.writeInt(lastTargetX);
            case 2 -> output.writeInt(lastTargetY);
            case 3 -> output.writeInt(lastTargetZ);
            case 4 -> output.writeInt(targetX);
            case 5 -> output.writeInt(targetY);
            case 6 -> output.writeInt(targetZ);
            case 7 -> output.writeBoolean(beam);
            case 8 -> output.writeBoolean(isOn);
            case 9 -> output.writeDouble(clientBreakProgress);
            case 10 -> tank.packetSerialize(output);
            case 11 -> output.writeBoolean(redstonePower);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> hbm$syncSet$MiningLaserBlockEntity$power(input.readLong());
            case 1 -> hbm$syncSet$MiningLaserBlockEntity$lastTargetX(input.readInt());
            case 2 -> hbm$syncSet$MiningLaserBlockEntity$lastTargetY(input.readInt());
            case 3 -> hbm$syncSet$MiningLaserBlockEntity$lastTargetZ(input.readInt());
            case 4 -> hbm$syncSet$MiningLaserBlockEntity$targetX(input.readInt());
            case 5 -> hbm$syncSet$MiningLaserBlockEntity$targetY(input.readInt());
            case 6 -> hbm$syncSet$MiningLaserBlockEntity$targetZ(input.readInt());
            case 7 -> hbm$syncSet$MiningLaserBlockEntity$beam(input.readBoolean());
            case 8 -> hbm$syncSet$MiningLaserBlockEntity$isOn(input.readBoolean());
            case 9 -> hbm$syncSet$MiningLaserBlockEntity$clientBreakProgress(input.readDouble());
            case 10 -> tank.packetDeserialize(input);
            case 11 -> hbm$syncSet$MiningLaserBlockEntity$redstonePower(input.readBoolean());
            default -> super.readSyncUnit(unit, input);
        }
    }

    public final void hbm$syncSet$MiningLaserBlockEntity$power(long next) {
        if (this.power != next) {
            this.power = next;
            if ((1L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L);
        }
    }

    public final void hbm$syncSet$MiningLaserBlockEntity$lastTargetX(int next) {
        if (this.lastTargetX != next) {
            this.lastTargetX = next;
            if ((2L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 2L);
        }
    }

    public final void hbm$syncSet$MiningLaserBlockEntity$lastTargetY(int next) {
        if (this.lastTargetY != next) {
            this.lastTargetY = next;
            if ((4L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 4L);
        }
    }

    public final void hbm$syncSet$MiningLaserBlockEntity$lastTargetZ(int next) {
        if (this.lastTargetZ != next) {
            this.lastTargetZ = next;
            if ((8L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 8L);
        }
    }

    public final void hbm$syncSet$MiningLaserBlockEntity$targetX(int next) {
        if (this.targetX != next) {
            this.targetX = next;
            if ((16L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 16L);
        }
    }

    public final void hbm$syncSet$MiningLaserBlockEntity$targetY(int next) {
        if (this.targetY != next) {
            this.targetY = next;
            if ((32L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 32L);
        }
    }

    public final void hbm$syncSet$MiningLaserBlockEntity$targetZ(int next) {
        if (this.targetZ != next) {
            this.targetZ = next;
            if ((64L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 64L);
        }
    }

    public final void hbm$syncSet$MiningLaserBlockEntity$beam(boolean next) {
        if (this.beam != next) {
            this.beam = next;
            if ((128L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 128L);
        }
    }

    public final void hbm$syncSet$MiningLaserBlockEntity$isOn(boolean next) {
        if (this.isOn != next) {
            this.isOn = next;
            if ((256L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 256L);
        }
    }

    public final void hbm$syncSet$MiningLaserBlockEntity$clientBreakProgress(double next) {
        if (Double.doubleToRawLongBits(this.clientBreakProgress)
                != Double.doubleToRawLongBits(next)) {
            this.clientBreakProgress = next;
            if ((512L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 512L);
        }
    }

    public final void hbm$syncSet$MiningLaserBlockEntity$redstonePower(boolean next) {
        if (this.redstonePower != next) {
            this.redstonePower = next;
            if ((2048L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 2048L);
        }
    }

    static {
        com.hbm.packet.SyncBindings.field(MiningLaserBlockEntity.class, "tank", 3, 1024L);
        com.hbm.inventory.container.SyncedData.fields(MiningLaserBlockEntity.class, "power");
    }
}
