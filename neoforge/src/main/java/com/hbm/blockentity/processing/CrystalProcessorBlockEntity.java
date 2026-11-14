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

package com.hbm.blockentity.processing;

import com.hbm.api.energymk2.EnergyHandler;
import com.hbm.api.energymk2.EnergyItem;
import com.hbm.api.energymk2.ItemEnergyTransfer;
import com.hbm.api.fluidmk2.FluidTankHandler;
import com.hbm.blockentity.AudioLoop;
import com.hbm.blockentity.FluidSettingsHandler;
import com.hbm.blockentity.MachineBlockEntity;
import com.hbm.blockentity.SoundEmitter;
import com.hbm.blockentity.UpgradeInfoProvider;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.inventory.container.CrystallizerMenu;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.CrystallizerRecipe;
import com.hbm.inventory.recipes.CrystallizerRecipes;
import com.hbm.inventory.recipes.loader.Outputs;
import com.hbm.items.machine.upgrade.MachineUpgradeItem;
import com.hbm.items.machine.upgrade.UpgradeInfo;
import com.hbm.items.machine.upgrade.UpgradeLimits;
import com.hbm.items.machine.upgrade.UpgradeManager;
import com.hbm.items.machine.upgrade.UpgradeType;
import com.hbm.packet.SyncInventory;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.sound.AudioSystem;
import com.hbm.sound.ModSounds;
import com.hbm.sound.SoundHandle;
import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
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

public class CrystalProcessorBlockEntity extends MachineBlockEntity
        implements EnergyHandler,
                FluidTankHandler,
                FluidSettingsHandler,
                AudioLoop,
                SoundEmitter,
                MenuProvider,
                UpgradeInfoProvider,
                SyncUnitSchema {
    public static final int SLOT_COUNT = 8;
    public static final int SLOT_INPUT = 0;
    public static final int SLOT_BATTERY = 1;
    public static final int SLOT_OUTPUT = 2;
    public static final int SLOT_FLUID_IN = 3;
    public static final int SLOT_FLUID_OUT = 4;
    public static final int SLOT_UPGRADE_START = 5;
    public static final int SLOT_UPGRADE_END = 6;
    public static final int SLOT_FLUID_ID = 7;
    public static final long MAX_POWER = 1_000_000L;
    public static final UpgradeLimits VALID_UPGRADES =
            UpgradeLimits.of(UpgradeType.SPEED, 3)
                    .and(UpgradeType.EFFECT, 3)
                    .and(UpgradeType.OVERDRIVE, 3);
    private static final int[] ITEM_ACCESS = {SLOT_INPUT, SLOT_OUTPUT};

    static {
        SyncInventory.slots(
                CrystalProcessorBlockEntity.class,
                new int[] {SLOT_INPUT, SLOT_UPGRADE_START, SLOT_UPGRADE_END},
                1L,
                false,
                true);
    }

    public static Consumer<CrystalProcessorBlockEntity> CLIENT_SOUND =
            _ -> {};
    public final FluidTankNTM tank = new FluidTankNTM(NTMFluids.PEROXIDE, 8_000);
    private final FluidTankNTM[] tanks = {tank};
    private final UpgradeManager upgrades = new UpgradeManager(this);
    public long power;
    public short progress;
    public short duration;

    {
        hbm$syncSet$CrystalProcessorBlockEntity$duration((short) (600));
    }

    public boolean isOn;
    private int rotationPerTick;

    {
        hbm$syncSet$CrystalProcessorBlockEntity$rotationPerTick(5);
    }

    public float angle;
    public float prevAngle;

    public CrystalProcessorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ACIDOMATIC.get(), pos, state, SLOT_COUNT);
        tank.bindOwner(this);
    }

    private @Nullable CrystallizerRecipe resolveRecipe() {
        return CrystallizerRecipes.INSTANCE.getOutput(getItem(SLOT_INPUT), tank.getTankType());
    }

    private short duration(@Nullable CrystallizerRecipe recipe) {
        int base = recipe == null ? 600 : recipe.duration;
        int speed = upgrades.getLevel(UpgradeType.SPEED);
        return speed == 0
                ? (short) base
                : (short) Math.ceil(base * Math.max(1F - 0.25F * speed, 0.25F));
    }

    public short getDuration() {
        return duration(resolveRecipe());
    }

    public int getCycleCount() {
        return Math.min(1 + 2 * upgrades.getLevel(UpgradeType.OVERDRIVE), 7);
    }

    public int getPowerRequired() {
        return 1_000
                * (1
                        + upgrades.getLevel(UpgradeType.SPEED)
                        + 2 * upgrades.getLevel(UpgradeType.EFFECT));
    }

    public int getRequiredAcid() {
        var recipe = resolveRecipe();
        return recipe == null ? 0 : recipe.acidAmount();
    }

    private float freeChance(CrystallizerRecipe recipe) {
        int effect = upgrades.getLevel(UpgradeType.EFFECT);
        return effect == 0 ? 0F : Math.min(effect * recipe.productivity, 0.99F);
    }

    public float getFreeChance() {
        var recipe = resolveRecipe();
        return recipe == null ? 0F : freeChance(recipe);
    }

    private boolean ready(CrystallizerRecipe recipe, int consumption) {
        var input = getItem(SLOT_INPUT);
        return power >= consumption
                && input.getCount() >= recipe.itemAmount()
                && recipe.matchesInputFluids(tanks)
                && Outputs.fits(getItem(SLOT_OUTPUT), recipe.outputItems(), 0);
    }

    public boolean canProcess() {
        var recipe = resolveRecipe();
        return recipe != null && ready(recipe, getPowerRequired());
    }

    private void complete(CrystallizerRecipe recipe) {
        var product = recipe.output().copy();
        if (!product.isEmpty()) {
            var stored = getItem(SLOT_OUTPUT);
            if (stored.isEmpty()) setItem(SLOT_OUTPUT, product);
            else stored.grow(product.getCount());
        }
        tank.setFill(tank.getFill() - recipe.acidAmount());
        float chance = freeChance(recipe);
        if (chance == 0F || chance < ((ServerLevel) level).getRandom().nextFloat()) {
            removeItem(SLOT_INPUT, recipe.itemAmount());
        }
        setChanged();
    }

    @Override
    public void tickServer() {
        long previousPower = power;
        short previousProgress = progress;
        hbm$syncSet$CrystalProcessorBlockEntity$power(
                (long)
                        (power
                                + (ItemEnergyTransfer.extract(
                                        this, SLOT_BATTERY, MAX_POWER - power, false))));
        if (tank.setType(SLOT_FLUID_ID, SLOT_FLUID_ID, inventory)) setChanged();
        if (tank.loadTank(SLOT_FLUID_IN, SLOT_FLUID_OUT, inventory)) setChanged();
        upgrades.scan(this, SLOT_UPGRADE_START, SLOT_UPGRADE_END);
        int consumption = getPowerRequired();
        int cycles = getCycleCount();
        hbm$syncSet$CrystalProcessorBlockEntity$rotationPerTick(5 * cycles);
        var recipe = resolveRecipe();
        hbm$syncSet$CrystalProcessorBlockEntity$duration(duration(recipe));
        hbm$syncSet$CrystalProcessorBlockEntity$isOn(false);
        for (int cycle = 0; cycle < cycles; cycle++) {
            if (recipe == null || !ready(recipe, consumption)) {
                hbm$syncSet$CrystalProcessorBlockEntity$progress((short) (0));
                continue;
            }
            hbm$syncSet$CrystalProcessorBlockEntity$progress((short) (progress + 1));
            hbm$syncSet$CrystalProcessorBlockEntity$power((long) (power - (consumption)));
            hbm$syncSet$CrystalProcessorBlockEntity$isOn(true);
            if (progress > duration) {
                complete(recipe);
                hbm$syncSet$CrystalProcessorBlockEntity$progress((short) (0));
                if (cycle + 1 < cycles) {
                    recipe = resolveRecipe();
                    hbm$syncSet$CrystalProcessorBlockEntity$duration(duration(recipe));
                }
            }
        }
        if (power != previousPower || progress != previousProgress) setChanged();
        networkPackNT(25);
    }

    @Override
    public void tickClient() {
        prevAngle = angle;
        if (isOn) {
            angle += rotationPerTick;
            if (angle >= 360F) {
                angle -= 360F;
                prevAngle -= 360F;
            }
            var random = level.getRandom();
            if (random.nextInt(20) == 0) {
                level.addParticle(
                        ParticleTypes.CLOUD,
                        worldPosition.getX() + random.nextDouble(),
                        worldPosition.getY() + 6.5D,
                        worldPosition.getZ() + random.nextDouble(),
                        0D,
                        0.1D,
                        0D);
            }
        }
        CLIENT_SOUND.accept(this);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        boolean inserted =
                slot >= SLOT_UPGRADE_START
                        && slot <= SLOT_UPGRADE_END
                        && MachineUpgradeItem.isUpgrade(stack)
                        && !ItemStack.matches(getItem(slot), stack);
        super.setItem(slot, stack);
        if (inserted && !level.isClientSide()) {
            level.playSound(
                    null, worldPosition, ModSounds.UPGRADE_PLUG.get(), SoundSource.BLOCKS, 1F, 1F);
        }
        if (slot == SLOT_INPUT || slot >= SLOT_UPGRADE_START && slot <= SLOT_UPGRADE_END) {
            syncUnitsChanged(-1, 1L);
        }
    }

    @Override
    public boolean tickSound() {
        return isOn;
    }

    @Override
    public SoundHandle createAudioLoop() {
        return AudioSystem.getLoopedSound(
                ModSounds.CHEMICAL_PLANT_LOOP.get(),
                SoundSource.BLOCKS,
                worldPosition.getX() + 0.5F,
                worldPosition.getY() + 0.5F,
                worldPosition.getZ() + 0.5F,
                1F,
                15F,
                0.75F,
                15);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ITEM_ACCESS;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_INPUT ->
                    CrystallizerRecipes.INSTANCE.getOutput(stack, tank.getTankType()) != null;
            case SLOT_BATTERY -> EnergyItem.matches(stack);
            default -> false;
        };
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == SLOT_INPUT && canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == SLOT_OUTPUT;
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
    public long getPower() {
        return power;
    }

    @Override
    public long getMaxPower() {
        return MAX_POWER;
    }

    @Override
    public void setPower(long stored) {
        hbm$syncSet$CrystalProcessorBlockEntity$power(Math.clamp(stored, 0L, MAX_POWER));
    }

    @Override
    public UpgradeLimits upgradeCaps() {
        return VALID_UPGRADES;
    }

    @Override
    public void appendUpgradeInfo(UpgradeType type, int tier, List<Component> tooltip) {
        if (type == UpgradeType.SPEED) UpgradeInfo.speedAndPower(tooltip, -25 * tier, 100 * tier);
        else if (type == UpgradeType.EFFECT) {
            tooltip.add(
                    Component.translatable("upgrade.efficiency", "x" + tier)
                            .withStyle(ChatFormatting.GREEN));
            UpgradeInfo.percent(tooltip, "upgrade.consumption", 200 * tier, false);
        } else if (type == UpgradeType.OVERDRIVE) UpgradeInfo.centrifuge(type, tier, tooltip);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.crystallizer");
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new CrystallizerMenu(id, playerInventory, this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        hbm$syncSet$CrystalProcessorBlockEntity$power(input.getLongOr("power", power));
        input.child("tank").ifPresent(tank::deserialize);
        hbm$syncSet$CrystalProcessorBlockEntity$progress((short) (0));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("power", power);
        tank.serialize(output.child("tank"));
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 31L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> {
                output.writeShort(progress);
                output.writeShort(duration);
            }
            case 1 -> output.writeLong(power);
            case 2 -> output.writeBoolean(isOn);
            case 3 -> tank.packetSerialize(output);
            case 4 -> output.writeByte(rotationPerTick);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> {
                hbm$syncSet$CrystalProcessorBlockEntity$progress(input.readShort());
                hbm$syncSet$CrystalProcessorBlockEntity$duration(input.readShort());
            }
            case 1 -> hbm$syncSet$CrystalProcessorBlockEntity$power(input.readLong());
            case 2 -> hbm$syncSet$CrystalProcessorBlockEntity$isOn(input.readBoolean());
            case 3 -> tank.packetDeserialize(input);
            case 4 ->
                    hbm$syncSet$CrystalProcessorBlockEntity$rotationPerTick(
                            input.readUnsignedByte());
            default -> super.readSyncUnit(unit, input);
        }
    }

    public final void hbm$syncSet$CrystalProcessorBlockEntity$power(long next) {
        if (this.power != next) {
            this.power = next;
            if ((2L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 2L);
        }
    }

    public final void hbm$syncSet$CrystalProcessorBlockEntity$progress(short next) {
        if (this.progress != next) {
            this.progress = next;
            if ((1L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L);
        }
    }

    public final void hbm$syncSet$CrystalProcessorBlockEntity$duration(short next) {
        if (this.duration != next) {
            this.duration = next;
            if ((1L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L);
        }
    }

    public final void hbm$syncSet$CrystalProcessorBlockEntity$isOn(boolean next) {
        if (this.isOn != next) {
            this.isOn = next;
            if ((4L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 4L);
        }
    }

    public final void hbm$syncSet$CrystalProcessorBlockEntity$rotationPerTick(int next) {
        if (this.rotationPerTick != next) {
            this.rotationPerTick = next;
            if ((16L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 16L);
        }
    }

    static {
        com.hbm.packet.SyncBindings.field(CrystalProcessorBlockEntity.class, "tank", 3, 8L);
        com.hbm.inventory.container.SyncedData.fields(CrystalProcessorBlockEntity.class, "power");
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

    private boolean hbm$soundActive;

    public boolean soundStarted(boolean active) {
        boolean started = active && !hbm$soundActive;
        hbm$soundActive = active;
        return started;
    }
}
