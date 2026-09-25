// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.missile;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.generic.BlockSellafieldSlaked;
import com.hbm.config.BombConfig;
import com.hbm.data.ExplosionData;
import com.hbm.entity.ModEntities;
import com.hbm.entity.effect.EntityBlackHole;
import com.hbm.entity.effect.EntityCloudFleija;
import com.hbm.entity.effect.EntityEMPBlast;
import com.hbm.entity.logic.EntityNukeExplosionMK3;
import com.hbm.explosion.ExplosionNukeGeneric;
import com.hbm.explosion.ExplosionNukeSmall;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.items.ModItems;
import com.hbm.items.special.Autogen;
import com.hbm.items.weapon.sedna.factory.GunFactory;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public abstract class EntityMissileTier0 extends EntityMissileBaseNT {

    public EntityMissileTier0(EntityType<? extends EntityMissileTier0> type, Level level) {
        super(type, level);
    }

    @Override
    protected float getContrailScale() {
        return 0.5F;
    }

    @Override
    public List<ItemStack> getDebris() {

        List<ItemStack> list = new ArrayList<>();
        list.add(autogen(MaterialShapes.WIRE, Mats.MAT_ALUMINIUM, 4));
        list.add(new ItemStack(ModItems.plate(Mats.MAT_TITANIUM), 4));
        list.add(autogen(MaterialShapes.SHELL, Mats.MAT_ALUMINIUM, 2));
        list.add(new ItemStack(ModItems.DUCTTAPE.get()));
        return list;
    }

    public static class EntityMissileTest extends EntityMissileTier0 {

        public EntityMissileTest(EntityType<? extends EntityMissileTest> type, Level level) {
            super(type, level);
        }

        public EntityMissileTest(Level level) {
            super(ModEntities.MISSILE_TEST.get(), level);
        }

        @Override
        public void onMissileImpact(BlockHitResult mop) {
            int x = (int) Math.floor(getX());
            int y = (int) Math.floor(getY());
            int z = (int) Math.floor(getZ());
            int range = 50;

            for (int iX = -range; iX <= range; iX++) {
                for (int iY = -range; iY <= range; iY++) {
                    for (int iZ = -range; iZ <= range; iZ++) {
                        double dist = Math.sqrt(iX * iX + iY * iY + iZ * iZ);
                        if (dist > range) continue;
                        BlockPos pos = new BlockPos(x + iX, y + iY, z + iZ);
                        BlockState state = level().getBlockState(pos);
                        boolean slaked = state.getBlock() == ModBlocks.SELLAFIELD_SLAKED.get();

                        int shade =
                                state.hasProperty(BlockSellafieldSlaked.SHADE)
                                        ? state.getValue(BlockSellafieldSlaked.SHADE)
                                        : 0;
                        int charMeta =
                                (int) Mth.clamp(12 - (dist / range) * (dist / range) * 13, 0, 12);

                        if (state.isSolidRender()) {
                            if (!slaked || shade < charMeta) {
                                level().setBlock(
                                                pos,
                                                ModBlocks.SELLAFIELD_SLAKED
                                                        .get()
                                                        .defaultBlockState()
                                                        .setValue(
                                                                BlockSellafieldSlaked.SHADE,
                                                                charMeta),
                                                3);
                            }
                        } else {
                            level().setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                        }
                    }
                }
            }
        }

        @Override
        public ItemStack getDebrisRareDrop() {
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack getMissileItemForInfo() {
            return new ItemStack(ModItems.MISSILE_TEST.get());
        }
    }

    public static class EntityMissileMicro extends EntityMissileTier0 {

        public EntityMissileMicro(EntityType<? extends EntityMissileMicro> type, Level level) {
            super(type, level);
        }

        public EntityMissileMicro(Level level) {
            super(ModEntities.MISSILE_MICRO.get(), level);
        }

        @Override
        public void onMissileImpact(BlockHitResult mop) {
            ExplosionNukeSmall.explode(
                    level(), getX(), getY() + 0.5D, getZ(), ExplosionNukeSmall.PARAMS_HIGH);
        }

        @Override
        public ItemStack getDebrisRareDrop() {

            return ModItems.AMMO_STANDARD.stack(GunFactory.EnumAmmo.NUKE_HIGH);
        }

        @Override
        public ItemStack getMissileItemForInfo() {
            return new ItemStack(ModItems.MISSILE_MICRO.get());
        }
    }

    public static class EntityMissileSchrabidium extends EntityMissileTier0 {

        public EntityMissileSchrabidium(
                EntityType<? extends EntityMissileSchrabidium> type, Level level) {
            super(type, level);
        }

        public EntityMissileSchrabidium(Level level) {
            super(ModEntities.MISSILE_SCHRABIDIUM.get(), level);
        }

        @Override
        public void onMissileImpact(BlockHitResult mop) {
            int radius = ExplosionData.A_SCHRAB_RADIUS.get();

            EntityNukeExplosionMK3 ex =
                    EntityNukeExplosionMK3.statFacFleija(level(), getX(), getY(), getZ(), radius);
            if (!ex.isRemoved()) {
                level().addFreshEntity(ex);
                level().addFreshEntity(
                                EntityCloudFleija.statFac(level(), radius, getX(), getY(), getZ()));
            }
        }

        @Override
        public ItemStack getDebrisRareDrop() {
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack getMissileItemForInfo() {
            return new ItemStack(ModItems.MISSILE_SCHRABIDIUM.get());
        }
    }

    public static class EntityMissileBHole extends EntityMissileTier0 {

        public EntityMissileBHole(EntityType<? extends EntityMissileBHole> type, Level level) {
            super(type, level);
        }

        public EntityMissileBHole(Level level) {
            super(ModEntities.MISSILE_BHOLE.get(), level);
        }

        @Override
        public void onMissileImpact(BlockHitResult mop) {
            level().explode(this, getX(), getY(), getZ(), 1.5F, Level.ExplosionInteraction.BLOCK);

            EntityBlackHole hole = new EntityBlackHole(level(), 1.5F);
            hole.setPos(getX(), getY(), getZ());
            level().addFreshEntity(hole);
        }

        @Override
        public ItemStack getDebrisRareDrop() {
            return new ItemStack(ModItems.BLACK_HOLE.get());
        }

        @Override
        public ItemStack getMissileItemForInfo() {
            return new ItemStack(ModItems.MISSILE_BHOLE.get());
        }
    }

    public static class EntityMissileTaint extends EntityMissileTier0 {

        public EntityMissileTaint(EntityType<? extends EntityMissileTaint> type, Level level) {
            super(type, level);
        }

        public EntityMissileTaint(Level level) {
            super(ModEntities.MISSILE_TAINT.get(), level);
        }

        @Override
        public void onMissileImpact(BlockHitResult mop) {
            Vec3 hit = mop.getLocation();
            level().explode(this, hit.x, hit.y, hit.z, 5F, Level.ExplosionInteraction.BLOCK);

            for (int i = 0; i < 100; i++) {
                BlockPos pos =
                        new BlockPos(
                                random.nextInt(11) + mop.getBlockPos().getX() - 5,
                                random.nextInt(11) + mop.getBlockPos().getY() - 5,
                                random.nextInt(11) + mop.getBlockPos().getZ() - 5);
                BlockState state = level().getBlockState(pos);
                if (state.isSolidRender() && !state.isAir()) {
                    level().setBlock(pos, ModBlocks.TAINT.get().defaultBlockState(), 2);
                }
            }
        }

        @Override
        public ItemStack getDebrisRareDrop() {
            return new ItemStack(ModItems.POWDER_SPARK_MIX.get());
        }

        @Override
        public ItemStack getMissileItemForInfo() {
            return new ItemStack(ModItems.MISSILE_TAINT.get());
        }
    }

    public static class EntityMissileEMP extends EntityMissileTier0 {

        public EntityMissileEMP(EntityType<? extends EntityMissileEMP> type, Level level) {
            super(type, level);
        }

        public EntityMissileEMP(Level level) {
            super(ModEntities.MISSILE_EMP.get(), level);
        }

        @Override
        public void onMissileImpact(BlockHitResult mop) {
            ExplosionNukeGeneric.empBlast(
                    level(), new BlockPos((int) getX(), (int) getY(), (int) getZ()), 50);

            EntityEMPBlast wave = new EntityEMPBlast(ModEntities.EMP_BLAST.get(), level());
            wave.setPos(getX(), getY(), getZ());
            wave.setMaxAge(50);
            level().addFreshEntity(wave);
        }

        @Override
        public ItemStack getDebrisRareDrop() {
            return new ItemStack(ModBlocks.EMP_BOMB.get());
        }

        @Override
        public ItemStack getMissileItemForInfo() {
            return new ItemStack(ModItems.MISSILE_EMP.get());
        }
    }
}
