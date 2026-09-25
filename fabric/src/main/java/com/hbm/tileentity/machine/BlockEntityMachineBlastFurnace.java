// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.api.fluidmk2.FluidFlushOutputs;
import com.hbm.api.fluidmk2.FluidFlushSender;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.inventory.container.MenuMachineBlastFurnace;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.fluid.trait.FluidTrait;
import com.hbm.inventory.recipes.BlastFurnaceRecipe;
import com.hbm.inventory.recipes.BlastFurnaceRecipesNT;
import com.hbm.inventory.recipes.ingredient.CountIngredient;
import com.hbm.modules.ModuleBurnTime;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.tileentity.Audible;
import com.hbm.tileentity.BlockEntityMachineBase;
import com.hbm.tileentity.IFluidCopiable;
import com.hbm.tileentity.Tiltable;
import com.hbm.util.TickPhase;
import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.random.Weighted;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class BlockEntityMachineBlastFurnace extends BlockEntityMachineBase
        implements Audible,
                Tiltable,
                FluidFlushSender,
                MenuProvider,
                IFluidCopiable,
                SyncUnitSchema {

    public static final int SLOT_FUEL = 0;
    public static final int SLOT_INPUT_1 = 1;
    public static final int SLOT_INPUT_2 = 2;
    public static final int SLOT_OUTPUT_1 = 3;
    public static final int SLOT_OUTPUT_2 = 4;
    public static final int SLOT_COUNT = 5;

    public static final int FUEL_COAL = 200 * 8;
    public static final int FUEL_RATE = 200 * 4;
    public static final int MAX_FUEL = FUEL_COAL * 24;
    public static final int FLUE_GAS = 8;

    private static final int[] ACCESSIBLE_SLOTS = {1, 2, 0, 3, 4};
    private static final int[] OUTPUT_SLOTS = {SLOT_OUTPUT_1, SLOT_OUTPUT_2};

    public static Consumer<BlockEntityMachineBlastFurnace> CLIENT_TOWER = be -> {};

    @SyncField(units = 1L << 4)
    public final FluidTankNTM[] tanks = new FluidTankNTM[2];

    private final ModuleBurnTime burnModule = new ModuleBurnTime().setWoodHeatMod(0D);

    @SyncField(units = 1L << 0)
    public int fuel;

    @SyncField(units = 1L << 1)
    public float progress;

    @SyncField(units = 1L << 2)
    public float speed;

    @SyncField(units = 1L << 3)
    public boolean processing;

    private final FluidFlushOutputs flush = new FluidFlushOutputs();
    private final FluidTankNTM[] sending;

    public BlockEntityMachineBlastFurnace(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BLAST_FURNACE.get(), pos, state, SLOT_COUNT);
        tanks[0] = new FluidTankNTM(NTMFluids.AIRBLAST, 4_000);
        tanks[1] = new FluidTankNTM(NTMFluids.FLUE, 1_000);
        sending = new FluidTankNTM[] {tanks[1]};
    }

    private static ItemStack fixedOutput(BlastFurnaceRecipe recipe, int i) {
        return recipe.outputItems()[i].unwrap().get(0).value();
    }

    private static boolean isPool(WeightedList<ItemStack> output) {
        int real = 0;
        for (Weighted<ItemStack> entry : output.unwrap()) if (!entry.value().isEmpty()) real++;
        return real > 1;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.blastFurnace");
    }

    @Override
    public void tickServer() {
        checkTilt(Tiltable.TiltType.CONFIG, false);

        flush.provide((ServerLevel) level, this);

        ItemStack fuelStack = inventory.get(SLOT_FUEL);
        if (!fuelStack.isEmpty()) {
            int capacity = MAX_FUEL - fuel;
            int burnValue = getBurnTime(fuelStack);
            if (burnValue > 0 && burnValue <= capacity) {
                fuel += burnValue;
                fuelStack.shrink(1);
                setChanged();
            }
        }

        this.speed = 0F;
        this.processing = false;
        BlastFurnaceRecipe recipe =
                BlastFurnaceRecipesNT.INSTANCE.getRecipe(
                        inventory.get(SLOT_INPUT_1), inventory.get(SLOT_INPUT_2));

        if (!isTilted()
                && recipe != null
                && fuel >= FUEL_RATE
                && hasQuantities(recipe)
                && canOutput(recipe)) {
            this.speed = Mth.clamp(1F + tanks[0].getFill() * 8F / tanks[0].getMaxFill(), 1F, 5F);
            this.processing = true;
            this.progress += speed / recipe.duration;

            if (progress >= 1F) {
                process(recipe);
                progress = 0F;
                fuel -= FUEL_RATE;
                int flue = (int) (tanks[1].getFill() + FLUE_GAS * (recipe.duration / speed));
                if (flue > tanks[1].getMaxFill()) {
                    FluidTrait.onRelease(
                            level,
                            worldPosition,
                            tanks[1].getTankType(),
                            tanks[1],
                            FluidTrait.FluidReleaseType.SPILL,
                            flue - tanks[1].getMaxFill());
                }
                tanks[1].setFill(flue);
            }

            if (level.getRandom().nextInt(10) == 0 && !isMuffled()) {
                level.playSound(
                        null,
                        worldPosition,
                        SoundEvents.FIRE_AMBIENT,
                        SoundSource.BLOCKS,
                        1.0F,
                        0.5F + level.getRandom().nextFloat() * 0.25F);
            }
        } else {
            this.progress = 0F;
        }

        if (tanks[0].getFill() > 0) tanks[0].setFill((int) (tanks[0].getFill() * 0.95F));

        networkPackNT(100);
    }

    @Override
    public void tickClient() {

        if (!processing) return;
        if (!TickPhase.every(this, 2)) return;
        BlockPos top = worldPosition.above(7);
        if (!level.getBlockState(top).isAir()) return;
        double px = worldPosition.getX() + 0.25 + level.getRandom().nextDouble() * 0.5;
        double pz = worldPosition.getZ() + 0.25 + level.getRandom().nextDouble() * 0.5;
        level.addParticle(ParticleTypes.LAVA, px, worldPosition.getY() + 7.25, pz, 0, 0, 0);

        if (tanks[1].getFill() >= 1_000) CLIENT_TOWER.accept(this);
    }

    private int getBurnTime(ItemStack stack) {

        if (stack.getCraftingRemainder() != null) return 0;

        return burnModule.getBurnHeat(burnModule.getBurnTime(level, stack, 0.0D), stack);
    }

    public List<String> getFuelBonuses() {
        return burnModule.getDesc();
    }

    private boolean hasQuantities(BlastFurnaceRecipe recipe) {
        ItemStack s1 = inventory.get(SLOT_INPUT_1), s2 = inventory.get(SLOT_INPUT_2);
        CountIngredient[] in = recipe.inputItem;
        if (in.length == 1) return in[0].test(s1) || in[0].test(s2);
        if (in[0].test(s1) && in[1].test(s2)) return true;
        return in[0].test(s2) && in[1].test(s1);
    }

    private boolean canOutput(BlastFurnaceRecipe recipe) {
        for (int i = 0; i < recipe.outputItems().length; i++) {
            ItemStack slot = inventory.get(SLOT_OUTPUT_1 + i);
            if (slot.isEmpty()) continue;

            if (isPool(recipe.outputItems()[i])) return false;
            ItemStack out = fixedOutput(recipe, i);
            if (!ItemStack.isSameItemSameComponents(out, slot)) return false;
            if (out.getCount() + slot.getCount() > slot.getMaxStackSize()) return false;
        }
        return true;
    }

    private void process(BlastFurnaceRecipe recipe) {
        for (int i = 0; i < recipe.outputItems().length; i++) {
            ItemStack out = fixedOutput(recipe, i).copy();
            ItemStack slot = inventory.get(SLOT_OUTPUT_1 + i);
            if (!slot.isEmpty()) slot.grow(out.getCount());
            else inventory.set(SLOT_OUTPUT_1 + i, out);
        }
        ItemStack s1 = inventory.get(SLOT_INPUT_1), s2 = inventory.get(SLOT_INPUT_2);
        CountIngredient[] in = recipe.inputItem;
        if (in.length == 1) {
            if (in[0].test(s1)) s1.shrink(in[0].count());
            else if (in[0].test(s2)) s2.shrink(in[0].count());
        } else if (in[0].test(s1) && in[1].test(s2)) {
            s1.shrink(in[0].count());
            s2.shrink(in[1].count());
        } else {
            s2.shrink(in[0].count());
            s1.shrink(in[1].count());
        }
        setChanged();
    }

    @Override
    public int getFloorCount() {
        return 2 * 2;
    }

    @Override
    public BlockPos getFloorPosFromIndex(int index) {
        return standardFloor3x3(index);
    }

    @Override
    public long getDemand(Fluid type, int pressure) {
        if (!tanks[0].accepts(type) || pressure != tanks[0].getPressure()) return 0L;
        return (long) tanks[0].getMaxFill() - tanks[0].getFill();
    }

    @Override
    public long transferFluid(Fluid type, int pressure, long amount) {
        if (!tanks[0].accepts(type) || pressure != tanks[0].getPressure()) return amount;
        int accepted = tanks[0].fill(type, (int) Math.min(amount, Integer.MAX_VALUE), true);
        if (accepted > 0) setChanged();
        return amount - accepted;
    }

    @Override
    public long getFluidAvailable(Fluid type, int pressure) {
        if (!tanks[1].provides(type) || pressure != tanks[1].getPressure()) return 0L;
        return tanks[1].getFill();
    }

    @Override
    public void useUpFluid(Fluid type, int pressure, long amount) {
        if (!tanks[1].provides(type) || pressure != tanks[1].getPressure()) return;
        int drained = tanks[1].drain((int) Math.min(amount, Integer.MAX_VALUE), true);
        if (drained > 0) setChanged();
    }

    @Override
    public FluidTankNTM[] getSendingTanks() {
        return sending;
    }

    @Override
    public long getProviderSpeed(Fluid type, int pressure) {
        return Math.max(tanks[1].getFill() * 50L / tanks[1].getMaxFill(), 8L);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == SLOT_FUEL) return getBurnTime(stack) > 0;
        return slot == SLOT_INPUT_1 || slot == SLOT_INPUT_2;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ACCESSIBLE_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {

        if (slot == SLOT_INPUT_1 || slot == SLOT_INPUT_2) {
            ItemStack other = inventory.get(slot == SLOT_INPUT_1 ? SLOT_INPUT_2 : SLOT_INPUT_1);
            if (!other.isEmpty() && ItemStack.isSameItemSameComponents(stack, other)) return false;
            for (BlastFurnaceRecipe recipe : BlastFurnaceRecipesNT.INSTANCE.recipes()) {
                for (CountIngredient in : recipe.inputItem) {
                    if (in.matchesItem(stack)) return true;
                }
            }
            return false;
        }
        return canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == SLOT_OUTPUT_1 || slot == SLOT_OUTPUT_2;
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new MenuMachineBlastFurnace(containerId, playerInventory, this);
    }

    @Override
    public FluidTankNTM[] getAllTanks() {
        return tanks;
    }

    private void writeTanks(ByteBuf output) {
        tanks[0].packetSerialize(output);
        tanks[1].packetSerialize(output);
    }

    private void readTanks(ByteBuf input) {
        tanks[0].packetDeserialize(input);
        tanks[1].packetDeserialize(input);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        fuel = input.getIntOr("fuel", 0);
        progress = input.getFloatOr("progress", 0F);
        input.child("airblast").ifPresent(tanks[0]::deserialize);
        input.child("flue").ifPresent(tanks[1]::deserialize);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("fuel", fuel);
        output.putFloat("progress", progress);
        tanks[0].serialize(output.child("airblast"));
        tanks[1].serialize(output.child("flue"));
    }

    @Override
    public long syncUnitMask() {
        return 0x1fL;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeInt(this.fuel);
            case 1 -> output.writeFloat(this.progress);
            case 2 -> output.writeFloat(this.speed);
            case 3 -> output.writeBoolean(this.processing);
            case 4 -> writeTanks(output);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.fuel = input.readInt();
            case 1 -> this.progress = input.readFloat();
            case 2 -> this.speed = input.readFloat();
            case 3 -> this.processing = input.readBoolean();
            case 4 -> readTanks(input);
            default -> throw new IllegalArgumentException();
        }
    }
}
