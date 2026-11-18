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

package com.hbm.entity.projectile;

import com.hbm.api.environment.Environment;
import com.hbm.blocks.ModBlocks;
import com.hbm.capability.NtmContracts;
import com.hbm.client.ClientEffects;
import com.hbm.client.ClientPlayerAccess;
import com.hbm.entity.FluidContact;
import com.hbm.entity.mob.glyphid.GlyphidEntity;
import com.hbm.entity.projectile.ProjectileEntity;
import com.hbm.handler.radiation.RadiationSystemNT;
import com.hbm.interfaces.Extinguishable;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluidProperty;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.trait.*;
import com.hbm.lib.ModDamageTypes;
import com.hbm.lib.ModifiedDamageSource;
import com.hbm.particle.helper.FlameCreator;
import com.hbm.util.ContaminationUtil.ContaminationType;
import com.hbm.util.ContaminationUtil.HazardType;
import com.hbm.util.ContaminationUtil;
import com.hbm.util.RegistryUtil;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class ChemicalEntity extends ProjectileEntity {

    private static final EntityDataAccessor<Integer> DATA_FLUID =
            SynchedEntityData.defineId(ChemicalEntity.class, EntityDataSerializers.INT);

    private final FluidContact.Memo<ChemicalStyle> styleMemo =
            new FluidContact.Memo<>((fluid, _) -> getStyleFromType(fluid));
    private final FluidContact.Memo<FluidContact<ChemicalEntity>> contactMemo =
            new FluidContact.Memo<>(this::contactOf);

    public ChemicalEntity(EntityType<? extends ChemicalEntity> type, Level level) {
        super(type, level);
    }

    public ChemicalEntity(
            EntityType<? extends ChemicalEntity> type,
            Level level,
            LivingEntity thrower,
            double sideOffset,
            double heightOffset,
            double frontOffset) {
        super(type, level);
        initThrower(thrower);
    }

    private static int colorOf(Fluid fluid) {
        NTMFluidProperty p = NTMFluidProperties.get(fluid);
        return p == null ? 0xFFFFFF : p.color();
    }

    private static int temperatureOf(Fluid fluid) {
        NTMFluidProperty p = NTMFluidProperties.get(fluid);
        return p == null ? 20 : p.temperature();
    }

    private static Extinguishable.@Nullable Agent extinguishTypeOf(Fluid type) {
        if (type == NTMFluids.CARBONDIOXIDE) return Extinguishable.Agent.CO2;
        if (type == NTMFluids.WATER || type == NTMFluids.HEAVYWATER || type == NTMFluids.COOLANT)
            return Extinguishable.Agent.WATER;
        return null;
    }

    private static Block block(String name) {
        return BuiltInRegistries.BLOCK.getOptional(RegistryUtil.id(name)).orElseThrow();
    }

    private static Block brickConcrete() {
        return block("brick_concrete");
    }

    private static Block brickConcreteMossy() {
        return block("brick_concrete_mossy");
    }

    public static ChemicalStyle getStyleFromType(Fluid type) {

        if (type == NTMFluids.IONGEL) {
            return ChemicalStyle.LIGHTNING;
        }

        if (NTMFluidProperties.hasTrait(type, FluidTraitSimple.FT_Amat.class)) {
            return ChemicalStyle.AMAT;
        }

        if (NTMFluidProperties.hasTrait(type, FluidTraitSimple.FT_Gaseous.class)
                || NTMFluidProperties.hasTrait(type, FluidTraitSimple.FT_Gaseous_ART.class)) {

            if (NTMFluidProperties.hasTrait(type, FlammableTrait.class)
                    || NTMFluidProperties.hasTrait(type, CombustibleTrait.class)) {
                return ChemicalStyle.GASFLAME;
            } else {
                return ChemicalStyle.GAS;
            }
        }

        if (NTMFluidProperties.hasTrait(type, FluidTraitSimple.FT_Liquid.class)) {

            if (NTMFluidProperties.hasTrait(type, CombustibleTrait.class)) {
                return ChemicalStyle.BURNING;
            } else {
                return ChemicalStyle.LIQUID;
            }
        }

        return ChemicalStyle.NULL;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_FLUID, 0);
    }

    public ChemicalEntity setFluid(Fluid fluid) {
        this.entityData.set(DATA_FLUID, BuiltInRegistries.FLUID.getId(fluid));
        return this;
    }

    public Fluid getChemType() {
        return BuiltInRegistries.FLUID.byId(this.entityData.get(DATA_FLUID));
    }

    @Override
    public void tick() {

        if (!level().isClientSide()) {

            if (this.tickCount > this.getMaxAge()) {
                discard();
                return;
            }

            Fluid type = this.getChemType();

            if (NTMFluidProperties.hasTrait(type, FluidTraitSimple.FT_Gaseous.class)
                    || NTMFluidProperties.hasTrait(type, FluidTraitSimple.FT_Gaseous_ART.class)) {

                double intensity = 1D - (double) this.tickCount / (double) this.getMaxAge();
                List<Entity> affected =
                        level().getEntities(
                                        this.getThrower(),
                                        this.getBoundingBox()
                                                .inflate(
                                                        intensity * 2.5,
                                                        intensity * 2.5,
                                                        intensity * 2.5));

                for (Entity e : affected) {
                    this.affect(e, intensity);
                }
            }

        } else {

            Fluid type = getChemType();
            ChemicalStyle style = getStyle();

            if (type == NTMFluids.BALEFIRE_FUEL) {

                if (nearClientPlayer())
                    FlameCreator.composeEffectClient(
                            level(), getX(), getY() - 0.125, getZ(), FlameCreator.META_BALEFIRE);

            } else if (style == ChemicalStyle.LIQUID) {

                int color = colorOf(type);
                Vec3 motion = getDeltaMovement();
                ClientEffects.spawnColorDust(
                        level(),
                        getX(),
                        getY(),
                        getZ(),
                        motion.x + random.nextGaussian() * 0.05,
                        motion.y - 0.2 + random.nextGaussian() * 0.05,
                        motion.z + random.nextGaussian() * 0.05,
                        ((color >> 16) & 0xFF) / 255F,
                        ((color >> 8) & 0xFF) / 255F,
                        (color & 0xFF) / 255F);

            } else if (style == ChemicalStyle.BURNING) {

                if (nearClientPlayer())
                    FlameCreator.composeEffectClient(
                            level(), getX(), getY() - 0.125, getZ(), FlameCreator.META_FIRE);
            }
        }
        super.tick();
    }

    private boolean nearClientPlayer() {
        Player player = ClientPlayerAccess.player();
        return player != null && player.getEyePosition().distanceTo(position()) < 100;
    }

    protected void affect(Entity e, double intensity) {
        var style = getStyle();
        var reach =
                style == ChemicalStyle.LIQUID || style == ChemicalStyle.BURNING ? 1D : intensity;
        contactMemo.get(getChemType()).touch(this, (ServerLevel) level(), e, reach);
    }

    private FluidContact<ChemicalEntity> contactOf(
            Fluid fluid, @Nullable NTMFluidProperty property) {
        var style = getStyleFromType(fluid);
        var types = level().registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE);
        var celsius = property != null ? property.temperature() : 20;
        var flammable =
                property != null ? property.<FlammableTrait>getTrait(FlammableTrait.class) : null;
        var fuel =
                property != null
                        ? property.<CombustibleTrait>getTrait(CombustibleTrait.class)
                        : null;
        var delicious = property != null && property.hasTrait(FluidTraitSimple.FT_Delicious.class);
        var contact = FluidContact.<ChemicalEntity>builder();

        if (style == ChemicalStyle.AMAT) {
            var gamma =
                    ModifiedDamageSource.bypassingCooldown(
                            level().damageSources().source(ModDamageTypes.RADIATION));
            contact.exclusive(
                    FluidContact.hurt(_ -> gamma, 1F),
                    (_, _, living, intensity) ->
                            ContaminationUtil.contaminate(
                                    living,
                                    HazardType.RADIATION,
                                    ContaminationType.CREATIVE,
                                    50F * (float) intensity));
        }
        if (style == ChemicalStyle.LIGHTNING) {
            var shock =
                    ModifiedDamageSource.bypassingCooldown(
                            level().damageSources().source(ModDamageTypes.ELECTRICITY));
            contact.exclusive(
                    FluidContact.hurt(_ -> shock, 0.5F),
                    FluidContact.effects(
                            LivingEntity.class,
                            new MobEffectInstance(MobEffects.SLOWNESS, 60, 9),
                            new MobEffectInstance(MobEffects.WEAKNESS, 60, 9)));
        }

        if (celsius >= 100) {
            var scald = types.getOrThrow(ModDamageTypes.BOIL);
            contact.any(
                    FluidContact.hurt(
                            chemical -> chemical.thrown(scald),
                            Math.min(0.25F + (celsius - 100) * 0.001F, 15F)));
            if (celsius >= 500) contact.any(FluidContact.ignite(10));
        }
        if (style == ChemicalStyle.LIQUID || style == ChemicalStyle.GAS) {
            if (celsius < -20) {
                var chill = types.getOrThrow(ModDamageTypes.BOIL);
                contact.living(
                        FluidContact.hurtLiving(
                                chemical -> chemical.thrown(chill),
                                Math.min(0.25F + (-celsius) * 0.01F, 2F)));
            }
            if (delicious) contact.living(FluidContact.heal());
        }
        if (style == ChemicalStyle.LIQUID) {
            if (flammable != null) contact.living(FluidContact.oil(300));
            if (delicious) contact.living(FluidContact.heal());
            if (celsius < 50 && flammable == null) contact.any(FluidContact.extinguish());
        }

        if (style == ChemicalStyle.BURNING) {
            var flame = types.getOrThrow(ModDamageTypes.FLAMETHROWER);
            var bonus = fuel != null ? Math.min(fuel.energyPerBucket() / 100_000F, 15F) : 0F;
            contact.any(FluidContact.hurt(chemical -> chemical.thrown(flame), 0.2F + bonus));
            contact.any(FluidContact.ignite(5));
        }
        if (style == ChemicalStyle.GASFLAME) {
            var flame = types.getOrThrow(ModDamageTypes.FLAMETHROWER);
            var heat =
                    Math.max(
                            flammable != null ? flammable.heatPerBucket() / 50_000F : 0F,
                            fuel != null ? Math.min(fuel.energyPerBucket() / 100_000F, 15F) : 0F);
            contact.any(
                    (chemical, level, target, intensity) -> {
                        var scaled = (float) (heat * intensity);
                        target.hurtServer(
                                level, chemical.thrown(flame), (0.2F + scaled) * (float) intensity);
                        target.igniteForSeconds((int) Math.ceil(5 * intensity));
                    });
        }

        if (property != null) {
            var corrosive = property.<CorrosiveTrait>getTrait(CorrosiveTrait.class);
            if (corrosive != null) {
                var acid = types.getOrThrow(ModDamageTypes.ACID_PLAYER);
                contact.living(
                        FluidContact.corrode(
                                chemical -> chemical.thrown(acid),
                                corrosive.rating() / 50F,
                                corrosive.rating() / 40));
            }
            var vent = property.<RadiationReleaseTrait>getTrait(RadiationReleaseTrait.class);
            if (vent != null) {
                var amount = vent.perMb() * 5;
                contact.living(FluidContact.irradiate(amount));
                contact.any(
                        (_, level, target, _) ->
                                RadiationSystemNT.incrementRad(
                                        level,
                                        target.blockPosition(),
                                        amount,
                                        amount * 1024D + 1D));
            }
            var poison = property.<PoisonTrait>getTrait(PoisonTrait.class);
            if (poison != null) contact.living(FluidContact.poison(poison));
            var toxin = property.<ToxinTrait>getTrait(ToxinTrait.class);
            if (toxin != null) contact.living(FluidContact.toxin(toxin));
            var pheromone = property.<PheromoneTrait>getTrait(PheromoneTrait.class);
            if (pheromone != null) {
                contact.living(
                        FluidContact.effects(
                                LivingEntity.class,
                                new MobEffectInstance(MobEffects.RESISTANCE, 2 * 60 * 20, 2),
                                new MobEffectInstance(MobEffects.SPEED, 5 * 60 * 20, 1),
                                new MobEffectInstance(MobEffects.HASTE, 2 * 60 * 20, 4)));
                if (pheromone.kind() == 1)
                    contact.living(
                            FluidContact.effects(
                                    GlyphidEntity.class,
                                    new MobEffectInstance(MobEffects.STRENGTH, 5 * 60 * 20, 4),
                                    new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 60 * 20, 0),
                                    new MobEffectInstance(MobEffects.ABSORPTION, 60 * 20, 19)));
                if (pheromone.kind() == 2)
                    contact.living(
                            FluidContact.effects(
                                    Player.class,
                                    new MobEffectInstance(MobEffects.STRENGTH, 2 * 60 * 20, 2)));
            }
        }

        if (fluid == NTMFluids.XPJUICE)
            contact.any(
                    (chemical, _, target, _) -> {
                        if (!(target instanceof Player player)) return;
                        player.giveExperiencePoints(1);
                        chemical.discard();
                    });
        if (fluid == NTMFluids.ENDERJUICE)
            contact.any(
                    (chemical, _, target, _) ->
                            chemical.teleportRandomly(target));
        return contact.build();
    }

    protected boolean isExtinguishing() {
        return this.getStyle() == ChemicalStyle.LIQUID
                && temperatureOf(this.getChemType()) < 50
                && !NTMFluidProperties.hasTrait(this.getChemType(), FlammableTrait.class);
    }

    private DamageSource thrown(Holder<DamageType> type) {
        var thrower = getThrower();
        return ModifiedDamageSource.bypassingCooldown(
                thrower != null ? new DamageSource(type, this, thrower) : new DamageSource(type));
    }

    public boolean teleportRandomly(Entity e) {
        return level() instanceof ServerLevel server && FluidContact.scatter(server, this, e);
    }

    public static boolean walkTo(ServerLevel level, Entity e, double x, double y, double z) {
        double fromX = e.getX(), fromY = e.getY(), fromZ = e.getZ();
        BlockPos pos = BlockPos.containing(x, y, z);
        if (level.hasChunkAt(pos)) {
            while (pos.getY() > level.getMinY()) {
                BlockPos below = pos.below();
                if (level.getBlockState(below).is(BlockTags.BLOCKS_MOTION)) {
                    e.teleportTo(x, y, z);
                    if (level.noCollision(e) && !level.containsAnyLiquid(e.getBoundingBox()))
                        return true;
                    break;
                }
                y--;
                pos = below;
            }
        }
        e.teleportTo(fromX, fromY, fromZ);
        return false;
    }

    @Override
    protected void onImpact(HitResult mop) {

        if (!level().isClientSide()) {

            if (mop instanceof EntityHitResult entityHit) {
                this.affect(
                        entityHit.getEntity(),
                        1D - (double) this.tickCount / (double) this.getMaxAge());
            }

            if (mop instanceof BlockHitResult blockHit) {

                Fluid type = getChemType();
                BlockPos pos = blockHit.getBlockPos();

                if (NTMFluidProperties.hasTrait(type, RadiationReleaseTrait.class)
                        && level() instanceof ServerLevel server) {
                    RadiationReleaseTrait trait =
                            NTMFluidProperties.getTrait(type, RadiationReleaseTrait.class);
                    float amount = trait.perMb() * 5;
                    RadiationSystemNT.incrementRad(server, pos, amount, amount * 1024D + 1D);
                }

                ChemicalStyle style = getStyle();

                if (style == ChemicalStyle.BURNING || style == ChemicalStyle.GASFLAME) {

                    for (Direction dir : Direction.VALUES) {

                        BlockState fire =
                                type == NTMFluids.BALEFIRE_FUEL
                                        ? ModBlocks.BALEFIRE.get().defaultBlockState()
                                        : Blocks.FIRE.defaultBlockState();
                        BlockPos target = pos.relative(dir);

                        if (level().getBlockState(target).isAir()) {
                            level().setBlockAndUpdate(target, fire);
                        }
                    }
                }

                if (this.isExtinguishing()) {

                    for (Direction dir : Direction.VALUES) {
                        BlockPos target = pos.relative(dir);
                        if (level().getBlockState(target).is(Blocks.FIRE)) {
                            level().setBlockAndUpdate(target, Blocks.AIR.defaultBlockState());
                        }
                    }
                }

                Extinguishable.Agent extinguish = extinguishTypeOf(type);

                if (extinguish != null) {
                    if (level() instanceof ServerLevel server) {
                        Extinguishable extinguishable = NtmContracts.EXTINGUISHABLE.at(server, pos);
                        if (extinguishable != null)
                            extinguishable.tryExtinguish(server, pos, extinguish);
                    }

                    if (extinguish == Extinguishable.Agent.WATER && style == ChemicalStyle.LIQUID) {
                        for (int i = -2; i <= 2; i++) {
                            for (int j = 0; j <= 1; j++) {
                                for (int k = -2; k <= 2; k++) {
                                    BlockPos target = pos.offset(i, j, k);
                                    if (level().getBlockState(target).is(ModBlocks.FALLOUT.get())) {
                                        level().setBlockAndUpdate(
                                                        target, Blocks.AIR.defaultBlockState());
                                    }
                                }
                            }
                        }
                    }
                }

                if (type == NTMFluids.SEEDSLURRY) seedSlurry(pos);

                Environment.chemicalImpact((ServerLevel) level(), pos, type);

                this.discard();
            }
        }
    }

    private void seedSlurry(BlockPos pos) {
        for (int i = -1; i <= 1; i++)
            for (int j = -1; j <= 1; j++)
                for (int k = -1; k <= 1; k++) seedBlock(pos.offset(i, j, k));
    }

    private void seedBlock(BlockPos target) {
        BlockState state = level().getBlockState(target);

        if (state.is(Blocks.DIRT)
                || state.is(ModBlocks.WASTE_EARTH.get())
                || state.is(ModBlocks.DIRT_DEAD.get())
                || state.is(ModBlocks.DIRT_OILY.get())) {
            BlockPos above = target.above();
            if (level().getMaxLocalRawBrightness(above) >= 9
                    && level().getBlockState(above).getLightDampening() <= 2
                    && Environment.fertilize((ServerLevel) level(), target)) {
                level().setBlockAndUpdate(target, Blocks.GRASS_BLOCK.defaultBlockState());
            }
        }
        if (state.is(Blocks.COBBLESTONE))
            level().setBlockAndUpdate(target, Blocks.MOSSY_COBBLESTONE.defaultBlockState());
        if (state.is(Blocks.STONE_BRICKS))
            level().setBlockAndUpdate(target, Blocks.MOSSY_STONE_BRICKS.defaultBlockState());
        if (state.is(ModBlocks.WASTE_EARTH.get()))
            level().setBlockAndUpdate(target, Blocks.GRASS_BLOCK.defaultBlockState());
        if (state.is(brickConcrete()))
            level().setBlockAndUpdate(target, brickConcreteMossy().defaultBlockState());
        if (state.is(block("brick_concrete_slab")))
            level().setBlockAndUpdate(
                            target, block("brick_concrete_mossy_slab").withPropertiesOf(state));
        if (state.is(block("brick_concrete_stairs")))
            level().setBlockAndUpdate(
                            target, block("brick_concrete_mossy_stairs").withPropertiesOf(state));
    }

    @Override
    protected float getAirDrag() {

        ChemicalStyle type = getStyle();

        if (type == ChemicalStyle.AMAT) return 1F;
        if (type == ChemicalStyle.LIGHTNING) return 1F;
        if (type == ChemicalStyle.GAS) return 0.95F;

        return 0.99F;
    }

    @Override
    public float getWaterDrag() {

        ChemicalStyle type = getStyle();

        if (type == ChemicalStyle.AMAT) return 1F;
        if (type == ChemicalStyle.LIGHTNING) return 1F;
        if (type == ChemicalStyle.GAS) return 1F;

        return 0.8F;
    }

    public int getMaxAge() {

        switch (this.getStyle()) {
            case AMAT:
                return 100;
            case LIGHTNING:
                return 5;
            case BURNING:
                return 600;
            case GAS:
                return 60;
            case GASFLAME:
                return 20;
            case LIQUID:
                return 600;
            default:
                return 100;
        }
    }

    @Override
    public double getGravityVelocity() {

        ChemicalStyle type = getStyle();

        if (type == ChemicalStyle.AMAT) return 0D;
        if (type == ChemicalStyle.LIGHTNING) return 0D;
        if (type == ChemicalStyle.GAS) return 0D;
        if (type == ChemicalStyle.GASFLAME) return -0.01D;

        return 0.03D;
    }

    @Override
    public boolean doesPenetrate() {
        return true;
    }

    @Override
    protected int penetrationLimit() {
        return 1;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.store("Fluid", BuiltInRegistries.FLUID.byNameCodec(), getChemType());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        input.read("Fluid", BuiltInRegistries.FLUID.byNameCodec()).ifPresent(this::setFluid);
    }

    public ChemicalStyle getStyle() {
        return styleMemo.get(getChemType());
    }

    public enum ChemicalStyle {
        AMAT,
        LIGHTNING,
        LIQUID,
        GAS,
        GASFLAME,
        BURNING,
        NULL
    }
}
