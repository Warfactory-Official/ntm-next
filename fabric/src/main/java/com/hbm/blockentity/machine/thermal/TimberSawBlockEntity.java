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

package com.hbm.blockentity.machine.thermal;

import com.hbm.api.tile.HeatHandler;
import com.hbm.api.tile.HeatIntake;
import com.hbm.blockentity.MachineBlockEntity;
import com.hbm.blockentity.PersistentDrop;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.capability.ContractLink;
import com.hbm.capability.NtmContracts;
import com.hbm.entity.projectile.SawbladeEntity;
import com.hbm.handler.threading.TargetPoint;
import com.hbm.inventory.material.MaterialTagRegistry;
import com.hbm.inventory.recipes.loader.RecipeSource;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModItems;
import com.hbm.lib.ModDamageTypes;
import com.hbm.packet.PacketWire;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.packet.layout.BlockDustBurstLayout;
import com.hbm.registration.OverridePoint;
import com.hbm.sound.EntitySounds;
import com.hbm.sound.ModSounds;
import com.hbm.sound.WorldSounds;
import com.hbm.util.Facing;
import com.hbm.util.RegistryUtil;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

public class TimberSawBlockEntity extends MachineBlockEntity
        implements PersistentDrop, SyncUnitSchema {

    public static final int PROCESSING_TIME = 600;
    private static final int[] SLOTS = {0, 1, 2};
    private static final AABB BLADE_AREA = new AABB(-1D, 0.375D, -1D, -0.875D, 2.375D, 1D);
    public static final OverridePoint<CuttingRule> CUTTING =
            OverridePoint.of(RegistryUtil.id("sawmill_cutting"), new CuttingRule() {});

    public interface CuttingRule {
        default @Nullable ItemStack output(ItemStack input) {
            return null;
        }

        default float sawdust(ItemStack input, ItemStack result, float chance) {
            return chance;
        }
    }

    public int heat;
    private int syncHeat;
    public int progress;
    public boolean hasBlade;

    {
        hbm$syncSet$TimberSawBlockEntity$hasBlade(true);
    }

    public float spin;
    public float lastSpin;

    private final ContractLink<HeatHandler> heatBelow =
            new ContractLink<>(NtmContracts.HEAT_SOURCE);
    private final BlockPos below;
    private AABB danger;
    private final RecipeManager.CachedCheck<CraftingInput, CraftingRecipe> crafting =
            RecipeManager.createCheck(RecipeType.CRAFTING);
    private Item memoItem = Items.AIR;
    private int memoTags;
    private RecipeSource.@Nullable Token memoRecipes;
    private @Nullable ItemStack memoOutput;
    private int overspeed;
    private int warningCooldown;

    public TimberSawBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SAWMILL.get(), pos, state, 3);
        below = pos.below();
        updateBladeArea(state);
    }

    @Override
    public void setBlockState(BlockState state) {
        super.setBlockState(state);
        updateBladeArea(state);
    }

    private void updateBladeArea(BlockState state) {
        danger =
                Facing.rotateAabb(
                                BLADE_AREA,
                                state.getValue(MultiblockCoreBlock.FACING).getClockWise())
                        .move(
                                worldPosition.getX() + 0.5D,
                                worldPosition.getY(),
                                worldPosition.getZ() + 0.5D);
    }

    public @Nullable ItemStack getOutput(ItemStack input) {
        if (input.isEmpty() || !input.getComponentsPatch().isEmpty()) return null;
        int tags =
                (input.is(MaterialTagRegistry.KEY_STICK) ? 1 : 0)
                        | (input.is(ItemTags.LOGS) ? 2 : 0)
                        | (input.is(ItemTags.PLANKS) ? 4 : 0)
                        | (input.is(ItemTags.SAPLINGS) ? 8 : 0);
        var recipes = RecipeSource.token();
        if (memoItem != input.getItem() || memoTags != tags || memoRecipes != recipes) {
            memoItem = input.getItem();
            memoTags = tags;
            memoRecipes = recipes;
            memoOutput = CUTTING.get().output(input);
            if (memoOutput == null) memoOutput = woodOutput(input, tags);
        }
        return memoOutput;
    }

    private @Nullable ItemStack woodOutput(ItemStack input, int tags) {
        if ((tags & 1) != 0) return new ItemStack(ModItems.POWDER_SAWDUST.get());
        if ((tags & 2) != 0) {
            var grid = CraftingInput.of(1, 1, List.of(input.copyWithCount(1)));
            var recipe = crafting.getRecipeFor(grid, (ServerLevel) level).orElse(null);
            if (recipe == null) return null;
            var result = recipe.value().assemble(grid);
            if (result.isEmpty()) return null;
            result.setCount(result.getCount() * 6 / 4);
            return result;
        }
        if ((tags & 4) != 0) return new ItemStack(Items.STICK, 6);
        return (tags & 8) != 0 ? new ItemStack(Items.STICK) : null;
    }

    public static float sawdustChance(ItemStack result) {
        return CUTTING.get().sawdust(ItemStack.EMPTY, result, baseSawdustChance(result));
    }

    private static float baseSawdustChance(ItemStack result) {
        return result.is(ModItems.POWDER_SAWDUST.get()) ? 0F : result.is(Items.STICK) ? 0.1F : 0.5F;
    }

    @Override
    public void tickServer() {
        if (hasBlade) {
            heat = HeatIntake.STORED.pull(heatBelow.get(level, below), heat, 0, 0.1D);
            if (warningCooldown > 0) warningCooldown--;
            if (heat >= 100) {
                var input = inventory.get(0);
                var output = getOutput(input);
                if (output == null) {
                    resetProgress();
                } else {
                    hbm$syncSet$TimberSawBlockEntity$progress((int) (progress + (heat / 10)));
                    if (progress >= PROCESSING_TIME) {
                        hbm$syncSet$TimberSawBlockEntity$progress(0);
                        float chance =
                                CUTTING.get().sawdust(input, output, baseSawdustChance(output));
                        inventory.set(0, ItemStack.EMPTY);
                        inventory.set(1, output.copy());
                        if (level.getRandom().nextFloat() < chance)
                            inventory.set(2, new ItemStack(ModItems.POWDER_SAWDUST.get()));
                    }
                    setChanged();
                }
                strikeBlade();
            } else {
                resetProgress();
            }
            if (heat > 300) {
                overspeed++;
                if (overspeed > 60 && warningCooldown == 0) {
                    warningCooldown = 100;
                    WorldSounds.machine(
                            level,
                            null,
                            worldPosition.getX() + 0.5,
                            worldPosition.getY() + 1D,
                            worldPosition.getZ() + 0.5,
                            ModSounds.WARN_OVERSPEED.get(),
                            SoundSource.BLOCKS,
                            2F,
                            1F);
                }
                if (overspeed > 300) shedBlade();
            } else {
                overspeed = 0;
            }
        } else {
            overspeed = 0;
            warningCooldown = 0;
        }
        hbm$syncSet$TimberSawBlockEntity$syncHeat(heat);
        networkPackNT(150);
        heat = 0;
    }

    private void resetProgress() {
        if (progress != 0) {
            hbm$syncSet$TimberSawBlockEntity$progress(0);
            setChanged();
        }
    }

    private void strikeBlade() {
        var server = (ServerLevel) level;
        for (var entity : server.getEntitiesOfClass(LivingEntity.class, danger)) {
            if (entity.isAlive()
                    && entity.hurtServer(
                            server, server.damageSources().source(ModDamageTypes.BLENDER), 100F)) {
                EntitySounds.play(
                        entity,
                        entity.getX(),
                        entity.getY(),
                        entity.getZ(),
                        SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR,
                        SoundSource.BLOCKS,
                        2F,
                        0.95F + server.getRandom().nextFloat() * 0.2F);
                int count = 4 * Math.min((int) Math.ceil(entity.getMaxHealth() / 4F), 250);
                var packet =
                        BlockDustBurstLayout.encode(
                                Block.BLOCK_STATE_REGISTRY.getId(
                                        Blocks.REDSTONE_BLOCK.defaultBlockState()),
                                entity.getX(),
                                entity.getY() + entity.getBbHeight() / 2D,
                                entity.getZ(),
                                count,
                                0.1D);
                PacketWire.sendToAllAround(
                        packet,
                        new TargetPoint(server, entity.getX(), entity.getY(), entity.getZ(), 50));
            }
        }
    }

    private void shedBlade() {
        hbm$syncSet$TimberSawBlockEntity$hasBlade(false);
        double x = worldPosition.getX() + 0.5;
        double y = worldPosition.getY() + 1D;
        double z = worldPosition.getZ() + 0.5;
        level.explode(null, x, y, z, 5F, false, Level.ExplosionInteraction.NONE);
        var facing = getBlockState().getValue(MultiblockCoreBlock.FACING);
        var sideways = facing.getCounterClockWise();
        var blade =
                new SawbladeEntity(level, x + facing.getStepX(), y, z + facing.getStepZ())
                        .setOrientation(facing.get3DDataValue());
        blade.setDeltaMovement(
                sideways.getStepX(), 1D + 0.0001D * (heat - 100), sideways.getStepZ());
        level.addFreshEntity(blade);
        setChanged();
    }

    @Override
    public void tickClient() {
        lastSpin = spin;
        spin += 25F * heat / 300F;
        if (spin >= 360F) {
            spin -= 360F;
            lastSpin -= 360F;
        }
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return SLOTS;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == 0 && isEmpty() && stack.getCount() == 1 && getOutput(stack) != null;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == 1 || slot == 2;
    }

    @Override
    public void writePersistent(DataComponentMap.Builder components) {
        if (!hasBlade) components.set(ModDataComponents.HAS_BLADE.get(), false);
    }

    @Override
    public void readPersistent(DataComponentGetter components) {
        hbm$syncSet$TimberSawBlockEntity$hasBlade(
                components.getOrDefault(ModDataComponents.HAS_BLADE.get(), true));
    }

    @Override
    public String[] persistentKeys() {
        return new String[] {"hasBlade"};
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        hbm$syncSet$TimberSawBlockEntity$hasBlade(input.getBooleanOr("hasBlade", hasBlade));
        hbm$syncSet$TimberSawBlockEntity$progress(input.getIntOr("progress", progress));

        heatBelow.load(input, "heatBelow");
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("hasBlade", hasBlade);
        output.putInt("progress", progress);

        heatBelow.save(output, "heatBelow");
    }

    @Override
    public long syncUnitMask() {
        return 15L;
    }

    @Override
    public boolean initialMatchesSyncUnits() {
        return heat == syncHeat;
    }

    @Override
    public void writeInitialSyncUnit(int unit, ByteBuf output) {
        if (unit == 0) output.writeInt(heat);
        else writeSyncUnit(unit, output);
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeInt(syncHeat);
            case 1 -> output.writeInt(progress);
            case 2 -> output.writeBoolean(hasBlade);
            case 3 -> {
                for (var stack : inventory) {
                    output.writeInt(BuiltInRegistries.ITEM.getId(stack.getItem()));
                    output.writeInt(stack.getCount());
                }
            }
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> {
                heat = input.readInt();
                hbm$syncSet$TimberSawBlockEntity$syncHeat(heat);
            }
            case 1 -> hbm$syncSet$TimberSawBlockEntity$progress(input.readInt());
            case 2 -> hbm$syncSet$TimberSawBlockEntity$hasBlade(input.readBoolean());
            case 3 -> {
                for (int slot = 0; slot < 3; slot++)
                    inventory.set(
                            slot,
                            new ItemStack(
                                    BuiltInRegistries.ITEM.byId(input.readInt()), input.readInt()));
            }
            default -> super.readSyncUnit(unit, input);
        }
    }

    public void refreshHeatBelow() {
        heatBelow.refresh(level, worldPosition.below());
    }

    public final void hbm$syncSet$TimberSawBlockEntity$syncHeat(int next) {
        if (this.syncHeat != next) {
            this.syncHeat = next;
            if ((1L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L);
        }
    }

    public final void hbm$syncSet$TimberSawBlockEntity$progress(int next) {
        if (this.progress != next) {
            this.progress = next;
            if ((2L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 2L);
        }
    }

    public final void hbm$syncSet$TimberSawBlockEntity$hasBlade(boolean next) {
        if (this.hasBlade != next) {
            this.hasBlade = next;
            if ((4L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 4L);
        }
    }

    static {
        com.hbm.packet.SyncInventory.slots(
                TimberSawBlockEntity.class, new int[] {}, 8L, true, false);
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
}
