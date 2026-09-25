// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.projectile;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.client.ClientEffects;
import com.hbm.client.ClientPlayerAccess;
import com.hbm.entity.mob.glyphid.EntityGlyphid;
import com.hbm.extprop.HbmLivingProps;
import com.hbm.handler.ArmorUtil;
import com.hbm.handler.radiation.RadiationSystemNT;
import com.hbm.interfaces.IRepairable.EnumExtinguishType;
import com.hbm.interfaces.IRepairable;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluidProperty;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.trait.*;
import com.hbm.lib.Library;
import com.hbm.lib.ModDamageTypes;
import com.hbm.particle.helper.FlameCreator;
import com.hbm.util.ContaminationUtil.ContaminationType;
import com.hbm.util.ContaminationUtil.HazardType;
import com.hbm.util.ContaminationUtil;
import com.hbm.util.EntityDamageUtil;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class EntityChemical extends EntityThrowableNT {

    private static final EntityDataAccessor<Integer> DATA_FLUID =
            SynchedEntityData.defineId(EntityChemical.class, EntityDataSerializers.INT);

    public EntityChemical(EntityType<? extends EntityChemical> type, Level level) {
        super(type, level);
    }

    public EntityChemical(
            EntityType<? extends EntityChemical> type,
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

    private static @Nullable EnumExtinguishType extinguishTypeOf(Fluid type) {
        if (type == NTMFluids.CARBONDIOXIDE) return EnumExtinguishType.CO2;
        if (type == NTMFluids.WATER || type == NTMFluids.HEAVYWATER || type == NTMFluids.COOLANT)
            return EnumExtinguishType.WATER;
        return null;
    }

    private static Block block(String name) {
        return BuiltInRegistries.BLOCK.getOptional(Library.id(name)).orElseThrow();
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

            if (NTMFluidProperties.hasTrait(type, FT_Flammable.class)
                    || NTMFluidProperties.hasTrait(type, FT_Combustible.class)) {
                return ChemicalStyle.GASFLAME;
            } else {
                return ChemicalStyle.GAS;
            }
        }

        if (NTMFluidProperties.hasTrait(type, FluidTraitSimple.FT_Liquid.class)) {

            if (NTMFluidProperties.hasTrait(type, FT_Combustible.class)) {
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

    public EntityChemical setFluid(Fluid fluid) {
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

        ChemicalStyle style = getStyle();
        Fluid type = getChemType();
        LivingEntity living = e instanceof LivingEntity l ? l : null;

        if (style == ChemicalStyle.LIQUID || style == ChemicalStyle.BURNING) intensity = 1D;

        if (style == ChemicalStyle.AMAT) {
            EntityDamageUtil.attackEntityFromIgnoreIFrame(
                    e, level().damageSources().source(ModDamageTypes.RADIATION), 1F);
            if (living != null) {
                ContaminationUtil.contaminate(
                        living,
                        HazardType.RADIATION,
                        ContaminationType.CREATIVE,
                        50F * (float) intensity);
                return;
            }
        }

        if (style == ChemicalStyle.LIGHTNING) {
            EntityDamageUtil.attackEntityFromIgnoreIFrame(
                    e, level().damageSources().source(ModDamageTypes.ELECTRICITY), 0.5F);
            if (living != null) {
                living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 9));
                living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 9));
                return;
            }
        }

        int temperature = temperatureOf(type);

        if (temperature >= 100) {
            EntityDamageUtil.attackEntityFromIgnoreIFrame(
                    e,
                    getDamage(ModDamageTypes.BOIL),
                    Math.min(0.25F + (temperature - 100) * 0.001F, 15F));

            if (temperature >= 500) {
                e.igniteForSeconds(10);
            }
        }

        if (style == ChemicalStyle.LIQUID || style == ChemicalStyle.GAS) {
            if (temperature < -20 && living != null) {
                EntityDamageUtil.attackEntityFromIgnoreIFrame(
                        e,
                        getDamage(ModDamageTypes.BOIL),
                        Math.min(0.25F + (-temperature) * 0.01F, 2F));
            }

            if (NTMFluidProperties.hasTrait(type, FluidTraitSimple.FT_Delicious.class)) {
                if (living != null && living.isAlive()) {
                    living.heal(2F * (float) intensity);
                }
            }
        }

        if (style == ChemicalStyle.LIQUID) {

            if (NTMFluidProperties.hasTrait(type, FT_Flammable.class) && living != null) {
                HbmLivingProps.getData(living).oil = 300;
            }
            if (NTMFluidProperties.hasTrait(type, FluidTraitSimple.FT_Delicious.class)) {
                if (living != null && living.isAlive()) {
                    living.heal(2F * (float) intensity);
                }
            }
        }

        if (this.isExtinguishing()) {
            e.clearFire();
        }

        if (style == ChemicalStyle.BURNING) {
            FT_Combustible trait = NTMFluidProperties.getTrait(type, FT_Combustible.class);
            EntityDamageUtil.attackEntityFromIgnoreIFrame(
                    e,
                    getDamage(ModDamageTypes.FLAMETHROWER),
                    0.2F
                            + (trait != null
                                    ? (Math.min(trait.getCombustionEnergy() / 100_000F, 15F))
                                    : 0));
            e.igniteForSeconds(5);
        }

        if (style == ChemicalStyle.GASFLAME) {
            FT_Flammable flammable = NTMFluidProperties.getTrait(type, FT_Flammable.class);
            FT_Combustible combustible = NTMFluidProperties.getTrait(type, FT_Combustible.class);

            float heat =
                    Math.max(
                            flammable != null ? flammable.getHeatEnergy() / 50_000F : 0,
                            combustible != null
                                    ? Math.min(combustible.getCombustionEnergy() / 100_000F, 15F)
                                    : 0);
            heat *= intensity;
            EntityDamageUtil.attackEntityFromIgnoreIFrame(
                    e, getDamage(ModDamageTypes.FLAMETHROWER), (0.2F + heat) * (float) intensity);
            e.igniteForSeconds((int) Math.ceil(5 * intensity));
        }

        if (NTMFluidProperties.hasTrait(type, FT_Corrosive.class)) {
            FT_Corrosive trait = NTMFluidProperties.getTrait(type, FT_Corrosive.class);

            if (living != null) {
                EntityDamageUtil.attackEntityFromIgnoreIFrame(
                        living, getDamage(ModDamageTypes.ACID_PLAYER), trait.getRating() / 50F);
                ArmorUtil.damageWholeSuit(living, trait.getRating() / 40);
            }
        }

        if (NTMFluidProperties.hasTrait(type, FT_VentRadiation.class)) {
            FT_VentRadiation trait = NTMFluidProperties.getTrait(type, FT_VentRadiation.class);
            if (living != null) {
                ContaminationUtil.contaminate(
                        living,
                        HazardType.RADIATION,
                        ContaminationType.CREATIVE,
                        trait.getRadPerMB() * 5);
            }
            if (level() instanceof ServerLevel server) {
                float amount = trait.getRadPerMB() * 5;
                RadiationSystemNT.incrementRad(
                        server, e.blockPosition(), amount, amount * 1024D + 1D);
            }
        }

        if (NTMFluidProperties.hasTrait(type, FT_Poison.class)) {
            FT_Poison trait = NTMFluidProperties.getTrait(type, FT_Poison.class);

            if (living != null) {
                living.addEffect(
                        new MobEffectInstance(
                                trait.isWithering() ? MobEffects.WITHER : MobEffects.POISON,
                                (int) (5 * 20 * intensity)));
            }
        }

        if (NTMFluidProperties.hasTrait(type, FT_Toxin.class)) {
            FT_Toxin trait = NTMFluidProperties.getTrait(type, FT_Toxin.class);

            if (living != null) {
                trait.affect(living, intensity);
            }
        }

        if (NTMFluidProperties.hasTrait(type, FT_Pheromone.class)) {
            FT_Pheromone trait = NTMFluidProperties.getTrait(type, FT_Pheromone.class);

            if (living != null) {
                living.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 2 * 60 * 20, 2));
                living.addEffect(new MobEffectInstance(MobEffects.SPEED, 5 * 60 * 20, 1));
                living.addEffect(new MobEffectInstance(MobEffects.HASTE, 2 * 60 * 20, 4));

                if (living instanceof EntityGlyphid && trait.getType() == 1) {
                    living.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 5 * 60 * 20, 4));
                    living.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 60 * 20, 0));
                    living.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 60 * 20, 19));
                } else if (living instanceof Player && trait.getType() == 2) {
                    living.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 2 * 60 * 20, 2));
                }
            }
        }

        if (type == NTMFluids.XPJUICE) {

            if (e instanceof Player player) {
                player.giveExperiencePoints(1);
                this.discard();
            }
        }

        if (type == NTMFluids.ENDERJUICE) {
            this.teleportRandomly(e);
        }
    }

    protected boolean isExtinguishing() {
        return this.getStyle() == ChemicalStyle.LIQUID
                && temperatureOf(this.getChemType()) < 50
                && !NTMFluidProperties.hasTrait(this.getChemType(), FT_Flammable.class);
    }

    protected DamageSource getDamage(ResourceKey<DamageType> key) {
        Holder<DamageType> holder =
                level().registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(key);
        if (getThrower() != null) {
            return new DamageSource(holder, this, getThrower());
        } else {
            return new DamageSource(holder);
        }
    }

    public boolean teleportRandomly(Entity e) {
        if (!(level() instanceof ServerLevel server)) return false;

        double x = getX() + (this.random.nextDouble() - 0.5D) * 64.0D;
        double y = getY() + (this.random.nextInt(64) - 32);
        double z = getZ() + (this.random.nextDouble() - 0.5D) * 64.0D;
        double fromX = e.getX(), fromY = e.getY(), fromZ = e.getZ();

        if (!(e instanceof LivingEntity living
                ? living.randomTeleport(x, y, z, true)
                : walkTo(server, e, x, y, z))) return false;

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
                e.getX(),
                e.getY(),
                e.getZ(),
                SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.HOSTILE,
                1F,
                1F);
        return true;
    }

    public static boolean walkTo(ServerLevel level, Entity e, double x, double y, double z) {
        double fromX = e.getX(), fromY = e.getY(), fromZ = e.getZ();
        BlockPos pos = BlockPos.containing(x, y, z);
        if (level.hasChunkAt(pos)) {
            while (pos.getY() > level.getMinY()) {
                BlockPos below = pos.below();
                if (level.getBlockState(below).blocksMotion()) {
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

                if (NTMFluidProperties.hasTrait(type, FT_VentRadiation.class)
                        && level() instanceof ServerLevel server) {
                    FT_VentRadiation trait =
                            NTMFluidProperties.getTrait(type, FT_VentRadiation.class);
                    float amount = trait.getRadPerMB() * 5;
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

                EnumExtinguishType extinguish = extinguishTypeOf(type);

                if (extinguish != null) {

                    if (level() instanceof ServerLevel server) {
                        BlockPos core = MultiblockSurface.indexedCore(server, pos);
                        if (core != null
                                && server.getBlockEntity(core) instanceof IRepairable repairable) {
                            repairable.tryExtinguish(server, pos, extinguish);
                        }
                    }

                    if (extinguish == EnumExtinguishType.WATER && style == ChemicalStyle.LIQUID) {
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

                if (type == NTMFluids.SEEDSLURRY) {

                    for (int i = -1; i <= 1; i++)
                        for (int j = -1; j <= 1; j++)
                            for (int k = -1; k <= 1; k++) {
                                BlockPos target = pos.offset(i, j, k);
                                BlockState state = level().getBlockState(target);

                                if (state.is(Blocks.DIRT)
                                        || state.is(ModBlocks.WASTE_EARTH.get())
                                        || state.is(ModBlocks.DIRT_DEAD.get())
                                        || state.is(ModBlocks.DIRT_OILY.get())) {
                                    BlockPos above = target.above();

                                    if (level().getMaxLocalRawBrightness(above) >= 9
                                            && level().getBlockState(above).getLightDampening()
                                                    <= 2) {
                                        level().setBlockAndUpdate(
                                                        target,
                                                        Blocks.GRASS_BLOCK.defaultBlockState());
                                    }
                                }
                                if (state.is(Blocks.COBBLESTONE))
                                    level().setBlockAndUpdate(
                                                    target,
                                                    Blocks.MOSSY_COBBLESTONE.defaultBlockState());
                                if (state.is(Blocks.STONE_BRICKS))
                                    level().setBlockAndUpdate(
                                                    target,
                                                    Blocks.MOSSY_STONE_BRICKS.defaultBlockState());
                                if (state.is(ModBlocks.WASTE_EARTH.get()))
                                    level().setBlockAndUpdate(
                                                    target, Blocks.GRASS_BLOCK.defaultBlockState());
                                if (state.is(brickConcrete()))
                                    level().setBlockAndUpdate(
                                                    target,
                                                    brickConcreteMossy().defaultBlockState());

                                if (state.is(block("brick_concrete_slab")))
                                    level().setBlockAndUpdate(
                                                    target,
                                                    block("brick_concrete_mossy_slab")
                                                            .withPropertiesOf(state));
                                if (state.is(block("brick_concrete_stairs")))
                                    level().setBlockAndUpdate(
                                                    target,
                                                    block("brick_concrete_mossy_stairs")
                                                            .withPropertiesOf(state));
                            }
                }

                this.discard();
            }
        }
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
    protected float getWaterDrag() {

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

    public ChemicalStyle getStyle() {
        return getStyleFromType(this.getChemType());
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
