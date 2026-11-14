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

package com.hbm.blockentity.machine.oil;

import com.hbm.api.energymk2.EnergyHandler;
import com.hbm.api.energymk2.EnergyItem;
import com.hbm.api.energymk2.ItemEnergyTransfer;
import com.hbm.api.environment.Environment;
import com.hbm.api.fluidmk2.FluidTankHandler;
import com.hbm.blockentity.FluidSettingsHandler;
import com.hbm.blockentity.MachineBlockEntity;
import com.hbm.blockentity.MachineSoundProvider;
import com.hbm.blockentity.Tiltable;
import com.hbm.blockentity.UpgradeInfoProvider;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.ModBlocks;
import com.hbm.client.ClientEffects;
import com.hbm.client.ClientPlayerAccess;
import com.hbm.config.BalanceConfig;
import com.hbm.data.MachineData;
import com.hbm.handler.threading.TargetPoint;
import com.hbm.inventory.container.GasFlareMenu;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluidProperty;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankAccess;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.fluid.trait.FlammableTrait;
import com.hbm.inventory.fluid.trait.FluidReleaseType;
import com.hbm.inventory.fluid.trait.FluidTraitSimple;
import com.hbm.items.machine.FluidIdentifierItem;
import com.hbm.items.machine.upgrade.MachineUpgradeItem;
import com.hbm.items.machine.upgrade.UpgradeInfo;
import com.hbm.items.machine.upgrade.UpgradeLimits;
import com.hbm.items.machine.upgrade.UpgradeManager;
import com.hbm.items.machine.upgrade.UpgradeType;
import com.hbm.packet.PacketWire;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.packet.layout.EffectGasFlameFlareLayout;
import com.hbm.particle.CoolingTowerParticleOptions;
import com.hbm.sound.ModSounds;
import com.hbm.sound.WorldSounds;
import com.hbm.uninos.ConnectionPriority;
import com.hbm.util.TickPhase;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

public class GasFlareBlockEntity extends MachineBlockEntity
        implements FluidTankAccess,
                EnergyHandler,
                FluidTankHandler,
                MenuProvider,
                FluidSettingsHandler,
                UpgradeInfoProvider,
                Tiltable,
                SyncUnitSchema {

    public static final int SLOT_BATTERY = 0;
    public static final int SLOT_FLUID_IN = 1;
    public static final int SLOT_FLUID_OUT = 2;
    public static final int SLOT_FLUID_ID = 3;
    public static final int SLOT_UPGRADE_START = 4;
    public static final int SLOT_UPGRADE_END = 5;
    public static final int SLOT_COUNT = 6;

    public static final long MAX_POWER = 100_000L;

    private static final int VENT_PER_LEVEL = 50;
    private static final int BURN_PER_LEVEL = 10;
    private static final int RELEASE_PERIOD = 5;
    private static final int RELEASE_FACTOR = 5;
    private static final float SOUND_VOLUME = 1.5F;
    private static final int TOP = 11;
    private static final UpgradeLimits UPGRADES =
            UpgradeLimits.of(UpgradeType.SPEED, 3).and(UpgradeType.EFFECT, 3);

    public long power;
    public boolean valve;
    public boolean ignition;
    public final FluidTankNTM tank = new FluidTankNTM(NTMFluids.GAS, 64_000);
    public long output;

    private final FluidTankNTM[] intake = {tank};
    private final UpgradeManager upgrades = new UpgradeManager(this);
    private final BlockPos top;

    public GasFlareBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GASFLARE.get(), pos, state, SLOT_COUNT);
        top = pos.above(TOP);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.gasFlare");
    }

    @Override
    public void tickServer() {
        tickTilt(Tiltable.Policy.CONFIGURED, false);
        output = 0L;
        boolean changed = tank.setType(SLOT_FLUID_ID, SLOT_FLUID_ID, inventory);
        changed |= tank.loadTank(SLOT_FLUID_IN, SLOT_FLUID_OUT, inventory);
        if (changed) markChanged();
        if (valve && tank.getFill() > 0 && !isTilted()) flare();
        long charged = ItemEnergyTransfer.insert(this, SLOT_BATTERY, power, false);
        if (charged != 0L) hbm$syncSet$GasFlareBlockEntity$power((long) (power - (charged)));
        networkPackNT(50);
    }

    private void flare() {
        upgrades.scan(this, SLOT_UPGRADE_START, SLOT_UPGRADE_END);
        int speed = upgrades.getLevel(UpgradeType.SPEED);
        Fluid fluid = tank.getTankType();
        NTMFluidProperty property = NTMFluidProperties.get(fluid);
        FlammableTrait fuel = property == null ? null : property.getTrait(FlammableTrait.class);
        int burnable = Math.min(BURN_PER_LEVEL * (1 + speed), tank.getFill());
        if (ignition && fuel != null && breatheAir(burnable)) {
            burn(fluid, fuel, gaseous(property), burnable, upgrades.getLevel(UpgradeType.EFFECT));
        } else if (gaseous(property)) {
            vent(fluid, Math.min(VENT_PER_LEVEL * (1 + speed), tank.getFill()));
        }
    }

    private static boolean gaseous(@Nullable NTMFluidProperty property) {
        return property != null
                && (property.hasTrait(FluidTraitSimple.FT_Gaseous.class)
                        || property.hasTrait(FluidTraitSimple.FT_Gaseous_ART.class));
    }

    private void vent(Fluid fluid, int amount) {
        tank.setFill(tank.getFill() - amount);
        markChanged();
        if (TickPhase.every(this, RELEASE_PERIOD)) {
            FluidReleaseType.SPILL.dispatch(
                    level, worldPosition, fluid, tank, RELEASE_FACTOR * amount);
        }
        if (TickPhase.every(this, 7)) {
            WorldSounds.machine(
                    level,
                    null,
                    top,
                    SoundEvents.FIRE_EXTINGUISH,
                    SoundSource.BLOCKS,
                    soundVolume(),
                    0.5F);
        }
        Environment.releaseGas((ServerLevel) level, fluid, amount);
    }

    private void burn(Fluid fluid, FlammableTrait fuel, boolean gaseous, int amount, int effect) {
        tank.setFill(tank.getFill() - amount);
        long heat = fuel.heatPerBucket() * amount / 1_000L / (gaseous ? 5L : 10L);
        output = heat + heat * effect / 3L;
        hbm$syncSet$GasFlareBlockEntity$power(Math.min(MAX_POWER, power + output));
        markChanged();

        int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
        ServerLevel server = (ServerLevel) level;
        PacketWire.sendToAllAround(
                EffectGasFlameFlareLayout.encode(x + 0.5, y + 11.75, z + 0.5),
                new TargetPoint(server, x + 0.5, y + 11.75, z + 0.5, 150));
        AABB flame = new AABB(x - 1, y + 12, z - 2, x + 2, y + 17, z + 2);
        for (Entity entity : level.getEntitiesOfClass(Entity.class, flame)) {
            entity.igniteForSeconds(5F);
            entity.hurtServer(server, level.damageSources().onFire(), 5F);
        }
        if (TickPhase.every(this, 3)) {
            WorldSounds.machine(
                    level,
                    null,
                    top,
                    ModSounds.FLAMETHROWER_SHOOT.get(),
                    SoundSource.PLAYERS,
                    soundVolume(),
                    0.75F);
        }
        if (TickPhase.every(this, RELEASE_PERIOD)) {
            FluidReleaseType.BURN.dispatch(
                    level, worldPosition, fluid, tank, RELEASE_FACTOR * amount);
        }
    }

    private float soundVolume() {
        return isMuffled() ? SOUND_VOLUME * MachineSoundProvider.MUFFLED_VOLUME : SOUND_VOLUME;
    }

    @Override
    public void tickClient() {
        if (!valve || tank.getFill() <= 0) return;
        NTMFluidProperty property = NTMFluidProperties.get(tank.getTankType());
        if (ignition && property != null && property.hasTrait(FlammableTrait.class)) {
            burnSmoke();
        } else if (gaseous(property)) {
            CoolingTowerParticleOptions plume =
                    new CoolingTowerParticleOptions.Builder()
                            .setLift(1F)
                            .setBaseScale(0.25F)
                            .setMaxScale(3F)
                            .setLife(150 + level.getRandom().nextInt(20))
                            .setColor(property.color())
                            .build();
            level.addParticle(
                    plume,
                    worldPosition.getX() + 0.5,
                    worldPosition.getY() + TOP,
                    worldPosition.getZ() + 0.5,
                    0,
                    0,
                    0);
        }
    }

    private void burnSmoke() {
        Player viewer = ClientPlayerAccess.player();
        if (viewer == null) return;
        int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
        double dx = viewer.getX() - x, dy = viewer.getEyeY() - (y + 10), dz = viewer.getZ() - z;
        if (dx * dx + dy * dy + dz * dz > 32 * 32) return;
        if (level.getGameTime() % 2 == 0)
            ClientEffects.spawnNoClipSmoke(x + 1.5, y + 10.75, z + 1.5, 50);
        else ClientEffects.spawnNoClipSmoke(x + 1.125, y + 11.75, z - 0.5, 50);
    }

    public void toggleValve() {
        hbm$syncSet$GasFlareBlockEntity$valve(!valve);
        markChanged();
    }

    public void toggleBurnMode() {
        hbm$syncSet$GasFlareBlockEntity$ignition(!ignition);
        markChanged();
    }

    @Override
    public CompoundTag copySettings(Level level, BlockPos pos) {
        CompoundTag settings =
                FluidSettingsHandler.writeFluids(new CompoundTag(), settingsFluidIds());
        settings.putBoolean("isOn", valve);
        settings.putBoolean("doesBurn", ignition);
        return settings;
    }

    @Override
    public void pasteSettings(
            CompoundTag settings, int option, Level level, Player player, BlockPos pos) {
        Fluid fluid = FluidSettingsHandler.readFluid(settings, option);
        if (fluid != null) tank.setTankTypeByIdentifier(fluid);
        if (settings.contains("isOn"))
            hbm$syncSet$GasFlareBlockEntity$valve(settings.getBooleanOr("isOn", valve));
        if (settings.contains("doesBurn"))
            hbm$syncSet$GasFlareBlockEntity$ignition(settings.getBooleanOr("doesBurn", ignition));
        markChanged();
    }

    @Override
    public UpgradeLimits upgradeCaps() {
        return UPGRADES;
    }

    @Override
    public void appendUpgradeInfo(UpgradeType type, int level, List<Component> tooltip) {
        if (type == UpgradeType.SPEED)
            UpgradeInfo.percent(tooltip, "upgrade.consumption", 100 * level, true);
        else if (type == UpgradeType.EFFECT)
            UpgradeInfo.percent(tooltip, "upgrade.efficiency", 100 * level / 3, true);
    }

    @Override
    public ConnectionPriority getFluidPriority() {
        return ConnectionPriority.LOW;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_BATTERY -> EnergyItem.matches(stack);
            case SLOT_FLUID_IN -> FluidTankNTM.isFluidContainer(stack);
            case SLOT_FLUID_ID -> stack.getItem() instanceof FluidIdentifierItem;
            case SLOT_UPGRADE_START, SLOT_UPGRADE_END -> MachineUpgradeItem.isUpgrade(stack);
            default -> false;
        };
    }

    @Override
    public int getSupportCheckCount() {
        return 4;
    }

    @Override
    public BlockPos getSupportCheckPosition(int index) {
        return floorGrid3(index);
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public void setPower(long stored) {
        hbm$syncSet$GasFlareBlockEntity$power(Math.clamp(stored, 0L, MAX_POWER));
    }

    @Override
    public long getMaxPower() {
        return MAX_POWER;
    }

    @Override
    public FluidTankNTM[] getReceivingTanks() {
        return intake;
    }

    @Override
    public int tankCount() {
        return 1;
    }

    @Override
    public FluidTankNTM tank(int index) {
        return intake[index];
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new GasFlareMenu(containerId, playerInventory, this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        hbm$syncSet$GasFlareBlockEntity$power(input.getLongOr("power", 0L));
        hbm$syncSet$GasFlareBlockEntity$valve(input.getBooleanOr("isOn", false));
        hbm$syncSet$GasFlareBlockEntity$ignition(input.getBooleanOr("doesBurn", false));
        input.child("tank").ifPresent(tank::deserialize);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("power", power);
        output.putBoolean("isOn", valve);
        output.putBoolean("doesBurn", ignition);
        tank.serialize(output.child("tank"));
    }

    @Override
    public long syncUnitMask() {
        return 0xfL;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeLong(power);
            case 1 -> output.writeBoolean(valve);
            case 2 -> output.writeBoolean(ignition);
            case 3 -> tank.packetSerialize(output);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> hbm$syncSet$GasFlareBlockEntity$power(input.readLong());
            case 1 -> hbm$syncSet$GasFlareBlockEntity$valve(input.readBoolean());
            case 2 -> hbm$syncSet$GasFlareBlockEntity$ignition(input.readBoolean());
            case 3 -> tank.packetDeserialize(input);
            default -> throw new IllegalArgumentException();
        }
    }

    public final void hbm$syncSet$GasFlareBlockEntity$power(long next) {
        if (this.power != next) {
            this.power = next;
            if ((1L << 0) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 0);
        }
    }

    public final void hbm$syncSet$GasFlareBlockEntity$valve(boolean next) {
        if (this.valve != next) {
            this.valve = next;
            if ((1L << 1) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 1);
        }
    }

    public final void hbm$syncSet$GasFlareBlockEntity$ignition(boolean next) {
        if (this.ignition != next) {
            this.ignition = next;
            if ((1L << 2) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 2);
        }
    }

    static {
        com.hbm.packet.SyncBindings.field(GasFlareBlockEntity.class, "tank", 3, 1L << 3);
    }

    private int hbm$tiltChecked;

    private int hbm$tiltValid;

    public boolean isTilted() {
        return getBlockState().getValue(Tiltable.TILT);
    }

    public void tickTilt(Tiltable.Policy policy, boolean extraHeavy) {
        Level level = getLevel();
        boolean doesTilt =
                (policy == Tiltable.Policy.UNAVOIDABLE
                                ? Environment.unavoidableGravity(level)
                                : MachineData.ENABLE_MACHINE_GRAVITY.get()
                                        || BalanceConfig.enable528MachineGravity)
                        && !Environment.orbital(level);

        if (!doesTilt || getSupportCheckCount() <= 0) {
            hbm$settle(false);
            return;
        }
        if (!TickPhase.every(this, SharedConstants.TICKS_PER_SECOND)) return;

        if (hbm$tiltChecked >= getSupportCheckCount()) {
            boolean tilted = hbm$tiltValid < hbm$tiltChecked * 0.95;
            if (tilted && !isTilted()) {
                SoundEvent impact = ModSounds.METAL_IMPACT.get();
                level.playSound(null, getBlockPos(), impact, SoundSource.BLOCKS, 3F, 1F);
            }
            hbm$settle(tilted);
            hbm$tiltChecked = 0;
            hbm$tiltValid = 0;
        }

        BlockPos pos = getSupportCheckPosition(hbm$tiltChecked);
        if (pos == null) return;

        BlockState ground = level.getBlockState(pos);
        hbm$tiltChecked++;

        if (extraHeavy) {
            if (!ground.isSolidRender() || ground.isSignalSource()) return;
            if (ground.getBlock() instanceof FallingBlock
                    || ground.is(BlockTags.MINEABLE_WITH_SHOVEL)
                    || ground.is(BlockTags.WOOL)) return;
            if (ground.getBlock().getExplosionResistance() < Blocks.STONE.getExplosionResistance())
                return;
            hbm$tiltValid++;
            return;
        }
        if (!ground.isFaceSturdy(level, pos, Direction.UP)) return;
        if (ground.getBlock() instanceof FallingBlock) return;
        if (ground.is(ModBlocks.DIRT_DEAD.get())
                || ground.is(ModBlocks.DIRT_OILY.get())
                || ground.is(ModBlocks.STONE_CRACKED.get())) return;
        hbm$tiltValid++;
    }

    private void hbm$settle(boolean tilted) {
        BlockState state = getBlockState();
        if (state.getValue(Tiltable.TILT) == tilted) return;
        BlockState settled = state.setValue(Tiltable.TILT, tilted);
        getLevel()
                .setBlock(
                        getBlockPos(),
                        settled,
                        Block.UPDATE_CLIENTS
                                | Block.UPDATE_KNOWN_SHAPE
                                | Block.UPDATE_SKIP_ON_PLACE);
    }
}
