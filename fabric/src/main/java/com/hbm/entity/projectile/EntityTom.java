// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.projectile;

import com.hbm.entity.ModEntities;
import com.hbm.entity.effect.EntityCloudTom;
import com.hbm.entity.logic.EntityTomBlast;
import com.hbm.sound.ModSounds;
import com.hbm.util.ChunkUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class EntityTom extends EntityThrowableNT {

    public EntityTom(EntityType<? extends EntityTom> type, Level level) {
        super(type, level);
    }

    @Override
    public void tick() {
        this.xo = getX();
        this.yo = getY();
        this.zo = getZ();

        Vec3 motion = getDeltaMovement();
        setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);
        ChunkUtil.holdOwnChunk(this);

        if (this.tickCount % 100 == 0) {
            level().playSound(
                            null,
                            getX(),
                            getY(),
                            getZ(),
                            ModSounds.ALARM_CHIME.get(),
                            SoundSource.RECORDS,
                            10000.0F,
                            1.0F);
        }

        setDeltaMovement(motion.x, -0.5D, motion.z);

        if (!level().getBlockState(new BlockPos((int) getX(), (int) getY(), (int) getZ())).isAir()
                || getY() < 10) {
            if (!level().isClientSide()) {

                EntityTomBlast tom = new EntityTomBlast(ModEntities.TOM_BUST.get(), level());
                tom.setPos(getX(), getY(), getZ());
                tom.destructionRange = 600;
                level().addFreshEntity(tom);

                level().addFreshEntity(
                                EntityCloudTom.statFac(level(), 500, getX(), getY(), getZ()));
            }
            this.discard();
        }
    }

    @Override
    protected void onImpact(HitResult mop) {}

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 500000D;
    }
}
