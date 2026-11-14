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

import com.hbm.api.conveyor.ConveyorBelt;
import com.hbm.api.energymk2.EnergyHandler;
import com.hbm.api.energymk2.ItemEnergyTransfer;
import com.hbm.api.fluidmk2.FluidTankHandler;
import com.hbm.blockentity.BedrockOreBlockEntity;
import com.hbm.blockentity.FluidSettingsHandler;
import com.hbm.blockentity.MachineBlockEntity;
import com.hbm.blockentity.UpgradeInfoProvider;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.gas.GasBlock;
import com.hbm.capability.NtmContracts;
import com.hbm.entity.item.MovingItemEntity;
import com.hbm.inventory.container.ExcavatorMenu;
import com.hbm.inventory.fluid.tank.FluidTankAccess;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.ShredderRecipes;
import com.hbm.items.ModItems;
import com.hbm.items.machine.DrillbitItem.EnumDrillType;
import com.hbm.items.machine.DrillbitItem;
import com.hbm.items.machine.upgrade.UpgradeInfo;
import com.hbm.items.machine.upgrade.UpgradeLimits;
import com.hbm.items.machine.upgrade.UpgradeManager;
import com.hbm.items.machine.upgrade.UpgradeType;
import com.hbm.items.special.BedrockOreBaseItem;
import com.hbm.items.special.BedrockOreSet;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.platform.Caps;
import com.hbm.tags.HbmBlockTags;
import com.hbm.util.ChunkUtil;
import com.hbm.util.InventoryUtil;
import com.hbm.util.TickPhase;
import com.hbm.world.NtmWorldgenFields;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.jspecify.annotations.Nullable;

public final class ExcavatorBlockEntity extends MachineBlockEntity
        implements MenuProvider,
                EnergyHandler,
                FluidTankHandler,
                FluidTankAccess,
                FluidSettingsHandler,
                UpgradeInfoProvider,
                SyncUnitSchema {

    public static final int SLOT_COUNT = 14;
    public static final int SLOT_BATTERY = 0;
    public static final int SLOT_FLUID_ID = 1;
    public static final int SLOT_UPGRADE_START = 2;
    public static final int SLOT_UPGRADE_END = 3;
    public static final int SLOT_DRILL = 4;
    public static final int SLOT_BUFFER_START = 5;
    public static final int SLOT_BUFFER_END = 13;
    public static final long MAX_POWER = 1_000_000L;
    public static final int CONSUMPTION = 10_000;
    public static final int TANK_CAPACITY = 16_000;

    private static final UpgradeLimits UPGRADE_CAPS =
            UpgradeLimits.of(UpgradeType.SPEED, 3)
                    .and(UpgradeType.POWER, 3)
                    .and(UpgradeType.EFFECT, 3);
    private static final TagKey<Block> ORES =
            TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("c", "ores"));
    private static final Direction[] NEIGHBORS = Direction.values();

    public long power;
    public final FluidTankNTM tank = new FluidTankNTM(TANK_CAPACITY);
    private final FluidTankNTM[] tanks = {tank};
    private final UpgradeManager upgrades = new UpgradeManager(this);
    private final BlockPos.MutableBlockPos scanPos = new BlockPos.MutableBlockPos();

    public boolean enableDrill;
    public boolean enableCrusher;
    public boolean enableWalling;
    public boolean enableVeinMiner;
    public boolean enableSilkTouch;
    public boolean operational;
    public int targetDepth;
    public int chuteTimer;

    public float drillRotation;
    public float prevDrillRotation;
    public float drillExtension;
    public float prevDrillExtension;
    public float crusherRotation;
    public float prevCrusherRotation;

    private int workTicks;
    private int radius = 1;
    private int powerConsumption = CONSUMPTION;
    private double drillSpeed = 1D;
    private boolean bedrockMode;

    public ExcavatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.EXCAVATOR.get(), pos, state, SLOT_COUNT);
        tank.bindOwner(this);
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public void setPower(long stored) {
        hbm$syncSet$ExcavatorBlockEntity$power(stored);
    }

    @Override
    public long getMaxPower() {
        return MAX_POWER;
    }

    @Override
    public FluidTankNTM[] getReceivingTanks() {
        return tanks;
    }

    @Override
    public int tankCount() {
        return 1;
    }

    @Override
    public FluidTankNTM tank(int index) {
        return tanks[index];
    }

    @Override
    public FluidTankNTM[] settingsTanks() {
        return tanks;
    }

    @Override
    public FluidTankNTM pasteTank() {
        return tank;
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new ExcavatorMenu(containerId, inventory, this);
    }

    @Override
    public UpgradeLimits upgradeCaps() {
        return UPGRADE_CAPS;
    }

    @Override
    public void appendUpgradeInfo(UpgradeType kind, int level, List<Component> tooltip) {
        switch (kind) {
            case SPEED -> {
                int delay = (int) (100D / (1D + level / 2D)) - 100;
                UpgradeInfo.speedAndPower(tooltip, delay, level * 100);
            }
            case POWER ->
                    UpgradeInfo.percent(
                            tooltip, "upgrade.consumption", 100 / (level + 1) - 100, true);
            case EFFECT -> UpgradeInfo.range(tooltip, level * 2);
            default -> {}
        }
    }

    public @Nullable EnumDrillType getInstalledDrill() {
        return DrillbitItem.typeOf(getItem(SLOT_DRILL));
    }

    public int getPowerConsumption() {
        return powerConsumption;
    }

    public boolean canVeinMine() {
        var drill = getInstalledDrill();
        return drill != null && drill.vein;
    }

    public boolean canSilkTouch() {
        var drill = getInstalledDrill();
        return drill != null && drill.silk;
    }

    private void refreshDrill() {
        upgrades.scan(this, SLOT_UPGRADE_START, SLOT_UPGRADE_END);
        int speed = upgrades.getLevel(UpgradeType.SPEED);
        powerConsumption = CONSUMPTION * (speed + 1) / (upgrades.getLevel(UpgradeType.POWER) + 1);
        radius = 1 + upgrades.getLevel(UpgradeType.EFFECT) * 2;
        var drill = getInstalledDrill();
        drillSpeed = drill == null ? 1D : drill.speed * (1D + speed / 2D);
    }

    @Override
    public void tickServer() {
        refreshDrill();
        if (tank.setType(SLOT_FLUID_ID, SLOT_FLUID_ID, inventory)) setChanged();
        if (TickPhase.every(this, 20)) ejectBuffer();
        if (chuteTimer > 0) hbm$syncSet$ExcavatorBlockEntity$chuteTimer((int) (chuteTimer - 1));
        hbm$syncSet$ExcavatorBlockEntity$power(
                (long)
                        (power
                                + (ItemEnergyTransfer.extract(
                                        this, SLOT_BATTERY, MAX_POWER - power, false))));
        hbm$syncSet$ExcavatorBlockEntity$operational(false);
        if (enableDrill && getInstalledDrill() != null && power >= powerConsumption) {
            hbm$syncSet$ExcavatorBlockEntity$operational(true);
            hbm$syncSet$ExcavatorBlockEntity$power((long) (power - (powerConsumption)));
            setChanged();
            excavateLayer();
        } else if (targetDepth != 0) {
            hbm$syncSet$ExcavatorBlockEntity$targetDepth(0);
            setChanged();
        }
        networkPackNT(150);
    }

    @Override
    public void tickClient() {
        refreshDrill();
        prevDrillExtension = drillExtension;
        float difference = targetDepth - drillExtension;
        float step = Math.max(0.15F, Math.abs(difference) / 10F);
        if (Math.abs(difference) <= step) drillExtension = targetDepth;
        else drillExtension += Math.signum(difference) * step;
        prevDrillRotation = drillRotation;
        prevCrusherRotation = crusherRotation;
        if (!operational) return;
        drillRotation += 15F;
        if (drillRotation >= 360F) {
            drillRotation -= 360F;
            prevDrillRotation -= 360F;
        }
        if (enableCrusher) {
            crusherRotation += 15F;
            if (crusherRotation >= 360F) {
                crusherRotation -= 360F;
                prevCrusherRotation -= 360F;
            }
        }
    }

    public int drillY() {
        return worldPosition.getY() - targetDepth - 4;
    }

    public BlockPos chutePos() {
        return worldPosition.relative(facing(), 4).below(3);
    }

    private Direction facing() {
        return getBlockState().getValue(HorizontalDirectionalBlock.FACING);
    }

    private void excavateLayer() {
        int y = drillY();
        int reach = targetDepth == 0 || y == level.getMinY() ? 1 : radius;
        bedrockMode = false;
        var cursor = scanPos;
        for (int ring = 1; ring <= reach; ring++) {
            float hardness = 0F;
            boolean hasWork = false;
            BlockPos node = null;
            for (int x = worldPosition.getX() - ring; x <= worldPosition.getX() + ring; x++) {
                for (int z = worldPosition.getZ() - ring; z <= worldPosition.getZ() + ring; z++) {
                    if (!inRing(x, z, ring)) continue;
                    cursor.set(x, y, z);
                    BlockState state = level.getBlockState(cursor);
                    if (state.is(HbmBlockTags.DEPTH_ROCK)) disarm();
                    if (state.is(ModBlocks.ORE_BEDROCK.get())) {
                        node = cursor.immutable();
                        hardness = 6_000F;
                        hasWork = true;
                        bedrockMode = true;
                        if (enableCrusher) {
                            hbm$syncSet$ExcavatorBlockEntity$enableCrusher(false);
                            setChanged();
                        }
                        break;
                    }
                    if (!shouldIgnoreBlock(cursor, state)) {
                        hardness += state.getDestroySpeed(level, cursor);
                        hasWork = true;
                    }
                }
            }
            if (hasWork) {
                int required = (int) Math.ceil(hardness / drillSpeed);
                boolean alreadyComplete = workTicks >= required;
                if (!alreadyComplete) workTicks++;
                if (workTicks < required) return;
                if (node != null) {
                    workTicks = required;
                    if (alreadyComplete && !TickPhase.every(this, 20)) return;
                    if (extractBedrock(node)) workTicks = 0;
                    return;
                }
                mineRing(ring, y);
                wallSquare(ring + 1, y);
                if (ring == reach) mineOuterOres(ring + 1, y);
                collectLooseItems(ring + 1, y);
                workTicks = 0;
                return;
            }
            collectLooseItems(ring + 1, y);
        }
        wallSquare(reach + 1, y);
        workTicks = 0;
        hbm$syncSet$ExcavatorBlockEntity$targetDepth((int) (targetDepth + 1));
        setChanged();
        if (targetDepth > worldPosition.getY() - 4 - level.getMinY() && !bedrockMode) disarm();
    }

    private boolean inRing(int x, int z, int ring) {
        return ring == 1
                || Math.abs(x - worldPosition.getX()) == ring
                || Math.abs(z - worldPosition.getZ()) == ring;
    }

    private void disarm() {
        if (!enableDrill) return;
        hbm$syncSet$ExcavatorBlockEntity$enableDrill(false);
        setChanged();
    }

    public boolean shouldIgnoreBlock(BlockPos pos, BlockState state) {
        return state.isAir()
                || state.getBlock() instanceof GasBlock
                || state.liquid()
                || state.is(Blocks.BEDROCK)
                || state.getDestroySpeed(level, pos) < 0F;
    }

    private void mineRing(int ring, int y) {
        var cursor = scanPos;
        for (int x = worldPosition.getX() - ring; x <= worldPosition.getX() + ring; x++) {
            for (int z = worldPosition.getZ() - ring; z <= worldPosition.getZ() + ring; z++) {
                if (!inRing(x, z, ring)) continue;
                cursor.set(x, y, z);
                tryMineAtLocation(cursor);
            }
        }
    }

    private void mineOuterOres(int ring, int y) {
        var cursor = scanPos;
        for (int x = worldPosition.getX() - ring; x <= worldPosition.getX() + ring; x++) {
            for (int z = worldPosition.getZ() - ring; z <= worldPosition.getZ() + ring; z++) {
                if (!inRing(x, z, ring)) continue;
                cursor.set(x, y, z);
                if (level.getBlockState(cursor).is(ORES)) tryMineAtLocation(cursor);
            }
        }
    }

    public void tryMineAtLocation(BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (shouldIgnoreBlock(pos, state) || state.is(ModBlocks.ORE_BEDROCK.get())) return;
        if (enableVeinMiner && canVeinMine() && state.is(ORES)) {
            mineVein(pos.immutable(), state.getBlock());
        } else {
            breakBlock(pos, state);
        }
    }

    private ItemStack lootTool() {
        var tool = new ItemStack(Items.NETHERITE_PICKAXE);
        var drill = getInstalledDrill();
        var enchantments = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        if (enableSilkTouch && drill.silk)
            tool.enchant(enchantments.getOrThrow(Enchantments.SILK_TOUCH), 1);
        else if (drill.fortune > 0)
            tool.enchant(enchantments.getOrThrow(Enchantments.FORTUNE), drill.fortune);
        return tool;
    }

    private void breakBlock(BlockPos pos, BlockState state) {
        if (!state.is(ModBlocks.BARRICADE.get())) {
            List<ItemStack> drops =
                    Block.getDrops(
                            state,
                            (ServerLevel) level,
                            pos,
                            level.getBlockEntity(pos),
                            null,
                            lootTool());
            if (enableCrusher) drops = crush(drops);
            for (var drop : drops) {
                if (!drop.isEmpty())
                    level.addFreshEntity(
                            new ItemEntity(
                                    level,
                                    pos.getX() + 0.5D,
                                    pos.getY() + 0.5D,
                                    pos.getZ() + 0.5D,
                                    drop));
            }
        }
        level.destroyBlock(pos, false);
    }

    public static List<ItemStack> crush(List<ItemStack> drops) {
        var products = new ArrayList<ItemStack>();
        for (var drop : drops) {
            var recipe = ShredderRecipes.getRecipe(drop);
            if (recipe == null) {
                mergeProduct(products, drop.copy());
                continue;
            }
            for (int consumed = 0; consumed < drop.getCount(); consumed++) {
                ItemStack product = recipe.roll();
                if (product.isEmpty()
                        || product.is(ModItems.SCRAP.get())
                        || product.is(ModItems.DUST.get())) {
                    product = drop.copyWithCount(1);
                }
                mergeProduct(products, product);
            }
        }
        return products;
    }

    private static void mergeProduct(List<ItemStack> products, ItemStack addition) {
        for (var product : products) {
            if (!ItemStack.isSameItemSameComponents(product, addition)) continue;
            product.grow(addition.getCount());
            return;
        }
        products.add(addition);
    }

    private void mineVein(BlockPos origin, Block identity) {
        var visited = new LongOpenHashSet();
        int[] bounds = {
            origin.getX(), origin.getY(), origin.getZ(), origin.getX(), origin.getY(), origin.getZ()
        };
        followVein(origin, identity, 0, visited, bounds);
        var box =
                new AABB(
                        bounds[0],
                        bounds[1],
                        bounds[2],
                        bounds[3] + 1D,
                        bounds[4] + 1D,
                        bounds[5] + 1D);
        for (var entity : level.getEntitiesOfClass(ItemEntity.class, box)) {
            entity.setPos(origin.getX() + 0.5D, origin.getY() + 0.5D, origin.getZ() + 0.5D);
        }
    }

    private void followVein(
            BlockPos pos, Block identity, int depth, LongOpenHashSet visited, int[] bounds) {
        if (depth > 10 || !visited.add(pos.asLong())) return;
        BlockState state = level.getBlockState(pos);
        if (!state.is(identity) || shouldIgnoreBlock(pos, state)) return;
        bounds[0] = Math.min(bounds[0], pos.getX());
        bounds[1] = Math.min(bounds[1], pos.getY());
        bounds[2] = Math.min(bounds[2], pos.getZ());
        bounds[3] = Math.max(bounds[3], pos.getX());
        bounds[4] = Math.max(bounds[4], pos.getY());
        bounds[5] = Math.max(bounds[5], pos.getZ());
        for (var direction : NEIGHBORS)
            followVein(pos.relative(direction), identity, depth + 1, visited, bounds);
        breakBlock(pos, state);
        if (enableWalling)
            level.setBlockAndUpdate(pos, ModBlocks.BARRICADE.get().defaultBlockState());
    }

    private void wallSquare(int reach, int y) {
        var cursor = scanPos;
        for (int x = worldPosition.getX() - reach; x <= worldPosition.getX() + reach; x++) {
            for (int z = worldPosition.getZ() - reach; z <= worldPosition.getZ() + reach; z++) {
                cursor.set(x, y, z);
                BlockState state = level.getBlockState(cursor);
                if (inRing(x, z, reach)) {
                    if (state.liquid() || enableWalling && state.canBeReplaced()) {
                        level.setBlockAndUpdate(
                                cursor, ModBlocks.BARRICADE.get().defaultBlockState());
                    }
                } else if (state.liquid()) {
                    level.setBlockAndUpdate(cursor, Blocks.AIR.defaultBlockState());
                }
            }
        }
    }

    private boolean extractBedrock(BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof BedrockOreBlockEntity node)) return true;
        var drill = getInstalledDrill();
        if (node.resource.isEmpty() || drill.tier < node.tier) return true;
        if (node.acid != null && !node.acid.matches(tank)) return true;

        ItemStack product = node.resource.copy();
        if (product.getItem() instanceof BedrockOreBaseItem) {
            BedrockOreBaseItem.setOreAmount(
                    product,
                    NtmWorldgenFields.get((ServerLevel) level).bedrock(),
                    BedrockOreSet.OF.get().apply(level),
                    pos.getX(),
                    pos.getZ(),
                    1D + drill.fortune * 0.1D);
        }

        BlockPos chute = chutePos();
        BlockState outletState = ChunkUtil.blockStateIfLoaded(level, chute);
        ConveyorBelt belt =
                outletState == null
                        ? null
                        : NtmContracts.CONVEYOR_BELT.at(level, chute, outletState);
        ResourceHandler<ItemResource> store =
                outletState == null
                        ? null
                        : Caps.findItemStore(level, chute, facing().getOpposite());
        ItemStack remainder;
        if (store != null) {
            remainder =
                    Caps.insertIf(
                            store,
                            product,
                            refused ->
                                    refused.isEmpty()
                                            || belt != null
                                            || InventoryUtil.doesArrayHaveSpace(
                                                    inventory,
                                                    SLOT_BUFFER_START,
                                                    SLOT_BUFFER_END,
                                                    new ItemStack[] {refused}));
            if (remainder == null) return false;
            hbm$syncSet$ExcavatorBlockEntity$chuteTimer(40);
        } else {
            remainder = product;
            if (belt == null
                    && !InventoryUtil.doesArrayHaveSpace(
                            inventory,
                            SLOT_BUFFER_START,
                            SLOT_BUFFER_END,
                            new ItemStack[] {remainder})) return false;
        }

        if (node.acid != null) {
            node.acid.drainFrom(tank);
            setChanged();
        }
        if (!remainder.isEmpty()) {
            if (belt != null) {
                sendRider(chute, belt, remainder);
            } else {
                InventoryUtil.tryAddItemToInventory(
                        inventory, SLOT_BUFFER_START, SLOT_BUFFER_END, remainder);
                hbm$syncSet$ExcavatorBlockEntity$chuteTimer(40);
                setChanged();
            }
        }
        return true;
    }

    private void ejectBuffer() {
        List<ItemStack> outputs = null;
        for (int slot = SLOT_BUFFER_START; slot <= SLOT_BUFFER_END; slot++) {
            ItemStack stack = inventory.get(slot);
            if (stack.isEmpty()) continue;
            if (outputs == null) outputs = new ArrayList<>(SLOT_BUFFER_END - SLOT_BUFFER_START + 1);
            outputs.add(stack.copy());
        }
        if (outputs == null) return;
        exportStacks(outputs);
        int output = 0;
        boolean changed = false;
        for (int slot = SLOT_BUFFER_START; slot <= SLOT_BUFFER_END; slot++) {
            while (output < outputs.size() && outputs.get(output).isEmpty()) output++;
            ItemStack replacement =
                    output == outputs.size() ? ItemStack.EMPTY : outputs.get(output++);
            if (ItemStack.matches(inventory.get(slot), replacement)) continue;
            inventory.set(slot, replacement);
            changed = true;
        }
        if (changed) setChanged();
    }

    private void exportStacks(List<ItemStack> outputs) {
        BlockPos chute = chutePos();
        BlockState outletState = ChunkUtil.blockStateIfLoaded(level, chute);
        if (outletState == null) return;
        ResourceHandler<ItemResource> store =
                Caps.findItemStore(level, chute, facing().getOpposite());
        if (store != null) {
            for (int index = 0; index < outputs.size(); index++) {
                ItemStack stack = outputs.get(index);
                if (stack.isEmpty()) continue;
                outputs.set(index, Caps.insert(store, stack));
                hbm$syncSet$ExcavatorBlockEntity$chuteTimer(40);
            }
        }
        ConveyorBelt belt = NtmContracts.CONVEYOR_BELT.at(level, chute, outletState);
        if (belt == null) return;
        for (int index = 0; index < outputs.size(); index++) {
            ItemStack stack = outputs.get(index);
            if (stack.isEmpty()) continue;
            sendRider(chute, belt, stack);
            outputs.set(index, ItemStack.EMPTY);
        }
    }

    private void sendRider(BlockPos chute, ConveyorBelt belt, ItemStack stack) {
        double x = chute.getX() + level.getRandom().nextDouble();
        double z = chute.getZ() + level.getRandom().nextDouble();
        double y = belt.getSnappingPosition(level, chute, new Vec3(x, chute.getY(), z)).y;
        var rider = new MovingItemEntity(level);
        rider.setPos(x, y, z);
        rider.setItemStack(stack.copy());
        level.addFreshEntity(rider);
        hbm$syncSet$ExcavatorBlockEntity$chuteTimer(40);
    }

    private void collectLooseItems(int reach, int y) {
        var box =
                new AABB(
                        worldPosition.getX() - reach,
                        y - 1D,
                        worldPosition.getZ() - reach,
                        worldPosition.getX() + reach + 1D,
                        y + 2D,
                        worldPosition.getZ() + reach + 1D);
        List<ItemEntity> entities = level.getEntitiesOfClass(ItemEntity.class, box);
        if (entities.isEmpty()) return;
        List<ItemStack> outputs = new ArrayList<>(entities.size());
        for (var entity : entities)
            outputs.add(entity.isRemoved() ? ItemStack.EMPTY : entity.getItem().copy());
        exportStacks(outputs);
        for (int index = 0; index < entities.size(); index++) {
            ItemEntity entity = entities.get(index);
            if (entity.isRemoved()) continue;
            ItemStack remainder = outputs.get(index);
            if (!remainder.isEmpty()) {
                int offered = remainder.getCount();
                remainder =
                        InventoryUtil.tryAddItemToInventory(
                                inventory, SLOT_BUFFER_START, SLOT_BUFFER_END, remainder);
                if (remainder.getCount() != offered) {
                    hbm$syncSet$ExcavatorBlockEntity$chuteTimer(40);
                    setChanged();
                }
            }
            if (remainder.isEmpty()) {
                entity.setPickUpDelay(60);
                entity.discard();
            } else if (!ItemStack.matches(entity.getItem(), remainder)) {
                entity.setItem(remainder);
            }
        }
    }

    public void setControlToggles(
            boolean drill, boolean crusher, boolean walling, boolean vein, boolean silk) {
        if (!(drill || crusher || walling || vein || silk)) return;
        if (drill) hbm$syncSet$ExcavatorBlockEntity$enableDrill(!enableDrill);
        if (crusher) hbm$syncSet$ExcavatorBlockEntity$enableCrusher(!enableCrusher);
        if (walling) hbm$syncSet$ExcavatorBlockEntity$enableWalling(!enableWalling);
        if (vein) hbm$syncSet$ExcavatorBlockEntity$enableVeinMiner(!enableVeinMiner);
        if (silk) hbm$syncSet$ExcavatorBlockEntity$enableSilkTouch(!enableSilkTouch);
        setChanged();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("d", enableDrill);
        output.putBoolean("c", enableCrusher);
        output.putBoolean("w", enableWalling);
        output.putBoolean("v", enableVeinMiner);
        output.putBoolean("s", enableSilkTouch);
        output.putInt("t", targetDepth);
        output.putLong("power", power);
        tank.serialize(output.child("tank"));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        hbm$syncSet$ExcavatorBlockEntity$enableDrill(input.getBooleanOr("d", false));
        hbm$syncSet$ExcavatorBlockEntity$enableCrusher(input.getBooleanOr("c", false));
        hbm$syncSet$ExcavatorBlockEntity$enableWalling(input.getBooleanOr("w", false));
        hbm$syncSet$ExcavatorBlockEntity$enableVeinMiner(input.getBooleanOr("v", false));
        hbm$syncSet$ExcavatorBlockEntity$enableSilkTouch(input.getBooleanOr("s", false));
        hbm$syncSet$ExcavatorBlockEntity$targetDepth(input.getIntOr("t", 0));
        hbm$syncSet$ExcavatorBlockEntity$power(input.getLongOr("power", power));
        input.child("tank").ifPresent(tank::deserialize);
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 1023L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeBoolean(enableDrill);
            case 1 -> output.writeBoolean(enableCrusher);
            case 2 -> output.writeBoolean(enableWalling);
            case 3 -> output.writeBoolean(enableVeinMiner);
            case 4 -> output.writeBoolean(enableSilkTouch);
            case 5 -> output.writeBoolean(operational);
            case 6 -> output.writeInt(targetDepth);
            case 7 -> output.writeInt(chuteTimer);
            case 8 -> output.writeLong(power);
            case 9 -> tank.packetSerialize(output);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> hbm$syncSet$ExcavatorBlockEntity$enableDrill(input.readBoolean());
            case 1 -> hbm$syncSet$ExcavatorBlockEntity$enableCrusher(input.readBoolean());
            case 2 -> hbm$syncSet$ExcavatorBlockEntity$enableWalling(input.readBoolean());
            case 3 -> hbm$syncSet$ExcavatorBlockEntity$enableVeinMiner(input.readBoolean());
            case 4 -> hbm$syncSet$ExcavatorBlockEntity$enableSilkTouch(input.readBoolean());
            case 5 -> hbm$syncSet$ExcavatorBlockEntity$operational(input.readBoolean());
            case 6 -> hbm$syncSet$ExcavatorBlockEntity$targetDepth(input.readInt());
            case 7 -> hbm$syncSet$ExcavatorBlockEntity$chuteTimer(input.readInt());
            case 8 -> hbm$syncSet$ExcavatorBlockEntity$power(input.readLong());
            case 9 -> tank.packetDeserialize(input);
            default -> super.readSyncUnit(unit, input);
        }
    }

    public final void hbm$syncSet$ExcavatorBlockEntity$power(long next) {
        if (this.power != next) {
            this.power = next;
            if ((1L << 8) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 8);
        }
    }

    public final void hbm$syncSet$ExcavatorBlockEntity$enableDrill(boolean next) {
        if (this.enableDrill != next) {
            this.enableDrill = next;
            if ((1L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L);
        }
    }

    public final void hbm$syncSet$ExcavatorBlockEntity$enableCrusher(boolean next) {
        if (this.enableCrusher != next) {
            this.enableCrusher = next;
            if ((1L << 1) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 1);
        }
    }

    public final void hbm$syncSet$ExcavatorBlockEntity$enableWalling(boolean next) {
        if (this.enableWalling != next) {
            this.enableWalling = next;
            if ((1L << 2) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 2);
        }
    }

    public final void hbm$syncSet$ExcavatorBlockEntity$enableVeinMiner(boolean next) {
        if (this.enableVeinMiner != next) {
            this.enableVeinMiner = next;
            if ((1L << 3) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 3);
        }
    }

    public final void hbm$syncSet$ExcavatorBlockEntity$enableSilkTouch(boolean next) {
        if (this.enableSilkTouch != next) {
            this.enableSilkTouch = next;
            if ((1L << 4) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 4);
        }
    }

    public final void hbm$syncSet$ExcavatorBlockEntity$operational(boolean next) {
        if (this.operational != next) {
            this.operational = next;
            if ((1L << 5) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 5);
        }
    }

    public final void hbm$syncSet$ExcavatorBlockEntity$targetDepth(int next) {
        if (this.targetDepth != next) {
            this.targetDepth = next;
            if ((1L << 6) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 6);
        }
    }

    public final void hbm$syncSet$ExcavatorBlockEntity$chuteTimer(int next) {
        if (this.chuteTimer != next) {
            this.chuteTimer = next;
            if ((1L << 7) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 7);
        }
    }

    static {
        com.hbm.packet.SyncBindings.field(ExcavatorBlockEntity.class, "tank", 3, 1L << 9);
        com.hbm.inventory.container.SyncedData.fields(ExcavatorBlockEntity.class, "power");
    }
}
