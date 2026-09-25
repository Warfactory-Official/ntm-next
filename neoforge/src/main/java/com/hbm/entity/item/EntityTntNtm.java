// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.item;

import com.hbm.api.block.IFuckingExplode;
import com.hbm.blocks.bomb.TntRule;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class EntityTntNtm extends PrimedTnt {

    public EntityTntNtm(EntityType<? extends EntityTntNtm> type, Level level) {
        super(type, level);
    }

    public EntityTntNtm(
            EntityType<? extends EntityTntNtm> type,
            Level level,
            double x,
            double y,
            double z,
            @Nullable LivingEntity igniter,
            BlockState bomb) {
        this(type, level);
        this.setPos(x, y, z);
        double rot = level.getRandom().nextDouble() * (Math.PI * 2);
        this.setDeltaMovement(-Math.sin(rot) * 0.02, 0.2, -Math.cos(rot) * 0.02);
        this.setFuse(80);
        this.xo = x;
        this.yo = y;
        this.zo = z;
        this.owner = EntityReference.of(igniter);
        this.setBlockState(bomb);
    }

    @Override
    public void explode() {
        if (!TntRule.explodes(level())) return;
        if (getBlockState().getBlock() instanceof IFuckingExplode bomb) {

            bomb.explodeEntity(level(), getX(), getY(0.5D), getZ(), this);
        }
    }
}
