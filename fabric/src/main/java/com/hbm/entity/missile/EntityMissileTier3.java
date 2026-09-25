// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.missile;

import com.hbm.entity.ModEntities;
import com.hbm.explosion.ExplosionChaos;
import com.hbm.explosion.ExplosionLarge;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.BlockAllocatorStandard;
import com.hbm.explosion.vanillant.standard.BlockProcessorStandard;
import com.hbm.explosion.vanillant.standard.EntityProcessorStandard;
import com.hbm.explosion.vanillant.standard.PlayerProcessorStandard;
import com.hbm.inventory.material.Mats;
import com.hbm.items.ModItems;
import com.hbm.particle.helper.ExplosionCreator;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public abstract class EntityMissileTier3 extends EntityMissileBaseNT {

    public EntityMissileTier3(EntityType<? extends EntityMissileTier3> type, Level level) {
        super(type, level);
    }

    @Override
    protected void spawnContrail(Vec3 step) {
        Vec3 thrust =
                new Vec3(0, 0, 0.5)
                        .yRot((renderYaw + 90F) * (float) Math.PI / 180F)
                        .xRot(renderPitch * (float) Math.PI / 180F)
                        .yRot(-(renderYaw + 90F) * (float) Math.PI / 180F);

        spawnContrailWithOffset(step, thrust.x, thrust.y, thrust.z);
        spawnContrailWithOffset(step, -thrust.z, thrust.y, thrust.x);
        spawnContrailWithOffset(step, -thrust.x, -thrust.z, -thrust.z);
        spawnContrailWithOffset(step, thrust.z, -thrust.z, -thrust.x);
    }

    @Override
    public List<ItemStack> getDebris() {
        return List.of(
                new ItemStack(ModItems.plate(Mats.MAT_STEEL), 16),
                new ItemStack(ModItems.plate(Mats.MAT_TITANIUM), 10),
                new ItemStack(ModItems.THRUSTER_LARGE));
    }

    public static class EntityMissileBurst extends EntityMissileTier3 {
        public EntityMissileBurst(EntityType<? extends EntityMissileBurst> type, Level level) {
            super(type, level);
        }

        public EntityMissileBurst(Level level) {
            super(ModEntities.MISSILE_BURST.get(), level);
        }

        @Override
        public void onMissileImpact(BlockHitResult mop) {
            explodeStandard(50F, 48, false);
            ExplosionCreator.composeEffectLarge(level(), getX(), getY(), getZ());
        }

        @Override
        public ItemStack getDebrisRareDrop() {
            return new ItemStack(ModItems.WARHEAD_GENERIC_LARGE);
        }

        @Override
        public ItemStack getMissileItemForInfo() {
            return new ItemStack(ModItems.MISSILE_BURST);
        }
    }

    public static class EntityMissileInferno extends EntityMissileTier3 {
        public EntityMissileInferno(EntityType<? extends EntityMissileInferno> type, Level level) {
            super(type, level);
        }

        public EntityMissileInferno(Level level) {
            super(ModEntities.MISSILE_INFERNO.get(), level);
        }

        @Override
        public void onMissileImpact(BlockHitResult mop) {
            explodeStandard(50F, 48, true);
            ExplosionCreator.composeEffectLarge(level(), getX(), getY(), getZ());
            if (level() instanceof ServerLevel server) {
                ExplosionChaos.igniteAllBlocks(server, getBlockX(), getBlockY(), getBlockZ(), 10);
                ExplosionChaos.igniteFlammableBlocks(
                        server, getBlockX(), getBlockY(), getBlockZ(), 25);
            }
        }

        @Override
        public ItemStack getDebrisRareDrop() {
            return new ItemStack(ModItems.WARHEAD_INCENDIARY_LARGE);
        }

        @Override
        public ItemStack getMissileItemForInfo() {
            return new ItemStack(ModItems.MISSILE_INFERNO);
        }
    }

    public static class EntityMissileRain extends EntityMissileTier3 {
        public EntityMissileRain(EntityType<? extends EntityMissileRain> type, Level level) {
            super(type, level);
            this.isCluster = true;
        }

        public EntityMissileRain(Level level) {
            this(ModEntities.MISSILE_RAIN.get(), level);
        }

        @Override
        public void onMissileImpact(BlockHitResult mop) {
            level().explode(this, getX(), getY(), getZ(), 25F, Level.ExplosionInteraction.BLOCK);
            ExplosionChaos.cluster(
                    level(),
                    getX(),
                    getY(),
                    getZ(),
                    100,
                    getYRot(),
                    getXRot(),
                    (float) Math.PI * 0.25F,
                    (float) Math.PI * 0.25F,
                    1F);
        }

        @Override
        public void cluster() {
            this.onMissileImpact(null);
        }

        @Override
        public ItemStack getDebrisRareDrop() {
            return new ItemStack(ModItems.WARHEAD_CLUSTER_LARGE);
        }

        @Override
        public ItemStack getMissileItemForInfo() {
            return new ItemStack(ModItems.MISSILE_RAIN);
        }
    }

    public static class EntityMissileDrill extends EntityMissileTier3 {
        public EntityMissileDrill(EntityType<? extends EntityMissileDrill> type, Level level) {
            super(type, level);
        }

        public EntityMissileDrill(Level level) {
            this(ModEntities.MISSILE_DRILL.get(), level);
        }

        @Override
        public void onMissileImpact(BlockHitResult mop) {

            for (int i = 0; i < 30; i++) {
                ExplosionVNT vnt = new ExplosionVNT(level(), getX(), getY() - i, getZ(), 10F, this);
                vnt.setBlockAllocator(new BlockAllocatorStandard(24));
                vnt.setBlockProcessor(new BlockProcessorStandard().setNoDrop());
                vnt.setEntityProcessor(new EntityProcessorStandard());
                vnt.setPlayerProcessor(new PlayerProcessorStandard());
                vnt.explode();
            }
            ExplosionLarge.spawnParticles(level(), getX(), getY(), getZ(), 25);
            ExplosionLarge.spawnShrapnels(level(), getX(), getY(), getZ(), 12);
            ExplosionLarge.jolt(level(), getX(), getY(), getZ(), 10, 50, 1);
        }

        @Override
        public ItemStack getDebrisRareDrop() {
            return new ItemStack(ModItems.WARHEAD_BUSTER_LARGE);
        }

        @Override
        public ItemStack getMissileItemForInfo() {
            return new ItemStack(ModItems.MISSILE_DRILL);
        }
    }
}
