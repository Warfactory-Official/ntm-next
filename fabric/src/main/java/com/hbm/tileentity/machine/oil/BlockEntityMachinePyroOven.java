// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine.oil;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.api.energymk2.ItemEnergyTransfer;
import com.hbm.api.fluidmk2.FluidFlushOutputs;
import com.hbm.api.fluidmk2.FluidTankEndpoint;
import com.hbm.api.fluidmk2.FlushFaces;
import com.hbm.api.fluidmk2.FlushLanes;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.handler.pollution.PollutionHandler;
import com.hbm.handler.pollution.PollutionType;
import com.hbm.inventory.container.MenuMachinePyroOven;
import com.hbm.inventory.fluid.FluidStackNTM;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.PyroOvenRecipe;
import com.hbm.inventory.recipes.PyroOvenRecipes;
import com.hbm.inventory.recipes.ingredient.CountIngredient;
import com.hbm.items.machine.FluidIdentifierItem;
import com.hbm.items.machine.upgrade.ItemMachineUpgrade;
import com.hbm.items.machine.upgrade.UpgradeManager;
import com.hbm.items.machine.upgrade.UpgradeType;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.particle.CoolingTowerParticleOptions;
import com.hbm.sound.AudioSystem;
import com.hbm.sound.AudioWrapper;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.AudioLoop;
import com.hbm.tileentity.BlockEntityMachinePolluting;
import com.hbm.tileentity.IFluidCopiable;
import com.hbm.tileentity.IUpgradeInfoProvider;
import com.hbm.util.TickPhase;
import io.netty.buffer.ByteBuf;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class BlockEntityMachinePyroOven extends BlockEntityMachinePolluting
        implements AudioLoop,
                IEnergyHandlerMK2,
                FluidTankEndpoint,
                MenuProvider,
                IUpgradeInfoProvider,
                IFluidCopiable,
                SyncUnitSchema {

    public static final int SLOT_BATTERY = 0;
    public static final int SLOT_ITEM_INPUT = 1;
    public static final int SLOT_ITEM_OUTPUT = 2;
    public static final int SLOT_FLUID_ID = 3;
    public static final int SLOT_UPGRADE_START = 4;
    public static final int SLOT_UPGRADE_END = 5;
    public static final int SLOT_COUNT = 6;

    public static final long MAX_POWER = 10_000_000L;
    public static final int CONSUMPTION = 10_000;

    private static final int[] ACCESSIBLE_SLOTS = {SLOT_ITEM_INPUT, SLOT_ITEM_OUTPUT};

    private static final int[] VALID_UPGRADES =
            IUpgradeInfoProvider.upgradeCaps(
                    UpgradeType.SPEED, 3, UpgradeType.POWER, 3, UpgradeType.OVERDRIVE, 3);
    public static @Nullable Consumer<BlockEntityMachinePyroOven> CLIENT_SOUND;

    @SyncField(units = 1L << 0)
    public final FluidTankNTM[] tanks = new FluidTankNTM[2];

    private final FluidTankNTM[] receiving;
    private final FluidTankNTM[] sending;
    private final UpgradeManager upgradeManager = new UpgradeManager(this);

    @SyncField(units = 1L << 1)
    public long power;

    @SyncField(units = 1L << 2)
    public boolean isVenting;

    @SyncField(units = 1L << 3)
    public boolean isProgressing;

    @SyncField(units = 1L << 4)
    public float progress;

    public int prevAnim;
    public int anim;
    private @Nullable PyroOvenRecipe lastValidRecipe;

    private final FluidFlushOutputs flush = new FluidFlushOutputs();

    public BlockEntityMachinePyroOven(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PYROOVEN.get(), pos, state, SLOT_COUNT, 50);
        tanks[0] = new FluidTankNTM(NTMFluids.NONE, 24_000);
        tanks[1] = new FluidTankNTM(NTMFluids.NONE, 24_000);
        receiving = new FluidTankNTM[] {tanks[0]};
        sending = new FluidTankNTM[] {tanks[1], smoke};
    }

    public static int getConsumption(int speed, int powerSaving) {
        return (int) (CONSUMPTION * Math.pow(speed + 1, 2)) / (powerSaving + 1);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.machinePyroOven");
    }

    private Direction facing() {
        return BlockMultiblockCore.coreFacing(getBlockState());
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        super.setItem(slot, stack);
        if (level == null || level.isClientSide()) return;
        if (slot >= SLOT_UPGRADE_START
                && slot <= SLOT_UPGRADE_END
                && ItemMachineUpgrade.isUpgrade(stack)) {
            level.playSound(
                    null,
                    worldPosition,
                    ModSounds.UPGRADE_PLUG.get(),
                    SoundSource.BLOCKS,
                    1.0F,
                    1.0F);
        }
    }

    @Override
    public void tickServer() {
        ServerLevel server = (ServerLevel) level;
        power += ItemEnergyTransfer.extract(this, SLOT_BATTERY, MAX_POWER - power, false);
        tanks[0].setType(SLOT_FLUID_ID, SLOT_FLUID_ID, inventory);

        flush.provide(server, this);

        upgradeManager.scan(this, SLOT_UPGRADE_START, SLOT_UPGRADE_END);
        int speed = upgradeManager.getLevel(UpgradeType.SPEED);
        int powerSaving = upgradeManager.getLevel(UpgradeType.POWER);
        int overdrive = upgradeManager.getLevel(UpgradeType.OVERDRIVE);

        this.isProgressing = false;
        this.isVenting = false;

        if (canProcess()) {
            PyroOvenRecipe recipe = getMatchingRecipe();
            this.progress +=
                    1F
                            / Math.max(
                                    (recipe.duration - speed * (recipe.duration / 4))
                                            / (overdrive * 2 + 1),
                                    1);
            this.isProgressing = true;

            this.power -= getConsumption(speed + overdrive * 2, powerSaving);

            if (progress >= 1F) {
                this.progress = 0F;
                finishRecipe(recipe);
                setChanged();
            }

            pollute(PollutionType.SOOT, PollutionHandler.SOOT_PER_SECOND);
        } else {
            this.progress = 0F;
        }

        networkPackNT(50);
    }

    @Override
    public void declareFlush(FlushLanes out) {
        out.add(tanks[1], FlushFaces.activePlane((cell, side) -> side.getAxis().isHorizontal()));
        out.add(smoke, FlushFaces.activePlane((cell, side) -> side == Direction.UP));
    }

    @Override
    public void tickClient() {
        this.prevAnim = this.anim;
        Direction dir = facing();
        Direction rot = dir.getCounterClockWise();

        if (CLIENT_SOUND != null) CLIENT_SOUND.accept(this);

        if (isProgressing) {
            this.anim++;

            if (level.getNearestPlayer(
                            worldPosition.getX() + 0.5,
                            worldPosition.getY() + 3,
                            worldPosition.getZ() + 0.5,
                            50,
                            false)
                    != null) {
                for (double d : new double[] {0.875, 2.375}) {
                    for (int s = -1; s <= 1; s += 2) {
                        if (level.getRandom().nextInt(20) == 0) {
                            level.addParticle(
                                    ParticleTypes.CLOUD,
                                    worldPosition.getX()
                                            + 0.5
                                            - rot.getStepX()
                                            + dir.getStepX() * d * s,
                                    worldPosition.getY() + 3,
                                    worldPosition.getZ()
                                            + 0.5
                                            - rot.getStepZ()
                                            + dir.getStepZ() * d * s,
                                    0.0,
                                    0.05,
                                    0.0);
                        }
                    }
                }
            }
        }

        if (isVenting && TickPhase.every(this, 2)) {

            CoolingTowerParticleOptions opts =
                    new CoolingTowerParticleOptions.Builder()
                            .setLift(10F)
                            .setBaseScale(0.25F)
                            .setMaxScale(2.5F)
                            .setLife(100 + level.getRandom().nextInt(20))
                            .setColor(0x202020)
                            .build();
            level.addParticle(
                    opts,
                    worldPosition.getX() + 0.5 - rot.getStepX(),
                    worldPosition.getY() + 3,
                    worldPosition.getZ() + 0.5 - rot.getStepZ(),
                    0,
                    0,
                    0);
        }
    }

    public @Nullable PyroOvenRecipe getMatchingRecipe() {
        if (lastValidRecipe != null && doesRecipeMatch(lastValidRecipe)) return lastValidRecipe;
        for (PyroOvenRecipe rec : PyroOvenRecipes.INSTANCE.recipes()) {
            if (doesRecipeMatch(rec)) {
                lastValidRecipe = rec;
                return rec;
            }
        }
        return null;
    }

    public boolean doesRecipeMatch(PyroOvenRecipe recipe) {
        FluidStackNTM inputFluid = recipe.inFluid();
        if (inputFluid != null && tanks[0].getTankType() != inputFluid.type()) return false;
        ItemStack in = inventory.get(SLOT_ITEM_INPUT);
        CountIngredient inputItem = recipe.inItem();
        if (inputItem != null) {
            if (in.isEmpty()) return false;
            return inputItem.matchesItem(in);
        } else return in.isEmpty();
    }

    public boolean canProcess() {
        int speed = upgradeManager.getLevel(UpgradeType.SPEED);
        int powerSaving = upgradeManager.getLevel(UpgradeType.POWER);
        if (power < getConsumption(speed, powerSaving)) return false;

        PyroOvenRecipe recipe = getMatchingRecipe();
        if (recipe == null) return false;
        FluidStackNTM inputFluid = recipe.inFluid();
        CountIngredient inputItem = recipe.inItem();
        FluidStackNTM outputFluid = recipe.outFluid();
        if (inputFluid != null && tanks[0].getFill() < inputFluid.amount()) return false;
        if (inputItem != null && inventory.get(SLOT_ITEM_INPUT).getCount() < inputItem.count())
            return false;
        if (outputFluid != null
                && outputFluid.type() == tanks[1].getTankType()
                && outputFluid.amount() + tanks[1].getFill() > tanks[1].getMaxFill()) return false;

        ItemStack out = inventory.get(SLOT_ITEM_OUTPUT);
        ItemStack outputItem = recipe.outItem();
        if (!outputItem.isEmpty() && !out.isEmpty()) {
            if (!ItemStack.isSameItemSameComponents(out, outputItem)) return false;
            return outputItem.getCount() + out.getCount() <= out.getMaxStackSize();
        }
        return true;
    }

    public void finishRecipe(PyroOvenRecipe recipe) {
        ItemStack outputItem = recipe.outItem();
        if (!outputItem.isEmpty()) {
            ItemStack out = inventory.get(SLOT_ITEM_OUTPUT);
            if (out.isEmpty()) inventory.set(SLOT_ITEM_OUTPUT, outputItem.copy());
            else out.grow(outputItem.getCount());
        }
        FluidStackNTM outputFluid = recipe.outFluid();
        if (outputFluid != null) {
            tanks[1].setTankType(outputFluid.type());
            tanks[1].setFill(tanks[1].getFill() + (int) outputFluid.amount());
        }
        CountIngredient inputItem = recipe.inItem();
        FluidStackNTM inputFluid = recipe.inFluid();
        if (inputItem != null) inventory.get(SLOT_ITEM_INPUT).shrink(inputItem.count());
        if (inputFluid != null) tanks[0].setFill(tanks[0].getFill() - (int) inputFluid.amount());
    }

    @Override
    public void pollute(PollutionType type, float amount) {
        FluidTankNTM tank =
                type == PollutionType.SOOT
                        ? smoke
                        : type == PollutionType.HEAVYMETAL ? smokeLeaded : smokePoison;
        int fluidAmount = (int) Math.ceil(amount * 100);
        int accepted = tank.fill(tank.getTankType(), fluidAmount, true);
        int overflow = fluidAmount - accepted;
        if (overflow > 0) {
            PollutionHandler.incrementPollution(level, worldPosition, type, overflow / 100F);
            this.isVenting = true;
        }
    }

    public AudioWrapper createAudioLoop() {
        return AudioSystem.getLoopedSound(
                ModSounds.PYRO_OVEN_LOOP.get(),
                SoundSource.BLOCKS,
                worldPosition.getX(),
                worldPosition.getY(),
                worldPosition.getZ(),
                1.0F,
                15F,
                1.0F,
                20);
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
    public FluidTankNTM[] getReceivingTanks() {
        return receiving;
    }

    @Override
    public FluidTankNTM[] getSendingTanks() {
        return sending;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ACCESSIBLE_SLOTS;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_BATTERY -> IBatteryItem.isBattery(stack);
            case SLOT_FLUID_ID -> stack.getItem() instanceof FluidIdentifierItem;
            case SLOT_UPGRADE_START, SLOT_UPGRADE_END -> ItemMachineUpgrade.isUpgrade(stack);
            case SLOT_ITEM_INPUT -> true;
            default -> false;
        };
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return slot == SLOT_ITEM_INPUT;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == SLOT_ITEM_OUTPUT;
    }

    @Override
    public int[] getValidUpgrades() {
        return VALID_UPGRADES;
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new MenuMachinePyroOven(containerId, playerInventory, this);
    }

    @Override
    public FluidTankNTM[] getAllTanks() {
        return new FluidTankNTM[] {tanks[0], tanks[1], smoke};
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.getLong("power").ifPresent(v -> power = v);

        progress = input.getFloatOr("progress", progress);
        input.child("t0").ifPresent(tanks[0]::deserialize);
        input.child("t1").ifPresent(tanks[1]::deserialize);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("power", power);
        output.putFloat("progress", progress);
        tanks[0].serialize(output.child("t0"));
        tanks[1].serialize(output.child("t1"));
    }

    private void writeTanks(ByteBuf output) {
        for (int i = 0; i < 2; i++) tanks[i].packetSerialize(output);
    }

    private void readTanks(ByteBuf input) {
        for (int i = 0; i < 2; i++) tanks[i].packetDeserialize(input);
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 0x1fL;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> writeTanks(output);
            case 1 -> output.writeLong(this.power);
            case 2 -> output.writeBoolean(this.isVenting);
            case 3 -> output.writeBoolean(this.isProgressing);
            case 4 -> output.writeFloat(this.progress);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> readTanks(input);
            case 1 -> this.power = input.readLong();
            case 2 -> this.isVenting = input.readBoolean();
            case 3 -> this.isProgressing = input.readBoolean();
            case 4 -> this.progress = input.readFloat();
            default -> super.readSyncUnit(unit, input);
        }
    }
}
