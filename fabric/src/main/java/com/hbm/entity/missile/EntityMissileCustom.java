// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.missile;

import com.hbm.api.entity.IRadarDetectableNT;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.bomb.BlockTaint;
import com.hbm.client.ClientEffects;
import com.hbm.entity.ModEntities;
import com.hbm.entity.effect.EntityNukeTorex;
import com.hbm.entity.logic.EntityBalefire;
import com.hbm.entity.logic.EntityNukeExplosionMK5;
import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.explosion.ExplosionChaos;
import com.hbm.explosion.ExplosionLarge;
import com.hbm.handler.MissileStruct;
import com.hbm.items.ModItems;
import com.hbm.items.weapon.ItemCustomMissilePart.FuelType;
import com.hbm.items.weapon.ItemCustomMissilePart.PartSize;
import com.hbm.items.weapon.ItemCustomMissilePart.WarheadType;
import com.hbm.items.weapon.ItemCustomMissilePart;
import com.hbm.items.weapon.sedna.factory.XFactoryWarhead;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class EntityMissileCustom extends EntityMissileBaseNT {

    private static final EntityDataAccessor<ItemStack> WARHEAD =
            SynchedEntityData.defineId(EntityMissileCustom.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<ItemStack> FUSELAGE =
            SynchedEntityData.defineId(EntityMissileCustom.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<ItemStack> FINS =
            SynchedEntityData.defineId(EntityMissileCustom.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<ItemStack> THRUSTER =
            SynchedEntityData.defineId(EntityMissileCustom.class, EntityDataSerializers.ITEM_STACK);

    public float fuel;
    public float consumption;

    public EntityMissileCustom(EntityType<? extends EntityMissileCustom> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(WARHEAD, ItemStack.EMPTY);
        builder.define(FUSELAGE, ItemStack.EMPTY);
        builder.define(FINS, ItemStack.EMPTY);
        builder.define(THRUSTER, ItemStack.EMPTY);
    }

    public void launch(
            double x,
            double y,
            double z,
            int targetX,
            int targetZ,
            ItemStack warhead,
            ItemStack fuselage,
            ItemStack fins,
            ItemStack thruster) {
        launch(x, y, z, targetX, targetZ);

        setParts(warhead, fuselage, fins, thruster);

        ItemCustomMissilePart fuselagePart = getPart(FUSELAGE);
        ItemCustomMissilePart thrusterPart = getPart(THRUSTER);
        this.fuel = fuselagePart != null ? fuselagePart.fuelAmount() : 0F;
        this.consumption = thrusterPart != null ? thrusterPart.consumption() : 0F;
    }

    public void setParts(
            ItemStack warhead, ItemStack fuselage, ItemStack fins, ItemStack thruster) {
        entityData.set(WARHEAD, warhead == null ? ItemStack.EMPTY : warhead);
        entityData.set(FUSELAGE, fuselage == null ? ItemStack.EMPTY : fuselage);
        entityData.set(FINS, fins == null ? ItemStack.EMPTY : fins);
        entityData.set(THRUSTER, thruster == null ? ItemStack.EMPTY : thruster);
    }

    private ItemCustomMissilePart getPart(EntityDataAccessor<ItemStack> slot) {
        return ItemCustomMissilePart.part(entityData.get(slot));
    }

    public MissileStruct getStruct() {
        return new MissileStruct(
                getPart(WARHEAD), getPart(FUSELAGE), getPart(FINS), getPart(THRUSTER));
    }

    public WarheadType getWarheadType() {
        ItemCustomMissilePart part = getPart(WARHEAD);
        return part != null ? part.warheadType() : null;
    }

    public float getWarheadStrength() {
        ItemCustomMissilePart part = getPart(WARHEAD);
        return part != null ? part.warheadStrength() : 0F;
    }

    @Override
    public boolean hasPropulsion() {
        return this.fuel > 0;
    }

    @Override
    public void tick() {

        WarheadType warhead = getWarheadType();
        if (warhead != null && warhead.updateCustom != null) {
            warhead.updateCustom.accept(this);
        }

        if (!level().isClientSide()) {
            if (this.hasPropulsion()) this.fuel -= this.consumption;
        }

        super.tick();
    }

    @Override
    protected void spawnContrail(Vec3 step) {

        Vec3 v = step.lengthSqr() < 1.0e-8 ? new Vec3(0, -1, 0) : step.normalize();

        ItemCustomMissilePart fuselage = getPart(FUSELAGE);
        FuelType fuel = fuselage != null ? fuselage.fuelType() : null;

        if (fuel == null) return;

        ClientEffects.Contrail contrail =
                switch (fuel) {
                    case KEROSENE -> ClientEffects.Contrail.KEROSENE;
                    case SOLID -> ClientEffects.Contrail.SOLID;
                    case HYDROGEN -> ClientEffects.Contrail.HYDROGEN;
                    case BALEFIRE -> ClientEffects.Contrail.BALEFIRE;
                    case XENON -> null;
                };
        if (contrail == null) return;

        double len = Math.max(step.length(), 1D);
        int count = (int) Math.min(len, 10);
        for (int i = 0; i < count; i++) {
            ClientEffects.spawnContrail(
                    level(), getX() - v.x * i, getY() - v.y * i, getZ() - v.z * i, contrail);
        }
    }

    @Override
    public void onMissileImpact(BlockHitResult mop) {

        WarheadType type = getWarheadType();
        float strength = getWarheadStrength();

        if (type == null) return;

        if (type.impactCustom != null) {
            type.impactCustom.accept(this);
            return;
        }

        switch (type) {
            case HE:
                ExplosionLarge.explode(
                        level(), getX(), getY(), getZ(), strength, true, false, true);
                ExplosionLarge.jolt(
                        level(),
                        getX(),
                        getY(),
                        getZ(),
                        (int) strength,
                        (int) (strength * 50),
                        0.25);
                break;
            case INC:
                ExplosionLarge.explodeFire(
                        level(), getX(), getY(), getZ(), strength, true, false, true);
                ExplosionLarge.jolt(
                        level(),
                        getX(),
                        getY(),
                        getZ(),
                        (int) (strength * 1.5F),
                        (int) (strength * 50),
                        0.25);
                break;
            case CLUSTER:
                break;
            case BUSTER:
                ExplosionLarge.buster(
                        level(),
                        getX(),
                        getY(),
                        getZ(),
                        new Vec3(getDeltaMovement().x, getDeltaMovement().y, getDeltaMovement().z),
                        strength,
                        (int) (strength * 4));
                break;
            case NUCLEAR:
            case TX:
                level().addFreshEntity(
                                EntityNukeExplosionMK5.statFac(
                                        level(), (int) strength, getX(), getY(), getZ()));
                EntityNukeTorex.statFac(level(), getX(), getY(), getZ(), strength);
                break;
            case BALEFIRE:
                EntityBalefire bf = new EntityBalefire(ModEntities.BALEFIRE.get(), level());
                bf.setPos(getX(), getY(), getZ());
                bf.destructionRange = (int) strength;
                level().addFreshEntity(bf);
                EntityNukeTorex.statFacBale(level(), getX(), getY(), getZ(), strength);
                break;
            case N2:
                level().addFreshEntity(
                                EntityNukeExplosionMK5.statFacNoRad(
                                        level(), (int) strength, getX(), getY(), getZ()));
                EntityNukeTorex.statFac(level(), getX(), getY(), getZ(), strength);
                break;
            case TAINT:
                int r = (int) strength;

                for (int i = 0; i < r * 10; i++) {
                    BlockPos pos =
                            new BlockPos(
                                    random.nextInt(r) + (int) getX() - (r / 2 - 1),
                                    random.nextInt(r) + (int) getY() - (r / 2 - 1),
                                    random.nextInt(r) + (int) getZ() - (r / 2 - 1));
                    BlockState hit = level().getBlockState(pos);
                    if (!hit.isSolidRender() || hit.isAir()) continue;
                    level().setBlock(
                                    pos,
                                    ModBlocks.TAINT
                                            .get()
                                            .defaultBlockState()
                                            .setValue(BlockTaint.AGE, random.nextInt(3) + 4),
                                    Block.UPDATE_CLIENTS);
                }
                break;
            case CLOUD:
                level().levelEvent(2002, BlockPos.containing(getX(), getY(), getZ()), 0);
                ExplosionChaos.spawnPoisonCloud(
                        level(),
                        getX() - getDeltaMovement().x,
                        getY() - getDeltaMovement().y,
                        getZ() - getDeltaMovement().z,
                        750,
                        2.5,
                        2);
                break;
            case TURBINE:
                ExplosionLarge.explode(level(), getX(), getY(), getZ(), 10, true, false, true);
                int count = (int) strength;

                Vec3 fan = new Vec3(0.5D, 0D, 0D);
                for (int i = 0; i < count; i++) {
                    level().addFreshEntity(
                                    new EntityBulletBaseMK4(
                                            level(),
                                            null,
                                            XFactoryWarhead.warhead_turbine,
                                            XFactoryWarhead.TURBINE_BLADE_DAMAGE,
                                            0F,
                                            getX() - getDeltaMovement().x,
                                            getY() - getDeltaMovement().y + random.nextGaussian(),
                                            getZ() - getDeltaMovement().z,
                                            fan.x,
                                            0D,
                                            fan.z));
                    fan = fan.yRot((float) (Math.PI * 2F / count));
                }
                break;
            default:
                break;
        }
    }

    @Override
    public List<ItemStack> getDebris() {
        return new ArrayList<>();
    }

    @Override
    public ItemStack getDebrisRareDrop() {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack getMissileItemForInfo() {
        return new ItemStack(ModItems.MISSILE_CUSTOM);
    }

    @Override
    public String getRadarName() {
        ItemCustomMissilePart fuselage = getPart(FUSELAGE);
        if (fuselage == null) return "radar.target.custom";
        if (fuselage.top == PartSize.SIZE_10 && fuselage.bottom == PartSize.SIZE_10)
            return "radar.target.custom10";
        if (fuselage.top == PartSize.SIZE_10 && fuselage.bottom == PartSize.SIZE_15)
            return "radar.target.custom1015";
        if (fuselage.top == PartSize.SIZE_15 && fuselage.bottom == PartSize.SIZE_15)
            return "radar.target.custom15";
        if (fuselage.top == PartSize.SIZE_15 && fuselage.bottom == PartSize.SIZE_20)
            return "radar.target.custom1520";
        if (fuselage.top == PartSize.SIZE_20 && fuselage.bottom == PartSize.SIZE_20)
            return "radar.target.custom20";
        return "radar.target.custom";
    }

    @Override
    public int getBlipLevel() {
        ItemCustomMissilePart fuselage = getPart(FUSELAGE);
        if (fuselage == null) return IRadarDetectableNT.TIER1;
        if (fuselage.top == PartSize.SIZE_10 && fuselage.bottom == PartSize.SIZE_10)
            return IRadarDetectableNT.TIER10;
        if (fuselage.top == PartSize.SIZE_10 && fuselage.bottom == PartSize.SIZE_15)
            return IRadarDetectableNT.TIER10_15;
        if (fuselage.top == PartSize.SIZE_15 && fuselage.bottom == PartSize.SIZE_15)
            return IRadarDetectableNT.TIER15;
        if (fuselage.top == PartSize.SIZE_15 && fuselage.bottom == PartSize.SIZE_20)
            return IRadarDetectableNT.TIER15_20;
        if (fuselage.top == PartSize.SIZE_20 && fuselage.bottom == PartSize.SIZE_20)
            return IRadarDetectableNT.TIER20;
        return IRadarDetectableNT.TIER1;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        fuel = input.getFloatOr("fuel", fuel);
        consumption = input.getFloatOr("consumption", consumption);
        input.read("warhead", ItemStack.OPTIONAL_CODEC).ifPresent(s -> entityData.set(WARHEAD, s));
        input.read("fuselage", ItemStack.OPTIONAL_CODEC)
                .ifPresent(s -> entityData.set(FUSELAGE, s));
        input.read("fins", ItemStack.OPTIONAL_CODEC).ifPresent(s -> entityData.set(FINS, s));
        input.read("thruster", ItemStack.OPTIONAL_CODEC)
                .ifPresent(s -> entityData.set(THRUSTER, s));
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
}
