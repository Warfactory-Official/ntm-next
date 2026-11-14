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

import com.hbm.api.entity.RadarTargetType;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.bomb.TaintBlock;
import com.hbm.client.ClientEffects;
import com.hbm.entity.ModEntities;
import com.hbm.entity.effect.NukeTorexEntity;
import com.hbm.entity.logic.BalefireEntity;
import com.hbm.entity.logic.NukeExplosionMK5Entity;
import com.hbm.entity.projectile.BulletBaseMK4Entity;
import com.hbm.explosion.BlastDebris;
import com.hbm.explosion.CompositeExplosion;
import com.hbm.explosion.ExplosionChaos;
import com.hbm.handler.MissileStruct;
import com.hbm.items.ModItems;
import com.hbm.items.weapon.CustomMissilePartItem.PartSize;
import com.hbm.items.weapon.CustomMissilePartItem.WarheadType;
import com.hbm.items.weapon.CustomMissilePartItem;
import com.hbm.items.weapon.sedna.factory.XFactoryWarhead;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class CustomMissile extends Missile {

    private static final EntityDataAccessor<ItemStack> WARHEAD =
            SynchedEntityData.defineId(CustomMissile.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<ItemStack> FUSELAGE =
            SynchedEntityData.defineId(CustomMissile.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<ItemStack> FINS =
            SynchedEntityData.defineId(CustomMissile.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<ItemStack> THRUSTER =
            SynchedEntityData.defineId(CustomMissile.class, EntityDataSerializers.ITEM_STACK);

    private static final Map<WarheadType, Effect> EFFECTS =
            Map.of(
                    WarheadType.HE, CustomMissile::highExplosive,
                    WarheadType.INC, CustomMissile::incendiary,
                    WarheadType.BUSTER,
                            (missile, strength) ->
                                    CompositeExplosion.bust(
                                            missile.level(),
                                            missile.getX(),
                                            missile.getY(),
                                            missile.getZ(),
                                            missile.getDeltaMovement(),
                                            strength,
                                            4F * strength),
                    WarheadType.NUCLEAR, CustomMissile::nuclear,
                    WarheadType.TX, CustomMissile::nuclear,
                    WarheadType.N2, CustomMissile::n2,
                    WarheadType.BALEFIRE, CustomMissile::balefire,
                    WarheadType.TAINT, CustomMissile::taint,
                    WarheadType.CLOUD, CustomMissile::cloud,
                    WarheadType.TURBINE, CustomMissile::turbine);

    public float fuel;
    public float consumption;
    private MissileStruct parts = MissileStruct.EMPTY;
    private @Nullable WarheadType warheadType;
    private float warheadStrength;
    private ClientEffects.@Nullable Contrail contrail;
    private String getRadarName = "radar.target.custom";
    private RadarTargetType blip = RadarTargetType.TIER1;

    public CustomMissile(EntityType<? extends CustomMissile> type, Level level) {
        super(type, level);
    }

    public void launch(
            double x, double y, double z, int targetX, int targetZ, MissileStruct parts) {
        launch(x, y, z, targetX, targetZ);
        setParts(
                stack(parts.warhead()),
                stack(parts.fuselage()),
                stack(parts.fins()),
                stack(parts.thruster()));
        fuel = parts.fuselage() != null ? parts.fuselage().fuelAmount() : 0F;
        consumption = parts.thruster() != null ? parts.thruster().consumption() : 0F;
    }

    private static ItemStack stack(@Nullable CustomMissilePartItem part) {
        return part == null ? ItemStack.EMPTY : new ItemStack(part);
    }

    public void setParts(
            @Nullable ItemStack warhead,
            @Nullable ItemStack fuselage,
            @Nullable ItemStack fins,
            @Nullable ItemStack thruster) {
        entityData.set(WARHEAD, warhead == null ? ItemStack.EMPTY : warhead);
        entityData.set(FUSELAGE, fuselage == null ? ItemStack.EMPTY : fuselage);
        entityData.set(FINS, fins == null ? ItemStack.EMPTY : fins);
        entityData.set(THRUSTER, thruster == null ? ItemStack.EMPTY : thruster);
    }

    public MissileStruct parts() {
        return parts;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(WARHEAD, ItemStack.EMPTY);
        builder.define(FUSELAGE, ItemStack.EMPTY);
        builder.define(FINS, ItemStack.EMPTY);
        builder.define(THRUSTER, ItemStack.EMPTY);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        super.onSyncedDataUpdated(accessor);
        if (!WARHEAD.equals(accessor)
                && !FUSELAGE.equals(accessor)
                && !FINS.equals(accessor)
                && !THRUSTER.equals(accessor)) return;
        var warhead = CustomMissilePartItem.part(entityData.get(WARHEAD));
        var fuselage = CustomMissilePartItem.part(entityData.get(FUSELAGE));
        parts =
                new MissileStruct(
                        warhead,
                        fuselage,
                        CustomMissilePartItem.part(entityData.get(FINS)),
                        CustomMissilePartItem.part(entityData.get(THRUSTER)));
        warheadType = warhead == null ? null : warhead.warheadType();
        warheadStrength = warhead == null ? 0F : warhead.warheadStrength();
        var fuelType = fuselage == null ? null : fuselage.fuelType();
        contrail = fuelType == null ? null : fuelType.contrail;
        getRadarName = "radar.target.custom";
        blip = RadarTargetType.TIER1;
        if (fuselage == null) return;
        if (fuselage.top == PartSize.SIZE_10 && fuselage.bottom == PartSize.SIZE_10) {
            getRadarName = "radar.target.custom10";
            blip = RadarTargetType.TIER10;
        } else if (fuselage.top == PartSize.SIZE_10 && fuselage.bottom == PartSize.SIZE_15) {
            getRadarName = "radar.target.custom1015";
            blip = RadarTargetType.TIER10_15;
        } else if (fuselage.top == PartSize.SIZE_15 && fuselage.bottom == PartSize.SIZE_15) {
            getRadarName = "radar.target.custom15";
            blip = RadarTargetType.TIER15;
        } else if (fuselage.top == PartSize.SIZE_15 && fuselage.bottom == PartSize.SIZE_20) {
            getRadarName = "radar.target.custom1520";
            blip = RadarTargetType.TIER15_20;
        } else if (fuselage.top == PartSize.SIZE_20 && fuselage.bottom == PartSize.SIZE_20) {
            getRadarName = "radar.target.custom20";
            blip = RadarTargetType.TIER20;
        }
    }

    @Override
    public void tick() {
        var type = warheadType;
        if (type != null && type.updateCustom != null) type.updateCustom.accept(this);
        if (!level().isClientSide() && fuel > 0F) fuel -= consumption;
        super.tick();
    }

    @Override
    protected boolean hasPropulsion() {
        return fuel > 0F;
    }

    @Override
    public String sourceName() {
        return "EntityMissileCustom";
    }

    @Override
    public Item missileItem() {
        return ModItems.MISSILE_CUSTOM.get();
    }

    @Override
    public void detonate(@Nullable BlockHitResult contact) {
        var type = warheadType;
        if (type == null) return;
        if (type.impactCustom != null) {
            type.impactCustom.accept(this);
            return;
        }
        var effect = EFFECTS.get(type);
        if (effect != null) effect.detonate(this, warheadStrength);
    }

    @Override
    public String getRadarName() {
        return getRadarName;
    }

    @Override
    public RadarTargetType getTargetType() {
        return blip;
    }

    @Override
    protected void exhaust(Vec3 step) {
        var colour = contrail;
        if (colour == null) return;
        var direction = getDeltaMovement().normalize();
        double x = getX(), y = getY(), z = getZ();
        for (int i = 0; i < speed; i++) {
            ClientEffects.spawnContrail(
                    level(), x - direction.x * i, y - direction.y * i, z - direction.z * i, colour);
        }
    }

    @FunctionalInterface
    private interface Effect {
        void detonate(CustomMissile missile, float strength);
    }

    private static void highExplosive(CustomMissile missile, float strength) {
        var level = missile.level();
        double x = missile.getX(), y = missile.getY(), z = missile.getZ();
        CompositeExplosion.detonate(
                level, x, y, z, strength, CompositeExplosion.CLOUD | CompositeExplosion.SHRAPNEL);
        BlastDebris.jolt(level, x, y, z, strength, (int) (50 * strength), 0.25D);
    }

    private static void incendiary(CustomMissile missile, float strength) {
        var level = missile.level();
        double x = missile.getX(), y = missile.getY(), z = missile.getZ();
        CompositeExplosion.incendiary(
                level, x, y, z, strength, CompositeExplosion.CLOUD | CompositeExplosion.SHRAPNEL);
        BlastDebris.jolt(level, x, y, z, 1.5D * strength, (int) (50 * strength), 0.25D);
    }

    private static void nuclear(CustomMissile missile, float strength) {
        var level = missile.level();
        double x = missile.getX(), y = missile.getY(), z = missile.getZ();
        level.addFreshEntity(NukeExplosionMK5Entity.statFac(level, (int) strength, x, y, z));
        NukeTorexEntity.statFac(level, x, y, z, strength);
    }

    private static void n2(CustomMissile missile, float strength) {
        var level = missile.level();
        double x = missile.getX(), y = missile.getY(), z = missile.getZ();
        level.addFreshEntity(NukeExplosionMK5Entity.statFacNoRad(level, (int) strength, x, y, z));
        NukeTorexEntity.statFac(level, x, y, z, strength);
    }

    private static void balefire(CustomMissile missile, float strength) {
        var level = missile.level();
        double x = missile.getX(), y = missile.getY(), z = missile.getZ();
        var blast = new BalefireEntity(ModEntities.BALEFIRE.get(), level);
        blast.setPos(x, y, z);
        blast.destructionRange = (int) strength;
        level.addFreshEntity(blast);
        NukeTorexEntity.statFacBale(level, x, y, z, strength);
    }

    private static void taint(CustomMissile missile, float strength) {
        var level = missile.level();
        var random = level.getRandom();
        int reach = (int) strength;
        int shift = reach / 2 - 1;
        int x = (int) missile.getX(), y = (int) missile.getY(), z = (int) missile.getZ();
        var cell = new BlockPos.MutableBlockPos();
        var taint = ModBlocks.TAINT.get().defaultBlockState();
        for (int i = 0; i < 10 * reach; i++) {
            int cx = random.nextInt(reach) + x - shift;
            int cy = random.nextInt(reach) + y - shift;
            int cz = random.nextInt(reach) + z - shift;
            var state = level.getBlockState(cell.set(cx, cy, cz));
            if (state.isSolidRender() && !state.isSignalSource() && !state.isAir()) {
                level.setBlock(
                        cell,
                        taint.setValue(TaintBlock.AGE, 4 + random.nextInt(3)),
                        Block.UPDATE_CLIENTS);
            }
        }
    }

    private static void cloud(CustomMissile missile, float strength) {
        var level = missile.level();
        double x = missile.getX(), y = missile.getY(), z = missile.getZ();
        var motion = missile.getDeltaMovement();
        level.levelEvent(
                2002,
                new BlockPos((int) Math.round(x), (int) Math.round(y), (int) Math.round(z)),
                0);
        ExplosionChaos.spawnPoisonCloud(
                level, x - motion.x, y - motion.y, z - motion.z, 750, 2.5D, 2);
    }

    private static void turbine(CustomMissile missile, float strength) {
        var level = missile.level();
        double x = missile.getX(), y = missile.getY(), z = missile.getZ();
        var motion = missile.getDeltaMovement();
        CompositeExplosion.detonate(
                level, x, y, z, 10F, CompositeExplosion.CLOUD | CompositeExplosion.SHRAPNEL);
        int blades = (int) strength;
        float turn = (float) (2D * Math.PI / blades);
        var heading = new Vec3(0.5D, 0D, 0D);
        var random = level.getRandom();
        for (int i = 0; i < blades; i++) {
            var blade =
                    new BulletBaseMK4Entity(
                            level,
                            XFactoryWarhead.warhead_turbine,
                            XFactoryWarhead.TURBINE_BLADE_DAMAGE,
                            0F,
                            0F,
                            0F);
            blade.setPos(x - motion.x, y - motion.y + random.nextGaussian(), z - motion.z);
            blade.setFlightDirection(heading);
            level.addFreshEntity(blade);
            heading = heading.yRot(turn);
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putFloat("fuel", fuel);
        output.putFloat("consumption", consumption);
        output.store("warhead", ItemStack.OPTIONAL_CODEC, entityData.get(WARHEAD));
        output.store("fuselage", ItemStack.OPTIONAL_CODEC, entityData.get(FUSELAGE));
        output.store("fins", ItemStack.OPTIONAL_CODEC, entityData.get(FINS));
        output.store("thruster", ItemStack.OPTIONAL_CODEC, entityData.get(THRUSTER));
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        fuel = input.getFloatOr("fuel", fuel);
        consumption = input.getFloatOr("consumption", consumption);
        input.read("warhead", ItemStack.OPTIONAL_CODEC)
                .ifPresent(stack -> entityData.set(WARHEAD, stack));
        input.read("fuselage", ItemStack.OPTIONAL_CODEC)
                .ifPresent(stack -> entityData.set(FUSELAGE, stack));
        input.read("fins", ItemStack.OPTIONAL_CODEC)
                .ifPresent(stack -> entityData.set(FINS, stack));
        input.read("thruster", ItemStack.OPTIONAL_CODEC)
                .ifPresent(stack -> entityData.set(THRUSTER, stack));
    }
}
