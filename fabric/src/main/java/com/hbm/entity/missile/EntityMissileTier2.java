// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.missile;

import com.hbm.entity.ModEntities;
import com.hbm.entity.logic.EntityEMP;
import com.hbm.explosion.ExplosionChaos;
import com.hbm.explosion.ExplosionLarge;
import com.hbm.inventory.material.Mats;
import com.hbm.items.ModItems;
import com.hbm.particle.helper.ExplosionCreator;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

public abstract class EntityMissileTier2 extends EntityMissileBaseNT {

    public EntityMissileTier2(EntityType<? extends EntityMissileTier2> type, Level level) {
        super(type, level);
    }

    @Override
    public List<ItemStack> getDebris() {
        return List.of(
                new ItemStack(ModItems.plate(Mats.MAT_STEEL), 10),
                new ItemStack(ModItems.plate(Mats.MAT_TITANIUM), 6),
                new ItemStack(ModItems.THRUSTER_MEDIUM));
    }

    public static class EntityMissileStrong extends EntityMissileTier2 {
        public EntityMissileStrong(EntityType<? extends EntityMissileStrong> type, Level level) {
            super(type, level);
        }

        public EntityMissileStrong(Level level) {
            super(ModEntities.MISSILE_STRONG.get(), level);
        }

        @Override
        public void onMissileImpact(BlockHitResult mop) {
            explodeStandard(30F, 32, false);
            ExplosionCreator.composeEffectStandard(level(), getX(), getY(), getZ());
        }

        @Override
        public ItemStack getDebrisRareDrop() {
            return new ItemStack(ModItems.WARHEAD_GENERIC_MEDIUM);
        }

        @Override
        public ItemStack getMissileItemForInfo() {
            return new ItemStack(ModItems.MISSILE_STRONG);
        }
    }

    public static class EntityMissileIncendiaryStrong extends EntityMissileTier2 {
        public EntityMissileIncendiaryStrong(
                EntityType<? extends EntityMissileIncendiaryStrong> type, Level level) {
            super(type, level);
        }

        public EntityMissileIncendiaryStrong(Level level) {
            super(ModEntities.MISSILE_INCENDIARY_STRONG.get(), level);
        }

        @Override
        public void onMissileImpact(BlockHitResult mop) {
            explodeStandard(30F, 32, true);
            ExplosionCreator.composeEffectStandard(level(), getX(), getY(), getZ());
            if (level() instanceof ServerLevel server)
                ExplosionChaos.igniteFlammableBlocks(
                        server, getBlockX(), getBlockY(), getBlockZ(), 25);
        }

        @Override
        public ItemStack getDebrisRareDrop() {
            return new ItemStack(ModItems.WARHEAD_INCENDIARY_MEDIUM);
        }

        @Override
        public ItemStack getMissileItemForInfo() {
            return new ItemStack(ModItems.MISSILE_INCENDIARY_STRONG);
        }
    }

    public static class EntityMissileBusterStrong extends EntityMissileTier2 {
        public EntityMissileBusterStrong(
                EntityType<? extends EntityMissileBusterStrong> type, Level level) {
            super(type, level);
        }

        public EntityMissileBusterStrong(Level level) {
            super(ModEntities.MISSILE_BUSTER_STRONG.get(), level);
        }

        @Override
        public void onMissileImpact(BlockHitResult mop) {
            for (int i = 0; i < 20; i++)
                level().explode(
                                this,
                                getX(),
                                getY() - i,
                                getZ(),
                                7.5F,
                                Level.ExplosionInteraction.BLOCK);
            ExplosionLarge.spawnParticles(level(), getX(), getY(), getZ(), 8);
            ExplosionLarge.spawnShrapnels(level(), getX(), getY(), getZ(), 8);
            ExplosionLarge.spawnRubble(level(), getX(), getY(), getZ(), 8);
        }

        @Override
        public ItemStack getDebrisRareDrop() {
            return new ItemStack(ModItems.WARHEAD_BUSTER_MEDIUM);
        }

        @Override
        public ItemStack getMissileItemForInfo() {
            return new ItemStack(ModItems.MISSILE_BUSTER_STRONG);
        }
    }

    public static class EntityMissileClusterStrong extends EntityMissileTier2 {
        public EntityMissileClusterStrong(
                EntityType<? extends EntityMissileClusterStrong> type, Level level) {
            super(type, level);
            this.isCluster = true;
        }

        public EntityMissileClusterStrong(Level level) {
            this(ModEntities.MISSILE_CLUSTER_STRONG.get(), level);
        }

        @Override
        public void onMissileImpact(BlockHitResult mop) {
            level().explode(this, getX(), getY(), getZ(), 15F, Level.ExplosionInteraction.BLOCK);
            ExplosionChaos.cluster(
                    level(),
                    getX(),
                    getY(),
                    getZ(),
                    50,
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
            return new ItemStack(ModItems.WARHEAD_CLUSTER_MEDIUM);
        }

        @Override
        public ItemStack getMissileItemForInfo() {
            return new ItemStack(ModItems.MISSILE_CLUSTER_STRONG);
        }
    }

    public static class EntityMissileEMPStrong extends EntityMissileTier2 {
        public EntityMissileEMPStrong(
                EntityType<? extends EntityMissileEMPStrong> type, Level level) {
            super(type, level);
        }

        public EntityMissileEMPStrong(Level level) {
            this(ModEntities.MISSILE_EMP_STRONG.get(), level);
        }

        @Override
        public void onMissileImpact(BlockHitResult mop) {
            EntityEMP emp = new EntityEMP(ModEntities.EMP_LOGIC.get(), level());
            emp.setPos(getX(), getY(), getZ());
            level().addFreshEntity(emp);
        }

        @Override
        public ItemStack getDebrisRareDrop() {
            return new ItemStack(ModItems.WARHEAD_GENERIC_MEDIUM);
        }

        @Override
        public ItemStack getMissileItemForInfo() {
            return new ItemStack(ModItems.MISSILE_EMP_STRONG);
        }
    }
}
