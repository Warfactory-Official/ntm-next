// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.effect;

import com.hbm.entity.ModEntities;
import com.hbm.entity.projectile.EntityRubble;
import com.hbm.items.ModItems;
import com.hbm.lib.Library;
import com.hbm.lib.ModDamageTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class EntityBlackHole extends Entity {

    protected static final EntityDataAccessor<Float> SIZE =
            SynchedEntityData.defineId(EntityBlackHole.class, EntityDataSerializers.FLOAT);
    private static final Identifier PELLET_ANTIMATTER = Library.id("pellet_antimatter");
    private boolean breaksBlocks = true;

    public EntityBlackHole(EntityType<? extends EntityBlackHole> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public EntityBlackHole(Level level, float size) {
        this(ModEntities.BLACK_HOLE.get(), level);
        setSize(size);
    }

    private static boolean isVortexTerminator(ItemEntity item) {
        return BuiltInRegistries.ITEM.getKey(item.getItem().getItem()).equals(PELLET_ANTIMATTER)
                || item.getItem().is(ModItems.FLAME_PONY.get());
    }

    public EntityBlackHole noBreak() {
        breaksBlocks = false;
        return this;
    }

    public float getSize() {
        return entityData.get(SIZE);
    }

    protected void setSize(float size) {
        entityData.set(SIZE, size);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(SIZE, 0.5F);
    }

    @Override
    public void tick() {
        super.tick();

        float size = getSize();
        if (level() instanceof ServerLevel server && breaksBlocks) {
            consumeBlocks(server, size);
        }

        double range = size * 15D;
        for (Entity entity :
                level().getEntities(
                                this,
                                new AABB(
                                        getX() - range,
                                        getY() - range,
                                        getZ() - range,
                                        getX() + range,
                                        getY() + range,
                                        getZ() + range))) {
            if (entity instanceof Player player && player.getAbilities().instabuild) continue;

            if (entity instanceof FallingBlockEntity falling
                    && level() instanceof ServerLevel server
                    && entity.tickCount > 1) {
                EntityRubble rubble =
                        new EntityRubble(level(), entity.getX(), entity.getY(), entity.getZ());
                rubble.setBlockState(falling.getBlockState());
                rubble.setDeltaMovement(entity.getDeltaMovement());
                entity.discard();
                server.addFreshEntity(rubble);
            }

            Vec3 direction =
                    new Vec3(
                            getX() - entity.getX(), getY() - entity.getY(), getZ() - entity.getZ());
            double distance = direction.length();
            if (distance > range) continue;

            direction = direction.normalize();
            if (!(entity instanceof ItemEntity))
                direction = direction.yRot((float) Math.toRadians(15D));
            entity.setDeltaMovement(
                    entity.getDeltaMovement()
                            .add(direction.x * 0.1D, direction.y * 0.2D, direction.z * 0.1D));

            if (entity instanceof EntityBlackHole) continue;
            if (distance < size * 1.5F) {
                if (level() instanceof ServerLevel server) {
                    entity.hurtServer(
                            server, server.damageSources().source(ModDamageTypes.BLACKHOLE), 1000F);
                }
                if (!(entity instanceof LivingEntity)) entity.discard();

                if (level() instanceof ServerLevel server
                        && entity instanceof ItemEntity item
                        && isVortexTerminator(item)) {
                    discard();

                    server.explode(
                            null,
                            null,
                            null,
                            getX(),
                            getY(),
                            getZ(),
                            5F,
                            false,
                            Level.ExplosionInteraction.BLOCK);
                    return;
                }
            }
        }

        Vec3 movement = getDeltaMovement();
        setPos(getX() + movement.x, getY() + movement.y, getZ() + movement.z);
        setDeltaMovement(movement.scale(0.99D));
    }

    private void consumeBlocks(ServerLevel level, float size) {
        for (int k = 0; k < size * 2F; k++) {
            double phi = random.nextDouble() * Math.PI * 2D;
            double cosineTheta = random.nextDouble() * 2D - 1D;
            double theta = Math.acos(cosineTheta);
            Vec3 ray =
                    new Vec3(
                            Math.sin(theta) * Math.cos(phi),
                            Math.sin(theta) * Math.sin(phi),
                            Math.cos(theta));
            int length = (int) Math.ceil(size * 15F);

            for (int i = 0; i < length; i++) {

                BlockPos pos =
                        new BlockPos(
                                (int) (getX() + ray.x * i),
                                (int) (getY() + ray.y * i),
                                (int) (getZ() + ray.z * i));
                BlockState state = level.getBlockState(pos);

                if (state.liquid()) {
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                    state = level.getBlockState(pos);
                }
                if (state.isAir()) continue;

                EntityRubble rubble =
                        new EntityRubble(level, pos.getX() + 0.5F, pos.getY(), pos.getZ() + 0.5F);
                rubble.setBlockState(state);
                level.addFreshEntity(rubble);
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                break;
            }
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        setSize(input.getFloatOr("size", 0.5F));
        breaksBlocks = input.getBooleanOr("breaksBlocks", true);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putFloat("size", getSize());
        output.putBoolean("breaksBlocks", breaksBlocks);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 25000D;
    }
}
