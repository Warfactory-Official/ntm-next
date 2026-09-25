// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.handler.pollution.PollutionHandler;
import com.hbm.handler.pollution.PollutionType;
import com.hbm.inventory.container.MenuFurnaceIron;
import com.hbm.items.machine.upgrade.UpgradeManager;
import com.hbm.items.machine.upgrade.UpgradeType;
import com.hbm.modules.ModuleBurnTime;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.tileentity.Audible;
import com.hbm.tileentity.BlockEntitySmeltingFurnace;
import com.hbm.tileentity.IUpgradeInfoProvider;
import com.hbm.util.TickPhase;
import io.netty.buffer.ByteBuf;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class BlockEntityFurnaceIron extends BlockEntitySmeltingFurnace
        implements Audible, MenuProvider, IUpgradeInfoProvider, SyncUnitSchema {

    public static final int SLOT_INPUT = 0;
    public static final int SLOT_FUEL_A = 1;
    public static final int SLOT_FUEL_B = 2;
    public static final int SLOT_OUTPUT = 3;
    public static final int SLOT_UPGRADE = 4;
    public static final int SLOT_COUNT = 5;

    public static final int BASE_TIME = 160;

    private static final int[] ACCESSIBLE_SLOTS = {
        SLOT_INPUT, SLOT_FUEL_A, SLOT_FUEL_B, SLOT_OUTPUT
    };
    private static final int[] VALID_UPGRADES =
            IUpgradeInfoProvider.upgradeCaps(UpgradeType.SPEED, 3);

    public final ModuleBurnTime burnModule =
            new ModuleBurnTime()
                    .setLigniteTimeMod(1.25)
                    .setCoalTimeMod(1.25)
                    .setCokeTimeMod(1.5)
                    .setSolidTimeMod(2)
                    .setRocketTimeMod(2)
                    .setBalefireTimeMod(2);
    private final UpgradeManager upgradeManager = new UpgradeManager(this);
    private final RecipeManager.CachedCheck<SingleRecipeInput, SmeltingRecipe> quickCheck =
            RecipeManager.createCheck(RecipeType.SMELTING);

    @SyncField(units = 1L << 0)
    public int maxBurnTime;

    @SyncField(units = 1L << 1)
    public int burnTime;

    @SyncField(units = 1L << 4)
    public boolean wasOn = false;

    @SyncField(units = 1L << 2)
    public int progress;

    @SyncField(units = 1L << 3)
    public int processingTime = BASE_TIME;

    public BlockEntityFurnaceIron(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FURNACE_IRON.get(), pos, state, SLOT_COUNT, SLOT_OUTPUT);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.furnaceIron");
    }

    @Override
    public int[] getValidUpgrades() {
        return VALID_UPGRADES;
    }

    @Override
    public void tickServer() {
        upgradeManager.scan(this, SLOT_UPGRADE, SLOT_UPGRADE);

        this.processingTime =
                BASE_TIME - ((BASE_TIME / 2) * upgradeManager.getLevel(UpgradeType.SPEED) / 3);

        wasOn = false;

        if (burnTime <= 0) {
            for (int i = SLOT_FUEL_A; i <= SLOT_FUEL_B; i++) {
                ItemStack fuel = inventory.get(i);
                if (fuel.isEmpty()) continue;
                int burn = burnModule.getBurnTime(level, fuel);
                if (burn > 0) {
                    maxBurnTime = burnTime = burn;
                    ItemStackTemplate remainder = fuel.getCraftingRemainder();
                    fuel.shrink(1);
                    if (fuel.isEmpty())
                        inventory.set(i, remainder != null ? remainder.create() : ItemStack.EMPTY);
                    break;
                }
            }
        }

        if (canSmelt()) {
            wasOn = true;
            progress++;
            burnTime--;

            if (progress % 15 == 0 && !isMuffled()) {
                level.playSound(
                        null,
                        worldPosition,
                        SoundEvents.FIRE_AMBIENT,
                        SoundSource.BLOCKS,
                        1.0F,
                        0.5F + level.getRandom().nextFloat() * 0.5F);
            }

            if (progress >= processingTime) {
                RecipeHolder<SmeltingRecipe> recipe = smeltRecipe();
                ItemStack result =
                        recipe.value().assemble(new SingleRecipeInput(inventory.get(SLOT_INPUT)));
                ItemStack out = inventory.get(SLOT_OUTPUT);
                if (out.isEmpty()) inventory.set(SLOT_OUTPUT, result.copy());
                else out.grow(result.getCount());
                inventory.get(SLOT_INPUT).shrink(1);
                recipesUsed.record(0, recipe);
                progress = 0;
                setChanged();
            }
            if (TickPhase.every(this, SharedConstants.TICKS_PER_SECOND)) {
                PollutionHandler.incrementPollution(
                        level, worldPosition, PollutionType.SOOT, PollutionHandler.SOOT_PER_SECOND);
            }
        } else {
            progress = 0;
        }

        networkPackNT(50);
    }

    @Override
    public void tickClient() {
        if (progress <= 0) return;
        Direction dir = coreFacing();
        Direction rot = dir.getClockWise();

        double offset = progress % 2 == 0 ? 1 : 0.5;
        level.addParticle(
                ParticleTypes.SMOKE,
                worldPosition.getX() + 0.5 - dir.getStepX() * offset - rot.getStepX() * 0.1875,
                worldPosition.getY() + 2,
                worldPosition.getZ() + 0.5 - dir.getStepZ() * offset - rot.getStepZ() * 0.1875,
                0.0,
                0.01,
                0.0);

        if (progress % 5 == 0) {
            double rand = level.getRandom().nextDouble();
            level.addParticle(
                    ParticleTypes.FLAME,
                    worldPosition.getX() + 0.5 + dir.getStepX() * 0.25 + rot.getStepX() * rand,
                    worldPosition.getY() + 0.25 + level.getRandom().nextDouble() * 0.25,
                    worldPosition.getZ() + 0.5 + dir.getStepZ() * 0.25 + rot.getStepZ() * rand,
                    0.0,
                    0.0,
                    0.0);
        }
    }

    private Direction coreFacing() {
        return BlockMultiblockCore.coreFacing(getBlockState());
    }

    private @Nullable RecipeHolder<SmeltingRecipe> smeltRecipe() {
        ItemStack in = inventory.get(SLOT_INPUT);
        if (in.isEmpty() || !(level instanceof ServerLevel server)) return null;
        return quickCheck.getRecipeFor(new SingleRecipeInput(in), server).orElse(null);
    }

    private @Nullable ItemStack smeltResult() {
        RecipeHolder<SmeltingRecipe> recipe = smeltRecipe();
        if (recipe == null) return null;
        return recipe.value().assemble(new SingleRecipeInput(inventory.get(SLOT_INPUT)));
    }

    public boolean canSmelt() {
        if (burnTime <= 0) return false;
        ItemStack result = smeltResult();
        if (result == null || result.isEmpty()) return false;
        ItemStack out = inventory.get(SLOT_OUTPUT);
        if (out.isEmpty()) return true;
        if (!ItemStack.isSameItemSameComponents(out, result)) return false;
        return out.getCount() + result.getCount() <= out.getMaxStackSize();
    }

    private boolean hasSmeltingResult(ItemStack stack) {
        if (stack.isEmpty() || !(level instanceof ServerLevel server)) return false;
        return quickCheck.getRecipeFor(new SingleRecipeInput(stack), server).isPresent();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == SLOT_INPUT) return hasSmeltingResult(stack);
        if (slot == SLOT_FUEL_A || slot == SLOT_FUEL_B)
            return burnModule.getBurnTime(level, stack) > 0;
        return false;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ACCESSIBLE_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return slot != SLOT_OUTPUT && slot != SLOT_UPGRADE && canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == SLOT_OUTPUT;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.getInt("maxBurnTime").ifPresent(v -> maxBurnTime = v);
        input.getInt("burnTime").ifPresent(v -> burnTime = v);
        input.getInt("progress").ifPresent(v -> progress = v);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("maxBurnTime", maxBurnTime);
        output.putInt("burnTime", burnTime);
        output.putInt("progress", progress);
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new MenuFurnaceIron(containerId, playerInventory, this);
    }

    @Override
    public long syncUnitMask() {
        return 0x1fL;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeInt(this.maxBurnTime);
            case 1 -> output.writeInt(this.burnTime);
            case 2 -> output.writeInt(this.progress);
            case 3 -> output.writeInt(this.processingTime);
            case 4 -> output.writeBoolean(this.wasOn);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.maxBurnTime = input.readInt();
            case 1 -> this.burnTime = input.readInt();
            case 2 -> this.progress = input.readInt();
            case 3 -> this.processingTime = input.readInt();
            case 4 -> this.wasOn = input.readBoolean();
            default -> throw new IllegalArgumentException();
        }
    }
}
