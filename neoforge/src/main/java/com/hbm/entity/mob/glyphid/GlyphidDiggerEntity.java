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

package com.hbm.entity.mob.glyphid;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.multiblock.MultiblockCellBlock;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.entity.mob.glyphid.GlyphidStats.StatBundle;
import com.hbm.entity.projectile.RubbleEntity;
import com.hbm.main.ResourceManager;
import com.hbm.platform.Platform;
import com.hbm.registration.Reg;
import com.hbm.registration.Registrar;
import com.hbm.util.BlockPath;
import com.hbm.util.MobUtil;
import com.hbm.util.RegistryUtil;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import org.jspecify.annotations.Nullable;

public class GlyphidDiggerEntity extends GlyphidEntity {

    public static Reg.@Nullable EntityHandle<GlyphidDiggerEntity> TYPE;

    public static void register() {
        TYPE =
                Reg.entity(
                        "entity_glyphid_digger",
                        () ->
                                EntityType.Builder.<GlyphidDiggerEntity>of(
                                                GlyphidDiggerEntity::new, MobCategory.MONSTER)
                                        .notInPeaceful()
                                        .sized(1.75F, 1F)
                                        .clientTrackingRange(5)
                                        .updateInterval(3)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id("entity_glyphid_digger"))));
        Registrar.registerLivingAttributes(TYPE, GlyphidDiggerEntity::createAttributes);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return GlyphidEntity.createAttributes()
                .add(Attributes.MAX_HEALTH, GlyphidStats.getStats().getDigger().health)
                .add(
                        Attributes.MOVEMENT_SPEED,
                        MobUtil.oldAiWalkSpeed(GlyphidStats.getStats().getDigger().speed))
                .add(Attributes.ATTACK_DAMAGE, GlyphidStats.getStats().getDigger().damage);
    }

    protected @Nullable LivingEntity lastTarget;
    protected double lastX;
    protected double lastY;
    protected double lastZ;
    public int timer = 0;

    public GlyphidDiggerEntity(EntityType<? extends GlyphidDiggerEntity> type, Level level) {
        super(type, level);
    }

    @Override
    public Identifier getSkin() {
        return ResourceManager.glyphid_digger_tex;
    }

    @Override
    public double getGlyphidScale() {
        return 1.3D;
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getAttribute(Attributes.MAX_HEALTH)
                .setBaseValue(GlyphidStats.getStats().getDigger().health);
        getAttribute(Attributes.MOVEMENT_SPEED)
                .setBaseValue(MobUtil.oldAiWalkSpeed(GlyphidStats.getStats().getDigger().speed));
        getAttribute(Attributes.ATTACK_DAMAGE)
                .setBaseValue(GlyphidStats.getStats().getDigger().damage);
    }

    @Override
    public StatBundle getStats() {
        return GlyphidStats.getStats().statsDigger;
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);

        LivingEntity target = getTarget();
        if (target != null && isAlive()) {

            lastX = target.getX();
            lastY = target.getY();
            lastZ = target.getZ();

            if (--timer <= 0) {
                groundSlam();
                timer = 120;
            }
        }
    }

    public void groundSlam() {
        LivingEntity target = getTarget();
        if (target != null
                && distanceTo(target) < 30
                && level() instanceof ServerLevel server
                && EventHooks.canEntityGrief(server, (Entity) this)) {

            boolean topAttack = false;

            int l = 6;
            float part = -1F / 16F;

            int bugX = (int) getX();
            int bugY = (int) getY();
            int bugZ = (int) getZ();

            Vec3 vec0 = getLookAngle();

            double velX = target.getX() - lastX;
            double velY = target.getY() - lastY;
            double velZ = target.getZ() - lastZ;

            if (lastTarget != target) {
                velX = velY = velZ = 0;
            }

            if (distanceTo(target) > 20) {
                topAttack = true;
            }

            int prediction = 60;
            double dx = target.getX() - getX() + velX * prediction;
            double dy =
                    (target.getY() + target.getBbHeight() / 2) - (getY() + 1) + velY * prediction;
            double dz = target.getZ() - getZ() + velZ * prediction;
            double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (len < 3) return;
            double targetYaw = -Math.atan2(dx, dz);

            double x = Math.sqrt(dx * dx + dz * dz);
            double y = dy;
            double v0 = 1.2;
            double v02 = v0 * v0;
            double g = 0.03D;
            double upperLower = topAttack ? 1 : -1;
            double targetPitch =
                    Math.atan(
                            (v02
                                            + Math.sqrt(v02 * v02 - g * (g * x * x + 2 * y * v02))
                                                    * upperLower)
                                    / (g * x));
            Vec3 fireVec = null;
            if (!Double.isNaN(targetPitch)) {

                fireVec = new Vec3(v0, 0, 0);
                fireVec = fireVec.zRot((float) -targetPitch);
                fireVec = fireVec.yRot((float) -(targetYaw + Math.PI * 0.5));
            }

            Vec3 launchVelocity = fireVec;
            Consumer<BlockPos> lift =
                    pos -> {
                        BlockState state = level().getBlockState(pos);
                        Block b = state.getBlock();
                        float k = b.getExplosionResistance();

                        if (k < ModBlocks.CONCRETE.get().getExplosionResistance()
                                && state.isSolidRender()
                                && !(b instanceof MultiblockCoreBlock)
                                && !(b instanceof MultiblockCellBlock)
                                && level().getBlockEntity(pos) == null
                                && Platform.canEntityDestroyBlock(server, pos, state, this)) {

                            RubbleEntity rubble =
                                    new RubbleEntity(
                                            level(),
                                            pos.getX() + 0.5,
                                            pos.getY() + 2,
                                            pos.getZ() + 0.5);
                            rubble.setBlockState(state);

                            if (launchVelocity != null) {
                                rubble.shoot(
                                        launchVelocity.x,
                                        launchVelocity.y,
                                        launchVelocity.z,
                                        (float) v0,
                                        random.nextFloat());
                            }

                            level().addFreshEntity(rubble);

                            level().setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                        }
                    };
            BlockPath.forEachHorizontal(bugX, bugY, bugZ, l, vec0, lift);
            for (int i = 0; i < 8; i++) {
                vec0 = vec0.yRot(part);
                BlockPath.forEachHorizontal(bugX, bugY - 1, bugZ, l, vec0, lift);
            }
        }
    }

    @Override
    public boolean isArmorBroken(float amount) {
        return random.nextInt(100) <= Math.min(Math.pow(amount * 0.25, 2), 100);
    }

    @Override
    protected boolean canDig() {
        return true;
    }
}
