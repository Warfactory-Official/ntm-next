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
import com.hbm.api.fluidmk2.FluidTankHandler;
import com.hbm.blockentity.AudioLoop;
import com.hbm.blockentity.MachineBlockEntity;
import com.hbm.blockentity.SoundEmitter;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.inventory.container.GasCentMenu;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankAccess;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.GasCentrifugeRecipe;
import com.hbm.inventory.recipes.GasCentrifugeRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.inventory.recipes.loader.Outputs;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModItems;
import com.hbm.items.machine.FluidIdentifierData;
import com.hbm.items.machine.FluidIdentifierItem;
import com.hbm.packet.SyncBindings;
import com.hbm.packet.SyncSource;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.sound.AudioSystem;
import com.hbm.sound.ModSounds;
import com.hbm.sound.SoundHandle;
import com.hbm.util.InventoryUtil;
import com.hbm.util.TickPhase;
import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class GasCentBlockEntity extends MachineBlockEntity
        implements SoundEmitter,
                FluidTankAccess,
                AudioLoop,
                EnergyHandler,
                FluidTankHandler,
                MenuProvider,
                SyncUnitSchema {

    public static final int SLOT_OUTPUT_START = 0;
    public static final int SLOT_OUTPUT_END = 3;
    public static final int SLOT_BATTERY = 4;
    public static final int SLOT_FLUID_ID = 5;
    public static final int SLOT_UPGRADE = 6;
    public static final int SLOT_COUNT = 7;

    public static final long MAX_POWER = 100_000L;
    public static final int TANK_CAPACITY = 2_000;
    public static final int STAGE_CAPACITY = 8_000;

    public static final int UPGRADE_TICKS_SAVED = 70;
    public static final long UPGRADE_EXTRA_DRAIN = 100L;
    public static final int TRANSFER_INTERVAL = 10;
    public static final String DEFAULT_INPUT_STAGE = "nuf6";
    public static final String DEFAULT_OUTPUT_STAGE = "leuf6";
    private static final int[] ACCESSIBLE_SLOTS = {0, 1, 2, 3};
    public static Consumer<GasCentBlockEntity> CLIENT_SOUND = be -> {};
    public final FluidTankNTM tank = new FluidTankNTM(NTMFluids.UF6, TANK_CAPACITY);
    private final FluidTankNTM[] receiving;
    public final StageTank inputTank = new StageTank(STAGE_CAPACITY, DEFAULT_INPUT_STAGE);
    public final StageTank outputTank = new StageTank(STAGE_CAPACITY, DEFAULT_OUTPUT_STAGE);
    public long power;
    public int progress;
    public boolean isProgressing;
    public int audioDuration;

    public GasCentBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GAS_CENTRIFUGE.get(), pos, state, SLOT_COUNT);
        receiving = new FluidTankNTM[] {tank};
    }

    private static ItemStack[] products(GasCentrifugeRecipe recipe) {
        WeightedList<ItemStack>[] rolled = recipe.outputItems();
        if (rolled == null) return new ItemStack[0];
        ItemStack[] out = new ItemStack[rolled.length];
        for (int i = 0; i < rolled.length; i++) {
            out[i] = rolled[i].getRandom(GenericRecipes.RNG).orElse(ItemStack.EMPTY).copy();
        }
        return out;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.gasCentrifuge");
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public void setPower(long p) {
        hbm$syncSet$GasCentBlockEntity$power(Math.max(0L, Math.min(p, MAX_POWER)));
    }

    @Override
    public long getMaxPower() {
        return MAX_POWER;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ACCESSIBLE_SLOTS;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot <= SLOT_OUTPUT_END;
    }

    @Override
    public void tickServer() {
        long prevPower = power;
        int prevProgress = progress;
        boolean prevProgressing = isProgressing;

        hbm$syncSet$GasCentBlockEntity$power(
                (long)
                        (power
                                + (ItemEnergyTransfer.extract(
                                        this, SLOT_BATTERY, MAX_POWER - power, false))));
        applyFluidIdentifier();

        GasCentrifugeRecipes recipes = GasCentrifugeRecipes.INSTANCE;
        if (recipes.isFeedstock(inputTank.getStage())) attemptConversion();

        GasCentrifugeRecipe recipe = recipes.byStage(inputTank.getStage());

        if (canEnrich(recipe)) {
            hbm$syncSet$GasCentBlockEntity$isProgressing(true);
            this.hbm$syncSet$GasCentBlockEntity$progress((int) (this.progress + 1));
            this.hbm$syncSet$GasCentBlockEntity$power((long) (this.power - (consumption(recipe))));

            if (this.power < 0) {
                hbm$syncSet$GasCentBlockEntity$power(0);
                this.hbm$syncSet$GasCentBlockEntity$progress(0);
            }

            if (progress >= processTime(recipe)) enrich(recipe);
        } else {
            hbm$syncSet$GasCentBlockEntity$isProgressing(false);
            this.hbm$syncSet$GasCentBlockEntity$progress(0);
        }

        if (level != null && TickPhase.every(this, TRANSFER_INTERVAL)) {
            Direction facing = getBlockState().getValue(MultiblockCoreBlock.FACING);
            BlockEntity behind = level.getBlockEntity(worldPosition.relative(facing.getOpposite()));

            if (!attemptTransfer(behind)) attemptDeadEnd(recipes.byStage(outputTank.getStage()));
        }

        if (power != prevPower || progress != prevProgress || isProgressing != prevProgressing)
            setChanged();
        networkPackNT(50);
    }

    @Override
    public void tickClient() {
        CLIENT_SOUND.accept(this);
    }

    public int processTime(@Nullable GasCentrifugeRecipe recipe) {
        int base = recipe == null ? 0 : recipe.duration;
        return hasSpeedUpgrade() ? base - UPGRADE_TICKS_SAVED : base;
    }

    public long consumption(@Nullable GasCentrifugeRecipe recipe) {
        long base = recipe == null ? 0L : recipe.power;
        return hasSpeedUpgrade() ? base + UPGRADE_EXTRA_DRAIN : base;
    }

    public boolean hasSpeedUpgrade() {
        return inventory.get(SLOT_UPGRADE).getItem() == ModItems.UPGRADE_GC_SPEED.get();
    }

    private boolean canEnrich(@Nullable GasCentrifugeRecipe recipe) {
        if (recipe == null || power <= 0) return false;
        if (inputTank.getFill() < recipe.consumed) return false;
        if (outputTank.getFill() + recipe.produced > outputTank.getMaxFill()) return false;
        if (recipe.requiresUpgrade && !hasSpeedUpgrade()) return false;

        WeightedList<ItemStack>[] outputs = recipe.outputItems();
        if (outputs == null || outputs.length < 1) return false;
        return everyOutcomeFits(outputs, 0, new ItemStack[outputs.length]);
    }

    private boolean everyOutcomeFits(
            WeightedList<ItemStack>[] outputs, int index, ItemStack[] picked) {
        if (index == outputs.length) {
            return InventoryUtil.doesArrayHaveSpace(
                    inventory, SLOT_OUTPUT_START, SLOT_OUTPUT_END, picked);
        }
        List<ItemStack> outcomes = Outputs.outcomes(outputs[index]);
        if (outcomes.isEmpty()) {
            picked[index] = ItemStack.EMPTY;
            return everyOutcomeFits(outputs, index + 1, picked);
        }
        for (ItemStack outcome : outcomes) {
            picked[index] = outcome;
            if (!everyOutcomeFits(outputs, index + 1, picked)) return false;
        }
        return true;
    }

    private void enrich(GasCentrifugeRecipe recipe) {
        ItemStack[] products = products(recipe);

        this.hbm$syncSet$GasCentBlockEntity$progress(0);
        inputTank.setFill(inputTank.getFill() - recipe.consumed);
        outputTank.setFill(outputTank.getFill() + recipe.produced);

        for (ItemStack product : products) {
            InventoryUtil.tryAddItemToInventory(
                    inventory, SLOT_OUTPUT_START, SLOT_OUTPUT_END, product);
        }
        setChanged();
    }

    private void attemptConversion() {
        if (inputTank.getFill() < inputTank.getMaxFill() && tank.getFill() > 0) {
            int fill = Math.min(inputTank.getMaxFill() - inputTank.getFill(), tank.getFill());

            tank.setFill(tank.getFill() - fill);
            inputTank.setFill(inputTank.getFill() + fill);
            markChanged();
        }
    }

    private boolean attemptTransfer(@Nullable BlockEntity behind) {
        if (behind instanceof GasCentBlockEntity cent
                && cent.tank.getTankType() == tank.getTankType()) {

            String product = outputTank.getStage();
            if (!Objects.equals(cent.inputTank.getStage(), product) && product != null) {
                GasCentrifugeRecipe recipe = GasCentrifugeRecipes.INSTANCE.byStage(product);
                cent.inputTank.setStage(product);
                cent.outputTank.setStage(recipe == null ? null : recipe.next);
                cent.markChanged();
            }

            if (cent.inputTank.getFill() < cent.inputTank.getMaxFill()
                    && outputTank.getFill() > 0) {
                int fill =
                        Math.min(
                                cent.inputTank.getMaxFill() - cent.inputTank.getFill(),
                                outputTank.getFill());

                outputTank.setFill(outputTank.getFill() - fill);
                cent.inputTank.setFill(cent.inputTank.getFill() + fill);
                cent.setChanged();
                setChanged();
            }

            return true;
        }

        return false;
    }

    private void attemptDeadEnd(@Nullable GasCentrifugeRecipe product) {
        if (product == null || product.deadEndVolume <= 0) return;

        ItemStack[] converted = product.deadEndItems();
        if (converted.length < 1) return;

        if (outputTank.getFill() >= product.deadEndVolume
                && InventoryUtil.doesArrayHaveSpace(
                        inventory, SLOT_OUTPUT_START, SLOT_OUTPUT_END, converted)) {
            outputTank.setFill(outputTank.getFill() - product.deadEndVolume);
            for (ItemStack stack : converted) {
                InventoryUtil.tryAddItemToInventory(
                        inventory, SLOT_OUTPUT_START, SLOT_OUTPUT_END, stack.copy());
            }
            setChanged();
        }
    }

    private void applyFluidIdentifier() {
        ItemStack held = inventory.get(SLOT_FLUID_ID);
        if (!(held.getItem() instanceof FluidIdentifierItem)) return;

        Fluid selected =
                held.getOrDefault(
                                ModDataComponents.FLUID_IDENTIFIER.get(), FluidIdentifierData.EMPTY)
                        .primary();
        if (tank.getDeclaredFluid() == selected && tank.isStrict()) return;

        GasCentrifugeRecipe entry =
                GasCentrifugeRecipes.INSTANCE.byFeed(
                        selected == null ? null : NTMFluidProperties.kindOf(selected));
        if (entry == null) return;

        inputTank.setStage(entry.stage);
        outputTank.setStage(entry.next);
        tank.setTankTypeByIdentifier(selected);
        setChanged();
    }

    @Override
    public SoundHandle createAudioLoop() {
        return AudioSystem.getLoopedSound(
                ModSounds.CENTRIFUGE_LOOP.get(),
                SoundSource.BLOCKS,
                worldPosition.getX(),
                worldPosition.getY(),
                worldPosition.getZ(),
                1.0F,
                10F,
                1.0F,
                20);
    }

    public boolean acceptsFeed() {
        return GasCentrifugeRecipes.INSTANCE.isFeedstock(inputTank.getStage());
    }

    @Override
    public FluidTankNTM[] getReceivingTanks() {
        return receiving;
    }

    @Override
    public int tankCount() {
        return 1;
    }

    @Override
    public FluidTankNTM tank(int index) {
        if (index != 0) throw new IndexOutOfBoundsException(index);
        return tank;
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new GasCentMenu(containerId, playerInventory, this);
    }

    private void writeInputStage(ByteBuf output) {
        writeStage(output, inputTank);
    }

    private void readInputStage(ByteBuf input) {
        readStage(input, inputTank);
    }

    private void writeOutputStage(ByteBuf output) {
        writeStage(output, outputTank);
    }

    private void readOutputStage(ByteBuf input) {
        readStage(input, outputTank);
    }

    private static void writeStage(ByteBuf output, StageTank tank) {
        output.writeInt(tank.getFill());
        ByteBufCodecs.STRING_UTF8.encode(output, tank.getStage() == null ? "" : tank.getStage());
    }

    private static void readStage(ByteBuf input, StageTank tank) {
        int fill = input.readInt();
        String stage = ByteBufCodecs.STRING_UTF8.decode(input);
        tank.setStage(stage.isEmpty() ? null : stage);
        tank.setFill(fill);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        hbm$syncSet$GasCentBlockEntity$power(input.getLongOr("power", 0L));
        hbm$syncSet$GasCentBlockEntity$progress(input.getIntOr("progress", 0));
        input.child("tank").ifPresent(tank::deserialize);
        input.child("inputTank").ifPresent(inputTank::deserialize);
        input.child("outputTank").ifPresent(outputTank::deserialize);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("power", power);
        output.putInt("progress", progress);
        tank.serialize(output.child("tank"));
        inputTank.serialize(output.child("inputTank"));
        outputTank.serialize(output.child("outputTank"));
    }

    public static final class StageTank implements SyncSource {

        private final int maxFill;
        private @Nullable String stage;
        private int fill;

        public StageTank(int maxFill, @Nullable String stage) {
            this.maxFill = maxFill;
            this.hbm$syncSet$StageTank$stage(stage);
        }

        public @Nullable String getStage() {
            return stage;
        }

        public void setStage(@Nullable String stage) {
            if (Objects.equals(this.stage, stage)) return;
            this.hbm$syncSet$StageTank$stage(stage);
            this.hbm$syncSet$StageTank$fill(0);
        }

        public int getFill() {
            return fill;
        }

        public void setFill(int amount) {
            this.hbm$syncSet$StageTank$fill(Math.clamp(amount, 0, maxFill));
        }

        public int getMaxFill() {
            return maxFill;
        }

        public void serialize(ValueOutput out) {
            out.putInt("fill", fill);
            if (stage != null) out.putString("stage", stage);
        }

        public void deserialize(ValueInput in) {
            hbm$syncSet$StageTank$fill(Math.clamp(in.getIntOr("fill", 0), 0, maxFill));
            String read = in.getStringOr("stage", "");
            hbm$syncSet$StageTank$stage(read.isEmpty() ? null : read);
        }

        public final void hbm$syncSet$StageTank$stage(String next) {
            if (this.stage != next) {
                boolean changed = !java.util.Objects.equals(this.stage, next);
                this.stage = next;
                if (changed)
                    if ((0L) == 0L) syncChanged(3);
                    else syncUnitsChanged(3, 0L);
            }
        }

        public final void hbm$syncSet$StageTank$fill(int next) {
            if (this.fill != next) {
                this.fill = next;
                if ((0L) == 0L) syncChanged(3);
                else syncUnitsChanged(3, 0L);
            }
        }

        static {
            com.hbm.packet.SyncBindings.field(StageTank.class, "stage", 3, 0L);
        }

        private SyncSource hbm$syncOwner;

        private int hbm$syncMask;

        private int hbm$syncBindings;

        private long hbm$syncUnits;

        public final boolean syncBound() {
            return hbm$syncOwner != null;
        }

        public final void syncChanged(int mask) {
            if (hbm$syncOwner == null) return;
            if (hbm$syncUnits == 0) hbm$syncOwner.syncChanged(hbm$syncMask);
            else hbm$syncOwner.syncUnitsChanged(hbm$syncMask, hbm$syncUnits);
        }

        public final void syncUnitsChanged(int mask, long units) {
            syncChanged(mask);
        }

        public final void bindSync(SyncSource owner, int mask) {
            bindSyncUnits(owner, mask, 0);
        }

        public final void bindSyncUnits(SyncSource owner, int mask, long units) {
            if (hbm$syncOwner != null && hbm$syncOwner != owner) {
                throw new IllegalStateException("Mutable sync state has two owners");
            }
            hbm$syncOwner = owner;
            hbm$syncMask |= mask;
            hbm$syncUnits |= units;
            if (hbm$syncBindings++ == 0) SyncBindings.bindFields(this, owner);
        }

        public final void unbindSync(SyncSource owner) {
            assert hbm$syncOwner == owner;
            if (--hbm$syncBindings != 0) return;
            SyncBindings.unbindFields(this, owner);
            hbm$syncOwner = null;
            hbm$syncMask = 0;
            hbm$syncUnits = 0;
        }
    }

    public boolean acceptsFluid(Fluid type, Direction dir) {
        return dir != null && tank.joins(type) && acceptsFeed();
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 0x3fL;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeLong(this.power);
            case 1 -> output.writeInt(this.progress);
            case 2 -> output.writeBoolean(this.isProgressing);
            case 3 -> writeInputStage(output);
            case 4 -> writeOutputStage(output);
            case 5 -> this.tank.packetSerialize(output);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.hbm$syncSet$GasCentBlockEntity$power(input.readLong());
            case 1 -> this.hbm$syncSet$GasCentBlockEntity$progress(input.readInt());
            case 2 -> this.hbm$syncSet$GasCentBlockEntity$isProgressing(input.readBoolean());
            case 3 -> readInputStage(input);
            case 4 -> readOutputStage(input);
            case 5 -> this.tank.packetDeserialize(input);
            default -> super.readSyncUnit(unit, input);
        }
    }

    @Override
    public boolean tickSound() {
        audioDuration = Mth.clamp(audioDuration + (isProgressing ? 2 : -3), 0, 60);
        return audioDuration > 10;
    }

    public final void hbm$syncSet$GasCentBlockEntity$power(long next) {
        if (this.power != next) {
            this.power = next;
            if ((1L << 0) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 0);
        }
    }

    public final void hbm$syncSet$GasCentBlockEntity$progress(int next) {
        if (this.progress != next) {
            this.progress = next;
            if ((1L << 1) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 1);
        }
    }

    public final void hbm$syncSet$GasCentBlockEntity$isProgressing(boolean next) {
        if (this.isProgressing != next) {
            this.isProgressing = next;
            if ((1L << 2) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 2);
        }
    }

    static {
        com.hbm.packet.SyncBindings.field(GasCentBlockEntity.class, "tank", 3, 1L << 5);
        com.hbm.packet.SyncBindings.field(GasCentBlockEntity.class, "inputTank", 3, 1L << 3);
        com.hbm.packet.SyncBindings.field(GasCentBlockEntity.class, "outputTank", 3, 1L << 4);
        com.hbm.inventory.container.SyncedData.fields(GasCentBlockEntity.class, "power");
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
