// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.effect;

import com.hbm.entity.ModEntities;
import com.hbm.extprop.HbmLivingProps;
import com.hbm.particle.helper.FlameCreator;
import java.util.List;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class EntityFireLingering extends Entity {

    public static final int TYPE_DIESEL = 0;
    public static final int TYPE_BALEFIRE = 1;
    public static final int TYPE_PHOSPHORUS = 2;
    public static final int TYPE_OXY = 3;
    public static final int TYPE_BLACK = 4;
    private static final EntityDataAccessor<Integer> TYPE =
            SynchedEntityData.defineId(EntityFireLingering.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> WIDTH =
            SynchedEntityData.defineId(EntityFireLingering.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> HEIGHT =
            SynchedEntityData.defineId(EntityFireLingering.class, EntityDataSerializers.FLOAT);
    public int maxAge = 150;

    public EntityFireLingering(EntityType<? extends EntityFireLingering> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public EntityFireLingering(Level level) {
        this(ModEntities.FIRE_LINGERING.get(), level);
    }

    public EntityFireLingering setArea(float width, float height) {
        this.entityData.set(WIDTH, width);
        this.entityData.set(HEIGHT, height);
        return this;
    }

    public EntityFireLingering setDuration(int duration) {
        this.maxAge = duration;
        return this;
    }

    public EntityFireLingering setType(int type) {
        this.entityData.set(TYPE, type);
        return this;
    }

    public int getFireType() {
        return this.entityData.get(TYPE);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(TYPE, 0);
        builder.define(WIDTH, 0F);
        builder.define(HEIGHT, 0F);
    }

    @Override
    public void tick() {
        float width = this.entityData.get(WIDTH);
        float height = this.entityData.get(HEIGHT);

        if (!this.level().isClientSide()) {

            if (this.tickCount >= maxAge) {
                this.discard();
            }

            List<Entity> affected =
                    this.level()
                            .getEntities(
                                    this,
                                    new AABB(
                                            getX() - width / 2,
                                            getY(),
                                            getZ() - width / 2,
                                            getX() + width / 2,
                                            getY() + height,
                                            getZ() + width / 2));

            for (Entity e : affected) {
                if (e instanceof LivingEntity living) {
                    HbmLivingProps props = HbmLivingProps.getData(living);
                    if (this.getFireType() == TYPE_DIESEL) {
                        if (props.fire < 60) props.fire = 60;
                    }
                    if (this.getFireType() == TYPE_PHOSPHORUS) {
                        if (props.fire < 300) props.fire = 300;
                    }
                    if (this.getFireType() == TYPE_BALEFIRE) {
                        if (props.balefire < 100) props.balefire = 100;
                    }
                    if (this.getFireType() == TYPE_BLACK) {
                        if (props.blackFire < 200) props.blackFire = 200;
                        else props.blackFire += 5;
                    }
                } else {
                    e.igniteForSeconds(4);
                }
            }
        } else {

            for (int i = 0; i < (width >= 5 ? 2 : 1); i++) {
                double x = getX() - width / 2 + this.random.nextDouble() * width;
                double z = getZ() - width / 2 + this.random.nextDouble() * width;

                Vec3 up = new Vec3(x, getY() + height, z);
                Vec3 down = new Vec3(x, getY() - height, z);
                BlockHitResult mop =
                        this.level()
                                .clip(
                                        new ClipContext(
                                                up,
                                                down,
                                                ClipContext.Block.COLLIDER,
                                                ClipContext.Fluid.NONE,
                                                this));
                if (mop.getType() == HitResult.Type.BLOCK) down = mop.getLocation();
                if (this.getFireType() == TYPE_DIESEL)
                    FlameCreator.composeEffectClient(
                            this.level(), x, down.y, z, FlameCreator.META_FIRE);
                if (this.getFireType() == TYPE_PHOSPHORUS)
                    FlameCreator.composeEffectClient(
                            this.level(), x, down.y, z, FlameCreator.META_FIRE);
                if (this.getFireType() == TYPE_BALEFIRE)
                    FlameCreator.composeEffectClient(
                            this.level(), x, down.y, z, FlameCreator.META_BALEFIRE);
                if (this.getFireType() == TYPE_BLACK)
                    FlameCreator.composeEffectClient(
                            this.level(), x, down.y, z, FlameCreator.META_BLACK);
            }
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {}

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        this.discard();
    }

    @Override
    public boolean displayFireAnimation() {
        return false;
    }
}
