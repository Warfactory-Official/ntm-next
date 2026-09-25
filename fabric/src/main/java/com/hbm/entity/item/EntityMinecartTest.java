// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.item;

import com.hbm.blocks.ModBlocks;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.minecart.MinecartTNT;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class EntityMinecartTest extends MinecartTNT {

    private static final EntityDataAccessor<Byte> SIGN_X =
            SynchedEntityData.defineId(EntityMinecartTest.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> SIGN_Z =
            SynchedEntityData.defineId(EntityMinecartTest.class, EntityDataSerializers.BYTE);

    public EntityMinecartTest(EntityType<? extends EntityMinecartTest> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SIGN_X, (byte) 0);
        builder.define(SIGN_Z, (byte) 0);
    }

    @Override
    public void tick() {
        Vec3 motion = getDeltaMovement();
        if (!level().isClientSide() && (motion.x != 0D || motion.z != 0D)) {
            entityData.set(SIGN_X, (byte) Mth.sign(motion.x));
            entityData.set(SIGN_Z, (byte) Mth.sign(motion.z));
        }
        super.tick();
    }

    public boolean isFlipped() {
        return entityData.get(SIGN_X) < 0 || entityData.get(SIGN_Z) < 0;
    }

    @Override
    public BlockState getDefaultDisplayBlockState() {
        return ModBlocks.CRATE.get().defaultBlockState();
    }
}
