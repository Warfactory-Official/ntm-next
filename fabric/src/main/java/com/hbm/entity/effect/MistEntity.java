/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) Contributors to Hbm's Nuclear Tech Mod. All rights reserved.
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

package com.hbm.entity.effect;

import com.hbm.client.ClientEffects;
import com.hbm.entity.FluidContact;
import com.hbm.entity.ModEntities;
import com.hbm.entity.mob.glyphid.GlyphidEntity;
import com.hbm.handler.radiation.RadiationSystemNT;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluidProperty;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.trait.CorrosiveTrait;
import com.hbm.inventory.fluid.trait.FlammableTrait;
import com.hbm.inventory.fluid.trait.FluidTraitSimple.FT_Delicious;
import com.hbm.inventory.fluid.trait.FluidTraitSimple.FT_Liquid;
import com.hbm.inventory.fluid.trait.PheromoneTrait;
import com.hbm.inventory.fluid.trait.PoisonTrait;
import com.hbm.inventory.fluid.trait.RadiationReleaseTrait;
import com.hbm.inventory.fluid.trait.ToxinTrait;
import com.hbm.lib.ModDamageTypes;
import com.hbm.lib.ModifiedDamageSource;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class MistEntity extends Entity {

    private static final EntityDataAccessor<Integer> TYPE =
            SynchedEntityData.defineId(MistEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> WIDTH =
            SynchedEntityData.defineId(MistEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> HEIGHT =
            SynchedEntityData.defineId(MistEntity.class, EntityDataSerializers.FLOAT);
    private static final double CORNER = 0.3D;
    private static final int PUFFS_PER_TICK = 2;
    private static final Cloud INERT =
            new Cloud(false, null, FluidContact.<MistEntity>builder().build());

    public int maxAge = 150;
    private final FluidContact.Memo<Cloud> memo = new FluidContact.Memo<>(this::condense);

    public MistEntity(EntityType<? extends MistEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public MistEntity(Level level) {
        this(ModEntities.MIST.get(), level);
    }

    public MistEntity setArea(float width, float height) {
        entityData.set(WIDTH, width);
        entityData.set(HEIGHT, height);
        return this;
    }

    public MistEntity setDuration(int duration) {
        maxAge = duration;
        return this;
    }

    public MistEntity setType(Fluid fluid) {
        entityData.set(TYPE, BuiltInRegistries.FLUID.getId(fluid));
        return this;
    }

    public Fluid getFluid() {
        return BuiltInRegistries.FLUID.byId(entityData.get(TYPE));
    }

    public int getMaxAge() {
        return maxAge;
    }

    public float width() {
        return entityData.get(WIDTH);
    }

    public float height() {
        return entityData.get(HEIGHT);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(TYPE, 0);
        builder.define(WIDTH, 0F);
        builder.define(HEIGHT, 0F);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        if (WIDTH.equals(accessor) || HEIGHT.equals(accessor)) refreshDimensions();
        super.onSyncedDataUpdated(accessor);
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return EntityDimensions.fixed(width(), height());
    }

    @Override
    protected AABB makeBoundingBox(Vec3 position) {
        var minX = position.x - CORNER;
        var minZ = position.z - CORNER;
        return new AABB(
                minX,
                position.y,
                minZ,
                minX + getBbWidth(),
                position.y + getBbHeight(),
                minZ + getBbWidth());
    }

    @Override
    public PushReaction getPistonPushReaction() {
        return ((PushReaction) (PushReaction.IGNORE_ENTITY));
    }

    @Override
    public void tick() {
        if (level() instanceof ServerLevel server) linger(server);
        else puff();
    }

    private void linger(ServerLevel server) {
        if (tickCount >= maxAge) discard();

        var cloud = memo.get(getFluid());
        if (cloud.vent() != null) {
            RadiationSystemNT.incrementRad(
                    server, blockPosition(), cloud.vent().perMb() * 2, 5120.0D);
        }

        var intensity = 1D - (double) tickCount / (double) maxAge;
        if (cloud.flammable() && isOnFire()) {
            server.explode(
                    this,
                    getX(),
                    getY() + getBbHeight() / 2,
                    getZ(),
                    (float) intensity * 15F,
                    Level.ExplosionInteraction.BLOCK);
            discard();
            return;
        }

        var reach = getBoundingBox().move(-getBbWidth() / 2, 0, -getBbWidth() / 2);
        for (var target : server.getEntities(this, reach))
            cloud.contact().touch(this, server, target, intensity);
    }

    private void puff() {
        var box = getBoundingBox();
        var property = NTMFluidProperties.get(getFluid());
        var color = property != null ? property.color() : 0xFFFFFF;
        for (var i = 0; i < PUFFS_PER_TICK; i++) {
            var x = box.minX + (random.nextDouble() - 0.5D) * box.getXsize();
            var y = box.minY + random.nextDouble() * box.getYsize();
            var z = box.minZ + (random.nextDouble() - 0.5D) * box.getZsize();
            ClientEffects.mistTower(level(), x, y, z, color);
        }
    }

    private Cloud condense(Fluid fluid, @Nullable NTMFluidProperty property) {
        if (property == null) return INERT;
        var flammable = property.hasTrait(FlammableTrait.class);
        var vent = property.<RadiationReleaseTrait>getTrait(RadiationReleaseTrait.class);
        var damage = level().damageSources();
        var celsius = property.temperature();
        var contact = FluidContact.<MistEntity>builder();

        if (celsius >= 100) {
            var scald = ModifiedDamageSource.bypassingCooldown(damage.source(ModDamageTypes.BOIL));
            contact.any(
                    FluidContact.hurt(
                            _ -> scald, 0.2F + (celsius - 100) * 0.02F));
            if (celsius >= 500) contact.any(FluidContact.ignite(10F));
        }
        if (celsius < -20) {
            var frost = ModifiedDamageSource.bypassingCooldown(damage.source(ModDamageTypes.ICE));
            contact.living(
                    FluidContact.hurtLiving(
                            _ -> frost, 0.2F + (celsius + 20) * -0.05F));
            contact.living(
                    FluidContact.effects(
                            LivingEntity.class,
                            new MobEffectInstance(MobEffects.SLOWNESS, 100, 2),
                            new MobEffectInstance(MobEffects.MINING_FATIGUE, 100, 4)));
        }
        if (property.hasTrait(FT_Delicious.class)) contact.living(FluidContact.heal());
        if (flammable && property.hasTrait(FT_Liquid.class)) contact.living(FluidContact.oil(200));
        if (celsius < 50 && !flammable) contact.any(FluidContact.extinguish());

        var corrosive = property.<CorrosiveTrait>getTrait(CorrosiveTrait.class);
        if (corrosive != null) {
            var acid = ModifiedDamageSource.of(damage.source(ModDamageTypes.ACID), true, 0, 0, 0);
            contact.living(
                    FluidContact.corrode(
                            _ -> acid,
                            corrosive.rating() / 60F,
                            corrosive.rating() / 50));
        }
        if (vent != null) contact.living(FluidContact.irradiate(vent.perMb() * 5));

        var poison = property.<PoisonTrait>getTrait(PoisonTrait.class);
        if (poison != null) contact.living(FluidContact.poison(poison));
        var toxin = property.<ToxinTrait>getTrait(ToxinTrait.class);
        if (toxin != null) contact.living(FluidContact.toxin(toxin));

        if (fluid == NTMFluids.ENDERJUICE) {
            contact.living(
                    (source, level, living, _) ->
                            FluidContact.scatter(level, source, living));
        }

        var pheromone = property.<PheromoneTrait>getTrait(PheromoneTrait.class);
        if (pheromone != null) {
            var kind = pheromone.kind();
            if (kind == 1) contact.living(FluidContact.effects(GlyphidEntity.class, rally(kind)));
            if (kind == 2) contact.living(FluidContact.effects(Player.class, rally(kind)));
        }

        return new Cloud(flammable, vent, contact.build());
    }

    private static MobEffectInstance[] rally(int strength) {
        var minutes = strength * 60 * 20;
        return new MobEffectInstance[] {
            new MobEffectInstance(MobEffects.SPEED, minutes, 1),
            new MobEffectInstance(MobEffects.HASTE, minutes, 1),
            new MobEffectInstance(MobEffects.REGENERATION, strength * 2 * 20, 0),
            new MobEffectInstance(MobEffects.RESISTANCE, minutes, 0),
            new MobEffectInstance(MobEffects.STRENGTH, minutes, 1),
            new MobEffectInstance(MobEffects.FIRE_RESISTANCE, minutes, 0)
        };
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean displayFireAnimation() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        entityData.set(TYPE, input.getIntOr("type", 0));
        setArea(input.getFloatOr("width", 0F), input.getFloatOr("height", 0F));
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putInt("type", entityData.get(TYPE));
        output.putFloat("width", width());
        output.putFloat("height", height());
    }

    private record Cloud(
            boolean flammable,
            @Nullable RadiationReleaseTrait vent,
            FluidContact<MistEntity> contact) {}
}
