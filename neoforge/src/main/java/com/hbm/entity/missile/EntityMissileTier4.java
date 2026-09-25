// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.missile;

import com.hbm.blocks.ModBlocks;
import com.hbm.config.BombConfig;
import com.hbm.data.ExplosionData;
import com.hbm.entity.ModEntities;
import com.hbm.entity.effect.EntityNukeTorex;
import com.hbm.entity.logic.EntityNukeExplosionMK5;
import com.hbm.explosion.ExplosionLarge;
import com.hbm.inventory.material.Mats;
import com.hbm.items.ModItems;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public abstract class EntityMissileTier4 extends EntityMissileBaseNT {

    public EntityMissileTier4(EntityType<? extends EntityMissileTier4> type, Level level) {
        super(type, level);
    }

    @Override
    protected void spawnContrail(Vec3 step) {
        Vec3 thrust =
                switch (getFacing()) {
                    case 2 -> new Vec3(0, 0, 1).yRot((float) -Math.PI / 2F);
                    case 4 -> new Vec3(0, 0, 1).yRot((float) -Math.PI);
                    case 3 -> new Vec3(0, 0, 1).yRot((float) -Math.PI / 2F * 3F);
                    default -> new Vec3(0, 0, 1);
                };
        thrust =
                thrust.yRot((renderYaw + 90F) * (float) Math.PI / 180F)
                        .xRot(renderPitch * (float) Math.PI / 180F)
                        .yRot(-(renderYaw + 90F) * (float) Math.PI / 180F);

        spawnContrailWithOffset(step, thrust.x, thrust.y, thrust.z);
        spawnContrailWithOffset(step, 0, 0, 0);
        spawnContrailWithOffset(step, -thrust.x, -thrust.z, -thrust.z);
    }

    @Override
    public List<ItemStack> getDebris() {
        return List.of(
                new ItemStack(ModItems.plate(Mats.MAT_TITANIUM), 16),
                new ItemStack(ModItems.plate(Mats.MAT_STEEL), 20),
                new ItemStack(ModItems.plate(Mats.MAT_ALUMINIUM), 12),
                new ItemStack(ModItems.THRUSTER_LARGE));
    }

    protected void detonateNuke(int radius) {
        EntityNukeExplosionMK5 mk5 =
                EntityNukeExplosionMK5.statFac(level(), radius, getX(), getY(), getZ());
        level().addFreshEntity(mk5);
        EntityNukeTorex.statFac(level(), getX(), getY(), getZ(), radius);
    }

    public static class EntityMissileNuclear extends EntityMissileTier4 {
        public EntityMissileNuclear(EntityType<? extends EntityMissileNuclear> type, Level level) {
            super(type, level);
        }

        public EntityMissileNuclear(Level level) {
            super(ModEntities.MISSILE_NUCLEAR.get(), level);
        }

        @Override
        public void onMissileImpact(BlockHitResult mop) {
            detonateNuke(ExplosionData.MISSILE_RADIUS.get());
        }

        @Override
        public ItemStack getDebrisRareDrop() {
            return new ItemStack(ModItems.WARHEAD_NUCLEAR);
        }

        @Override
        public ItemStack getMissileItemForInfo() {
            return new ItemStack(ModItems.MISSILE_NUCLEAR);
        }
    }

    public static class EntityMissileMirv extends EntityMissileTier4 {
        public EntityMissileMirv(EntityType<? extends EntityMissileMirv> type, Level level) {
            super(type, level);
        }

        public EntityMissileMirv(Level level) {
            super(ModEntities.MISSILE_NUCLEAR_CLUSTER.get(), level);
        }

        @Override
        public void onMissileImpact(BlockHitResult mop) {
            detonateNuke(ExplosionData.MISSILE_RADIUS.get() * 2);
        }

        @Override
        public ItemStack getDebrisRareDrop() {
            return new ItemStack(ModItems.WARHEAD_MIRV);
        }

        @Override
        public ItemStack getMissileItemForInfo() {
            return new ItemStack(ModItems.MISSILE_NUCLEAR_CLUSTER);
        }
    }

    public static class EntityMissileDoomsday extends EntityMissileTier4 {
        public EntityMissileDoomsday(
                EntityType<? extends EntityMissileDoomsday> type, Level level) {
            super(type, level);
        }

        public EntityMissileDoomsday(Level level) {
            super(ModEntities.MISSILE_DOOMSDAY.get(), level);
        }

        @Override
        public ItemStack getMissileItemForInfo() {
            return new ItemStack(ModItems.MISSILE_DOOMSDAY);
        }

        protected void detonateDoomsday(int radius) {
            EntityNukeExplosionMK5 mk5 =
                    EntityNukeExplosionMK5.statFac(level(), radius, getX(), getY(), getZ())
                            .moreFallout(100);
            level().addFreshEntity(mk5);
            EntityNukeTorex.statFac(level(), getX(), getY(), getZ(), radius);
        }

        @Override
        public void onMissileImpact(BlockHitResult mop) {
            detonateDoomsday(ExplosionData.MISSILE_RADIUS.get() * 2);
        }

        @Override
        public List<ItemStack> getDebris() {
            return List.of();
        }

        @Override
        public ItemStack getDebrisRareDrop() {
            return ItemStack.EMPTY;
        }

        @Override
        public String getRadarName() {
            return "radar.target.doomsday";
        }
    }

    public static class EntityMissileDoomsdayRusted extends EntityMissileDoomsday {
        public EntityMissileDoomsdayRusted(
                EntityType<? extends EntityMissileDoomsdayRusted> type, Level level) {
            super(type, level);
        }

        public EntityMissileDoomsdayRusted(Level level) {
            super(ModEntities.MISSILE_DOOMSDAY_RUSTED.get(), level);
        }

        @Override
        public void onMissileImpact(BlockHitResult mop) {
            detonateDoomsday(ExplosionData.MISSILE_RADIUS.get());
        }

        @Override
        public ItemStack getMissileItemForInfo() {
            return new ItemStack(ModItems.MISSILE_DOOMSDAY_RUSTED);
        }
    }

    public static class EntityMissileVolcano extends EntityMissileTier4 {
        public EntityMissileVolcano(EntityType<? extends EntityMissileVolcano> type, Level level) {
            super(type, level);
        }

        public EntityMissileVolcano(Level level) {
            this(ModEntities.MISSILE_VOLCANO.get(), level);
        }

        @Override
        public void onMissileImpact(BlockHitResult mop) {
            ExplosionLarge.explode(level(), getX(), getY(), getZ(), 10.0F, true, true, true);

            BlockPos centre = BlockPos.containing(getX(), getY(), getZ());
            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    for (int z = -1; z <= 1; z++) {
                        level().setBlockAndUpdate(
                                        centre.offset(x, y, z),
                                        ModBlocks.VOLCANIC_LAVA_BLOCK.get().defaultBlockState());
                    }
                }
            }
            level().setBlockAndUpdate(centre, ModBlocks.VOLCANO_CORE.get().defaultBlockState());
        }

        @Override
        public ItemStack getDebrisRareDrop() {
            return new ItemStack(ModItems.WARHEAD_VOLCANO);
        }

        @Override
        public ItemStack getMissileItemForInfo() {
            return new ItemStack(ModItems.MISSILE_VOLCANO);
        }
    }
}
