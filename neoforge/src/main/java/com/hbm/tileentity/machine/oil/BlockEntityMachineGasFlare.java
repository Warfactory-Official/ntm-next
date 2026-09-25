// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine.oil;

import com.hbm.api.control.IControlReceiver;
import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.api.energymk2.ItemEnergyTransfer;
import com.hbm.api.fluidmk2.FluidTankEndpoint;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.client.ClientEffects;
import com.hbm.client.ClientPlayerAccess;
import com.hbm.handler.threading.TargetPoint;
import com.hbm.inventory.container.MenuMachineGasFlare;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluidProperty;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.fluid.trait.FT_Flammable;
import com.hbm.inventory.fluid.trait.FluidTrait.FluidReleaseType;
import com.hbm.inventory.fluid.trait.FluidTrait;
import com.hbm.inventory.fluid.trait.FluidTraitSimple.FT_Gaseous;
import com.hbm.inventory.fluid.trait.FluidTraitSimple.FT_Gaseous_ART;
import com.hbm.items.machine.FluidIdentifierItem;
import com.hbm.items.machine.upgrade.ItemMachineUpgrade;
import com.hbm.items.machine.upgrade.UpgradeManager;
import com.hbm.items.machine.upgrade.UpgradeType;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.packet.toclient.EffectNTPayload;
import com.hbm.particle.CoolingTowerParticleOptions;
import com.hbm.particle.HbmEffectNT;
import com.hbm.platform.Services;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.BlockEntityMachineBase;
import com.hbm.tileentity.IFluidCopiable;
import com.hbm.tileentity.IUpgradeInfoProvider;
import com.hbm.tileentity.Tiltable;
import com.hbm.util.TickPhase;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

public class BlockEntityMachineGasFlare extends BlockEntityMachineBase
        implements IEnergyHandlerMK2,
                FluidTankEndpoint,
                MenuProvider,
                IUpgradeInfoProvider,
                IControlReceiver,
                Tiltable,
                IFluidCopiable,
                SyncUnitSchema {

    public static final int SLOT_BATTERY = 0;
    public static final int SLOT_FLUID_IN = 1;
    public static final int SLOT_FLUID_OUT = 2;
    public static final int SLOT_FLUID_ID = 3;
    public static final int SLOT_UPGRADE_START = 4;
    public static final int SLOT_UPGRADE_END = 5;
    public static final int SLOT_COUNT = 6;

    public static final long maxPower = 100_000L;
    public static final int TANK_CAPACITY = 64_000;

    private static final int[] VALID_UPGRADES =
            IUpgradeInfoProvider.upgradeCaps(UpgradeType.SPEED, 3, UpgradeType.EFFECT, 3);

    @SyncField(units = 1L << 3)
    public final FluidTankNTM tank = new FluidTankNTM(NTMFluids.GAS, TANK_CAPACITY);

    private final FluidTankNTM[] receiving;
    private final UpgradeManager upgradeManager = new UpgradeManager(this);

    @SyncField(units = 1L << 0)
    public long power;

    @SyncField(units = 1L << 1)
    public boolean isOn;

    @SyncField(units = 1L << 2)
    public boolean doesBurn;

    public int fluidUsed;
    public int output;

    public BlockEntityMachineGasFlare(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GASFLARE.get(), pos, state, SLOT_COUNT);
        receiving = new FluidTankNTM[] {tank};
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.gasFlare");
    }

    @Override
    public void tickServer() {
        checkTilt(Tiltable.TiltType.CONFIG, false);

        fluidUsed = 0;
        output = 0;

        boolean changed = tank.setType(SLOT_FLUID_ID, SLOT_FLUID_ID, inventory);
        changed |= tank.loadTank(SLOT_FLUID_IN, SLOT_FLUID_OUT, inventory);
        if (changed) setChanged();

        int maxVent = 50;
        int maxBurn = 10;

        if (isOn && tank.getFill() > 0 && !isTilted()) {
            upgradeManager.scan(this, SLOT_UPGRADE_START, SLOT_UPGRADE_END);
            int burn = upgradeManager.getLevel(UpgradeType.SPEED);
            int yield = upgradeManager.getLevel(UpgradeType.EFFECT);

            maxVent += maxVent * burn;
            maxBurn += maxBurn * burn;

            Fluid type = tank.getTankType();
            boolean flammable = NTMFluidProperties.hasTrait(type, FT_Flammable.class);
            boolean gaseous =
                    NTMFluidProperties.hasTrait(type, FT_Gaseous.class)
                            || NTMFluidProperties.hasTrait(type, FT_Gaseous_ART.class);

            if (!doesBurn || !flammable) {
                if (gaseous) {
                    int eject = Math.min(maxVent, tank.getFill());
                    fluidUsed = eject;
                    tank.setFill(tank.getFill() - eject);

                    if (TickPhase.every(this, 5)) {
                        FluidTrait.onRelease(
                                level,
                                worldPosition,
                                type,
                                tank,
                                FluidReleaseType.SPILL,
                                eject * 5);
                    }

                    if (TickPhase.every(this, 7)) {

                        level.playSound(
                                null,
                                worldPosition.getX(),
                                worldPosition.getY() + 11,
                                worldPosition.getZ(),
                                SoundEvents.FIRE_EXTINGUISH,
                                SoundSource.BLOCKS,
                                1.5F,
                                0.5F);
                    }
                }
            } else {
                int eject = Math.min(maxBurn, tank.getFill());
                fluidUsed = eject;
                tank.setFill(tank.getFill() - eject);

                int penalty = gaseous ? 5 : 10;
                FT_Flammable trait = NTMFluidProperties.getTrait(type, FT_Flammable.class);
                long powerProd = trait.getHeatEnergy() * eject / 1_000L;
                powerProd /= penalty;
                powerProd += powerProd * yield / 3;

                output = (int) powerProd;
                power = Math.min(maxPower, power + powerProd);

                ServerLevel server = (ServerLevel) level;
                double px = worldPosition.getX() + 0.5D;
                double py = worldPosition.getY() + 11.75D;
                double pz = worldPosition.getZ() + 0.5D;

                Services.NETWORK.sendToAllAround(
                        new EffectNTPayload(HbmEffectNT.GasFlameFlare, px, py, pz),
                        new TargetPoint(server, px, py, pz, 75));

                AABB box =
                        new AABB(
                                worldPosition.getX() - 1,
                                worldPosition.getY() + 12,
                                worldPosition.getZ() - 2,
                                worldPosition.getX() + 2,
                                worldPosition.getY() + 17,
                                worldPosition.getZ() + 2);
                for (Entity e : level.getEntitiesOfClass(Entity.class, box)) {
                    e.igniteForSeconds(5F);
                    e.hurtServer(server, level.damageSources().onFire(), 5F);
                }

                if (TickPhase.every(this, 3)) {
                    level.playSound(
                            null,
                            worldPosition.getX(),
                            worldPosition.getY() + 11,
                            worldPosition.getZ(),
                            ModSounds.FLAMETHROWER_SHOOT.get(),
                            SoundSource.BLOCKS,
                            1.5F,
                            0.75F);
                }

                if (TickPhase.every(this, 5)) {
                    FluidTrait.onRelease(
                            level, worldPosition, type, tank, FluidReleaseType.BURN, eject * 5);
                }
            }
        }

        power -= ItemEnergyTransfer.insert(this, SLOT_BATTERY, power, false);
        networkPackNT(50);
    }

    @Override
    public void tickClient() {
        if (!isOn || tank.getFill() <= 0) return;

        Fluid type = tank.getTankType();
        if (doesBurn && NTMFluidProperties.hasTrait(type, FT_Flammable.class)) {
            burnSmoke();
            return;
        }
        if (!NTMFluidProperties.hasTrait(type, FT_Gaseous.class)
                && !NTMFluidProperties.hasTrait(type, FT_Gaseous_ART.class)) return;

        NTMFluidProperty prop = NTMFluidProperties.get(tank.getFluid());
        CoolingTowerParticleOptions opts =
                new CoolingTowerParticleOptions.Builder()
                        .setLift(1F)
                        .setBaseScale(0.25F)
                        .setMaxScale(3F)
                        .setLife(150 + level.getRandom().nextInt(20))
                        .setColor(prop != null ? prop.color() : CoolingTowerParticleOptions.NO_TINT)
                        .build();
        level.addParticle(
                opts,
                worldPosition.getX() + 0.5,
                worldPosition.getY() + 11,
                worldPosition.getZ() + 0.5,
                0,
                0,
                0);
    }

    private void burnSmoke() {
        Player player = ClientPlayerAccess.player();
        if (player == null
                || player.getEyePosition()
                                .distanceToSqr(
                                        worldPosition.getX(),
                                        worldPosition.getY() + 10,
                                        worldPosition.getZ())
                        > 1024) return;
        boolean even = level.getGameTime() % 2 == 0;
        ClientEffects.spawnNoClipSmoke(
                worldPosition.getX() + (even ? 1.5 : 1.125),
                worldPosition.getY() + (even ? 10.75 : 11.75),
                worldPosition.getZ() + (even ? 1.5 : -0.5),
                50);
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
    public void receiveControl(CompoundTag data) {
        if (data.contains("valve")) isOn = !isOn;
        if (data.contains("dial")) doesBurn = !doesBurn;
        setChanged();
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public void setPower(long p) {
        power = Math.max(0L, Math.min(p, maxPower));
    }

    @Override
    public long getMaxPower() {
        return maxPower;
    }

    @Override
    public FluidTankNTM[] getReceivingTanks() {
        return receiving;
    }

    @Override
    public FluidTankNTM[] getAllTanks() {
        return receiving;
    }

    @Override
    public String[] getFluidIDToCopy() {
        Fluid type = tank.getDeclaredFluid();
        return new String[] {
            BuiltInRegistries.FLUID.getKey(type == null ? NTMFluids.NONE : type).toString()
        };
    }

    @Override
    public FluidTankNTM getTankToPaste() {
        return tank;
    }

    @Override
    public CompoundTag getSettings(Level level, BlockPos pos) {
        CompoundTag tag = IFluidCopiable.super.getSettings(level, pos);
        tag.putBoolean("isOn", isOn);
        tag.putBoolean("doesBurn", doesBurn);
        return tag;
    }

    @Override
    public void pasteSettings(
            CompoundTag nbt, int index, Level level, Player player, BlockPos pos) {
        IFluidCopiable.super.pasteSettings(nbt, index, level, player, pos);
        if (nbt.contains("isOn")) isOn = nbt.getBooleanOr("isOn", isOn);
        if (nbt.contains("doesBurn")) doesBurn = nbt.getBooleanOr("doesBurn", doesBurn);
        setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_BATTERY -> IBatteryItem.isBattery(stack);
            case SLOT_FLUID_IN -> FluidTankNTM.isFluidContainer(stack);
            case SLOT_FLUID_ID -> stack.getItem() instanceof FluidIdentifierItem;
            case SLOT_UPGRADE_START, SLOT_UPGRADE_END -> ItemMachineUpgrade.isUpgrade(stack);
            default -> false;
        };
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
        return new MenuMachineGasFlare(containerId, playerInventory, this);
    }

    @Override
    public boolean hasPermission(Player player) {
        return player.getEyePosition()
                        .distanceToSqr(
                                worldPosition.getX(), worldPosition.getY(), worldPosition.getZ())
                <= 256.0D;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.getLong("power").ifPresent(v -> power = v);
        isOn = input.getBooleanOr("isOn", isOn);
        doesBurn = input.getBooleanOr("doesBurn", doesBurn);
        input.child("tank").ifPresent(tank::deserialize);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("power", power);
        output.putBoolean("isOn", isOn);
        output.putBoolean("doesBurn", doesBurn);
        tank.serialize(output.child("tank"));
    }

    @Override
    public long syncUnitMask() {
        return 0xfL;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeLong(this.power);
            case 1 -> output.writeBoolean(this.isOn);
            case 2 -> output.writeBoolean(this.doesBurn);
            case 3 -> this.tank.packetSerialize(output);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.power = input.readLong();
            case 1 -> this.isOn = input.readBoolean();
            case 2 -> this.doesBurn = input.readBoolean();
            case 3 -> this.tank.packetDeserialize(input);
            default -> throw new IllegalArgumentException();
        }
    }
}
