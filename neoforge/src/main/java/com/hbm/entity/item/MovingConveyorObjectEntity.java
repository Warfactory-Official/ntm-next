/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.entity.item;

import com.hbm.api.conveyor.ConveyorBelt;
import com.hbm.api.conveyor.ConveyorInputHandler;
import com.hbm.capability.NtmContracts;
import com.hbm.config.HbmConfig;
import com.hbm.config.RuntimeConfig;
import com.hbm.explosion.vanillant.ComposableExplosion;
import com.hbm.explosion.vanillant.standard.ExplosionEffectTiny;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.LinearInterpolationHandler;
import net.minecraft.world.entity.MoveSimulationType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public abstract class MovingConveyorObjectEntity extends Entity {
    protected MovingConveyorObjectEntity(
            EntityType<? extends MovingConveyorObjectEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    @Override
    protected InterpolationHandler createInterpolationHandler() {
        return LinearInterpolationHandler.create(this);
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean isAttackable() {
        return true;
    }

    @Override
    public boolean skipAttackInteraction(Entity attacker) {
        if (attacker instanceof Player) discard();
        return false;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        skipAttackInteraction(source.getEntity());
        return true;
    }

    @Override
    public MoveSimulationType getMoveSimulationType() {
        return MoveSimulationType.SERVER_AND_CLIENT;
    }

    @Override
    public void tick() {
        super.tick();

        if (!(level() instanceof ServerLevel server)) return;

        if (this.tickCount <= 5) return;

        if ((this.tickCount + getId()) % 400 == 0) cramCheck(server);

        BlockPos pos = blockPosition();
        Vec3 self = position();
        ConveyorBelt belt = NtmContracts.CONVEYOR_BELT.at(server, pos);

        if (belt == null || !belt.canItemStay(server, pos, self)) {
            if (onLeaveConveyor()) return;
        } else {
            setDeltaMovement(
                    belt.getTravelPosition(server, pos, self, getMoveSpeed()).subtract(self));
        }

        BlockPos lastPos = blockPosition();
        move(MoverType.SELF, getDeltaMovement());
        BlockPos newPos = blockPosition();

        if (lastPos.equals(newPos)) return;

        ConveyorInputHandler entered = NtmContracts.ENTERABLE.at(server, newPos);

        if (entered != null) {
            enterBlock(entered, newPos, enterDirection(lastPos, newPos));
        } else if (!server.getBlockState(newPos).isSolid()) {
            ConveyorInputHandler below = NtmContracts.ENTERABLE.at(server, newPos.below());
            if (below != null) enterBlockFalling(below, newPos);
        }
    }

    private static @Nullable Direction enterDirection(BlockPos from, BlockPos to) {
        int dx = Integer.signum(to.getX() - from.getX());
        int dy = Integer.signum(to.getY() - from.getY());
        int dz = Integer.signum(to.getZ() - from.getZ());

        if (dx != 0 && dy == 0 && dz == 0) return dx < 0 ? Direction.EAST : Direction.WEST;
        if (dy != 0 && dx == 0 && dz == 0) return dy < 0 ? Direction.UP : Direction.DOWN;
        if (dz != 0 && dx == 0 && dy == 0) return dz < 0 ? Direction.SOUTH : Direction.NORTH;
        return null;
    }

    private void cramCheck(ServerLevel server) {
        List<MovingConveyorObjectEntity> objs =
                server.getEntitiesOfClass(
                        MovingConveyorObjectEntity.class, getBoundingBox().inflate(0.125D));

        if (objs.size() < ((RuntimeConfig) HbmConfig.RUNTIME).conveyorCramMax()) return;

        for (MovingConveyorObjectEntity obj : objs) obj.discard();

        ComposableExplosion vnt =
                new ComposableExplosion(server, getX(), getY() + 0.125, getZ(), 1, this);
        vnt.effects(new ExplosionEffectTiny());
        vnt.explode();

        BlockPos pos = blockPosition();
        if (tickCount > 400
                && ((RuntimeConfig) HbmConfig.RUNTIME).conveyorCramExplode()
                && server.getBlockState(pos).getBlock() instanceof ConveyorBelt)
            server.destroyBlock(pos, false);
    }

    public abstract void enterBlock(
            ConveyorInputHandler enterable, BlockPos pos, @Nullable Direction dir);

    public void enterBlockFalling(ConveyorInputHandler enterable, BlockPos pos) {
        this.enterBlock(enterable, pos.below(), Direction.UP);
    }

    public abstract boolean onLeaveConveyor();

    public double getMoveSpeed() {
        return 0.0625D;
    }
}
