// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.projectile;

import com.hbm.api.entity.IRadarDetectableNT;
import com.hbm.blocks.ModBlocks;
import com.hbm.client.ClientEffects;
import com.hbm.client.ClientPlayerAccess;
import com.hbm.data.WorldData;
import com.hbm.entity.ModEntities;
import com.hbm.explosion.ExplosionLarge;
import com.hbm.platform.Services;
import com.hbm.sound.AudioSystem;
import com.hbm.sound.AudioWrapper;
import com.hbm.sound.ModSounds;
import com.hbm.util.DamageResistanceHandler;
import com.hbm.util.DropHeight;
import com.hbm.world.feature.Meteorite;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

public class EntityMeteor extends Entity implements IRadarDetectableNT {

    public boolean safe = false;
    private AudioWrapper audioFly;

    public EntityMeteor(EntityType<? extends EntityMeteor> type, Level level) {
        super(type, level);
    }

    public EntityMeteor(Level level) {
        this(ModEntities.METEOR.get(), level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {}

    @Override
    public void tick() {
        if (!level().isClientSide() && !WorldData.ENABLE_METEOR_STRIKES.get()) {
            discard();
            return;
        }

        this.xo = getX();
        this.yo = getY();
        this.zo = getZ();

        Vec3 motion = getDeltaMovement();
        setDeltaMovement(motion.x, Math.max(motion.y - 0.03D, -2.5D), motion.z);
        move(MoverType.SELF, getDeltaMovement());

        if (level() instanceof ServerLevel server
                && getY() < DropHeight.clearOfTerrain(server, 260, getX(), getZ())) {
            clearMeteorPath(server, (int) getX(), (int) getY(), (int) getZ());

            if (onGround()) {
                server.explode(
                        this,
                        null,
                        null,
                        getX(),
                        getY(),
                        getZ(),
                        5F + random.nextFloat(),
                        false,
                        safe ? Level.ExplosionInteraction.NONE : Level.ExplosionInteraction.BLOCK);

                if (WorldData.ENABLE_METEOR_TAILS.get()) {
                    ExplosionLarge.spawnRubble(server, getX(), getY(), getZ(), 15);

                    ExplosionLarge.spawnParticles(server, getX(), getY() + 5, getZ(), 75);
                    ExplosionLarge.spawnParticles(server, getX() + 5, getY(), getZ(), 75);
                    ExplosionLarge.spawnParticles(server, getX() - 5, getY(), getZ(), 75);
                    ExplosionLarge.spawnParticles(server, getX(), getY(), getZ() + 5, 75);
                    ExplosionLarge.spawnParticles(server, getX(), getY(), getZ() - 5, 75);
                }

                double motionZ = getDeltaMovement().z;
                int spawnPosX = (int) (Math.round(getX() - 0.5D) + (safe ? 0 : (motionZ * 4)));
                int spawnPosY = (int) Math.round(getY() - (safe ? 0 : 4));
                int spawnPosZ = (int) (Math.round(getZ() - 0.5D) + (safe ? 0 : (motionZ * 4)));

                Meteorite.INSTANCE.generate(
                        server,
                        random,
                        new BlockPos(spawnPosX, spawnPosY, spawnPosZ),
                        safe,
                        true,
                        true);
                clearMeteorPath(server, spawnPosX, spawnPosY, spawnPosZ);

                level().playSound(
                                null,
                                getX(),
                                getY(),
                                getZ(),
                                ModSounds.OLD_EXPLOSION.get(),
                                SoundSource.HOSTILE,
                                10000.0F,
                                0.5F + random.nextFloat() * 0.1F);

                discard();
            }
        }

        if (level().isClientSide()) {
            tickClientAudio();

            if (WorldData.ENABLE_METEOR_TAILS.get()) {

                Vec3 step = getDeltaMovement();
                for (int i = 0; i < 10; i++) {
                    ClientEffects.spawnRocketFlame(
                            level(),
                            getX() - step.x + random.nextGaussian(),
                            getY() - step.y + random.nextGaussian(),
                            getZ() - step.z + random.nextGaussian(),
                            1F,
                            0D,
                            0D,
                            0D,
                            300 + random.nextInt(50));
                }
            }
        }
    }

    private void tickClientAudio() {
        if (isRemoved()) {
            if (audioFly != null) audioFly.stopSound();
            return;
        }

        if (audioFly == null) {

            audioFly =
                    AudioSystem.getLoopedSound(
                            ModSounds.METEORITE_FALLING_LOOP.get(),
                            SoundSource.BLOCKS,
                            0F,
                            0F,
                            0F,
                            1F,
                            200F,
                            0.9F + random.nextFloat() * 0.2F,
                            10);
        }
        if (audioFly == null) return;

        if (audioFly.isPlaying()) {
            audioFly.keepAlive();
            audioFly.updateVolume(1F);
            audioFly.updatePosition(
                    (float) getX(), (float) (getY() + getBbHeight() / 2F), (float) getZ());
        } else {

            Player player = ClientPlayerAccess.player();
            if (player != null
                    && player.getEyePosition().distanceToSqr(getX(), getY(), getZ()) < 210D * 210D)
                audioFly.startSound();
        }
    }

    private void clearMeteorPath(ServerLevel level, int x, int y, int z) {
        int radius = 5;
        int rSq = radius * radius;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx * dx + dy * dy + dz * dz <= rSq) {
                        damageOrDestroyBlock(level, pos.set(x + dx, y + dy, z + dz));
                    }
                }
            }
        }
    }

    private void damageOrDestroyBlock(ServerLevel level, BlockPos pos) {
        if (safe) return;

        BlockState state = level.getBlockState(pos);

        if (state.isAir()) return;

        float hardness = state.getDestroySpeed(level, pos);

        if (state.is(BlockTags.LEAVES)
                || state.is(BlockTags.LOGS)
                || (hardness >= 0F && hardness <= 0.3F)) {
            level.removeBlock(pos, false);
        } else {
            if (hardness < 0F || hardness > 5F) return;

            if (random.nextInt(6) == 1) {

                if (state.is(BlockTags.DIRT)) {
                    level.setBlockAndUpdate(pos, ModBlocks.DIRT_DEAD.get().defaultBlockState());

                } else if (state.is(BlockTags.SAND)) {
                    level.setBlockAndUpdate(
                            pos,
                            random.nextInt(2) == 1
                                    ? Blocks.SANDSTONE.defaultBlockState()
                                    : Blocks.GLASS.defaultBlockState());

                } else if (state.is(BlockTags.BASE_STONE_OVERWORLD)) {
                    level.setBlockAndUpdate(pos, Blocks.COBBLESTONE.defaultBlockState());

                } else if (state.is(BlockTags.GRASS_BLOCKS)) {
                    level.setBlockAndUpdate(pos, ModBlocks.WASTE_EARTH.get().defaultBlockState());
                }
            }
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (amount >= 250F
                && DamageResistanceHandler.CATEGORY_ENERGY.equals(
                        DamageResistanceHandler.typeToCategory(source))) discard();
        return false;
    }

    @Override
    public String getRadarName() {
        return "radar.target.meteor";
    }

    @Override
    public int getBlipLevel() {
        return SPECIAL;
    }

    @Override
    public boolean canBeSeenBy(Object radar) {
        return true;
    }

    @Override
    public boolean paramsApplicable(RadarScanParams params) {
        return params.scanMissiles;
    }

    @Override
    public boolean suppliesRedstone(RadarScanParams params) {
        return params.scanMissiles;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return true;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        this.safe = input.getBooleanOr("safe", false);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putBoolean("safe", safe);
    }
}
