// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.lib.ModDamageTypes;
import com.hbm.sound.AudioSystem;
import com.hbm.sound.AudioWrapper;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.Audible;
import com.hbm.tileentity.AudioLoop;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class BlockEntityBroadcaster extends BlockEntity implements Audible {

    private static final double CONFUSION_RANGE = 25.0D;
    private static final double DAMAGE_RANGE = 15.0D;
    private static final float MAX_DAMAGE = 10.0F;
    private static final int CONFUSION_TICKS = 300;

    private static final int CONFUSION_REFRESH_BELOW = 100;
    private static final float VOLUME = 25.0F;

    private @Nullable AudioWrapper audio;

    public BlockEntityBroadcaster(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BROADCASTER.get(), pos, state);
    }

    public static void tickServer(
            Level level, BlockPos pos, BlockState state, BlockEntityBroadcaster be) {
        Vec3 center = Vec3.atCenterOf(pos);
        AABB box = new AABB(center, center).inflate(CONFUSION_RANGE);

        List<LivingEntity> living = level.getEntitiesOfClass(LivingEntity.class, box);
        for (LivingEntity entity : living) {
            double d = entity.position().distanceTo(center);

            if (d <= CONFUSION_RANGE) {
                MobEffectInstance confusion = entity.getEffect(MobEffects.NAUSEA);
                if (confusion == null || confusion.getDuration() < CONFUSION_REFRESH_BELOW) {
                    entity.addEffect(new MobEffectInstance(MobEffects.NAUSEA, CONFUSION_TICKS, 0));
                }
            }

            if (d <= DAMAGE_RANGE) {
                float amount = (float) ((DAMAGE_RANGE - d) / DAMAGE_RANGE * MAX_DAMAGE);
                entity.hurt(level.damageSources().source(ModDamageTypes.BROADCAST), amount);
            }
        }
    }

    public static void tickClient(
            Level level, BlockPos pos, BlockState state, BlockEntityBroadcaster be) {
        be.audio = AudioLoop.loop(be, be.audio, true, VOLUME, 1.0F, false);
    }

    @Override
    public @Nullable AudioWrapper createAudioLoop() {

        RandomSource rand =
                RandomSource.create(
                        worldPosition.getX() + worldPosition.getY() + worldPosition.getZ());
        return AudioSystem.getLoopedSound(
                ModSounds.BROADCAST_TRACK[rand.nextInt(3)].get(),
                SoundSource.BLOCKS,
                worldPosition.getX(),
                worldPosition.getY(),
                worldPosition.getZ(),
                getVolume(VOLUME),
                (float) CONFUSION_RANGE,
                1.0F,
                20);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        audio = AudioLoop.loop(this, audio, false, 0.0F, 1.0F, false);
    }
}
