/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: HbmMods (The Bobcat)
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) HbmMods (The Bobcat). All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only,
 * as published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this code. If not, see <https://www.gnu.org/licenses/>.
 *
 * Under GPLv3 §7(c) and §7(e), these additional terms apply to LGPL-covered
 * material authored by movblock:
 *
 * - the license grants no rights to the name "NTM NEXT";
 * - a modified version must not be named NTM NEXT or presented as the
 *   official release, and must be marked as different from it.
 *
 * Accurately identifying a work as derived from NTM NEXT does not violate
 * these additional terms, provided it is clearly distinguished from the
 * official release and does not imply endorsement. Ports are encouraged to
 * acknowledge NTM NEXT as their upstream project and provide a link to it.
 */

package com.hbm.entity.missile;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.generic.SellafieldSlakedBlock;
import com.hbm.data.ExplosionData;
import com.hbm.entity.ModEntities;
import com.hbm.entity.effect.BlackHoleEntity;
import com.hbm.entity.effect.CloudFleijaEntity;
import com.hbm.entity.effect.EMPBlastEntity;
import com.hbm.entity.logic.NukeExplosionMK3Entity;
import com.hbm.entity.missile.Missile;
import com.hbm.explosion.ElectromagneticPulse;
import com.hbm.explosion.MiniNukeExplosion;
import com.hbm.inventory.material.MaterialShape;
import com.hbm.inventory.material.Materials;
import com.hbm.items.ModItems;
import com.hbm.items.special.Autogen;
import com.hbm.items.weapon.sedna.factory.GunFactory;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public abstract class MissileTier0Entity extends Missile {

    public MissileTier0Entity(EntityType<? extends MissileTier0Entity> type, Level level) {
        super(type, level);
    }

    @Override
    protected float contrailScale() {
        return 0.5F;
    }

    @Override
    public List<ItemStack> debris() {
        List<ItemStack> list = new ArrayList<>();
        list.add(new ItemStack(Autogen.require(MaterialShape.FINE_WIRE, Materials.ALUMINUM), 4));
        list.add(new ItemStack(ModItems.plate(Materials.TITANIUM), 4));
        list.add(new ItemStack(Autogen.require(MaterialShape.SHELL, Materials.ALUMINUM), 2));
        list.add(new ItemStack(ModItems.DUCTTAPE.get()));
        return list;
    }

    public static class EntityMissileTest extends MissileTier0Entity {

        public EntityMissileTest(EntityType<? extends EntityMissileTest> type, Level level) {
            super(type, level);
        }

        public EntityMissileTest(Level level) {
            super(ModEntities.MISSILE_TEST.get(), level);
        }

        @Override
        public void detonate(BlockHitResult mop) {
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
                                state.hasProperty(SellafieldSlakedBlock.SHADE)
                                        ? state.getValue(SellafieldSlakedBlock.SHADE)
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
                                                                SellafieldSlakedBlock.SHADE,
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
        public ItemStack rareDebris() {
            return ItemStack.EMPTY;
        }

        @Override
        public Item missileItem() {
            return ModItems.MISSILE_TEST.get();
        }
    }

    public static class EntityMissileMicro extends MissileTier0Entity {

        public EntityMissileMicro(EntityType<? extends EntityMissileMicro> type, Level level) {
            super(type, level);
        }

        public EntityMissileMicro(Level level) {
            super(ModEntities.MISSILE_MICRO.get(), level);
        }

        @Override
        public void detonate(BlockHitResult mop) {
            MiniNukeExplosion.high().detonate(level(), getX(), getY() + 0.5D, getZ());
        }

        @Override
        public ItemStack rareDebris() {
            return ModItems.AMMO_STANDARD.stack(GunFactory.EnumAmmo.NUKE_HIGH);
        }

        @Override
        public Item missileItem() {
            return ModItems.MISSILE_MICRO.get();
        }
    }

    public static class EntityMissileSchrabidium extends MissileTier0Entity {

        public EntityMissileSchrabidium(
                EntityType<? extends EntityMissileSchrabidium> type, Level level) {
            super(type, level);
        }

        public EntityMissileSchrabidium(Level level) {
            super(ModEntities.MISSILE_SCHRABIDIUM.get(), level);
        }

        @Override
        public void detonate(BlockHitResult mop) {
            int radius = ExplosionData.A_SCHRAB_RADIUS.get();

            NukeExplosionMK3Entity ex =
                    NukeExplosionMK3Entity.statFacFleija(level(), getX(), getY(), getZ(), radius);
            if (!ex.isRemoved()) {
                level().addFreshEntity(ex);
                level().addFreshEntity(
                                CloudFleijaEntity.statFac(level(), radius, getX(), getY(), getZ()));
            }
        }

        @Override
        public ItemStack rareDebris() {
            return ItemStack.EMPTY;
        }

        @Override
        public Item missileItem() {
            return ModItems.MISSILE_SCHRABIDIUM.get();
        }
    }

    public static class EntityMissileBHole extends MissileTier0Entity {

        public EntityMissileBHole(EntityType<? extends EntityMissileBHole> type, Level level) {
            super(type, level);
        }

        public EntityMissileBHole(Level level) {
            super(ModEntities.MISSILE_BHOLE.get(), level);
        }

        @Override
        public void detonate(BlockHitResult mop) {
            level().explode(this, getX(), getY(), getZ(), 1.5F, Level.ExplosionInteraction.BLOCK);

            BlackHoleEntity hole = new BlackHoleEntity(level(), 1.5F);
            hole.setPos(getX(), getY(), getZ());
            level().addFreshEntity(hole);
        }

        @Override
        public ItemStack rareDebris() {
            return new ItemStack(ModItems.BLACK_HOLE.get());
        }

        @Override
        public Item missileItem() {
            return ModItems.MISSILE_BHOLE.get();
        }
    }

    public static class EntityMissileTaint extends MissileTier0Entity {

        public EntityMissileTaint(EntityType<? extends EntityMissileTaint> type, Level level) {
            super(type, level);
        }

        public EntityMissileTaint(Level level) {
            super(ModEntities.MISSILE_TAINT.get(), level);
        }

        @Override
        public void detonate(BlockHitResult mop) {
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
        public ItemStack rareDebris() {
            return new ItemStack(ModItems.POWDER_SPARK_MIX.get());
        }

        @Override
        public Item missileItem() {
            return ModItems.MISSILE_TAINT.get();
        }
    }

    public static class EntityMissileEMP extends MissileTier0Entity {

        public EntityMissileEMP(EntityType<? extends EntityMissileEMP> type, Level level) {
            super(type, level);
        }

        public EntityMissileEMP(Level level) {
            super(ModEntities.MISSILE_EMP.get(), level);
        }

        @Override
        public void detonate(BlockHitResult mop) {
            ElectromagneticPulse.sphere(
                    level(), new BlockPos((int) getX(), (int) getY(), (int) getZ()), 50);

            EMPBlastEntity wave = new EMPBlastEntity(ModEntities.EMP_BLAST.get(), level());
            wave.setPos(getX(), getY(), getZ());
            wave.setMaxAge(50);
            level().addFreshEntity(wave);
        }

        @Override
        public ItemStack rareDebris() {
            return new ItemStack(ModBlocks.EMP_BOMB.get());
        }

        @Override
        public Item missileItem() {
            return ModItems.MISSILE_EMP.get();
        }
    }
}
