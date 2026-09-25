// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.missile;

import com.hbm.api.entity.IRadarDetectableNT;
import com.hbm.entity.ModEntities;
import com.hbm.explosion.ExplosionChaos;
import com.hbm.explosion.ExplosionLarge;
import com.hbm.inventory.material.Mats;
import com.hbm.items.ModItems;
import com.hbm.particle.helper.ExplosionCreator;
import java.util.List;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

public abstract class EntityMissileTier1 extends EntityMissileBaseNT {

    public EntityMissileTier1(EntityType<? extends EntityMissileTier1> type, Level level) {
        super(type, level);
    }

    @Override
    protected float getContrailScale() {
        return 0.5F;
    }

    @Override
    public List<ItemStack> getDebris() {
        return List.of(
                new ItemStack(ModItems.plate(Mats.MAT_TITANIUM), 4),
                new ItemStack(ModItems.THRUSTER_SMALL));
    }

    public static class EntityMissileGeneric extends EntityMissileTier1 {
        public EntityMissileGeneric(EntityType<? extends EntityMissileGeneric> type, Level level) {
            super(type, level);
        }

        public EntityMissileGeneric(Level level) {
            super(ModEntities.MISSILE_GENERIC.get(), level);
        }

        @Override
        public void onMissileImpact(BlockHitResult mop) {
            explodeStandard(15F, 24, false);
            ExplosionCreator.composeEffectSmall(level(), getX(), getY(), getZ());
        }

        @Override
        public ItemStack getDebrisRareDrop() {
            return new ItemStack(ModItems.WARHEAD_GENERIC_SMALL);
        }

        @Override
        public ItemStack getMissileItemForInfo() {
            return new ItemStack(ModItems.MISSILE_GENERIC);
        }
    }

    public static class EntityMissileIncendiary extends EntityMissileTier1 {
        public EntityMissileIncendiary(
                EntityType<? extends EntityMissileIncendiary> type, Level level) {
            super(type, level);
        }

        public EntityMissileIncendiary(Level level) {
            super(ModEntities.MISSILE_INCENDIARY.get(), level);
        }

        @Override
        public void onMissileImpact(BlockHitResult mop) {
            explodeStandard(15F, 24, true);
            ExplosionCreator.composeEffectSmall(level(), getX(), getY(), getZ());
        }

        @Override
        public ItemStack getDebrisRareDrop() {
            return new ItemStack(ModItems.WARHEAD_INCENDIARY_SMALL);
        }

        @Override
        public ItemStack getMissileItemForInfo() {
            return new ItemStack(ModItems.MISSILE_INCENDIARY);
        }
    }

    public static class EntityMissileDecoy extends EntityMissileTier1 {
        public EntityMissileDecoy(EntityType<? extends EntityMissileDecoy> type, Level level) {
            super(type, level);
        }

        public EntityMissileDecoy(Level level) {
            super(ModEntities.MISSILE_DECOY.get(), level);
        }

        @Override
        public void onMissileImpact(BlockHitResult mop) {
            level().explode(this, getX(), getY(), getZ(), 4F, Level.ExplosionInteraction.NONE);
        }

        @Override
        public ItemStack getDebrisRareDrop() {
            return new ItemStack(ModItems.ingot(Mats.MAT_STEEL));
        }

        @Override
        public ItemStack getMissileItemForInfo() {
            return new ItemStack(ModItems.MISSILE_DECOY);
        }

        @Override
        public String getRadarName() {
            return "radar.target.tier4";
        }

        @Override
        public int getBlipLevel() {
            return IRadarDetectableNT.TIER4;
        }
    }

    public static class EntityMissileBunkerBuster extends EntityMissileTier1 {
        public EntityMissileBunkerBuster(
                EntityType<? extends EntityMissileBunkerBuster> type, Level level) {
            super(type, level);
        }

        public EntityMissileBunkerBuster(Level level) {
            super(ModEntities.MISSILE_BUSTER.get(), level);
        }

        @Override
        public void onMissileImpact(BlockHitResult mop) {
            for (int i = 0; i < 15; i++)
                level().explode(
                                this,
                                getX(),
                                getY() - i,
                                getZ(),
                                5F,
                                Level.ExplosionInteraction.BLOCK);
            ExplosionLarge.spawnParticles(level(), getX(), getY(), getZ(), 5);
            ExplosionLarge.spawnShrapnels(level(), getX(), getY(), getZ(), 5);
            ExplosionLarge.spawnRubble(level(), getX(), getY(), getZ(), 5);
        }

        @Override
        public ItemStack getDebrisRareDrop() {
            return new ItemStack(ModItems.WARHEAD_BUSTER_SMALL);
        }

        @Override
        public ItemStack getMissileItemForInfo() {
            return new ItemStack(ModItems.MISSILE_BUSTER);
        }
    }

    public static class EntityMissileCluster extends EntityMissileTier1 {
        public EntityMissileCluster(EntityType<? extends EntityMissileCluster> type, Level level) {
            super(type, level);
            this.isCluster = true;
        }

        public EntityMissileCluster(Level level) {
            this(ModEntities.MISSILE_CLUSTER.get(), level);
        }

        @Override
        public void onMissileImpact(BlockHitResult mop) {
            level().explode(this, getX(), getY(), getZ(), 5F, Level.ExplosionInteraction.BLOCK);
            ExplosionChaos.cluster(
                    level(),
                    getX(),
                    getY(),
                    getZ(),
                    25,
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
            return new ItemStack(ModItems.WARHEAD_CLUSTER_SMALL);
        }

        @Override
        public ItemStack getMissileItemForInfo() {
            return new ItemStack(ModItems.MISSILE_CLUSTER);
        }
    }
}
