// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.projectile;

import com.hbm.entity.ModEntities;
import com.hbm.items.ModItems;
import com.hbm.potion.HbmPotion;
import com.hbm.tileentity.machine.rbmk.RBMKConfig;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class EntityRBMKDebris extends Entity {

    private static final EntityDataAccessor<Integer> TYPE =
            SynchedEntityData.defineId(EntityRBMKDebris.class, EntityDataSerializers.INT);

    private static final int RAD_DURATION = 60 * SharedConstants.TICKS_PER_SECOND;
    private static final int RAD_LEVEL_FUEL = 9;
    private static final int RAD_LEVEL_GRAPHITE = 4;

    public float rot;
    public float lastRot;

    public EntityRBMKDebris(EntityType<? extends EntityRBMKDebris> type, Level level) {
        super(type, level);
        this.rot = this.lastRot = this.random.nextFloat() * 360F;
    }

    public EntityRBMKDebris(Level level, double x, double y, double z, DebrisType type) {
        this(ModEntities.RBMK_DEBRIS.get(), level);
        setPos(x, y, z);
        setDebrisType(type);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(TYPE, 0);
    }

    public DebrisType getDebrisType() {
        return DebrisType.VALUES[Math.abs(entityData.get(TYPE)) % DebrisType.VALUES.length];
    }

    public void setDebrisType(DebrisType type) {
        entityData.set(TYPE, type.ordinal());
        refreshDimensions();
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return switch (getDebrisType()) {
            case BLANK -> EntityDimensions.scalable(0.5F, 0.5F);
            case ELEMENT -> EntityDimensions.scalable(1F, 1F);
            case FUEL, GRAPHITE -> EntityDimensions.scalable(0.25F, 0.25F);
            case LID -> EntityDimensions.scalable(1F, 0.5F);
            case ROD -> EntityDimensions.scalable(0.75F, 0.5F);
        };
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public void tick() {
        this.lastRot = this.rot;
        Level level = level();

        if (!level.isClientSide()) {
            Vec3 d = getDeltaMovement();
            if (getDebrisType() == DebrisType.LID && d.y > 0) {
                Vec3 from = position();
                Vec3 to = from.add(d.scale(2));
                BlockHitResult hit =
                        level.clip(
                                new ClipContext(
                                        from,
                                        to,
                                        ClipContext.Block.COLLIDER,
                                        ClipContext.Fluid.NONE,
                                        this));
                if (hit.getType() == HitResult.Type.BLOCK) {
                    BlockPos bp = hit.getBlockPos();
                    for (int i = -1; i <= 1; i++)
                        for (int j = -1; j <= 1; j++)
                            for (int k = -1; k <= 1; k++) {
                                int rn = Math.abs(i) + Math.abs(j) + Math.abs(k);
                                if (rn <= 1 || random.nextInt(rn) == 0)
                                    level.removeBlock(bp.offset(i, j, k), false);
                            }
                    discard();
                    return;
                }
            }
            if (getDebrisType() == DebrisType.FUEL || getDebrisType() == DebrisType.GRAPHITE) {
                int amplifier =
                        getDebrisType() == DebrisType.FUEL ? RAD_LEVEL_FUEL : RAD_LEVEL_GRAPHITE;
                for (LivingEntity e :
                        level.getEntitiesOfClass(
                                LivingEntity.class, getBoundingBox().inflate(2.5D))) {
                    e.addEffect(
                            new MobEffectInstance(HbmPotion.radiation(), RAD_DURATION, amplifier));
                }
            }
            if (!RBMKConfig.getPermaScrap(level) && tickCount > getLifetime() + getId() % 50) {
                discard();
                return;
            }
        }

        Vec3 pre = getDeltaMovement().add(0, -0.04, 0);
        setDeltaMovement(pre);
        move(MoverType.SELF, pre);
        Vec3 post = getDeltaMovement();
        double nx = pre.x != post.x ? -pre.x * 0.75 : post.x;
        double ny = pre.y != post.y ? 0.0 : post.y;
        double nz = pre.z != post.z ? -pre.z * 0.75 : post.z;
        if (onGround()) {
            nx *= 0.85;
            nz *= 0.85;
            ny *= -0.5;
        } else {
            this.rot += 10F;
            if (rot >= 360F) {
                this.rot -= 360F;
                this.lastRot -= 360F;
            }
        }
        setDeltaMovement(nx, ny, nz);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        if (!level().isClientSide() && isAlive()) {
            ItemStack give =
                    switch (getDebrisType()) {
                        case FUEL -> new ItemStack(ModItems.DEBRIS_FUEL);
                        case GRAPHITE -> new ItemStack(ModItems.DEBRIS_GRAPHITE);
                        case LID -> new ItemStack(ModItems.RBMK_LID);
                        default -> new ItemStack(ModItems.DEBRIS_METAL);
                    };
            player.getInventory().placeItemBackInInventory(give);
            discard();
        }
        return InteractionResult.SUCCESS;
    }

    public int getLifetime() {
        return switch (getDebrisType()) {
            case BLANK, ELEMENT -> 3 * SharedConstants.TICKS_PER_MINUTE;
            case FUEL -> 10 * SharedConstants.TICKS_PER_MINUTE;
            case GRAPHITE -> 15 * SharedConstants.TICKS_PER_MINUTE;
            case LID -> 30 * SharedConstants.TICKS_PER_SECOND;
            case ROD -> 60 * SharedConstants.TICKS_PER_SECOND;
        };
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putInt("debtype", entityData.get(TYPE));
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        entityData.set(TYPE, input.getIntOr("debtype", 0));
    }

    public enum DebrisType {
        BLANK,
        ELEMENT,
        FUEL,
        ROD,
        GRAPHITE,
        LID;
        public static final DebrisType[] VALUES = values();
    }
}
