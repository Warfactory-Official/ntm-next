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

package com.hbm.entity.projectile;

import com.hbm.entity.FarVisible;
import com.hbm.entity.ModEntities;
import com.hbm.items.ModItems;
import com.hbm.lib.ModDamageTypes;
import com.hbm.particle.EffectKinds.GibletType;
import com.hbm.particle.helper.ParticleCreators;
import com.hbm.sound.EntitySounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class SawbladeEntity extends InterpolatedProjectileEntity implements FarVisible {

    private static final EntityDataAccessor<Integer> ORIENTATION =
            SynchedEntityData.defineId(SawbladeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> META =
            SynchedEntityData.defineId(SawbladeEntity.class, EntityDataSerializers.INT);

    public SawbladeEntity(EntityType<? extends SawbladeEntity> type, Level level) {
        super(type, level);
    }

    public SawbladeEntity(Level level, double x, double y, double z) {
        this(ModEntities.STRAY_SAW.get(), level);
        this.setPos(x, y, z);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ORIENTATION, 0);
        builder.define(META, 0);
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return EntityDimensions.scalable(1F, 1F);
    }

    public SawbladeEntity setOrientation(int rot) {
        entityData.set(ORIENTATION, rot);
        return this;
    }

    public int getOrientation() {
        return entityData.get(ORIENTATION);
    }

    public int getMeta() {
        return entityData.get(META);
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {

        if (!level().isClientSide()) {
            player.getInventory().placeItemBackInInventory(new ItemStack(ModItems.SAWBLADE));
            this.discard();
        }

        return InteractionResult.PASS;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    protected void onImpact(HitResult mop) {

        if (mop instanceof EntityHitResult hit && hit.getEntity().isAlive()) {
            Entity e = hit.getEntity();
            if (level() instanceof ServerLevel server) {
                e.hurtServer(server, damageSources().source(ModDamageTypes.RUBBLE), 1000F);

                if (!e.isAlive() && e instanceof LivingEntity living) {
                    ParticleCreators.giblets(server, living, GibletType.MEAT, 5);

                    EntitySounds.play(
                            living,
                            living.getX(),
                            living.getY(),
                            living.getZ(),
                            SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR,
                            SoundSource.HOSTILE,
                            2.0F,
                            0.95F + level().getRandom().nextFloat() * 0.2F);
                }
            }
        }

        if (this.tickCount > 1 && mop instanceof BlockHitResult hit) {

            int orientation = getOrientation();

            if (orientation < 6) {

                if (getDeltaMovement().length() < 0.75D) {
                    orientation += 6;
                    setOrientation(orientation);
                } else {
                    Direction side = hit.getDirection();
                    Vec3 motion = getDeltaMovement();
                    setDeltaMovement(
                            motion.x * (1 - Math.abs(side.getStepX()) * 2),
                            motion.y * (1 - Math.abs(side.getStepY()) * 2),
                            motion.z * (1 - Math.abs(side.getStepZ()) * 2));

                    level().explode(
                                    this,
                                    getX(),
                                    getY(),
                                    getZ(),
                                    3F,
                                    Level.ExplosionInteraction.NONE);

                    BlockPos pos = hit.getBlockPos();
                    if (level().getBlockState(pos).getBlock().getExplosionResistance() < 50) {
                        if (level() instanceof ServerLevel server) {
                            server.destroyBlock(pos, false);
                        }
                    }
                }
            }

            if (orientation >= 6) {
                getStuck(hit.getBlockPos(), hit.getDirection().get3DDataValue());
            }
        }
    }

    @Override
    public void tick() {

        if (!level().isClientSide()) {
            int orientation = this.getOrientation();
            if (orientation >= 6 && !this.inGround) {
                this.setOrientation(orientation - 6);
            }
        }

        super.tick();
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double dist) {
        return true;
    }

    @Override
    public double getGravityVelocity() {
        return this.inGround ? 0 : 0.03D;
    }

    @Override
    protected int groundDespawn() {
        return 0;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("rot", this.getOrientation());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.setOrientation(input.getIntOr("rot", 0));
    }
}
