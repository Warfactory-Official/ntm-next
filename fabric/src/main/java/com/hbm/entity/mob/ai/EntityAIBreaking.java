// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.mob.ai;

import com.hbm.platform.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class EntityAIBreaking extends Goal {

    private final Mob digger;
    private @Nullable LivingEntity target;
    private @Nullable BlockPos marked;
    private int digTick;
    private int scanTick;

    public EntityAIBreaking(Mob digger) {
        this.digger = digger;
    }

    @Override
    public boolean canUse() {
        target = digger.getTarget();

        if (target != null
                && digger.getNavigation().isDone()
                && digger.distanceTo(target) > 1D
                && (target.onGround() || !digger.hasLineOfSight(target))) {

            BlockHitResult hit = nextObstacle(digger, 2D);
            if (hit == null) return false;

            if (digger.level()
                            .getBlockState(hit.getBlockPos())
                            .getDestroySpeed(digger.level(), hit.getBlockPos())
                    >= 0) {
                marked = hit.getBlockPos();
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (marked == null) return false;

        Vec3 toBlock =
                new Vec3(
                        marked.getX() - digger.getX(),
                        marked.getY() - (digger.getY() + digger.getEyeHeight()),
                        marked.getZ() - digger.getZ());

        return digger.isAlive() && toBlock.length() <= 4;
    }

    @Override
    public void tick() {
        BlockHitResult hit = digger.tickCount % 10 == 0 ? nextObstacle(digger, 2D) : null;

        if (hit != null) marked = hit.getBlockPos();

        Level level = digger.level();

        if (marked == null || level.getBlockState(marked).isAir()) {
            digTick = 0;
            return;
        }

        BlockState state = level.getBlockState(marked);
        digTick++;

        int health = (int) state.getDestroySpeed(level, marked) / 3;

        if (health < 0) {
            marked = null;
            return;
        }

        float progress = (digTick * 0.05F) / (float) health;

        if (progress >= 1F) {
            digTick = 0;
            if (level instanceof ServerLevel server
                    && Services.PLATFORM.canEntityDestroyBlock(server, marked, state, digger)) {
                level.destroyBlock(marked, false);
            }
            marked = null;

            if (target != null) {
                digger.getNavigation().moveTo(digger.getNavigation().createPath(target, 0), 1D);
            }
        } else if (digTick % 5 == 0) {
            SoundType sound = state.getSoundType();

            level.playSound(
                    null,
                    digger.getX(),
                    digger.getY(),
                    digger.getZ(),
                    sound.getStepSound(),
                    digger.getSoundSource(),
                    sound.getVolume() + 1F,
                    sound.getPitch());
            digger.swing(InteractionHand.MAIN_HAND);
            level.destroyBlockProgress(digger.getId(), marked, (int) (progress * 10F));
        }
    }

    @Override
    public void stop() {
        marked = null;
        digTick = 0;
    }

    private @Nullable BlockHitResult nextObstacle(Mob mob, double dist) {
        float pitch = mob.xRotO + (mob.getXRot() - mob.xRotO);
        float yaw = mob.yRotO + (mob.getYRot() - mob.yRotO);

        int digWidth = Mth.ceil(mob.getBbWidth());
        int digHeight = Mth.ceil(mob.getBbHeight());
        int passMax = digWidth * digWidth * digHeight;

        int x = scanTick % digWidth - (digWidth / 2);
        int y = scanTick / (digWidth * digWidth);
        int z = (scanTick % (digWidth * digWidth)) / digWidth - (digWidth / 2);

        BlockHitResult hit =
                rayCast(
                        mob.level(),
                        mob,
                        x + mob.getX(),
                        y + mob.getY(),
                        z + mob.getZ(),
                        yaw,
                        pitch,
                        dist);

        if (hit != null
                && hit.getType() == HitResult.Type.BLOCK
                && mob.level()
                                .getBlockState(hit.getBlockPos())
                                .getDestroySpeed(mob.level(), hit.getBlockPos())
                        >= 0) {
            scanTick = 0;
            return hit;
        }

        scanTick = passMax == 0 ? 0 : (scanTick + 1) % passMax;
        return null;
    }

    private static @Nullable BlockHitResult rayCast(
            Level level,
            Mob mob,
            double x,
            double y,
            double z,
            float yaw,
            float pitch,
            double dist) {
        Vec3 from = new Vec3(x, y, z);
        float f3 = Mth.cos(-yaw * 0.017453292F - (float) Math.PI);
        float f4 = Mth.sin(-yaw * 0.017453292F - (float) Math.PI);
        float f5 = -Mth.cos(-pitch * 0.017453292F);
        float f6 = Mth.sin(-pitch * 0.017453292F);
        Vec3 to = from.add(f4 * f5 * dist, f6 * dist, f3 * f5 * dist);

        BlockHitResult hit =
                level.clip(
                        new ClipContext(
                                from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mob));
        return hit.getType() == HitResult.Type.BLOCK ? hit : null;
    }
}
