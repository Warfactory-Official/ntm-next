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

package com.hbm.blockentity.turret;

import com.hbm.api.energymk2.EnergyHandler;
import com.hbm.api.energymk2.ItemEnergyTransfer;
import com.hbm.api.entity.RadarProvider;
import com.hbm.api.redstoneoverradio.RadioControlHandler;
import com.hbm.blockentity.MachineBlockEntity;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.entity.logic.BomberEntity;
import com.hbm.entity.missile.CustomMissile;
import com.hbm.entity.missile.Missile;
import com.hbm.entity.projectile.BulletBaseMK4Entity;
import com.hbm.entity.train.RailCarBaseEntity;
import com.hbm.handler.CasingEjector;
import com.hbm.handler.threading.TargetPoint;
import com.hbm.items.ModItems;
import com.hbm.items.machine.TurretBiometryItem;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.packet.PacketWire;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.packet.layout.MuzzleBlastLayout;
import com.hbm.particle.casing.Casing;
import com.hbm.particle.helper.CasingCreator;
import com.hbm.platform.Caps;
import com.hbm.sound.ModSounds;
import com.hbm.sound.WorldSounds;
import com.hbm.uninos.graph.PhysicalGraph;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.fish.WaterAnimal;
import net.minecraft.world.entity.animal.golem.AbstractGolem;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.Npc;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.common.util.FakePlayer;
import org.jspecify.annotations.Nullable;

import static com.hbm.api.redstoneoverradio.RadioCommandParser.FUN;
import static com.hbm.api.redstoneoverradio.RadioCommandParser.NAME_SEP;

public abstract class TurretBaseNTBlockEntity extends MachineBlockEntity
        implements EnergyHandler, MenuProvider, SyncUnitSchema, RadioControlHandler {

    public static final int SLOT_CHIP = 0;
    public static final int SLOT_AMMO_FIRST = 1;
    public static final int SLOT_AMMO_LAST = 9;
    public static final int SLOT_BATTERY = 10;
    public static final int SLOT_COUNT = 11;

    private static final int[] AMMO_SLOTS = {1, 2, 3, 4, 5, 6, 7, 8, 9};
    private static final double MUZZLE_FLASH_RANGE = 50D;

    public double rotationYaw;
    public double rotationPitch;
    public double lastRotationYaw;
    public double lastRotationPitch;
    public double syncRotationYaw;
    public double syncRotationPitch;
    public boolean isOn;
    public boolean aligned;
    public int searchTimer;
    public long power;
    public boolean targetPlayers;
    public boolean targetAnimals;
    public boolean targetMobs;

    {
        hbm$syncSet$TurretBaseNTBlockEntity$targetMobs(true);
    }

    public boolean targetMachines;

    {
        hbm$syncSet$TurretBaseNTBlockEntity$targetMachines(true);
    }

    public int stattrak;
    protected @Nullable List<ItemStack> ammoStacks;
    public int casingDelay;
    public @Nullable Entity target;
    public @Nullable Vec3 tPos;
    protected @Nullable Casing cachedCasingConfig;
    public int connectorMask;
    private boolean connectorsDirty = true;
    private @Nullable Fluid connectorFluidSeen;
    private boolean plugCapsWatched;
    private @Nullable Object plugCapListener;
    private int beats;

    protected TurretBaseNTBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, SLOT_COUNT);
    }

    public abstract void updateFiringTick();

    protected abstract @Nullable List<BulletConfig> getAmmoList();

    @Override
    public void tickServer() {
        long powerBefore = power;
        int killsBefore = stattrak;
        beforeTracking();
        aligned = false;

        dropDeadTarget();
        if (target != null && !aimsManually() && (target.isSpectator() || !entityInLOS(target)))
            target = null;
        if (target != null) hbm$syncSet$TurretBaseNTBlockEntity$tPos(getEntityPos(target));
        else if (!aimsManually()) hbm$syncSet$TurretBaseNTBlockEntity$tPos(null);

        boolean active = isOn() && hasPower();
        if (active) {
            engage();
        } else {
            target = null;
            hbm$syncSet$TurretBaseNTBlockEntity$tPos(null);
        }
        if (dropDeadTarget()) hbm$syncSet$TurretBaseNTBlockEntity$tPos(null);

        if (active) {
            setPower(getPower() - getConsumption());
            if (--searchTimer <= 0) {
                searchTimer = getDetectorInterval();
                if (target == null && !aimsManually()) seekNewTarget();
            }
        } else {
            searchTimer = 0;
        }

        if (readyToFire()) updateFiringTick();

        hbm$syncSet$TurretBaseNTBlockEntity$power(
                (long)
                        (power
                                + (ItemEnergyTransfer.extract(
                                        this, SLOT_BATTERY, getMaxPower() - power, false))));
        if (power != powerBefore || stattrak != killsBefore) markChanged();
        if (level instanceof ServerLevel server) refreshConnectors(server);
        networkPackNT(250);
        afterSync();
    }

    private boolean dropDeadTarget() {
        if (target == null || target.isAlive()) return false;
        target = null;
        hbm$syncSet$TurretBaseNTBlockEntity$stattrak((int) (stattrak + 1));
        return true;
    }

    protected void beforeTracking() {}

    protected boolean aimsManually() {
        return false;
    }

    protected void engage() {
        if (tPos != null) alignTurret();
    }

    protected boolean readyToFire() {
        return aligned;
    }

    protected void afterSync() {
        if (!usesCasings() || casingDelay() <= 0) return;
        if (casingDelay > 0) casingDelay--;
        else spawnCasing();
    }

    protected final boolean beat(int period) {
        return ++beats % period == 0;
    }

    protected final void fire(
            BulletConfig round, float baseDamage, SoundEvent sound, float volume) {
        if (usesCasings()) cachedCasingConfig = round.casing;
        spawnBullet(round, baseDamage);
        consumeAmmo(round);
        WorldSounds.machine(level, null, worldPosition, sound, SoundSource.BLOCKS, volume, 1.0F);
    }

    @Override
    public void tickClient() {
        lastRotationPitch = rotationPitch;
        lastRotationYaw = rotationYaw;
        hbm$syncSet$TurretBaseNTBlockEntity$rotationPitch(syncRotationPitch);
        hbm$syncSet$TurretBaseNTBlockEntity$rotationYaw(syncRotationYaw);

        if (Math.abs(lastRotationYaw - rotationYaw) > Math.PI) {
            if (lastRotationYaw < rotationYaw) lastRotationYaw += Math.PI * 2;
            else lastRotationYaw -= Math.PI * 2;
        }
    }

    protected void seekNewTarget() {
        Vec3 pos = getTurretPos();
        double range = getDetectorRange();
        Entity found = null;
        double closest = range;

        for (Entity entity :
                level.getEntitiesOfClass(Entity.class, new AABB(pos, pos).inflate(range))) {
            double dist = getEntityPos(entity).subtract(pos).length();
            if (dist > range) continue;
            if (!entityAcceptableTarget(entity)) continue;
            if (!entityInLOS(entity)) continue;

            if (dist < closest) {
                closest = dist;
                found = entity;
            }
        }

        target = found;
        if (target != null) hbm$syncSet$TurretBaseNTBlockEntity$tPos(getEntityPos(target));
    }

    protected void alignTurret() {
        turnTowards(tPos);
    }

    public void turnTowards(Vec3 ent) {
        Vec3 delta = ent.subtract(getTurretPos());
        double targetPitch = Math.asin(delta.y / delta.length());
        double targetYaw = -Math.atan2(delta.x, delta.z);
        turnTowardsAngle(targetPitch, targetYaw);
    }

    public void turnTowardsAngle(double targetPitch, double targetYaw) {
        double turnYaw = Math.toRadians(getTurretYawSpeed());
        double turnPitch = Math.toRadians(getTurretPitchSpeed());
        double pi2 = Math.PI * 2;

        if (Math.abs(rotationPitch - targetPitch) < turnPitch
                || Math.abs(rotationPitch - targetPitch) > pi2 - turnPitch) {
            hbm$syncSet$TurretBaseNTBlockEntity$rotationPitch(targetPitch);
        } else if (targetPitch > rotationPitch) {
            hbm$syncSet$TurretBaseNTBlockEntity$rotationPitch(
                    (double) (rotationPitch + (turnPitch)));
        } else {
            hbm$syncSet$TurretBaseNTBlockEntity$rotationPitch(
                    (double) (rotationPitch - (turnPitch)));
        }

        double deltaYaw = (targetYaw - rotationYaw) % pi2;
        int dir = 0;
        if (deltaYaw < -Math.PI) dir = 1;
        else if (deltaYaw < 0) dir = -1;
        else if (deltaYaw > Math.PI) dir = -1;
        else if (deltaYaw > 0) dir = 1;

        if (Math.abs(rotationYaw - targetYaw) < turnYaw
                || Math.abs(rotationYaw - targetYaw) > pi2 - turnYaw) {
            hbm$syncSet$TurretBaseNTBlockEntity$rotationYaw(targetYaw);
        } else {
            hbm$syncSet$TurretBaseNTBlockEntity$rotationYaw(
                    (double) (rotationYaw + (turnYaw * dir)));
        }

        double deltaPitch = targetPitch - rotationPitch;
        deltaYaw = targetYaw - rotationYaw;
        double deltaAngle = Math.sqrt(deltaYaw * deltaYaw + deltaPitch * deltaPitch);

        hbm$syncSet$TurretBaseNTBlockEntity$rotationYaw(rotationYaw % pi2);
        hbm$syncSet$TurretBaseNTBlockEntity$rotationPitch(rotationPitch % pi2);

        if (deltaAngle <= Math.toRadians(getAcceptableInaccuracy())) aligned = true;
    }

    public boolean entityInLOS(Entity e) {
        if (!e.isAlive()) return false;
        if (!hasThermalVision()
                && e instanceof LivingEntity living
                && living.hasEffect(MobEffects.INVISIBILITY)) return false;

        Vec3 pos = getTurretPos();
        Vec3 ent = getEntityPos(e);
        Vec3 delta = ent.subtract(pos);
        double length = delta.length();

        if (length < getDetectorGrace() || length > getDetectorRange() * 1.1) return false;

        Vec3 unit = delta.normalize();
        double pitchDeg = Math.toDegrees(Math.asin(unit.y / unit.length()));
        if (pitchDeg < -getTurretDepression() || pitchDeg > getTurretElevation()) return false;

        return !isObstructedOpaque(ent, pos);
    }

    protected boolean isObstructedOpaque(Vec3 from, Vec3 to) {
        return level.clip(
                                new ClipContext(
                                        from,
                                        to,
                                        ClipContext.Block.COLLIDER,
                                        ClipContext.Fluid.NONE,
                                        CollisionContext.empty()))
                        .getType()
                != HitResult.Type.MISS;
    }

    public boolean entityAcceptableTarget(Entity e) {
        if (!e.isAlive()) return false;

        if (e instanceof Player player && (player.isSpectator() || player.isCreative()))
            return false;

        List<String> wl = getWhitelist();
        if (wl != null) {
            if (e instanceof Player player) {
                if (wl.contains(player.getGameProfile().name())) return false;
            } else if (e instanceof Mob mob) {
                Component name = mob.getCustomName();
                if (name != null && wl.contains(name.getString())) return false;
            }
        }

        if (targetAnimals) {
            if (e instanceof Animal
                    || e instanceof AmbientCreature
                    || e instanceof WaterAnimal
                    || e instanceof AbstractGolem
                    || e instanceof Enemy) return true;
            if (e instanceof Npc) return true;
        }

        if (targetMobs) {
            if (e instanceof EnderDragon) return false;
            if (e instanceof EnderDragonPart) return true;
            if (e instanceof Enemy) return true;
        }

        if (targetMachines) {
            if (e instanceof RadarProvider detectable && !detectable.canBeSeenBy(this))
                return false;
            if (e instanceof Missile) return e.getDeltaMovement().y < 0;
            if (e instanceof CustomMissile) return e.getDeltaMovement().y < 0;
            if (e instanceof AbstractMinecart) return true;
            if (e instanceof RailCarBaseEntity) return true;
            if (e instanceof BomberEntity) return true;
        }

        if (targetPlayers) {
            if (e instanceof Player player && (player instanceof FakePlayer)) return false;
            if (e instanceof Player) return true;
        }

        return false;
    }

    public List<ItemStack> getAmmoTypesForDisplay() {
        if (ammoStacks != null) return ammoStacks;

        List<ItemStack> stacks = new ArrayList<>();
        List<BulletConfig> list = getAmmoList();
        if (list != null) {
            for (BulletConfig config : list)
                if (config != null && config.ammoItem != null) stacks.add(config.ammoStack());
        }

        ammoStacks = stacks;
        return ammoStacks;
    }

    public @Nullable BulletConfig getFirstConfigLoaded() {
        List<BulletConfig> list = getAmmoList();
        if (list == null || list.isEmpty()) return null;

        for (int i = SLOT_AMMO_FIRST; i <= SLOT_AMMO_LAST; i++) {
            ItemStack stack = getItem(i);
            if (stack.isEmpty()) continue;
            for (BulletConfig conf : list) if (conf.matchesAmmo(stack)) return conf;
        }

        return null;
    }

    protected Vec3 alongBarrel(double forward, double up, double side) {
        return new Vec3(forward, up, side)
                .zRot((float) -rotationPitch)
                .yRot((float) -(rotationYaw + Math.PI * 0.5));
    }

    protected Vec3 barrelTip() {
        return getTurretPos().add(alongBarrel(getBarrelLength(), 0, 0));
    }

    protected void muzzleFlash(Vec3 at, float size, int puffs) {
        if (!(level instanceof ServerLevel server)) return;
        PacketWire.sendToAllAround(
                MuzzleBlastLayout.encode(at.x, at.y, at.z, size, puffs),
                new TargetPoint(
                        server,
                        worldPosition.getX(),
                        worldPosition.getY(),
                        worldPosition.getZ(),
                        MUZZLE_FLASH_RANGE));
    }

    public void manualSetup() {}

    public void spawnBullet(BulletConfig bullet, float baseDamage) {
        Vec3 tip = barrelTip();

        BulletBaseMK4Entity proj =
                new BulletBaseMK4Entity(
                        level,
                        bullet,
                        baseDamage,
                        bullet.spread,
                        (float) rotationYaw,
                        (float) rotationPitch);
        proj.snapTo(tip.x, tip.y, tip.z, proj.getYRot(), proj.getXRot());
        level.addFreshEntity(proj);

        if (usesCasings()) {
            if (casingDelay() == 0) spawnCasing();
            else casingDelay = casingDelay();
        }
    }

    public boolean usesCasings() {
        return false;
    }

    public int casingDelay() {
        return 0;
    }

    protected Vec3 getCasingSpawnPos() {
        return getTurretPos();
    }

    protected @Nullable CasingEjector getEjector() {
        return null;
    }

    protected void spawnCasing() {
        if (cachedCasingConfig == null) return;
        CasingEjector ej = getEjector();
        if (ej == null) return;

        Vec3 spawn = getCasingSpawnPos();
        Vec3 motion = ej.getMotion();

        CasingCreator.composeEffect(
                level,
                spawn.x,
                spawn.y,
                spawn.z,
                (float) Math.toDegrees(rotationYaw),
                (float) Math.toDegrees(rotationPitch),
                motion.z,
                motion.y,
                motion.x,
                ej.getPitchFactor(),
                (float) (level.getRandom().nextGaussian() * 5F),
                (float) (level.getRandom().nextGaussian() * 10F),
                cachedCasingConfig.name(),
                false,
                0,
                0D,
                0);

        cachedCasingConfig = null;
    }

    public void consumeAmmo(BulletConfig ammo) {
        for (int i = SLOT_AMMO_FIRST; i <= SLOT_AMMO_LAST; i++) {
            if (ammo.matchesAmmo(getItem(i))) {
                removeItem(i, 1);
                return;
            }
        }
        setChanged();
    }

    public @Nullable List<String> getWhitelist() {
        ItemStack chip = getItem(SLOT_CHIP);
        if (chip.is(ModItems.TURRET_CHIP.get())) {
            String[] array = TurretBiometryItem.getNames(chip);
            if (array == null) return null;
            return Arrays.asList(array);
        }
        return null;
    }

    public void addName(String name) {
        ItemStack chip = getItem(SLOT_CHIP);
        if (chip.is(ModItems.TURRET_CHIP.get())) {
            TurretBiometryItem.addName(chip, name);
            markChanged();
        }
    }

    public void removeName(int index) {
        ItemStack chip = getItem(SLOT_CHIP);
        if (!chip.is(ModItems.TURRET_CHIP.get())) return;

        String[] array = TurretBiometryItem.getNames(chip);
        if (array == null) return;

        List<String> names = new ArrayList<>(Arrays.asList(array));
        TurretBiometryItem.clearNames(chip);
        if (index >= 0 && index < names.size()) names.remove(index);
        for (String name : names) TurretBiometryItem.addName(chip, name);
        markChanged();
    }

    public void handleControlButton(int meta) {
        handleButtonPacket(meta);
        markChanged();
    }

    @Override
    public void startOpen(ContainerUser user) {
        super.startOpen(user);
        if (level != null)
            WorldSounds.machine(
                    level,
                    null,
                    worldPosition,
                    ModSounds.BLOCK_OPEN_C.get(),
                    SoundSource.BLOCKS,
                    1.0F,
                    1.0F);
    }

    @Override
    public void stopOpen(ContainerUser user) {
        super.stopOpen(user);
        if (level != null)
            WorldSounds.machine(
                    level,
                    null,
                    worldPosition,
                    ModSounds.BLOCK_CLOSE_C.get(),
                    SoundSource.BLOCKS,
                    1.0F,
                    1.0F);
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public String[] getFunctionInfo() {
        return new String[] {
            FUN + "setactive" + NAME_SEP + "active (0 or 1)",
            FUN + "targetplayers" + NAME_SEP + "enabled (0 or 1)",
            FUN + "targetanimals" + NAME_SEP + "enabled (0 or 1)",
            FUN + "targetmobs" + NAME_SEP + "enabled (0 or 1)",
            FUN + "targetmachines" + NAME_SEP + "enabled (0 or 1)",
            FUN + "addwhitelist" + NAME_SEP + "name",
            FUN + "removewhitelist" + NAME_SEP + "name"
        };
    }

    @Override
    public void runRadioFunction(String name, String[] params) {
        if ((FUN + "setactive").equals(name) && params.length > 0) {
            try {
                hbm$syncSet$TurretBaseNTBlockEntity$isOn(Integer.parseInt(params[0]) == 1);
                markChanged();
            } catch (NumberFormatException ignored) {
            }
        }
        if ((FUN + "targetplayers").equals(name) && params.length > 0) {
            try {
                hbm$syncSet$TurretBaseNTBlockEntity$targetPlayers(Integer.parseInt(params[0]) == 1);
                markChanged();
            } catch (NumberFormatException ignored) {
            }
        }
        if ((FUN + "targetanimals").equals(name) && params.length > 0) {
            try {
                hbm$syncSet$TurretBaseNTBlockEntity$targetAnimals(Integer.parseInt(params[0]) == 1);
                markChanged();
            } catch (NumberFormatException ignored) {
            }
        }
        if ((FUN + "targetmobs").equals(name) && params.length > 0) {
            try {
                hbm$syncSet$TurretBaseNTBlockEntity$targetMobs(Integer.parseInt(params[0]) == 1);
                markChanged();
            } catch (NumberFormatException ignored) {
            }
        }
        if ((FUN + "targetmachines").equals(name) && params.length > 0) {
            try {
                hbm$syncSet$TurretBaseNTBlockEntity$targetMachines(
                        Integer.parseInt(params[0]) == 1);
                markChanged();
            } catch (NumberFormatException ignored) {
            }
        }
        if ((FUN + "addwhitelist").equals(name) && params.length > 0) {
            addName(params[0]);
            markChanged();
        }
        if ((FUN + "removewhitelist").equals(name) && params.length > 0) {
            List<String> whitelist = getWhitelist();
            if (whitelist != null && whitelist.contains(params[0]))
                removeName(whitelist.indexOf(params[0]));
            markChanged();
        }
    }

    public void handleButtonPacket(int meta) {
        switch (meta) {
            case 0 -> hbm$syncSet$TurretBaseNTBlockEntity$isOn(!isOn);
            case 1 -> hbm$syncSet$TurretBaseNTBlockEntity$targetPlayers(!targetPlayers);
            case 2 -> hbm$syncSet$TurretBaseNTBlockEntity$targetAnimals(!targetAnimals);
            case 3 -> hbm$syncSet$TurretBaseNTBlockEntity$targetMobs(!targetMobs);
            case 4 -> hbm$syncSet$TurretBaseNTBlockEntity$targetMachines(!targetMachines);
            default -> {}
        }
    }

    public double getAcceptableInaccuracy() {
        return 5;
    }

    public double getTurretYawSpeed() {
        return 4.5D;
    }

    public double getTurretPitchSpeed() {
        return 3D;
    }

    public double getTurretDepression() {
        return 30D;
    }

    public double getTurretElevation() {
        return 30D;
    }

    public int getDetectorInterval() {
        return 10;
    }

    public double getDetectorRange() {
        return 32D;
    }

    public double getDetectorGrace() {
        return 3D;
    }

    public double getHeightOffset() {
        return 1.5D;
    }

    public double getBarrelLength() {
        return 1.0D;
    }

    public boolean hasThermalVision() {
        return true;
    }

    public Vec3 getTurretPos() {
        Vec3 offset = getHorizontalOffset();
        return new Vec3(
                worldPosition.getX() + offset.x,
                worldPosition.getY() + getHeightOffset(),
                worldPosition.getZ() + offset.z);
    }

    public Vec3 getHorizontalOffset() {
        Direction facing = MultiblockCoreBlock.coreFacing(getBlockState());
        double x = facing == Direction.NORTH || facing == Direction.WEST ? 1D : 0D;
        double z = facing == Direction.NORTH || facing == Direction.EAST ? 1D : 0D;
        return new Vec3(x, 0D, z);
    }

    protected boolean hasConnectorPlugs() {
        return true;
    }

    protected @Nullable Fluid connectorFluid() {
        return null;
    }

    public void markConnectorsDirty() {
        connectorsDirty = true;
    }

    private void refreshConnectors(ServerLevel world) {
        if (!hasConnectorPlugs()) return;
        if (!plugCapsWatched) {
            plugCapsWatched = true;
            plugCapListener =
                    Caps.listenForCapChanges(
                            world,
                            List.of(plugCells()),
                            () -> {
                                connectorsDirty = true;
                                return !isRemoved();
                            });
        }
        Fluid fluid = connectorFluid();
        if (!connectorsDirty && fluid == connectorFluidSeen) return;
        connectorsDirty = false;
        connectorFluidSeen = fluid;
        BlockPos[] cells = plugCells();
        int mask = 0;
        for (int i = 0; i < cells.length; i++)
            mask |= plugBit(world, cells[i], PLUG_FACES[i], fluid) << i;
        hbm$syncSet$TurretBaseNTBlockEntity$connectorMask(mask);
    }

    private BlockPos[] plugCells() {
        Vec3 offset = getHorizontalOffset();
        int x = worldPosition.getX() + (int) offset.x;
        int y = worldPosition.getY();
        int z = worldPosition.getZ() + (int) offset.z;
        return new BlockPos[] {
            new BlockPos(x - 2, y, z),
            new BlockPos(x - 2, y, z - 1),
            new BlockPos(x - 1, y, z + 1),
            new BlockPos(x, y, z + 1),
            new BlockPos(x + 1, y, z),
            new BlockPos(x + 1, y, z - 1),
            new BlockPos(x, y, z - 2),
            new BlockPos(x - 1, y, z - 2)
        };
    }

    private static final Direction[] PLUG_FACES = {
        Direction.EAST,
        Direction.EAST,
        Direction.NORTH,
        Direction.NORTH,
        Direction.WEST,
        Direction.WEST,
        Direction.SOUTH,
        Direction.SOUTH
    };

    private static int plugBit(
            ServerLevel world, BlockPos cell, Direction side, @Nullable Fluid fluid) {
        boolean connects =
                PhysicalGraph.powerContact(world, cell, side)
                        || (fluid != null && PhysicalGraph.fluidContact(world, cell, side, fluid));
        return connects ? 1 : 0;
    }

    public Vec3 getEntityPos(Entity e) {
        return new Vec3(e.getX(), e.getY() + e.getBbHeight() * 0.5, e.getZ());
    }

    public boolean hasPower() {
        return getPower() >= getConsumption();
    }

    public boolean isOn() {
        return isOn;
    }

    public long getConsumption() {
        return 100;
    }

    public int getPowerScaled(int scale) {
        return (int) (power * scale / getMaxPower());
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public void setPower(long i) {
        hbm$syncSet$TurretBaseNTBlockEntity$power(i);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return AMMO_SLOTS;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return true;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        hbm$syncSet$TurretBaseNTBlockEntity$power(input.getLongOr("power", 0L));
        hbm$syncSet$TurretBaseNTBlockEntity$isOn(input.getBooleanOr("isOn", false));
        hbm$syncSet$TurretBaseNTBlockEntity$targetPlayers(
                input.getBooleanOr("targetPlayers", false));
        hbm$syncSet$TurretBaseNTBlockEntity$targetAnimals(
                input.getBooleanOr("targetAnimals", false));
        hbm$syncSet$TurretBaseNTBlockEntity$targetMobs(input.getBooleanOr("targetMobs", false));
        hbm$syncSet$TurretBaseNTBlockEntity$targetMachines(
                input.getBooleanOr("targetMachines", false));
        hbm$syncSet$TurretBaseNTBlockEntity$stattrak(input.getIntOr("stattrak", 0));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("power", power);
        output.putBoolean("isOn", isOn);
        output.putBoolean("targetPlayers", targetPlayers);
        output.putBoolean("targetAnimals", targetAnimals);
        output.putBoolean("targetMobs", targetMobs);
        output.putBoolean("targetMachines", targetMachines);
        output.putInt("stattrak", stattrak);
    }

    private void writeTarget(ByteBuf output) {
        output.writeBoolean(tPos != null);
        if (tPos != null) {
            output.writeDouble(tPos.x);
            output.writeDouble(tPos.y);
            output.writeDouble(tPos.z);
        }
    }

    private void readTarget(ByteBuf input) {
        hbm$syncSet$TurretBaseNTBlockEntity$tPos(
                input.readBoolean()
                        ? new Vec3(input.readDouble(), input.readDouble(), input.readDouble())
                        : null);
    }

    private void readPitch(ByteBuf input) {
        syncRotationPitch = input.readDouble();
    }

    private void readYaw(ByteBuf input) {
        syncRotationYaw = input.readDouble();
    }

    private void writeConnectors(ByteBuf output) {
        output.writeByte(connectorMask);
    }

    private void readConnectors(ByteBuf input) {
        hbm$syncSet$TurretBaseNTBlockEntity$connectorMask(input.readUnsignedByte());
    }

    @Override
    public long syncUnitMask() {
        return 0x7ffL;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> writeTarget(output);
            case 1 -> output.writeDouble(this.rotationPitch);
            case 2 -> output.writeDouble(this.rotationYaw);
            case 3 -> output.writeLong(this.power);
            case 4 -> output.writeBoolean(this.isOn);
            case 5 -> output.writeBoolean(this.targetPlayers);
            case 6 -> output.writeBoolean(this.targetAnimals);
            case 7 -> output.writeBoolean(this.targetMobs);
            case 8 -> output.writeBoolean(this.targetMachines);
            case 9 -> output.writeInt(this.stattrak);
            case 10 -> writeConnectors(output);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> readTarget(input);
            case 1 -> readPitch(input);
            case 2 -> readYaw(input);
            case 3 -> this.hbm$syncSet$TurretBaseNTBlockEntity$power(input.readLong());
            case 4 -> this.hbm$syncSet$TurretBaseNTBlockEntity$isOn(input.readBoolean());
            case 5 -> this.hbm$syncSet$TurretBaseNTBlockEntity$targetPlayers(input.readBoolean());
            case 6 -> this.hbm$syncSet$TurretBaseNTBlockEntity$targetAnimals(input.readBoolean());
            case 7 -> this.hbm$syncSet$TurretBaseNTBlockEntity$targetMobs(input.readBoolean());
            case 8 -> this.hbm$syncSet$TurretBaseNTBlockEntity$targetMachines(input.readBoolean());
            case 9 -> this.hbm$syncSet$TurretBaseNTBlockEntity$stattrak(input.readInt());
            case 10 -> readConnectors(input);
            default -> throw new IllegalArgumentException();
        }
    }

    public final void hbm$syncSet$TurretBaseNTBlockEntity$rotationYaw(double next) {
        if (Double.doubleToRawLongBits(this.rotationYaw) != Double.doubleToRawLongBits(next)) {
            this.rotationYaw = next;
            if ((1L << 2) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 2);
        }
    }

    public final void hbm$syncSet$TurretBaseNTBlockEntity$rotationPitch(double next) {
        if (Double.doubleToRawLongBits(this.rotationPitch) != Double.doubleToRawLongBits(next)) {
            this.rotationPitch = next;
            if ((1L << 1) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 1);
        }
    }

    public final void hbm$syncSet$TurretBaseNTBlockEntity$isOn(boolean next) {
        if (this.isOn != next) {
            this.isOn = next;
            if ((1L << 4) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 4);
        }
    }

    public final void hbm$syncSet$TurretBaseNTBlockEntity$power(long next) {
        if (this.power != next) {
            this.power = next;
            if ((1L << 3) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 3);
        }
    }

    public final void hbm$syncSet$TurretBaseNTBlockEntity$targetPlayers(boolean next) {
        if (this.targetPlayers != next) {
            this.targetPlayers = next;
            if ((1L << 5) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 5);
        }
    }

    public final void hbm$syncSet$TurretBaseNTBlockEntity$targetAnimals(boolean next) {
        if (this.targetAnimals != next) {
            this.targetAnimals = next;
            if ((1L << 6) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 6);
        }
    }

    public final void hbm$syncSet$TurretBaseNTBlockEntity$targetMobs(boolean next) {
        if (this.targetMobs != next) {
            this.targetMobs = next;
            if ((1L << 7) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 7);
        }
    }

    public final void hbm$syncSet$TurretBaseNTBlockEntity$targetMachines(boolean next) {
        if (this.targetMachines != next) {
            this.targetMachines = next;
            if ((1L << 8) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 8);
        }
    }

    public final void hbm$syncSet$TurretBaseNTBlockEntity$stattrak(int next) {
        if (this.stattrak != next) {
            this.stattrak = next;
            if ((1L << 9) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 9);
        }
    }

    public final void hbm$syncSet$TurretBaseNTBlockEntity$tPos(Vec3 next) {
        if (this.tPos != next) {
            boolean bound = syncBound();
            if (bound) com.hbm.packet.SyncBindings.unbind(this, this.tPos);
            this.tPos = next;
            if (bound) bindSyncValue(next, 3, 1L);
            if ((1L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L);
        }
    }

    public final void hbm$syncSet$TurretBaseNTBlockEntity$connectorMask(int next) {
        if (this.connectorMask != next) {
            this.connectorMask = next;
            if ((1L << 10) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 10);
        }
    }

    static {
        com.hbm.packet.SyncBindings.field(TurretBaseNTBlockEntity.class, "tPos", 3, 1L);
    }
}
