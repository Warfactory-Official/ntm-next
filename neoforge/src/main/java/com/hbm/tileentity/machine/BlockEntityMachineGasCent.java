// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.api.energymk2.ItemEnergyTransfer;
import com.hbm.api.fluidmk2.FluidTankEndpoint;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.inventory.container.ContainerSync;
import com.hbm.inventory.container.MenuMachineGasCent;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.GasCentrifugeRecipe;
import com.hbm.inventory.recipes.GasCentrifugeRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModItems;
import com.hbm.items.machine.FluidIdentifierData;
import com.hbm.items.machine.FluidIdentifierItem;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncSource;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.sound.AudioSystem;
import com.hbm.sound.AudioWrapper;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.AudioLoop;
import com.hbm.tileentity.BlockEntityMachineBase;
import com.hbm.tileentity.IFluidCopiable;
import com.hbm.util.InventoryUtil;
import com.hbm.util.TickPhase;
import io.netty.buffer.ByteBuf;
import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.sounds.SoundSource;
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

public class BlockEntityMachineGasCent extends BlockEntityMachineBase
        implements AudioLoop,
                IEnergyHandlerMK2,
                FluidTankEndpoint,
                MenuProvider,
                IFluidCopiable,
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

    public static Consumer<BlockEntityMachineGasCent> CLIENT_SOUND = be -> {};

    @SyncField(units = 1L << 5)
    public final FluidTankNTM tank = new FluidTankNTM(NTMFluids.UF6, TANK_CAPACITY);

    private final FluidTankNTM[] receiving;

    @SyncField(units = 1L << 3)
    public final StageTank inputTank = new StageTank(STAGE_CAPACITY, DEFAULT_INPUT_STAGE);

    @SyncField(units = 1L << 4)
    public final StageTank outputTank = new StageTank(STAGE_CAPACITY, DEFAULT_OUTPUT_STAGE);

    @SyncField(units = 1L << 0)
    @ContainerSync
    public long power;

    @SyncField(units = 1L << 1)
    public int progress;

    @SyncField(units = 1L << 2)
    public boolean isProgressing;

    public int audioDuration;

    public BlockEntityMachineGasCent(BlockPos pos, BlockState state) {
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
        power = Math.max(0L, Math.min(p, MAX_POWER));
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

        power += ItemEnergyTransfer.extract(this, SLOT_BATTERY, MAX_POWER - power, false);
        applyFluidIdentifier();

        GasCentrifugeRecipes recipes = GasCentrifugeRecipes.INSTANCE;
        if (recipes.isFeedstock(inputTank.getStage())) attemptConversion();

        GasCentrifugeRecipe recipe = recipes.byStage(inputTank.getStage());

        if (canEnrich(recipe)) {
            isProgressing = true;
            this.progress++;
            this.power -= consumption(recipe);

            if (this.power < 0) {
                power = 0;
                this.progress = 0;
            }

            if (progress >= processTime(recipe)) enrich(recipe);
        } else {
            isProgressing = false;
            this.progress = 0;
        }

        if (level != null && TickPhase.every(this, TRANSFER_INTERVAL)) {
            Direction facing = getBlockState().getValue(BlockMultiblockCore.FACING);
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

        ItemStack[] products = products(recipe);
        if (products.length < 1) return false;
        return InventoryUtil.doesArrayHaveSpace(
                inventory, SLOT_OUTPUT_START, SLOT_OUTPUT_END, products);
    }

    private void enrich(GasCentrifugeRecipe recipe) {
        ItemStack[] products = products(recipe);

        this.progress = 0;
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
        }
    }

    private boolean attemptTransfer(@Nullable BlockEntity behind) {
        if (behind instanceof BlockEntityMachineGasCent cent
                && cent.tank.getTankType() == tank.getTankType()) {

            String product = outputTank.getStage();
            if (!Objects.equals(cent.inputTank.getStage(), product) && product != null) {
                GasCentrifugeRecipe recipe = GasCentrifugeRecipes.INSTANCE.byStage(product);
                cent.inputTank.setStage(product);
                cent.outputTank.setStage(recipe == null ? null : recipe.next);
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
    public AudioWrapper createAudioLoop() {
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
    public FluidTankNTM[] getAllTanks() {
        return new FluidTankNTM[] {tank};
    }

    @Override
    public FluidTankNTM getTankToPaste() {
        return tank;
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new MenuMachineGasCent(containerId, playerInventory, this);
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
        power = input.getLongOr("power", 0L);
        progress = input.getIntOr("progress", 0);
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
        @SyncField private @Nullable String stage;
        @SyncField private int fill;

        public StageTank(int maxFill, @Nullable String stage) {
            this.maxFill = maxFill;
            this.stage = stage;
        }

        public @Nullable String getStage() {
            return stage;
        }

        public void setStage(@Nullable String stage) {
            if (Objects.equals(this.stage, stage)) return;
            this.stage = stage;
            this.fill = 0;
        }

        public int getFill() {
            return fill;
        }

        public void setFill(int amount) {
            this.fill = Math.clamp(amount, 0, maxFill);
        }

        public int getMaxFill() {
            return maxFill;
        }

        public void serialize(ValueOutput out) {
            out.putInt("fill", fill);
            if (stage != null) out.putString("stage", stage);
        }

        public void deserialize(ValueInput in) {
            fill = Math.clamp(in.getIntOr("fill", 0), 0, maxFill);
            String read = in.getStringOr("stage", "");
            stage = read.isEmpty() ? null : read;
        }
    }

    public boolean acceptsFluid(Fluid type, Direction dir) {
        return dir != null && tank.accepts(type) && acceptsFeed();
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
            case 0 -> this.power = input.readLong();
            case 1 -> this.progress = input.readInt();
            case 2 -> this.isProgressing = input.readBoolean();
            case 3 -> readInputStage(input);
            case 4 -> readOutputStage(input);
            case 5 -> this.tank.packetDeserialize(input);
            default -> super.readSyncUnit(unit, input);
        }
    }
}
