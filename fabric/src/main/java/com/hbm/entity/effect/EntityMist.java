// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.effect;

import com.hbm.client.ClientEffects;
import com.hbm.entity.ModEntities;
import com.hbm.entity.mob.glyphid.EntityGlyphid;
import com.hbm.extprop.HbmLivingProps;
import com.hbm.handler.ArmorUtil;
import com.hbm.handler.radiation.RadiationSystemNT;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluidProperty;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.trait.*;
import com.hbm.inventory.fluid.trait.FluidTraitSimple.FT_Delicious;
import com.hbm.inventory.fluid.trait.FluidTraitSimple.FT_Liquid;
import com.hbm.lib.ModDamageTypes;
import com.hbm.util.ContaminationUtil.ContaminationType;
import com.hbm.util.ContaminationUtil.HazardType;
import com.hbm.util.ContaminationUtil;
import com.hbm.util.EntityDamageUtil;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

public class EntityMist extends Entity {

    private static final EntityDataAccessor<Integer> TYPE =
            SynchedEntityData.defineId(EntityMist.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> WIDTH =
            SynchedEntityData.defineId(EntityMist.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> HEIGHT =
            SynchedEntityData.defineId(EntityMist.class, EntityDataSerializers.FLOAT);

    public int maxAge = 150;

    public EntityMist(EntityType<? extends EntityMist> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public EntityMist(Level level) {
        this(ModEntities.MIST.get(), level);
    }

    private static DamageSource source(ServerLevel server, ResourceKey<DamageType> key) {
        Holder<DamageType> type =
                server.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(key);
        return new DamageSource(type);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    public EntityMist setArea(float width, float height) {
        this.entityData.set(WIDTH, width);
        this.entityData.set(HEIGHT, height);
        return this;
    }

    public EntityMist setDuration(int duration) {
        this.maxAge = duration;
        return this;
    }

    public EntityMist setType(Fluid fluid) {
        this.entityData.set(TYPE, BuiltInRegistries.FLUID.getId(fluid));
        return this;
    }

    public Fluid getFluid() {
        Fluid fluid = BuiltInRegistries.FLUID.byId(this.entityData.get(TYPE));
        return fluid == null ? Fluids.EMPTY : fluid;
    }

    public int getMaxAge() {
        return maxAge;
    }

    public float width() {
        return this.entityData.get(WIDTH);
    }

    public float height() {
        return this.entityData.get(HEIGHT);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(TYPE, 0);
        builder.define(WIDTH, 0F);
        builder.define(HEIGHT, 0F);
    }

    @Override
    public void tick() {

        float height = height();

        if (level() instanceof ServerLevel server) {

            if (this.tickCount >= maxAge) {
                this.discard();
                return;
            }

            Fluid type = getFluid();
            NTMFluidProperty prop = NTMFluidProperties.get(type);
            if (prop == null) return;

            if (prop.hasTrait(FT_VentRadiation.class)) {
                FT_VentRadiation trait = prop.getTrait(FT_VentRadiation.class);

                RadiationSystemNT.incrementRad(
                        server,
                        new BlockPos(Mth.floor(getX()), Mth.floor(getY()), Mth.floor(getZ())),
                        trait.getRadPerMB() * 2,
                        5120.0D);
            }

            double intensity = 1D - (double) this.tickCount / (double) maxAge;

            if (prop.hasTrait(FT_Flammable.class) && this.isOnFire()) {

                server.explode(
                        this,
                        getX(),
                        getY() + height / 2,
                        getZ(),
                        (float) intensity * 15F,
                        Level.ExplosionInteraction.BLOCK);
                this.discard();
                return;
            }

            double w = width();
            List<Entity> affected = level().getEntities(this, mistBox().move(-w / 2, 0, -w / 2));

            for (Entity e : affected) {
                affect(server, e, prop, intensity);
            }
        } else {
            spawnClientMist();
        }
    }

    private void affect(ServerLevel server, Entity e, NTMFluidProperty prop, double intensity) {

        LivingEntity living = e instanceof LivingEntity ? (LivingEntity) e : null;

        int temperature = prop.temperature();

        if (temperature >= 100) {
            EntityDamageUtil.attackEntityFromIgnoreIFrame(
                    e, source(server, ModDamageTypes.BOIL), 0.2F + (temperature - 100) * 0.02F);

            if (temperature >= 500) {
                e.igniteForSeconds(10.0F);
            }
        }

        if (temperature < -20) {

            if (living != null) {
                EntityDamageUtil.attackEntityFromIgnoreIFrame(
                        e, source(server, ModDamageTypes.ICE), 0.2F + (temperature + 20) * -0.05F);
                living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 100, 2));
                living.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 100, 4));
            }
        }

        if (prop.hasTrait(FT_Delicious.class) && living != null && living.isAlive()) {
            living.heal(2F * (float) intensity);
        }

        if (prop.hasTrait(FT_Flammable.class) && prop.hasTrait(FT_Liquid.class) && living != null) {
            HbmLivingProps.getData(living).oil = 200;
        }

        if (isExtinguishing(prop)) {
            e.clearFire();
        }

        if (prop.hasTrait(FT_Corrosive.class)) {
            FT_Corrosive trait = prop.getTrait(FT_Corrosive.class);
            if (living != null) {
                EntityDamageUtil.attackEntityFromNT(
                        living,
                        source(server, ModDamageTypes.ACID),
                        trait.getRating() / 60F,
                        true,
                        false,
                        0,
                        0,
                        0);
                ArmorUtil.damageWholeSuit(living, trait.getRating() / 50);
            }
        }

        if (prop.hasTrait(FT_VentRadiation.class)) {
            FT_VentRadiation trait = prop.getTrait(FT_VentRadiation.class);
            if (living != null) {
                ContaminationUtil.contaminate(
                        living,
                        HazardType.RADIATION,
                        ContaminationType.CREATIVE,
                        trait.getRadPerMB() * 5);
            }
        }

        if (prop.hasTrait(FT_Poison.class)) {
            FT_Poison trait = prop.getTrait(FT_Poison.class);
            if (living != null) trait.affect(living, intensity);
        }

        if (prop.hasTrait(FT_Toxin.class)) {
            FT_Toxin trait = prop.getTrait(FT_Toxin.class);
            if (living != null) trait.affect(living, intensity);
        }

        if (getFluid() == NTMFluids.ENDERJUICE && living != null) teleportRandomly(server, living);

        if (prop.hasTrait(FT_Pheromone.class)) {
            FT_Pheromone trait = prop.getTrait(FT_Pheromone.class);

            if (living != null) {
                if ((living instanceof EntityGlyphid && trait.getType() == 1)
                        || (living instanceof Player && trait.getType() == 2)) {
                    int mult = trait.getType();

                    living.addEffect(new MobEffectInstance(MobEffects.SPEED, mult * 60 * 20, 1));
                    living.addEffect(new MobEffectInstance(MobEffects.HASTE, mult * 60 * 20, 1));
                    living.addEffect(
                            new MobEffectInstance(MobEffects.REGENERATION, mult * 2 * 20, 0));
                    living.addEffect(
                            new MobEffectInstance(MobEffects.RESISTANCE, mult * 60 * 20, 0));
                    living.addEffect(new MobEffectInstance(MobEffects.STRENGTH, mult * 60 * 20, 1));
                    living.addEffect(
                            new MobEffectInstance(MobEffects.FIRE_RESISTANCE, mult * 60 * 20, 0));
                }
            }
        }
    }

    private void teleportRandomly(ServerLevel server, LivingEntity living) {
        double x = getX() + (this.random.nextDouble() - 0.5D) * 64.0D;
        double y = getY() + (this.random.nextInt(64) - 32);
        double z = getZ() + (this.random.nextDouble() - 0.5D) * 64.0D;
        double fromX = living.getX(), fromY = living.getY(), fromZ = living.getZ();

        if (!living.randomTeleport(x, y, z, true)) return;

        server.playSound(
                null,
                fromX,
                fromY,
                fromZ,
                SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.HOSTILE,
                1F,
                1F);
        server.playSound(
                null,
                living.getX(),
                living.getY(),
                living.getZ(),
                SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.HOSTILE,
                1F,
                1F);
    }

    private boolean isExtinguishing(NTMFluidProperty prop) {
        return prop.temperature() < 50 && !prop.hasTrait(FT_Flammable.class);
    }

    private AABB mistBox() {
        double w = width();
        return new AABB(
                getX() - 0.3D,
                getY(),
                getZ() - 0.3D,
                getX() - 0.3D + w,
                getY() + height(),
                getZ() - 0.3D + w);
    }

    private void spawnClientMist() {
        AABB bb = mistBox();
        NTMFluidProperty prop = NTMFluidProperties.get(getFluid());
        int color = prop != null ? prop.color() : 0xFFFFFF;

        for (int i = 0; i < 2; i++) {
            double x = bb.minX + (random.nextDouble() - 0.5D) * (bb.maxX - bb.minX);
            double y = bb.minY + random.nextDouble() * (bb.maxY - bb.minY);
            double z = bb.minZ + (random.nextDouble() - 0.5D) * (bb.maxZ - bb.minZ);
            ClientEffects.mistTower(level(), x, y, z, color);
        }
    }

    @Override
    public boolean displayFireAnimation() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        this.entityData.set(TYPE, input.getIntOr("type", 0));
        setArea(input.getFloatOr("width", 0F), input.getFloatOr("height", 0F));
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putInt("type", this.entityData.get(TYPE));
        output.putFloat("width", width());
        output.putFloat("height", height());
    }
}
