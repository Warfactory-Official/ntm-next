// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.item;

import com.hbm.entity.ModEntities;
import com.hbm.particle.helper.ParticleCreators;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

public class EntityFireworks extends Entity {

    private int color;
    private int character;

    public EntityFireworks(EntityType<? extends EntityFireworks> type, Level level) {
        super(type, level);
    }

    public EntityFireworks(Level level, double x, double y, double z, int color, int character) {
        this(ModEntities.FIREWORK_BALL.get(), level);
        this.snapTo(x, y, z, 0.0F, 0.0F);
        this.color = color;
        this.character = character;
    }

    public int getColor() {
        return color;
    }

    public int getCharacter() {
        return character;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {}

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    public void tick() {

        move(MoverType.SELF, new Vec3(0.0D, 3.0D, 0.0D));

        if (level().isClientSide()) {
            level().addParticle(ParticleTypes.FLAME, getX(), getY(), getZ(), 0.0D, -0.3D, 0.0D);
            level().addParticle(ParticleTypes.SMOKE, getX(), getY(), getZ(), 0.0D, -0.2D, 0.0D);
            return;
        }

        if (tickCount < 16) return;

        level().playSound(
                        null,
                        getX(),
                        getY(),
                        getZ(),
                        SoundEvents.FIREWORK_ROCKET_BLAST,
                        SoundSource.AMBIENT,
                        20F,
                        1F + random.nextFloat() * 0.2F);

        ParticleCreators.fireworkLetter(level(), getX(), getY(), getZ(), color, (char) character);

        discard();
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        character = input.getIntOr("char", 0);
        color = input.getIntOr("color", 0);
        tickCount = input.getIntOr("ticksExisted", 0);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putInt("char", character);
        output.putInt("color", color);
        output.putInt("ticksExisted", tickCount);
    }
}
