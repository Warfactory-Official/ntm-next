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
import com.hbm.api.fluidmk2.FluidFlushOutputs;
import com.hbm.api.fluidmk2.FluidTankHandler;
import com.hbm.api.fluidmk2.FlushFaces;
import com.hbm.api.fluidmk2.FlushLanes;
import com.hbm.blockentity.AudioLoop;
import com.hbm.blockentity.FluidSettingsHandler;
import com.hbm.blockentity.PollutingMachineBlockEntity;
import com.hbm.blockentity.SoundEmitter;
import com.hbm.blockentity.UpgradeInfoProvider;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.handler.threading.TargetPoint;
import com.hbm.inventory.BlockMenus;
import com.hbm.inventory.container.TurbofanMenu;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.fluid.trait.CombustibleTrait;
import com.hbm.inventory.fluid.trait.FluidReleaseType;
import com.hbm.items.ModItems;
import com.hbm.items.machine.upgrade.UpgradeInfo;
import com.hbm.items.machine.upgrade.UpgradeLimits;
import com.hbm.items.machine.upgrade.UpgradeManager;
import com.hbm.items.machine.upgrade.UpgradeType;
import com.hbm.lib.ModDamageTypes;
import com.hbm.packet.PacketWire;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.packet.layout.GasFlameLayout;
import com.hbm.particle.EffectKinds.GibletType;
import com.hbm.particle.helper.ParticleCreators;
import com.hbm.sound.AudioSystem;
import com.hbm.sound.ModSounds;
import com.hbm.sound.SoundHandle;
import com.hbm.util.TickPhase;
import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public final class TurbofanBlockEntity extends PollutingMachineBlockEntity
        implements MenuProvider,
                EnergyHandler,
                FluidTankHandler,
                FluidSettingsHandler,
                UpgradeInfoProvider,
                AudioLoop,
                SoundEmitter,
                SyncUnitSchema {

    public static final int SLOT_FLUID_IN = 0;
    public static final int SLOT_CONTAINER_OUT = 1;
    public static final int SLOT_UPGRADE = 2;
    public static final int SLOT_BATTERY = 3;
    public static final int SLOT_FLUID_ID = 4;
    public static final int SLOT_COUNT = 5;
    public static final long MAX_POWER = 1_000_000;
    public static final int PONY_AFTERBURNER = 100;
    private static final UpgradeLimits UPGRADES = UpgradeLimits.of(UpgradeType.AFTERBURN, 3);
    private static final FlushFaces OUTPUT_FACES = FlushFaces.own();
    private static final Vec3 INTAKE_DRAG = new Vec3(0.25, 0.05, 0.25);

    public static Consumer<TurbofanBlockEntity> CLIENT_SOUND = machine -> {};

    public long power;
    public int afterburner;
    public boolean running;
    public boolean showBlood;
    public final FluidTankNTM tank = new FluidTankNTM(NTMFluids.KEROSENE, 24_000);
    public final FluidTankNTM blood = new FluidTankNTM(NTMFluids.BLOOD, 24_000);

    public int momentum;
    public float spin;
    public float lastSpin;

    private final UpgradeManager upgrades = new UpgradeManager(this);
    private final FluidFlushOutputs flush = new FluidFlushOutputs();
    private final BlockPos.MutableBlockPos signalProbe = new BlockPos.MutableBlockPos();
    private final FluidTankNTM[] intake;
    private final FluidTankNTM[] outputs;
    private final FluidTankNTM[] inspection;

    public TurbofanBlockEntity(BlockPos position, BlockState state) {
        super(ModBlockEntities.MACHINE_TURBOFAN.get(), position, state, SLOT_COUNT, 150);
        intake = new FluidTankNTM[] {tank};
        outputs = new FluidTankNTM[] {blood, smoke, smokeLeaded, smokePoison};
        inspection = new FluidTankNTM[] {tank, blood, smoke, smokeLeaded, smokePoison};
    }

    @Override
    public void tickServer() {
        var server = (ServerLevel) level;
        if (tank.setType(SLOT_FLUID_ID, SLOT_FLUID_ID, inventory)) setChanged();
        if (tank.loadTank(SLOT_FLUID_IN, SLOT_CONTAINER_OUT, inventory)) setChanged();

        hbm$syncSet$TurbofanBlockEntity$running(false);
        upgrades.scan(this, SLOT_UPGRADE, SLOT_UPGRADE + 1);
        hbm$syncSet$TurbofanBlockEntity$afterburner(upgrades.getLevel(UpgradeType.AFTERBURN));
        if (getItem(SLOT_UPGRADE).is(ModItems.FLAME_PONY.get()))
            hbm$syncSet$TurbofanBlockEntity$afterburner(PONY_AFTERBURNER);

        var facing = MultiblockCoreBlock.coreFacing(getBlockState());
        var thrust = facing.getClockWise();
        int burned = Math.min(1 + afterburner, tank.getFill());
        if (poweredConnection(server, facing, thrust)) burned = 0;
        if (burned > 0 && !breatheAir(burned)) burned = 0;

        Fluid fuel = tank.getFluid();
        long fuelValue = aviationValue(fuel);
        if (burned > 0) {
            hbm$syncSet$TurbofanBlockEntity$running(true);
            tank.setFill(tank.getFill() - burned);
            int generated = (int) (fuelValue * burned * (1 + Math.min(afterburner / 3.0, 4)));
            hbm$syncSet$TurbofanBlockEntity$power((long) (power + (generated)));
            if (TickPhase.every(this, 20)) pollute(fuel, FluidReleaseType.BURN, 5F * burned);
            setChanged();
        }

        long charged = ItemEnergyTransfer.insert(this, SLOT_BATTERY, power, false);
        if (charged > 0) {
            hbm$syncSet$TurbofanBlockEntity$power((long) (power - (charged)));
            setChanged();
        }
        if (burned > 0 && fuelValue > 0) applyBlast(server, facing, thrust);
        if (power > MAX_POWER) {
            hbm$syncSet$TurbofanBlockEntity$power(MAX_POWER);
            setChanged();
        }
        flush.provide(server, this);
        networkPackNT(150);
    }

    private static long aviationValue(@Nullable Fluid fluid) {
        var fuel = NTMFluidProperties.getTrait(fluid, CombustibleTrait.class);
        if (fuel == null || fuel.grade() != CombustibleTrait.Grade.AERO) return 0;
        return fuel.energyPerBucket() / 1_000;
    }

    private boolean poweredConnection(ServerLevel server, Direction facing, Direction thrust) {
        int x = worldPosition.getX();
        int y = worldPosition.getY();
        int z = worldPosition.getZ();
        int frontX = facing.getStepX() * 2;
        int frontZ = facing.getStepZ() * 2;
        int backX = thrust.getStepX();
        int backZ = thrust.getStepZ();
        return poweredAt(server, x + frontX, y, z + frontZ)
                || poweredAt(server, x + frontX - backX, y, z + frontZ - backZ)
                || poweredAt(server, x - frontX, y, z - frontZ)
                || poweredAt(server, x - frontX - backX, y, z - frontZ - backZ);
    }

    private boolean poweredAt(ServerLevel server, int x, int y, int z) {
        signalProbe.set(x, y, z);
        return server.hasChunkAt(signalProbe) && server.hasNeighborSignal(signalProbe);
    }

    private AABB zone(Direction thrust, double low, double high) {
        double x = worldPosition.getX() + 0.5;
        double z = worldPosition.getZ() + 0.5;
        int dx = thrust.getStepX();
        int dz = thrust.getStepZ();
        double acrossX = Math.abs(dz) * 1.5;
        double acrossZ = Math.abs(dx) * 1.5;
        return new AABB(
                x + Math.min(dx * low, dx * high) - acrossX,
                worldPosition.getY(),
                z + Math.min(dz * low, dz * high) - acrossZ,
                x + Math.max(dx * low, dx * high) + acrossX,
                worldPosition.getY() + 3,
                z + Math.max(dz * low, dz * high) + acrossZ);
    }

    private void applyBlast(ServerLevel server, Direction facing, Direction thrust) {
        for (var entity : server.getEntities((Entity) null, zone(thrust, -19.5, -3.5))) {
            if (afterburner > 0) {
                entity.igniteForSeconds(5);
                entity.hurtServer(server, entity.damageSources().onFire(), 5);
            }
            push(entity, thrust);
        }
        for (var entity : server.getEntities((Entity) null, zone(thrust, 3.5, 8.5))) {
            push(entity, thrust);
        }
        for (var entity : server.getEntities((Entity) null, zone(thrust, 3.5, 3.75))) {
            entity.hurtServer(server, server.damageSources().source(ModDamageTypes.BLENDER), 1_000);
            entity.makeStuckInBlock(Blocks.COBWEB.defaultBlockState(), INTAKE_DRAG);
            if (!entity.isAlive() && entity instanceof LivingEntity) {
                ParticleCreators.giblets(server, entity, GibletType.MEAT, 5);
                server.playSound(
                        null,
                        entity.getX(),
                        entity.getY(),
                        entity.getZ(),
                        SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR,
                        SoundSource.BLOCKS,
                        2,
                        0.95F + server.getRandom().nextFloat() * 0.2F);
                blood.setFill(Math.min(blood.getFill() + 50, blood.getMaxFill()));
                hbm$syncSet$TurbofanBlockEntity$showBlood(true);
                setChanged();
            }
        }
        if (afterburner > 0) emitFlames(server, facing, thrust);
    }

    private static void push(Entity entity, Direction thrust) {
        entity.push(-0.2 * thrust.getStepX(), 0, -0.2 * thrust.getStepZ());
        if (entity instanceof ServerPlayer player) ((Entity) player).syncVelocity = true;
    }

    private void emitFlames(ServerLevel server, Direction across, Direction thrust) {
        var random = server.getRandom();
        double centerX = worldPosition.getX() + 0.5;
        double centerZ = worldPosition.getZ() + 0.5;
        double y = worldPosition.getY() + 1.5;
        int dx = thrust.getStepX();
        int dz = thrust.getStepZ();
        for (int distance = 3; distance >= 2; distance--) {
            double speed = 2 + random.nextDouble() * 3;
            double deviation = random.nextGaussian() * 0.2;
            flame(
                    server,
                    centerX - dx * distance,
                    y,
                    centerZ - dz * distance,
                    -dx * speed + deviation,
                    0,
                    -dz * speed + deviation,
                    8);
        }
        if (afterburner > 90) {
            if (random.nextInt(30) == 0) {
                server.playSound(
                        null,
                        centerX,
                        y,
                        centerZ,
                        ModSounds.BLOCK_DAMAGE.get(),
                        SoundSource.BLOCKS,
                        3,
                        0.95F + random.nextFloat() * 0.2F);
            }
            double x =
                    centerX
                            + dx * (random.nextDouble() * 4 - 2)
                            + across.getStepX() * (random.nextDouble() * 2 - 1);
            double specialY = worldPosition.getY() + 1 + random.nextDouble() * 2;
            double z =
                    centerZ
                            - dz * (random.nextDouble() * 4 - 2)
                            + across.getStepZ() * (random.nextDouble() * 2 - 1);
            flame(server, x, specialY, z, 0, random.nextDouble() * 0.1, 0, 4);
        }
    }

    private void flame(
            ServerLevel server,
            double x,
            double y,
            double z,
            double motionX,
            double motionY,
            double motionZ,
            float scale) {
        var effect = GasFlameLayout.encode(x, y, z, motionX, motionY, motionZ, scale);
        PacketWire.sendToAllAround(
                effect,
                new TargetPoint(
                        server,
                        worldPosition.getX(),
                        worldPosition.getY(),
                        worldPosition.getZ(),
                        150));
    }

    @Override
    public boolean tickSound() {
        if (running) momentum = Math.min(momentum + 1, 100);
        else momentum = Math.max(momentum - 1, 0);
        return momentum > 0;
    }

    @Override
    public void tickClient() {
        tickSound();
        lastSpin = spin;
        spin += momentum / 2;
        if (spin >= 360) {
            spin -= 360;
            lastSpin -= 360;
        }
        CLIENT_SOUND.accept(this);
    }

    @Override
    public @Nullable SoundHandle createAudioLoop() {
        return AudioSystem.getLoopedSound(
                ModSounds.TURBOFAN_LOOP.get(),
                SoundSource.BLOCKS,
                worldPosition.getX(),
                worldPosition.getY(),
                worldPosition.getZ(),
                1,
                50,
                1,
                20);
    }

    @Override
    public FluidTankNTM[] getReceivingTanks() {
        return intake;
    }

    @Override
    public FluidTankNTM[] getSendingTanks() {
        return outputs;
    }

    @Override
    public FluidTankNTM[] settingsTanks() {
        return inspection;
    }

    @Override
    public int tankCount() {
        return inspection.length;
    }

    @Override
    public FluidTankNTM tank(int index) {
        return inspection[index];
    }

    @Override
    public void declareFlush(FlushLanes lanes) {
        for (var output : outputs) lanes.add(output, OUTPUT_FACES);
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
        hbm$syncSet$TurbofanBlockEntity$power(Math.clamp(stored, 0, MAX_POWER));
    }

    @Override
    public UpgradeLimits upgradeCaps() {
        return UPGRADES;
    }

    @Override
    public void appendUpgradeInfo(UpgradeType kind, int tier, List<Component> tooltip) {
        UpgradeInfo.percent(tooltip, "upgrade.efficiency", tier * 100 / 3, true);
        UpgradeInfo.percent(tooltip, "upgrade.consumption", tier * 100, false);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.machineTurbofan");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new TurbofanMenu(id, inventory, this);
    }

    @Override
    public boolean stillValid(Player player) {
        return BlockMenus.withinTwelveBlocks(this, player);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        hbm$syncSet$TurbofanBlockEntity$power(input.getLongOr("power", 0));
        hbm$syncSet$TurbofanBlockEntity$showBlood(input.getBooleanOr("showBlood", false));
        input.child("tank").ifPresent(tank::deserialize);
        input.child("blood").ifPresent(blood::deserialize);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("power", power);
        output.putBoolean("showBlood", showBlood);
        tank.serialize(output.child("tank"));
        blood.serialize(output.child("blood"));
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 63L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeLong(power);
            case 1 -> output.writeByte(afterburner);
            case 2 -> output.writeBoolean(running);
            case 3 -> output.writeBoolean(showBlood);
            case 4 -> tank.packetSerialize(output);
            case 5 -> blood.packetSerialize(output);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> hbm$syncSet$TurbofanBlockEntity$power(input.readLong());
            case 1 -> {
                int tier = input.readByte();
                if ((tier < 0 || tier > 3) && tier != PONY_AFTERBURNER)
                    throw new IllegalArgumentException();
                hbm$syncSet$TurbofanBlockEntity$afterburner(tier);
            }
            case 2 -> hbm$syncSet$TurbofanBlockEntity$running(input.readBoolean());
            case 3 -> hbm$syncSet$TurbofanBlockEntity$showBlood(input.readBoolean());
            case 4 -> tank.packetDeserialize(input);
            case 5 -> blood.packetDeserialize(input);
            default -> super.readSyncUnit(unit, input);
        }
    }

    public final void hbm$syncSet$TurbofanBlockEntity$power(long next) {
        if (this.power != next) {
            this.power = next;
            if ((1L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L);
        }
    }

    public final void hbm$syncSet$TurbofanBlockEntity$afterburner(int next) {
        if (this.afterburner != next) {
            this.afterburner = next;
            if ((2L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 2L);
        }
    }

    public final void hbm$syncSet$TurbofanBlockEntity$running(boolean next) {
        if (this.running != next) {
            this.running = next;
            if ((4L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 4L);
        }
    }

    public final void hbm$syncSet$TurbofanBlockEntity$showBlood(boolean next) {
        if (this.showBlood != next) {
            this.showBlood = next;
            if ((8L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 8L);
        }
    }

    static {
        com.hbm.packet.SyncBindings.field(TurbofanBlockEntity.class, "tank", 3, 16L);
        com.hbm.packet.SyncBindings.field(TurbofanBlockEntity.class, "blood", 3, 32L);
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
