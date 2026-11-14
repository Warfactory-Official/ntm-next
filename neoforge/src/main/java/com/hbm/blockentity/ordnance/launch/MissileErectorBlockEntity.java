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

package com.hbm.blockentity.ordnance.launch;

import com.hbm.blockentity.MachineSoundProvider;
import com.hbm.blockentity.SoundEmitter;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.entity.missile.Missile;
import com.hbm.inventory.container.LaunchPadLargeMenu;
import com.hbm.items.weapon.MissileItem.MissileFormFactor;
import com.hbm.items.weapon.MissileItem;
import com.hbm.particle.CoolingTowerParticleOptions;
import com.hbm.particle.HbmParticles;
import com.hbm.sound.AudioSystem;
import com.hbm.sound.ModSounds;
import com.hbm.sound.SoundHandle;
import com.hbm.sound.WorldSounds;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

public final class MissileErectorBlockEntity extends LaunchPlatformBlockEntity
        implements MenuProvider, MachineSoundProvider, SoundEmitter {

    private static final long POSE_TARGETS = (1L << 10) | (1L << 11);

    public float liftHeight;

    {
        hbm$syncSet$MissileErectorBlockEntity$liftHeight(1F);
    }

    public float previousLiftHeight = 1F;
    public float armAngle;

    {
        hbm$syncSet$MissileErectorBlockEntity$armAngle(90F);
    }

    public float previousArmAngle = 90F;
    public boolean standingMissile;
    public boolean deploymentAvailable;
    public boolean liftMoving;
    public boolean armMoving;
    public int formIndex;

    {
        hbm$syncSet$MissileErectorBlockEntity$formIndex(-1);
    }

    private final AABB exhaustBounds;
    private int pauseTicks = 20;
    private boolean pendingStanding;
    private float receivedLiftHeight = 1F;
    private float receivedArmAngle = 90F;
    private int convergenceTicks;
    private @Nullable SoundHandle liftLoop;
    private @Nullable SoundHandle armLoop;

    public MissileErectorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LAUNCHPAD_LARGE.get(), pos, state);
        exhaustBounds =
                new AABB(
                        pos.getX() - 0.5D,
                        pos.getY(),
                        pos.getZ() - 0.5D,
                        pos.getX() + 1.5D,
                        pos.getY() + 10D,
                        pos.getZ() + 1.5D);
    }

    @Override
    protected double launchHeight() {
        return 2D;
    }

    @Override
    protected int signalRadius() {
        return 4;
    }

    @Override
    public boolean mechanismReady() {
        return standingMissile;
    }

    @Override
    protected void onMissileLaunched() {
        hbm$syncSet$MissileErectorBlockEntity$standingMissile(false);
    }

    @Override
    protected boolean syncMuffled() {
        return true;
    }

    @Override
    public void tickServer() {
        float oldLift = liftHeight;
        float oldArm = armAngle;
        int oldForm = formIndex;
        boolean oldStanding = standingMissile;
        boolean oldAvailable = deploymentAvailable;
        boolean wasLiftMoving = liftMoving;
        boolean wasArmMoving = armMoving;

        advanceMechanism();
        hbm$syncSet$MissileErectorBlockEntity$liftMoving(liftHeight != oldLift);
        hbm$syncSet$MissileErectorBlockEntity$armMoving(armAngle != oldArm);
        if (wasLiftMoving && !liftMoving) {
            WorldSounds.machine(
                    level,
                    null,
                    worldPosition,
                    ModSounds.DOOR_WGH_STOP.get(),
                    SoundSource.BLOCKS,
                    getVolume(2F),
                    1F);
        }
        if (wasArmMoving && !armMoving) {
            WorldSounds.machine(
                    level,
                    null,
                    worldPosition,
                    ModSounds.DOOR_GARAGE_STOP.get(),
                    SoundSource.BLOCKS,
                    getVolume(2F),
                    1F);
        }
        if (oldLift != liftHeight
                || oldArm != armAngle
                || oldForm != formIndex
                || oldStanding != standingMissile
                || oldAvailable != deploymentAvailable) {
            setChanged();
        }
        if (!acceptsMissile(getItem(SLOT_MISSILE)) || !hasPropellant()) {
            hbm$syncSet$LaunchPlatformBlockEntity$statusCode(MISSING);
        } else {
            hbm$syncSet$LaunchPlatformBlockEntity$statusCode(
                    standingMissile && launchReady() ? READY : LOADING);
        }
        advancePlatform();
    }

    private void advanceMechanism() {
        var payload = getItem(SLOT_MISSILE);
        if (acceptsMissile(payload)) {
            var missile = (MissileItem) payload.getItem();
            hbm$syncSet$MissileErectorBlockEntity$formIndex(missile.formFactor.ordinal());
            if (armAngle == 90F && liftHeight == 1F)
                hbm$syncSet$MissileErectorBlockEntity$deploymentAvailable(true);
        } else {
            hbm$syncSet$MissileErectorBlockEntity$deploymentAvailable(false);
            hbm$syncSet$MissileErectorBlockEntity$standingMissile(false);
            pauseTicks = 20;
        }
        if (storedHe < LAUNCH_HE) return;

        float armStep = 1.5F;
        float liftStep = 0.025F;
        if (formIndex == MissileFormFactor.HUGE.ordinal()
                || formIndex == MissileFormFactor.ATLAS.ordinal()) {
            armStep *= 0.5F;
            liftStep *= 0.5F;
        }
        if (pauseTicks > 0 && deploymentAvailable) {
            pauseTicks--;
            if (pauseTicks < 10 && pendingStanding) {
                hbm$syncSet$MissileErectorBlockEntity$standingMissile(true);
                pendingStanding = false;
            }
            return;
        }
        if (deploymentAvailable && !standingMissile) {
            if (armAngle > 0F) {
                hbm$syncSet$MissileErectorBlockEntity$armAngle(Math.max(0F, armAngle - armStep));
                if (armAngle == 0F) pauseTicks = 20;
            } else if (liftHeight > 0F) {
                hbm$syncSet$MissileErectorBlockEntity$liftHeight(
                        Math.max(0F, liftHeight - liftStep));
                if (liftHeight == 0F) {
                    pauseTicks = 20;
                    pendingStanding = true;
                }
            }
        } else if (armAngle < 90F) {
            hbm$syncSet$MissileErectorBlockEntity$armAngle(Math.min(90F, armAngle + armStep));
        } else if (liftHeight < 1F) {
            hbm$syncSet$MissileErectorBlockEntity$liftHeight(Math.min(1F, liftHeight + liftStep));
            if (liftHeight == 1F) {
                hbm$syncSet$MissileErectorBlockEntity$deploymentAvailable(true);
                pauseTicks = 20;
            }
        }
    }

    @Override
    public void tickClient() {
        previousLiftHeight = liftHeight;
        previousArmAngle = armAngle;
        if (convergenceTicks > 0) {
            hbm$syncSet$MissileErectorBlockEntity$liftHeight(
                    (float) (liftHeight + ((receivedLiftHeight - liftHeight) / convergenceTicks)));
            hbm$syncSet$MissileErectorBlockEntity$armAngle(
                    (float) (armAngle + ((receivedArmAngle - armAngle) / convergenceTicks)));
            convergenceTicks--;
        }
        liftLoop = updateLoop(liftLoop, liftMoving, ModSounds.DOOR_WGH_START.get(), 0.75F);
        armLoop = updateLoop(armLoop, armMoving, ModSounds.DOOR_GARAGE_MOVE.get(), 1.5F);
        emitVapour();
        emitExhaust();
    }

    private @Nullable SoundHandle updateLoop(
            @Nullable SoundHandle loop, boolean moving, SoundEvent sound, float volume) {
        if (!moving) {
            if (loop != null) loop.stop();
            return null;
        }
        if (loop == null || !loop.isPlaying()) {
            if (loop != null) loop.stop();
            loop =
                    AudioSystem.getLoopedSound(
                            sound,
                            SoundSource.BLOCKS,
                            worldPosition.getX() + 0.5F,
                            worldPosition.getY() + 0.5F,
                            worldPosition.getZ() + 0.5F,
                            getVolume(volume),
                            25F,
                            1F,
                            5);
            if (loop != null) loop.start(worldPosition);
        }
        if (loop != null) {
            loop.setVolume(getVolume(volume));
            loop.refresh();
        }
        return loop;
    }

    private void emitVapour() {
        if (!standingMissile || oxidizerTank.getFill() == 0) return;
        if (formIndex != MissileFormFactor.HUGE.ordinal()
                && formIndex != MissileFormFactor.ATLAS.ordinal()) return;
        var random = level.getRandom();
        int lifetime = 70 + random.nextInt(30);
        double x = worldPosition.getX() + 0.5D + random.nextGaussian() * 0.5D;
        double z = worldPosition.getZ() + 0.5D + random.nextGaussian() * 0.5D;
        var vapour =
                new CoolingTowerParticleOptions(
                        0.5F,
                        2F,
                        0F,
                        lifetime,
                        0.05F,
                        false,
                        2F,
                        CoolingTowerParticleOptions.NO_TINT);
        for (int particle = 0; particle < 3; particle++) {
            level.addParticle(vapour, true, true, x, worldPosition.getY() + 2D, z, 0D, 0D, 0D);
        }
    }

    private void emitExhaust() {
        if (level.getEntitiesOfClass(Missile.class, exhaustBounds).isEmpty()) return;
        var facing = MultiblockCoreBlock.coreFacing(getBlockState());
        var random = level.getRandom();
        for (int particle = 0; particle < 15; particle++) {
            var direction = random.nextBoolean() ? facing.getOpposite() : facing;
            float motionX = (float) (random.nextGaussian() * 0.15F + 0.75D);
            motionX *= direction.getStepX();
            float motionZ = (float) (random.nextGaussian() * 0.15F + 0.75D);
            motionZ *= direction.getStepZ();
            level.addParticle(
                    HbmParticles.LAUNCH_SMOKE.get(),
                    true,
                    true,
                    worldPosition.getX() + 0.5D,
                    worldPosition.getY() + 0.25D,
                    worldPosition.getZ() + 0.5D,
                    motionX,
                    0D,
                    motionZ);
        }
    }

    @Override
    public boolean tickSound() {
        return liftMoving || armMoving;
    }

    @Override
    public void setRemoved() {
        if (liftLoop != null) liftLoop.stop();
        if (armLoop != null) armLoop.stop();
        super.setRemoved();
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new LaunchPadLargeMenu(containerId, playerInventory, this);
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 4095L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 5 -> output.writeBoolean(liftMoving);
            case 6 -> output.writeBoolean(armMoving);
            case 7 -> output.writeBoolean(standingMissile);
            case 8 -> output.writeBoolean(deploymentAvailable);
            case 9 -> output.writeByte(formIndex);
            case 10 -> output.writeFloat(liftHeight);
            case 11 -> output.writeFloat(armAngle);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 5 -> hbm$syncSet$MissileErectorBlockEntity$liftMoving(input.readBoolean());
            case 6 -> hbm$syncSet$MissileErectorBlockEntity$armMoving(input.readBoolean());
            case 7 -> hbm$syncSet$MissileErectorBlockEntity$standingMissile(input.readBoolean());
            case 8 ->
                    hbm$syncSet$MissileErectorBlockEntity$deploymentAvailable(input.readBoolean());
            case 9 -> hbm$syncSet$MissileErectorBlockEntity$formIndex(input.readByte());
            case 10 -> receivedLiftHeight = input.readFloat();
            case 11 -> receivedArmAngle = input.readFloat();
            default -> super.readSyncUnit(unit, input);
        }
    }

    @Override
    public void afterSyncUnits(long units) {
        if ((units & POSE_TARGETS) != 0
                && (liftHeight != receivedLiftHeight || armAngle != receivedArmAngle)) {
            convergenceTicks = 3;
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("erected", standingMissile);
        output.putBoolean("readyToLoad", deploymentAvailable);
        output.putFloat("lift", liftHeight);
        output.putFloat("erector", armAngle);
        output.putInt("formFactor", formIndex);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        hbm$syncSet$MissileErectorBlockEntity$standingMissile(input.getBooleanOr("erected", false));
        hbm$syncSet$MissileErectorBlockEntity$deploymentAvailable(
                input.getBooleanOr("readyToLoad", false));
        hbm$syncSet$MissileErectorBlockEntity$liftHeight(input.getFloatOr("lift", 1F));
        hbm$syncSet$MissileErectorBlockEntity$armAngle(input.getFloatOr("erector", 90F));
        hbm$syncSet$MissileErectorBlockEntity$formIndex(input.getIntOr("formFactor", -1));
        previousLiftHeight = liftHeight;
        previousArmAngle = armAngle;
        receivedLiftHeight = liftHeight;
        receivedArmAngle = armAngle;
        pauseTicks = 20;
    }

    public final void hbm$syncSet$MissileErectorBlockEntity$liftHeight(float next) {
        if (Float.floatToRawIntBits(this.liftHeight) != Float.floatToRawIntBits(next)) {
            this.liftHeight = next;
            if ((1L << 10) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 10);
        }
    }

    public final void hbm$syncSet$MissileErectorBlockEntity$armAngle(float next) {
        if (Float.floatToRawIntBits(this.armAngle) != Float.floatToRawIntBits(next)) {
            this.armAngle = next;
            if ((1L << 11) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 11);
        }
    }

    public final void hbm$syncSet$MissileErectorBlockEntity$standingMissile(boolean next) {
        if (this.standingMissile != next) {
            this.standingMissile = next;
            if ((1L << 7) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 7);
        }
    }

    public final void hbm$syncSet$MissileErectorBlockEntity$deploymentAvailable(boolean next) {
        if (this.deploymentAvailable != next) {
            this.deploymentAvailable = next;
            if ((1L << 8) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 8);
        }
    }

    public final void hbm$syncSet$MissileErectorBlockEntity$liftMoving(boolean next) {
        if (this.liftMoving != next) {
            this.liftMoving = next;
            if ((1L << 5) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 5);
        }
    }

    public final void hbm$syncSet$MissileErectorBlockEntity$armMoving(boolean next) {
        if (this.armMoving != next) {
            this.armMoving = next;
            if ((1L << 6) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 6);
        }
    }

    public final void hbm$syncSet$MissileErectorBlockEntity$formIndex(int next) {
        if (this.formIndex != next) {
            this.formIndex = next;
            if ((1L << 9) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 9);
        }
    }

    private boolean hbm$soundActive;

    public boolean soundStarted(boolean active) {
        boolean started = active && !hbm$soundActive;
        hbm$soundActive = active;
        return started;
    }
}
