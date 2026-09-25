// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.bomb;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.entity.missile.EntityMissileBaseNT;
import com.hbm.inventory.container.MenuLaunchPadLarge;
import com.hbm.items.weapon.ItemMissile.MissileFormFactor;
import com.hbm.items.weapon.ItemMissile;
import com.hbm.packet.SyncField;
import com.hbm.particle.CoolingTowerParticleOptions;
import com.hbm.particle.HbmParticles;
import com.hbm.sound.AudioSystem;
import com.hbm.sound.AudioWrapper;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.Audible;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

public class BlockEntityLaunchPadLarge extends BlockEntityLaunchPadBase implements Audible {

    private static final double LAUNCH_OFFSET = 2D;
    private static final float ERECTOR_SPEED = 1.5F;
    private static final float LIFT_SPEED = 0.025F;

    private static final float ERECTOR_REST = 90F;
    private static final float LIFT_REST = 1F;
    private static final int PAUSE_TICKS = 20;

    private static final int ERECT_AT = 10;
    private static final int FOOTPRINT_RADIUS = 4;

    private static final int SYNC_TICKS = 3;

    @SyncField(units = 1L << 9)
    public int formFactor = -1;

    @SyncField(units = 1L << 7)
    public boolean erected;

    @SyncField(units = 1L << 8)
    public boolean readyToLoad;

    @SyncField(units = 1L << 10)
    public float lift = LIFT_REST;

    @SyncField(units = 1L << 11)
    public float erector = ERECTOR_REST;

    public float prevLift = LIFT_REST;
    public float prevErector = ERECTOR_REST;
    private boolean scheduleErect;
    private int delay = PAUSE_TICKS;

    @SyncField(units = 1L << 5)
    private boolean liftMoving;

    @SyncField(units = 1L << 6)
    private boolean erectorMoving;

    private float syncLift = LIFT_REST;
    private float syncErector = ERECTOR_REST;
    private int sync;
    private @Nullable AudioWrapper audioLift;
    private @Nullable AudioWrapper audioErector;

    public BlockEntityLaunchPadLarge(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LAUNCHPAD_LARGE.get(), pos, state);
    }

    @Override
    public void tickServer() {
        prevLift = lift;
        prevErector = erector;

        float erectorSpeed = ERECTOR_SPEED;
        float liftSpeed = LIFT_SPEED;

        ItemStack missile = getItem(SLOT_MISSILE);
        if (isMissileValid(missile)) {
            if (missile.getItem() instanceof ItemMissile item) {
                formFactor = item.formFactor.ordinal();
                if (item.formFactor == MissileFormFactor.ATLAS
                        || item.formFactor == MissileFormFactor.HUGE) {
                    erectorSpeed /= 2F;
                    liftSpeed /= 2F;
                }
            }
            if (erector == ERECTOR_REST && lift == LIFT_REST) readyToLoad = true;
        } else {
            readyToLoad = false;
            erected = false;
            delay = PAUSE_TICKS;
        }

        if (power >= LAUNCH_POWER) {
            if (delay > 0) {
                delay--;
                if (delay < ERECT_AT && scheduleErect) {
                    erected = true;
                    scheduleErect = false;
                }
                if (missile.isEmpty() || !readyToLoad) retract(erectorSpeed, liftSpeed);
            } else if (!erected && readyToLoad) {
                state = STATE_LOADING;
                if (erector != 0F) {
                    erector = Math.max(erector - erectorSpeed, 0F);
                    if (erector == 0F) delay = PAUSE_TICKS;
                } else if (lift > 0F) {
                    lift = Math.max(lift - liftSpeed, 0F);
                    if (lift == 0F) {
                        scheduleErect = true;
                        delay = PAUSE_TICKS;
                    }
                }
            } else {
                retract(erectorSpeed, liftSpeed);
            }
        }

        if (!hasFuel() || !isMissileValid(missile)) state = STATE_MISSING;
        if (erected && canLaunch()) state = STATE_READY;

        boolean prevLiftMoving = liftMoving;
        boolean prevErectorMoving = erectorMoving;
        liftMoving = prevLift != lift;
        erectorMoving = prevErector != erector;

        BlockPos core = getBlockPos();
        if (prevLiftMoving && !liftMoving) {
            getLevel()
                    .playSound(
                            null, core, ModSounds.DOOR_WGH_STOP.get(), SoundSource.BLOCKS, 2F, 1F);
        }
        if (prevErectorMoving && !erectorMoving) {
            getLevel()
                    .playSound(
                            null,
                            core,
                            ModSounds.DOOR_GARAGE_STOP.get(),
                            SoundSource.BLOCKS,
                            2F,
                            1F);
        }

        tickShared();
    }

    private void retract(float erectorSpeed, float liftSpeed) {
        if (erector < ERECTOR_REST) {
            erector = Math.min(erector + erectorSpeed, ERECTOR_REST);
            if (erector == ERECTOR_REST) delay = PAUSE_TICKS;
        } else if (lift < LIFT_REST) {
            lift = Math.min(lift + liftSpeed, LIFT_REST);
            if (lift == LIFT_REST) {
                readyToLoad = true;
                delay = PAUSE_TICKS;
            }
        }
    }

    @Override
    public void tickClient() {
        prevLift = lift;
        prevErector = erector;

        if (sync > 0) {
            lift += (syncLift - lift) / sync;
            erector += (syncErector - erector) / sync;
            sync--;
        } else {
            lift = syncLift;
            erector = syncErector;
        }

        audioLift = drive(audioLift, liftMoving, ModSounds.DOOR_WGH_START.get(), 0.75F);
        audioErector = drive(audioErector, erectorMoving, ModSounds.DOOR_GARAGE_MOVE.get(), 1.5F);

        if (erected
                && (formFactor == MissileFormFactor.HUGE.ordinal()
                        || formFactor == MissileFormFactor.ATLAS.ordinal())
                && oxidizerTank.getFill() > 0) {
            CoolingTowerParticleOptions vapour =
                    new CoolingTowerParticleOptions.Builder()
                            .setBaseScale(0.5F)
                            .setMaxScale(2F)
                            .setLift(0F)
                            .setLife(70 + level.getRandom().nextInt(30))
                            .setStrafe(0.05F)
                            .noWind()
                            .alphaMod(2F)
                            .build();
            for (int i = 0; i < 3; i++) {
                level.addParticle(
                        vapour,
                        true,
                        false,
                        worldPosition.getX() + 0.5D + level.getRandom().nextGaussian() * 0.5D,
                        worldPosition.getY() + 2D,
                        worldPosition.getZ() + 0.5D + level.getRandom().nextGaussian() * 0.5D,
                        0D,
                        0D,
                        0D);
            }
        }

        if (level.getEntitiesOfClass(
                        EntityMissileBaseNT.class,
                        new AABB(
                                worldPosition.getX() - .5D,
                                worldPosition.getY(),
                                worldPosition.getZ() - .5D,
                                worldPosition.getX() + 1.5D,
                                worldPosition.getY() + 10D,
                                worldPosition.getZ() + 1.5D))
                .isEmpty()) return;
        Direction facing = BlockMultiblockCore.coreFacing(getBlockState());
        for (int i = 0; i < 15; i++) {
            Direction direction = level.getRandom().nextBoolean() ? facing : facing.getOpposite();
            double motionX = level.getRandom().nextGaussian() * .15D + .75D;
            double motionZ = level.getRandom().nextGaussian() * .15D + .75D;
            level.addParticle(
                    HbmParticles.LAUNCH_SMOKE.get(),
                    true,
                    false,
                    worldPosition.getX() + .5D,
                    worldPosition.getY() + .25D,
                    worldPosition.getZ() + .5D,
                    motionX * direction.getStepX(),
                    0D,
                    motionZ * direction.getStepZ());
        }
    }

    private @Nullable AudioWrapper drive(
            @Nullable AudioWrapper audio, boolean running, SoundEvent sound, float volume) {
        if (!running) {
            if (audio != null) audio.stopSound();
            return null;
        }
        if (audio == null || !audio.isPlaying()) {
            if (audio != null) audio.stopSound();
            audio =
                    AudioSystem.getLoopedSound(
                            sound,
                            SoundSource.BLOCKS,
                            worldPosition.getX(),
                            worldPosition.getY(),
                            worldPosition.getZ(),
                            volume,
                            25F,
                            1.0F,
                            5);
            audio.startSound();
        }
        audio.updateVolume(getVolume(volume));
        audio.keepAlive();
        return audio;
    }

    public void refreshRedstone() {
        BlockPos core = getBlockPos();
        boolean powered = false;
        for (int dx = -FOOTPRINT_RADIUS; dx <= FOOTPRINT_RADIUS && !powered; dx++) {
            for (int dz = -FOOTPRINT_RADIUS; dz <= FOOTPRINT_RADIUS && !powered; dz++) {
                if (getLevel().hasNeighborSignal(core.offset(dx, 0, dz))) powered = true;
            }
        }
        redstone = powered;
    }

    @Override
    public boolean isReadyForLaunch() {
        return erected && readyToLoad;
    }

    @Override
    protected double getLaunchOffset() {
        return LAUNCH_OFFSET;
    }

    @Override
    protected void afterLaunch() {
        erected = false;
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 0xfe0L;
    }

    @Override
    protected boolean syncMuffled() {
        return true;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 5 -> output.writeBoolean(liftMoving);
            case 6 -> output.writeBoolean(erectorMoving);
            case 7 -> output.writeBoolean(erected);
            case 8 -> output.writeBoolean(readyToLoad);
            case 9 -> output.writeByte(formFactor);
            case 10 -> output.writeFloat(lift);
            case 11 -> output.writeFloat(erector);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 5 -> liftMoving = input.readBoolean();
            case 6 -> erectorMoving = input.readBoolean();
            case 7 -> erected = input.readBoolean();
            case 8 -> readyToLoad = input.readBoolean();
            case 9 -> formFactor = input.readByte();
            case 10 -> syncLift = input.readFloat();
            case 11 -> syncErector = input.readFloat();
            default -> super.readSyncUnit(unit, input);
        }
    }

    @Override
    public void afterSyncUnits(long units) {
        super.afterSyncUnits(units);
        if ((units & 0xc00L) != 0 && (lift != syncLift || erector != syncErector))
            sync = SYNC_TICKS;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        erected = input.getBooleanOr("erected", false);
        readyToLoad = input.getBooleanOr("readyToLoad", false);
        lift = input.getFloatOr("lift", LIFT_REST);
        erector = input.getFloatOr("erector", ERECTOR_REST);
        formFactor = input.getIntOr("formFactor", -1);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("erected", erected);
        output.putBoolean("readyToLoad", readyToLoad);
        output.putFloat("lift", lift);
        output.putFloat("erector", erector);
        output.putInt("formFactor", formFactor);
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new MenuLaunchPadLarge(containerId, inventory, this);
    }
}
