// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.item;

import com.hbm.api.conveyor.IConveyorBelt;
import com.hbm.api.conveyor.IEnterableBlock;
import com.hbm.capability.NtmContracts;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.ExplosionEffectTiny;
import com.hbm.platform.Services;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public abstract class EntityMovingConveyorObject extends Entity {

    private final InterpolationHandler interpolation = new InterpolationHandler(this);

    protected EntityMovingConveyorObject(
            EntityType<? extends EntityMovingConveyorObject> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    @Override
    public InterpolationHandler getInterpolation() {
        return this.interpolation;
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
    public void tick() {
        this.interpolation.interpolate();
        super.tick();

        if (!(level() instanceof ServerLevel server)) return;

        if (this.tickCount <= 5) return;

        if ((this.tickCount + getId()) % 400 == 0) cramCheck(server);

        BlockPos pos = blockPosition();
        Vec3 self = position();

        IConveyorBelt belt = NtmContracts.CONVEYOR_BELT.at(server, pos);

        if (belt == null || !belt.canItemStay(server, pos, self)) {
            if (onLeaveConveyor()) return;
        } else {
            setDeltaMovement(
                    belt.getTravelLocation(server, pos, self, getMoveSpeed()).subtract(self));
        }

        BlockPos lastPos = blockPosition();
        move(MoverType.SELF, getDeltaMovement());
        BlockPos newPos = blockPosition();

        if (lastPos.equals(newPos)) return;

        IEnterableBlock entered = NtmContracts.ENTERABLE.at(server, newPos);

        if (entered != null) {
            enterBlock(entered, newPos, enterDirection(lastPos, newPos));
        } else if (!server.getBlockState(newPos).isSolid()) {
            IEnterableBlock below = NtmContracts.ENTERABLE.at(server, newPos.below());
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
        List<EntityMovingConveyorObject> objs =
                server.getEntitiesOfClass(
                        EntityMovingConveyorObject.class, getBoundingBox().inflate(0.125D));

        if (objs.size() < Services.CONFIG.runtime().conveyorCramMax()) return;

        for (EntityMovingConveyorObject obj : objs) obj.discard();

        ExplosionVNT vnt = new ExplosionVNT(server, getX(), getY() + 0.125, getZ(), 1, this);
        vnt.setSFX(new ExplosionEffectTiny());
        vnt.explode();

        BlockPos pos = blockPosition();
        if (tickCount > 400
                && Services.CONFIG.runtime().conveyorCramExplode()
                && server.getBlockState(pos).getBlock() instanceof IConveyorBelt)
            server.destroyBlock(pos, false);
    }

    public abstract void enterBlock(
            IEnterableBlock enterable, BlockPos pos, @Nullable Direction dir);

    public void enterBlockFalling(IEnterableBlock enterable, BlockPos pos) {
        this.enterBlock(enterable, pos.below(), Direction.UP);
    }

    public abstract boolean onLeaveConveyor();

    public double getMoveSpeed() {
        return 0.0625D;
    }
}
