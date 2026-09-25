// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.api.tile.IHeatSource;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.capability.ContractLink;
import com.hbm.capability.NtmContracts;
import com.hbm.handler.pollution.PollutionHandler;
import com.hbm.handler.pollution.PollutionType;
import com.hbm.inventory.OreDictManager;
import com.hbm.inventory.container.MenuFurnaceSteel;
import com.hbm.packet.SyncArrays;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.tileentity.BlockEntitySmeltingFurnace;
import com.hbm.tileentity.NeighborDerived;
import com.hbm.util.TickPhase;
import io.netty.buffer.ByteBuf;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class BlockEntityFurnaceSteel extends BlockEntitySmeltingFurnace
        implements MenuProvider, SyncUnitSchema {

    public static final int SLOT_COUNT = 6;

    public static final int PROCESS_TIME = 40_000;
    public static final int MAX_HEAT = 100_000;
    public static final double DIFFUSION = 0.05D;

    private static final int[] ACCESSIBLE_SLOTS = {0, 1, 2, 3, 4, 5};
    private static final TagKey<Item> C_ORES =
            TagKey.create(Registries.ITEM, Identifier.parse("c:ores"));

    private final ItemStack[] lastItems = {ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY};
    private final RecipeManager.CachedCheck<SingleRecipeInput, SmeltingRecipe> quickCheck =
            RecipeManager.createCheck(RecipeType.SMELTING);

    @SyncField(units = 1L << 0)
    public int[] progress = new int[3];

    @SyncField(units = 1L << 1)
    public int[] bonus = new int[3];

    @SyncField(units = 1L << 2)
    public int heat;

    @SyncField(units = 1L << 3)
    public boolean wasOn = false;

    @NeighborDerived(at = "below")
    private final ContractLink<IHeatSource> heatBelow =
            new ContractLink<>(NtmContracts.HEAT_SOURCE);

    public BlockEntityFurnaceSteel(BlockPos pos, BlockState state) {

        super(ModBlockEntities.FURNACE_STEEL.get(), pos, state, SLOT_COUNT, 3, 4, 5);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.furnaceSteel");
    }

    @Override
    public void tickServer() {
        tryPullHeat();

        wasOn = false;

        int burn = (heat - MAX_HEAT / 3) / 10;

        for (int i = 0; i < 3; i++) {
            ItemStack in = inventory.get(i);
            if (in.isEmpty()
                    || lastItems[i].isEmpty()
                    || !ItemStack.isSameItemSameComponents(in, lastItems[i])) {
                progress[i] = 0;
                bonus[i] = 0;
            }

            if (canSmelt(i)) {
                progress[i] += burn;
                heat -= burn;
                wasOn = true;
                if (TickPhase.every(this, SharedConstants.TICKS_PER_SECOND)) {
                    PollutionHandler.incrementPollution(
                            level,
                            worldPosition,
                            PollutionType.SOOT,
                            PollutionHandler.SOOT_PER_SECOND * 2);
                }
            }

            lastItems[i] = in.copy();

            if (progress[i] >= PROCESS_TIME) {
                RecipeHolder<SmeltingRecipe> recipe = smeltRecipe(inventory.get(i));
                if (recipe == null) {
                    progress[i] = 0;
                    continue;
                }
                ItemStack result = recipe.value().assemble(new SingleRecipeInput(inventory.get(i)));

                ItemStack out = inventory.get(i + 3);
                if (out.isEmpty()) inventory.set(i + 3, result.copy());
                else out.grow(result.getCount());

                addBonus(inventory.get(i), i);

                while (bonus[i] >= 100) {
                    ItemStack cur = inventory.get(i + 3);
                    cur.setCount(
                            Math.min(cur.getMaxStackSize(), cur.getCount() + result.getCount()));
                    bonus[i] -= 100;
                }

                inventory.get(i).shrink(1);
                recipesUsed.record(i, recipe);
                progress[i] = 0;
                setChanged();
            }
        }

        networkPackNT(50);
    }

    @Override
    public void tickClient() {
        if (!wasOn) return;
        Direction dir = coreFacing();
        Direction rot = dir.getClockWise();

        level.addParticle(
                ParticleTypes.SMOKE,
                worldPosition.getX() + 0.5 - dir.getStepX() * 1.125 - rot.getStepX() * 0.75,
                worldPosition.getY() + 2.625,
                worldPosition.getZ() + 0.5 - dir.getStepZ() * 1.125 - rot.getStepZ() * 0.75,
                0.0,
                0.05,
                0.0);

        if (level.getRandom().nextInt(20) == 0) {
            level.addParticle(
                    ParticleTypes.CLOUD,
                    worldPosition.getX() + 0.5 + dir.getStepX() * 0.75,
                    worldPosition.getY() + 2,
                    worldPosition.getZ() + 0.5 + dir.getStepZ() * 0.75,
                    0.0,
                    0.05,
                    0.0);
        }

        if (level.getRandom().nextInt(15) == 0) {
            level.addParticle(
                    ParticleTypes.LAVA,
                    worldPosition.getX()
                            + 0.5
                            + dir.getStepX() * 1.5
                            + rot.getStepX() * (level.getRandom().nextDouble() - 0.5),
                    worldPosition.getY() + 0.75,
                    worldPosition.getZ()
                            + 0.5
                            + dir.getStepZ() * 1.5
                            + rot.getStepZ() * (level.getRandom().nextDouble() - 0.5),
                    dir.getStepX() * 0.5,
                    0.05,
                    dir.getStepZ() * 0.5);
        }
    }

    private Direction coreFacing() {
        return BlockMultiblockCore.coreFacing(getBlockState());
    }

    private void addBonus(ItemStack stack, int index) {
        if (stack.is(C_ORES)) {
            bonus[index] += 25;
            return;
        }
        if (stack.is(ItemTags.LOGS)) {
            bonus[index] += 50;
            return;
        }
        if (stack.is(OreDictManager.KEY_ANY_TAR)) {
            bonus[index] += 50;
        }
    }

    protected void tryPullHeat() {
        if (heat >= MAX_HEAT) return;

        BlockPos heatPos = worldPosition.below();
        IHeatSource source = heatBelow.get(level, heatPos);
        if (source != null) {
            int diff = source.getHeatStored(level, heatPos) - heat;
            if (diff == 0) return;
            if (diff > 0) {
                diff = (int) Math.ceil(diff * DIFFUSION);
                source.useUpHeat(level, heatPos, diff);
                heat += diff;
                if (heat > MAX_HEAT) heat = MAX_HEAT;
                return;
            }
        }

        heat = Math.max(heat - Math.max(heat / 1000, 1), 0);
    }

    public boolean canSmelt(int index) {
        if (heat < MAX_HEAT / 3) return false;
        ItemStack result = smeltResult(inventory.get(index));
        if (result == null || result.isEmpty()) return false;
        ItemStack out = inventory.get(index + 3);
        if (out.isEmpty()) return true;
        if (!ItemStack.isSameItemSameComponents(out, result)) return false;
        return out.getCount() + result.getCount() <= out.getMaxStackSize();
    }

    private @Nullable RecipeHolder<SmeltingRecipe> smeltRecipe(ItemStack in) {
        if (in.isEmpty() || !(level instanceof ServerLevel server)) return null;
        return quickCheck.getRecipeFor(new SingleRecipeInput(in), server).orElse(null);
    }

    private @Nullable ItemStack smeltResult(ItemStack in) {
        RecipeHolder<SmeltingRecipe> recipe = smeltRecipe(in);
        if (recipe == null) return null;
        return recipe.value().assemble(new SingleRecipeInput(in));
    }

    private boolean hasSmeltingResult(ItemStack stack) {
        return smeltResult(stack) != null;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot < 3 && hasSmeltingResult(stack);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ACCESSIBLE_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return slot < 3 && hasSmeltingResult(stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot > 2;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.getIntArray("progress")
                .ifPresent(
                        a ->
                                SyncArrays.copy(
                                        this, 3, 1L, a, 0, progress, 0, Math.min(a.length, 3)));
        input.getIntArray("bonus")
                .ifPresent(
                        a -> SyncArrays.copy(this, 3, 2L, a, 0, bonus, 0, Math.min(a.length, 3)));
        input.getInt("heat").ifPresent(v -> heat = v);
        input.child("lastItems")
                .ifPresent(
                        list -> {
                            for (int i = 0; i < 3; i++) {
                                lastItems[i] =
                                        list.read("lastItem" + i, ItemStack.CODEC)
                                                .orElse(ItemStack.EMPTY);
                            }
                        });
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putIntArray("progress", progress);
        output.putIntArray("bonus", bonus);
        output.putInt("heat", heat);
        ValueOutput list = output.child("lastItems");
        for (int i = 0; i < 3; i++) {
            if (!lastItems[i].isEmpty()) list.store("lastItem" + i, ItemStack.CODEC, lastItems[i]);
        }
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new MenuFurnaceSteel(containerId, playerInventory, this);
    }

    private void writeProgress(ByteBuf output) {
        for (int value : progress) output.writeInt(value);
    }

    private void readProgress(ByteBuf input) {
        for (int i = 0; i < 3; i++) progress[i] = input.readInt();
    }

    private void writeBonus(ByteBuf output) {
        for (int value : bonus) output.writeInt(value);
    }

    private void readBonus(ByteBuf input) {
        for (int i = 0; i < 3; i++) bonus[i] = input.readInt();
    }

    @Override
    public long syncUnitMask() {
        return 0xfL;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> writeProgress(output);
            case 1 -> writeBonus(output);
            case 2 -> output.writeInt(this.heat);
            case 3 -> output.writeBoolean(this.wasOn);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> readProgress(input);
            case 1 -> readBonus(input);
            case 2 -> this.heat = input.readInt();
            case 3 -> this.wasOn = input.readBoolean();
            default -> throw new IllegalArgumentException();
        }
    }
}
