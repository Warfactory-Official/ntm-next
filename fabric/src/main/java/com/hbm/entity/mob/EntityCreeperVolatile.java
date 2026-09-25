// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.mob;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.generic.BlockSlag;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.BlockAllocatorBulkie;
import com.hbm.explosion.vanillant.standard.BlockMutatorBulkie;
import com.hbm.explosion.vanillant.standard.BlockProcessorStandard;
import com.hbm.explosion.vanillant.standard.EntityProcessorStandard;
import com.hbm.explosion.vanillant.standard.ExplosionEffectStandard;
import com.hbm.explosion.vanillant.standard.PlayerProcessorStandard;
import com.hbm.items.ModItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

public class EntityCreeperVolatile extends EntityCreeperBase {

    public EntityCreeperVolatile(EntityType<? extends EntityCreeperVolatile> type, Level level) {
        super(type, level);
    }

    @Override
    protected void detonate(ServerLevel level) {
        ExplosionVNT vnt =
                new ExplosionVNT(level, getX(), getY(), getZ(), isPowered() ? 14 : 7, this);
        vnt.setBlockAllocator(new BlockAllocatorBulkie(60, isPowered() ? 32 : 16));
        vnt.setBlockProcessor(
                new BlockProcessorStandard()
                        .withBlockEffect(
                                new BlockMutatorBulkie(
                                        ModBlocks.BLOCK_SLAG
                                                .get()
                                                .defaultBlockState()
                                                .setValue(BlockSlag.CRACKED, true))));
        vnt.setEntityProcessor(new EntityProcessorStandard().withRangeMod(0.5F));
        vnt.setPlayerProcessor(new PlayerProcessorStandard());
        vnt.setSFX(new ExplosionEffectStandard());
        vnt.explode();
    }

    @Override
    public boolean checkSpawnRules(LevelAccessor level, EntitySpawnReason reason) {
        return super.checkSpawnRules(level, reason)
                && getY() <= 40D
                && level().dimension() == Level.OVERWORLD;
    }

    @Override
    protected void dropCustomDeathLoot(
            ServerLevel level, DamageSource source, boolean killedByPlayer) {
        super.dropCustomDeathLoot(level, source, killedByPlayer);
        spawnAtLocation(level, new ItemStack(ModItems.SULFUR.get(), 2 + random.nextInt(3)));
        spawnAtLocation(level, new ItemStack(ModItems.STICK_TNT.get(), 1 + random.nextInt(2)));
    }
}
