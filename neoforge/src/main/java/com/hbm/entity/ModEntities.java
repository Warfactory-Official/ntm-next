/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.entity;

import com.hbm.entity.cart.MinecartCrateEntity;
import com.hbm.entity.cart.MinecartDestroyerEntity;
import com.hbm.entity.cart.MinecartOreEntity;
import com.hbm.entity.cart.MinecartPowderEntity;
import com.hbm.entity.cart.MinecartSemtexEntity;
import com.hbm.entity.effect.*;
import com.hbm.entity.effect.*;
import com.hbm.entity.grenade.DisperserCanisterEntity;
import com.hbm.entity.grenade.GrenadeBouncyGenericEntity;
import com.hbm.entity.grenade.GrenadeUniversalEntity;
import com.hbm.entity.grenade.WastePearlEntity;
import com.hbm.entity.item.BoatRubberEntity;
import com.hbm.entity.item.DeliveryDroneEntity;
import com.hbm.entity.item.FallingBlockBatchEntity;
import com.hbm.entity.item.FallingBlockNTEntity;
import com.hbm.entity.item.FallingMultiblockEntity;
import com.hbm.entity.item.FireworksEntity;
import com.hbm.entity.item.ItemBuoyantEntity;
import com.hbm.entity.item.ItemWasteEntity;
import com.hbm.entity.item.MinecartTestEntity;
import com.hbm.entity.item.MovingItemEntity;
import com.hbm.entity.item.MovingItemEntity;
import com.hbm.entity.item.MovingPackageEntity;
import com.hbm.entity.item.MovingPackageEntity;
import com.hbm.entity.item.ParachuteCrateEntity;
import com.hbm.entity.item.RequestDroneEntity;
import com.hbm.entity.item.TntNtmEntity;
import com.hbm.entity.logic.BalefireEntity;
import com.hbm.entity.logic.BomberEntity;
import com.hbm.entity.logic.C130Entity;
import com.hbm.entity.logic.DeathBlastEntity;
import com.hbm.entity.logic.EMPEntity;
import com.hbm.entity.logic.NukeExplosionMK3Entity;
import com.hbm.entity.logic.NukeExplosionMK5Entity;
import com.hbm.entity.logic.OrbitalLaserEntity;
import com.hbm.entity.logic.TomBlastEntity;
import com.hbm.entity.logic.WaypointEntity;
import com.hbm.entity.missile.BobmazonEntity;
import com.hbm.entity.missile.CustomMissile;
import com.hbm.entity.missile.Missile;
import com.hbm.entity.missile.MissileAntiBallisticEntity;
import com.hbm.entity.missile.MissileTier0Entity.EntityMissileBHole;
import com.hbm.entity.missile.MissileTier0Entity.EntityMissileEMP;
import com.hbm.entity.missile.MissileTier0Entity.EntityMissileMicro;
import com.hbm.entity.missile.MissileTier0Entity.EntityMissileSchrabidium;
import com.hbm.entity.missile.MissileTier0Entity.EntityMissileTaint;
import com.hbm.entity.missile.MissileTier0Entity.EntityMissileTest;
import com.hbm.entity.missile.MissileVariant;
import com.hbm.entity.missile.MissileVariants;
import com.hbm.entity.missile.SatellitePod;
import com.hbm.entity.missile.SoyuzCapsule;
import com.hbm.entity.missile.SoyuzEntity;
import com.hbm.entity.missile.StandardMissile;
import com.hbm.entity.mob.BlockSpiderEntity;
import com.hbm.entity.mob.CreeperGoldEntity;
import com.hbm.entity.mob.CreeperNuclearEntity;
import com.hbm.entity.mob.CreeperPhosgeneEntity;
import com.hbm.entity.mob.CreeperTaintedEntity;
import com.hbm.entity.mob.CreeperVolatileEntity;
import com.hbm.entity.mob.CyberCrabEntity;
import com.hbm.entity.mob.DuckEntity;
import com.hbm.entity.mob.DummyEntity;
import com.hbm.entity.mob.FBIDroneEntity;
import com.hbm.entity.mob.FBIEntity;
import com.hbm.entity.mob.GhostEntity;
import com.hbm.entity.mob.HunterChopperEntity;
import com.hbm.entity.mob.MaskManEntity;
import com.hbm.entity.mob.ParasiteMaggotEntity;
import com.hbm.entity.mob.PigeonEntity;
import com.hbm.entity.mob.PlasticBagEntity;
import com.hbm.entity.mob.QuackosEntity;
import com.hbm.entity.mob.RADBeastEntity;
import com.hbm.entity.mob.TaintCrabEntity;
import com.hbm.entity.mob.TeslaCrabEntity;
import com.hbm.entity.mob.UFOEntity;
import com.hbm.entity.mob.UndeadSoldierEntity;
import com.hbm.entity.mob.botprime.BOTPrimeBodyEntity;
import com.hbm.entity.mob.botprime.BOTPrimeHeadEntity;
import com.hbm.entity.mob.glyphid.GlyphidBehemothEntity;
import com.hbm.entity.mob.glyphid.GlyphidBlasterEntity;
import com.hbm.entity.mob.glyphid.GlyphidBombardierEntity;
import com.hbm.entity.mob.glyphid.GlyphidBrawlerEntity;
import com.hbm.entity.mob.glyphid.GlyphidBrendaEntity;
import com.hbm.entity.mob.glyphid.GlyphidDiggerEntity;
import com.hbm.entity.mob.glyphid.GlyphidEntity;
import com.hbm.entity.mob.glyphid.GlyphidNuclearEntity;
import com.hbm.entity.mob.glyphid.GlyphidScoutEntity;
import com.hbm.entity.particle.ChlorineFXEntity;
import com.hbm.entity.particle.CloudFXEntity;
import com.hbm.entity.particle.OrangeFXEntity;
import com.hbm.entity.particle.PinkCloudFXEntity;
import com.hbm.entity.projectile.AcidBombEntity;
import com.hbm.entity.projectile.ArtilleryRocketEntity;
import com.hbm.entity.projectile.ArtilleryShellEntity;
import com.hbm.entity.projectile.B92BeamEntity;
import com.hbm.entity.projectile.BombletZetaEntity;
import com.hbm.entity.projectile.BoxcarEntity;
import com.hbm.entity.projectile.BuildingEntity;
import com.hbm.entity.projectile.BulletBaseMK4Entity;
import com.hbm.entity.projectile.BulletBeamBaseEntity;
import com.hbm.entity.projectile.BulletEntity;
import com.hbm.entity.projectile.BurningFOEQEntity;
import com.hbm.entity.projectile.ChemicalEntity;
import com.hbm.entity.projectile.ChopperMineEntity;
import com.hbm.entity.projectile.CogEntity;
import com.hbm.entity.projectile.CoinEntity;
import com.hbm.entity.projectile.DuchessGambitEntity;
import com.hbm.entity.projectile.FallingNukeEntity;
import com.hbm.entity.projectile.MeteorEntity;
import com.hbm.entity.projectile.RBMKDebrisEntity;
import com.hbm.entity.projectile.RubbleEntity;
import com.hbm.entity.projectile.SawbladeEntity;
import com.hbm.entity.projectile.ShrapnelEntity;
import com.hbm.entity.projectile.SiegeLaserEntity;
import com.hbm.entity.projectile.TomEntity;
import com.hbm.entity.projectile.TorpedoEntity;
import com.hbm.entity.projectile.ZirnoxDebrisEntity;
import com.hbm.entity.train.RailCarBaseEntity;
import com.hbm.entity.train.RailCarRidableEntity;
import com.hbm.entity.train.TrainCargoTram;
import com.hbm.entity.train.TrainCargoTramTrailer;
import com.hbm.platform.BiomeInjection;
import com.hbm.registration.Reg;
import com.hbm.registration.Registrar;
import com.hbm.registration.RegistryHandle;
import com.hbm.util.RegistryUtil;
import com.hbm.world.BiomeTarget;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;

public final class ModEntities {

    public static RegistryHandle<EntityType<DischargeBeamEntity>> BEAM_DISCHARGE;
    public static RegistryHandle<EntityType<NukeExplosionMK5Entity>> NUKE_EXPLOSION_MK5;
    public static RegistryHandle<EntityType<FalloutRainEntity>> FALLOUT_RAIN;
    public static RegistryHandle<EntityType<NukeTorexEntity>> NUKE_TOREX;
    public static RegistryHandle<EntityType<VortexEntity>> VORTEX;
    public static RegistryHandle<EntityType<UndeadSoldierEntity>> UNDEAD_SOLDIER;
    public static RegistryHandle<EntityType<FBIEntity>> FBI;

    public static RegistryHandle<EntityType<CreeperGoldEntity>> CREEPER_GOLD;
    public static RegistryHandle<EntityType<CreeperVolatileEntity>> CREEPER_VOLATILE;
    public static RegistryHandle<EntityType<CreeperPhosgeneEntity>> CREEPER_PHOSGENE;
    public static RegistryHandle<EntityType<CreeperTaintedEntity>> CREEPER_TAINTED;
    public static RegistryHandle<EntityType<CreeperNuclearEntity>> CREEPER_NUCLEAR;

    public static RegistryHandle<EntityType<CyberCrabEntity>> CYBER_CRAB;
    public static RegistryHandle<EntityType<TeslaCrabEntity>> TESLA_CRAB;
    public static RegistryHandle<EntityType<TaintCrabEntity>> TAINT_CRAB;

    public static RegistryHandle<EntityType<DuckEntity>> DUCK;
    public static RegistryHandle<EntityType<GhostEntity>> GHOST;
    public static RegistryHandle<EntityType<DummyEntity>> TEST_DUMMY;
    public static RegistryHandle<EntityType<BlockSpiderEntity>> BLOCK_SPIDER;
    public static RegistryHandle<EntityType<QuackosEntity>> QUACKOS;
    public static RegistryHandle<EntityType<PlasticBagEntity>> PLASTIC_BAG;
    public static RegistryHandle<EntityType<PigeonEntity>> PIGEON;

    public static RegistryHandle<EntityType<GlyphidEntity>> GLYPHID;
    public static RegistryHandle<EntityType<GlyphidBrawlerEntity>> GLYPHID_BRAWLER;
    public static RegistryHandle<EntityType<GlyphidBehemothEntity>> GLYPHID_BEHEMOTH;
    public static RegistryHandle<EntityType<GlyphidBrendaEntity>> GLYPHID_BRENDA;
    public static RegistryHandle<EntityType<GlyphidBombardierEntity>> GLYPHID_BOMBARDIER;
    public static RegistryHandle<EntityType<GlyphidBlasterEntity>> GLYPHID_BLASTER;
    public static RegistryHandle<EntityType<GlyphidScoutEntity>> GLYPHID_SCOUT;
    public static RegistryHandle<EntityType<GlyphidNuclearEntity>> GLYPHID_NUCLEAR;
    public static RegistryHandle<EntityType<GlyphidDiggerEntity>> GLYPHID_DIGGER;
    public static RegistryHandle<EntityType<WaypointEntity>> GLYPHID_WAYPOINT;
    public static RegistryHandle<EntityType<AcidBombEntity>> ACID_BOMB;
    public static RegistryHandle<EntityType<ParasiteMaggotEntity>> PARASITE_MAGGOT;

    public static RegistryHandle<EntityType<StandardMissile>> MISSILE_GENERIC;
    public static RegistryHandle<EntityType<StandardMissile>> MISSILE_INCENDIARY;
    public static RegistryHandle<EntityType<StandardMissile>> MISSILE_DECOY;
    public static RegistryHandle<EntityType<StandardMissile>> MISSILE_BUSTER;
    public static RegistryHandle<EntityType<StandardMissile>> MISSILE_STRONG;
    public static RegistryHandle<EntityType<StandardMissile>> MISSILE_INCENDIARY_STRONG;
    public static RegistryHandle<EntityType<StandardMissile>> MISSILE_BUSTER_STRONG;
    public static RegistryHandle<EntityType<StandardMissile>> MISSILE_BURST;
    public static RegistryHandle<EntityType<StandardMissile>> MISSILE_INFERNO;
    public static RegistryHandle<EntityType<StandardMissile>> MISSILE_NUCLEAR;
    public static RegistryHandle<EntityType<StandardMissile>> MISSILE_NUCLEAR_CLUSTER;
    public static RegistryHandle<EntityType<StandardMissile>> MISSILE_STEALTH;
    public static RegistryHandle<EntityType<CustomMissile>> MISSILE_CUSTOM;
    public static RegistryHandle<EntityType<StandardMissile>> MISSILE_SHUTTLE;
    public static RegistryHandle<EntityType<EntityMissileTest>> MISSILE_TEST;
    public static RegistryHandle<EntityType<EntityMissileMicro>> MISSILE_MICRO;
    public static RegistryHandle<EntityType<EntityMissileSchrabidium>> MISSILE_SCHRABIDIUM;
    public static RegistryHandle<EntityType<EntityMissileBHole>> MISSILE_BHOLE;
    public static RegistryHandle<EntityType<EntityMissileTaint>> MISSILE_TAINT;
    public static RegistryHandle<EntityType<EntityMissileEMP>> MISSILE_EMP;
    public static RegistryHandle<EntityType<StandardMissile>> MISSILE_CLUSTER;
    public static RegistryHandle<EntityType<StandardMissile>> MISSILE_CLUSTER_STRONG;
    public static RegistryHandle<EntityType<StandardMissile>> MISSILE_EMP_STRONG;
    public static RegistryHandle<EntityType<StandardMissile>> MISSILE_RAIN;
    public static RegistryHandle<EntityType<StandardMissile>> MISSILE_DRILL;
    public static RegistryHandle<EntityType<StandardMissile>> MISSILE_VOLCANO;
    public static RegistryHandle<EntityType<StandardMissile>> MISSILE_DOOMSDAY;
    public static RegistryHandle<EntityType<StandardMissile>> MISSILE_DOOMSDAY_RUSTED;

    public static RegistryHandle<EntityType<ShrapnelEntity>> SHRAPNEL;
    public static RegistryHandle<EntityType<BulletBaseMK4Entity>> BULLET_MK4;
    public static RegistryHandle<EntityType<BulletBeamBaseEntity>> BULLET_BEAM;
    public static RegistryHandle<EntityType<FireLingeringEntity>> FIRE_LINGERING;
    public static RegistryHandle<EntityType<CoinEntity>> COIN;
    public static RegistryHandle<EntityType<RubbleEntity>> RUBBLE;
    public static RegistryHandle<EntityType<CogEntity>> COG;
    public static RegistryHandle<EntityType<RBMKDebrisEntity>> RBMK_DEBRIS;
    public static RegistryHandle<EntityType<ZirnoxDebrisEntity>> ZIRNOX_DEBRIS;
    public static RegistryHandle<EntityType<SpearEntity>> SPEAR;
    public static RegistryHandle<EntityType<ItemWasteEntity>> WASTE_ITEM;
    public static RegistryHandle<EntityType<MistEntity>> MIST;
    public static RegistryHandle<EntityType<OrangeFXEntity>> AGENT_ORANGE;
    public static RegistryHandle<EntityType<FireworksEntity>> FIREWORK_BALL;
    public static RegistryHandle<EntityType<MinecartOreEntity>> CART_ORE;
    public static RegistryHandle<EntityType<MinecartCrateEntity>> CART_CRATE;
    public static RegistryHandle<EntityType<TrainCargoTram>> CARGO_TRAM;
    public static RegistryHandle<EntityType<TrainCargoTramTrailer>> CARGO_TRAM_TRAILER;
    public static RegistryHandle<EntityType<RailCarBaseEntity.BoundingBoxDummyEntity>>
            BOUNDING_DUMMY;
    public static RegistryHandle<EntityType<RailCarRidableEntity.SeatDummyEntity>> SEAT_DUMMY;
    public static RegistryHandle<EntityType<MinecartDestroyerEntity>> CART_DESTROYER;
    public static RegistryHandle<EntityType<MinecartTestEntity>> CART_TEST;
    public static RegistryHandle<EntityType<BoatRubberEntity>> BOAT_RUBBER;
    public static RegistryHandle<EntityType<MovingItemEntity>> MOVING_ITEM;
    public static RegistryHandle<EntityType<MovingPackageEntity>> MOVING_PACKAGE;
    public static RegistryHandle<EntityType<DeliveryDroneEntity>> DELIVERY_DRONE;
    public static RegistryHandle<EntityType<RequestDroneEntity>> REQUEST_DRONE;
    public static RegistryHandle<EntityType<FBIDroneEntity>> FBI_DRONE;
    public static RegistryHandle<EntityType<RADBeastEntity>> RAD_BEAST;
    public static RegistryHandle<EntityType<MaskManEntity>> MASK_MAN;
    public static RegistryHandle<EntityType<UFOEntity>> UFO;
    public static RegistryHandle<EntityType<BOTPrimeHeadEntity>> BALLS_O_TRON;
    public static RegistryHandle<EntityType<BOTPrimeBodyEntity>> BALLS_O_TRON_SEG;
    public static RegistryHandle<EntityType<HunterChopperEntity>> HUNTER_CHOPPER;
    public static RegistryHandle<EntityType<MinecartPowderEntity>> CART_POWDER;
    public static RegistryHandle<EntityType<MinecartSemtexEntity>> CART_SEMTEX;
    public static RegistryHandle<EntityType<ChlorineFXEntity>> CHLORINE_FX;
    public static RegistryHandle<EntityType<CloudFXEntity>> CLOUD_FX;
    public static RegistryHandle<EntityType<PinkCloudFXEntity>> PINK_CLOUD_FX;
    public static RegistryHandle<EntityType<MeteorEntity>> METEOR;

    public static RegistryHandle<EntityType<TntNtmEntity>> TNT_NTM;
    public static RegistryHandle<EntityType<FallingBlockNTEntity>> FALLING_BLOCK_NT;
    public static RegistryHandle<EntityType<FallingBlockBatchEntity>> FALLING_BLOCK_BATCH;
    public static RegistryHandle<EntityType<FallingMultiblockEntity>> FALLING_MULTIBLOCK;

    public static RegistryHandle<EntityType<C130Entity>> C130;
    public static RegistryHandle<EntityType<BomberEntity>> BOMBER;
    public static RegistryHandle<EntityType<BombletZetaEntity>> BOMBLET_ZETA;
    public static RegistryHandle<EntityType<GrenadeUniversalEntity>> GRENADE_UNIVERSAL;
    public static RegistryHandle<EntityType<GrenadeBouncyGenericEntity>> GRENADE_BOUNCY_GENERIC;
    public static RegistryHandle<EntityType<DisperserCanisterEntity>> DISPERSER_CANISTER;
    public static RegistryHandle<EntityType<WastePearlEntity>> WASTE_PEARL;
    public static RegistryHandle<EntityType<ItemBuoyantEntity>> ITEM_BUOYANT;
    public static RegistryHandle<EntityType<NukeExplosionMK3Entity>> NUKE_EXPLOSION_MK3;
    public static RegistryHandle<EntityType<CloudFleijaEntity>> CLOUD_FLEIJA;
    public static RegistryHandle<EntityType<CloudSoliniumEntity>> CLOUD_SOLINIUM;
    public static RegistryHandle<EntityType<CloudFleijaRainbowEntity>> CLOUD_RAINBOW;
    public static RegistryHandle<EntityType<ParachuteCrateEntity>> PARACHUTE_CRATE;
    public static RegistryHandle<EntityType<BoxcarEntity>> BOXCAR;
    public static RegistryHandle<EntityType<TorpedoEntity>> TORPEDO;
    public static RegistryHandle<EntityType<DuchessGambitEntity>> DUCHESS_GAMBIT;
    public static RegistryHandle<EntityType<BuildingEntity>> FALLING_BUILDING;
    public static RegistryHandle<EntityType<ChemicalEntity>> CHEMICAL;

    public static RegistryHandle<EntityType<SoyuzEntity>> SOYUZ;
    public static RegistryHandle<EntityType<SoyuzCapsule>> SOYUZ_CAPSULE;
    public static RegistryHandle<EntityType<SatellitePod>> SATELLITE_POD;
    public static RegistryHandle<EntityType<ChopperMineEntity>> CHOPPER_MINE;

    public static RegistryHandle<EntityType<BobmazonEntity>> BOBMAZON_DELIVERY;
    public static RegistryHandle<EntityType<MissileAntiBallisticEntity>> MISSILE_ANTI;
    public static RegistryHandle<EntityType<BlackHoleEntity>> BLACK_HOLE;
    public static RegistryHandle<EntityType<QuasarEntity>> DIGAMMA_QUASAR;
    public static RegistryHandle<EntityType<EMPBlastEntity>> EMP_BLAST;
    public static RegistryHandle<EntityType<EMPEntity>> EMP_LOGIC;
    public static RegistryHandle<EntityType<BalefireEntity>> BALEFIRE;
    public static RegistryHandle<EntityType<DeathBlastEntity>> LASER_BLAST;
    public static RegistryHandle<EntityType<OrbitalLaserEntity>> ORBITAL_LASER;
    public static RegistryHandle<EntityType<TomBlastEntity>> TOM_BUST;
    public static RegistryHandle<EntityType<RagingVortexEntity>> RAGING_VORTEX;
    public static RegistryHandle<EntityType<CloudTomEntity>> MOONSTONE_BLAST;
    public static RegistryHandle<EntityType<ArtilleryShellEntity>> ARTILLERY_SHELL;
    public static RegistryHandle<EntityType<ArtilleryRocketEntity>> HIMARS;
    public static RegistryHandle<EntityType<B92BeamEntity>> BEAM_BOMB;
    public static RegistryHandle<EntityType<BulletEntity>> BULLET;
    public static RegistryHandle<EntityType<BurningFOEQEntity>> BURNING_FOEQ;
    public static RegistryHandle<EntityType<FallingNukeEntity>> FALLING_BOMB;
    public static RegistryHandle<EntityType<SiegeLaserEntity>> SIEGE_LASER;
    public static RegistryHandle<EntityType<SawbladeEntity>> STRAY_SAW;
    public static RegistryHandle<EntityType<TomEntity>> TOM_THE_MOONSTONE;

    private ModEntities() {}

    private static final TagKey<Biome> NO_DEFAULT_MONSTERS = convention("no_default_monsters");

    public static void register() {
        BEAM_DISCHARGE =
                Reg.entity(
                        "beam_discharge",
                        () ->
                                EntityType.Builder.of(DischargeBeamEntity::new, MobCategory.MISC)
                                        .noLootTable()
                                        .noSave()
                                        .fireImmune()
                                        .sized(0.5F, 0.5F)
                                        .clientTrackingRange(16)
                                        .updateInterval(Integer.MAX_VALUE)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id("beam_discharge"))));
        NUKE_EXPLOSION_MK5 =
                Reg.entity(
                        "nuke_mk5",
                        () ->
                                EntityType.Builder.of(NukeExplosionMK5Entity::new, MobCategory.MISC)
                                        .noLootTable()
                                        .fireImmune()
                                        .sized(1.0F, 1.0F)
                                        .clientTrackingRange(0)
                                        .updateInterval(Integer.MAX_VALUE)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id("nuke_mk5"))));
        FALLOUT_RAIN =
                Reg.entity(
                        "fallout_rain",
                        () ->
                                EntityType.Builder.of(FalloutRainEntity::new, MobCategory.MISC)
                                        .noLootTable()
                                        .fireImmune()
                                        .sized(1.0F, 1.0F)
                                        .clientTrackingRange(63)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id("fallout_rain"))));
        UNDEAD_SOLDIER =
                Reg.entity(
                        "entity_undead_soldier",
                        () ->
                                EntityType.Builder.of(UndeadSoldierEntity::new, MobCategory.MONSTER)
                                        .notInPeaceful()
                                        .noLootTable()
                                        .sized(.6F, 1.95F)
                                        .clientTrackingRange(5)
                                        .updateInterval(3)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id("entity_undead_soldier"))));
        Registrar.registerLivingAttributes(UNDEAD_SOLDIER, UndeadSoldierEntity::createAttributes);

        FBI =
                Reg.entity(
                        "entity_fbi",
                        () ->
                                EntityType.Builder.<FBIEntity>of(
                                                FBIEntity::new, MobCategory.MONSTER)
                                        .notInPeaceful()
                                        .sized(0.6F, 1.8F)
                                        .fireImmune()
                                        .clientTrackingRange(5)
                                        .updateInterval(3)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id("entity_fbi"))));
        Registrar.registerLivingAttributes(FBI, FBIEntity::createAttributes);

        CREEPER_GOLD =
                Reg.entity(
                        "entity_mob_gold_creeper",
                        () ->
                                EntityType.Builder.<CreeperGoldEntity>of(
                                                CreeperGoldEntity::new, MobCategory.MONSTER)
                                        .notInPeaceful()
                                        .sized(0.6F, 1.7F)
                                        .clientTrackingRange(5)
                                        .updateInterval(3)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id(
                                                                "entity_mob_gold_creeper"))));
        Registrar.registerLivingAttributes(CREEPER_GOLD, Creeper::createAttributes);
        Registrar.registerSpawnPlacement(
                CREEPER_GOLD,
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules);

        CREEPER_VOLATILE =
                Reg.entity(
                        "entity_mob_volatile_creeper",
                        () ->
                                EntityType.Builder.<CreeperVolatileEntity>of(
                                                CreeperVolatileEntity::new, MobCategory.MONSTER)
                                        .notInPeaceful()
                                        .sized(0.6F, 1.7F)
                                        .clientTrackingRange(5)
                                        .updateInterval(3)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id(
                                                                "entity_mob_volatile_creeper"))));
        Registrar.registerLivingAttributes(CREEPER_VOLATILE, Creeper::createAttributes);
        Registrar.registerSpawnPlacement(
                CREEPER_VOLATILE,
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules);

        CREEPER_PHOSGENE =
                Reg.entity(
                        "entity_mob_phosgene_creeper",
                        () ->
                                EntityType.Builder.<CreeperPhosgeneEntity>of(
                                                CreeperPhosgeneEntity::new, MobCategory.MONSTER)
                                        .notInPeaceful()
                                        .sized(0.6F, 1.7F)
                                        .clientTrackingRange(5)
                                        .updateInterval(3)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id(
                                                                "entity_mob_phosgene_creeper"))));
        Registrar.registerLivingAttributes(CREEPER_PHOSGENE, Creeper::createAttributes);
        Registrar.registerSpawnPlacement(
                CREEPER_PHOSGENE,
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules);

        CREEPER_TAINTED =
                Reg.entity(
                        "entity_mob_tainted_creeper",
                        () ->
                                EntityType.Builder.<CreeperTaintedEntity>of(
                                                CreeperTaintedEntity::new, MobCategory.MONSTER)
                                        .notInPeaceful()
                                        .sized(0.6F, 1.7F)
                                        .clientTrackingRange(5)
                                        .updateInterval(3)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id(
                                                                "entity_mob_tainted_creeper"))));
        Registrar.registerLivingAttributes(CREEPER_TAINTED, CreeperTaintedEntity::createAttributes);

        CREEPER_NUCLEAR =
                Reg.entity(
                        "entity_mob_nuclear_creeper",
                        () ->
                                EntityType.Builder.<CreeperNuclearEntity>of(
                                                CreeperNuclearEntity::new, MobCategory.MONSTER)
                                        .notInPeaceful()
                                        .sized(0.6F, 1.7F)
                                        .clientTrackingRange(5)
                                        .updateInterval(3)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id(
                                                                "entity_mob_nuclear_creeper"))));
        Registrar.registerLivingAttributes(CREEPER_NUCLEAR, CreeperNuclearEntity::createAttributes);

        CYBER_CRAB =
                Reg.entity(
                        "entity_cyber_crab",
                        () ->
                                EntityType.Builder.<CyberCrabEntity>of(
                                                CyberCrabEntity::new, MobCategory.MONSTER)
                                        .notInPeaceful()
                                        .noLootTable()
                                        .sized(0.75F, 0.35F)
                                        .clientTrackingRange(5)
                                        .updateInterval(3)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id("entity_cyber_crab"))));
        Registrar.registerLivingAttributes(CYBER_CRAB, CyberCrabEntity::createAttributes);

        TESLA_CRAB =
                Reg.entity(
                        "entity_tesla_crab",
                        () ->
                                EntityType.Builder.<TeslaCrabEntity>of(
                                                TeslaCrabEntity::new, MobCategory.MONSTER)
                                        .notInPeaceful()
                                        .sized(0.75F, 1.25F)
                                        .clientTrackingRange(5)
                                        .updateInterval(3)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id("entity_tesla_crab"))));
        Registrar.registerLivingAttributes(TESLA_CRAB, TeslaCrabEntity::createAttributes);

        TAINT_CRAB =
                Reg.entity(
                        "entity_taint_crab",
                        () ->
                                EntityType.Builder.<TaintCrabEntity>of(
                                                TaintCrabEntity::new, MobCategory.MONSTER)
                                        .notInPeaceful()
                                        .sized(1.25F, 1.25F)
                                        .clientTrackingRange(5)
                                        .updateInterval(3)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id("entity_taint_crab"))));
        Registrar.registerLivingAttributes(TAINT_CRAB, TaintCrabEntity::createAttributes);

        DUCK =
                Reg.entity(
                        "entity_fucc_a_ducc",
                        () ->
                                EntityType.Builder.<DuckEntity>of(
                                                DuckEntity::new, MobCategory.CREATURE)
                                        .sized(0.4F, 0.7F)
                                        .clientTrackingRange(5)
                                        .updateInterval(3)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id("entity_fucc_a_ducc"))));
        Registrar.registerLivingAttributes(DUCK, Chicken::createAttributes);

        GHOST =
                Reg.entity(
                        "entity_ghost",
                        () ->
                                EntityType.Builder.<GhostEntity>of(
                                                GhostEntity::new, MobCategory.MONSTER)
                                        .noLootTable()
                                        .sized(0.6F, 1.8F)
                                        .clientTrackingRange(63)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id("entity_ghost"))));
        Registrar.registerLivingAttributes(GHOST, GhostEntity::createAttributes);

        TEST_DUMMY =
                Reg.entity(
                        "entity_test_dummy",
                        () ->
                                EntityType.Builder.<DummyEntity>of(
                                                DummyEntity::new, MobCategory.CREATURE)
                                        .noLootTable()
                                        .sized(0.6F, 1.8F)
                                        .clientTrackingRange(5)
                                        .updateInterval(3)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id("entity_test_dummy"))));
        Registrar.registerLivingAttributes(TEST_DUMMY, DummyEntity::createAttributes);

        BLOCK_SPIDER =
                Reg.entity(
                        "entity_taintcrawler",
                        () ->
                                EntityType.Builder.<BlockSpiderEntity>of(
                                                BlockSpiderEntity::new, MobCategory.MONSTER)
                                        .notInPeaceful()
                                        .noLootTable()
                                        .sized(0.95F, 1.25F)
                                        .clientTrackingRange(63)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id("entity_taintcrawler"))));
        Registrar.registerLivingAttributes(BLOCK_SPIDER, BlockSpiderEntity::createAttributes);

        QUACKOS =
                Reg.entity(
                        "entity_elder_one",
                        () ->
                                EntityType.Builder.<QuackosEntity>of(
                                                QuackosEntity::new, MobCategory.CREATURE)
                                        .noLootTable()
                                        .sized(7.5F, 17.5F)
                                        .clientTrackingRange(5)
                                        .updateInterval(3)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id("entity_elder_one"))));
        Registrar.registerLivingAttributes(QUACKOS, Chicken::createAttributes);

        PLASTIC_BAG =
                Reg.entity(
                        "entity_plastic_bag",
                        () ->
                                EntityType.Builder.<PlasticBagEntity>of(
                                                PlasticBagEntity::new, MobCategory.WATER_CREATURE)
                                        .noLootTable()
                                        .sized(0.45F, 0.45F)
                                        .clientTrackingRange(5)
                                        .updateInterval(3)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id("entity_plastic_bag"))));
        Registrar.registerLivingAttributes(PLASTIC_BAG, PlasticBagEntity::createAttributes);
        Registrar.registerSpawnPlacement(
                PLASTIC_BAG,
                SpawnPlacementTypes.IN_WATER,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) -> true);

        PIGEON =
                Reg.entity(
                        "entity_pigeon",
                        () ->
                                EntityType.Builder.<PigeonEntity>of(
                                                PigeonEntity::new, MobCategory.CREATURE)
                                        .sized(0.5F, 1.0F)
                                        .clientTrackingRange(5)
                                        .updateInterval(3)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id("entity_pigeon"))));
        Registrar.registerLivingAttributes(PIGEON, PigeonEntity::createAttributes);
        Registrar.registerSpawnPlacement(
                PIGEON,
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Mob::checkMobSpawnRules);

        spawn(CREEPER_PHOSGENE, BiomeTarget.tags(BiomeTags.IS_OVERWORLD), 5, 1, 1);
        spawn(CREEPER_VOLATILE, BiomeTarget.tags(BiomeTags.IS_OVERWORLD), 10, 1, 1);
        spawn(CREEPER_GOLD, BiomeTarget.tags(BiomeTags.IS_OVERWORLD), 1, 1, 1);
        spawn(PLASTIC_BAG, BiomeTarget.tags(convention("is_ocean")), 1, 1, 3);
        spawn(
                PIGEON,
                BiomeTarget.tags(convention("is_plains"), convention("is_savanna")),
                1,
                5,
                10);

        Reg.EntityHandle<GlyphidEntity> grunt =
                Reg.entity(
                        "entity_glyphid",
                        () ->
                                EntityType.Builder.<GlyphidEntity>of(
                                                GlyphidEntity::new, MobCategory.MONSTER)
                                        .notInPeaceful()
                                        .sized(1.75F, 1F)
                                        .clientTrackingRange(5)
                                        .updateInterval(3)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id("entity_glyphid"))));
        GLYPHID = grunt;
        Registrar.registerLivingAttributes(GLYPHID, GlyphidEntity::createAttributes);
        GlyphidBrendaEntity.GRUNT_TYPE = grunt;

        GlyphidBrawlerEntity.register();
        GLYPHID_BRAWLER = GlyphidBrawlerEntity.TYPE;

        GLYPHID_BEHEMOTH =
                Reg.entity(
                        "entity_glyphid_behemoth",
                        () ->
                                EntityType.Builder.<GlyphidBehemothEntity>of(
                                                GlyphidBehemothEntity::new, MobCategory.MONSTER)
                                        .notInPeaceful()
                                        .sized(2.5F, 1.5F)
                                        .clientTrackingRange(5)
                                        .updateInterval(3)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id(
                                                                "entity_glyphid_behemoth"))));
        Registrar.registerLivingAttributes(
                GLYPHID_BEHEMOTH, GlyphidBehemothEntity::createAttributes);

        GLYPHID_BRENDA =
                Reg.entity(
                        "entity_glyphid_brenda",
                        () ->
                                EntityType.Builder.<GlyphidBrendaEntity>of(
                                                GlyphidBrendaEntity::new, MobCategory.MONSTER)
                                        .notInPeaceful()
                                        .sized(2.5F, 1.75F)
                                        .clientTrackingRange(5)
                                        .updateInterval(3)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id("entity_glyphid_brenda"))));
        Registrar.registerLivingAttributes(GLYPHID_BRENDA, GlyphidBrendaEntity::createAttributes);

        GlyphidBombardierEntity.register();
        GLYPHID_BOMBARDIER = GlyphidBombardierEntity.TYPE;

        GlyphidBlasterEntity.register();
        GLYPHID_BLASTER = GlyphidBlasterEntity.TYPE;

        GLYPHID_SCOUT =
                Reg.entity(
                        "entity_glyphid_scout",
                        () ->
                                EntityType.Builder.<GlyphidScoutEntity>of(
                                                GlyphidScoutEntity::new, MobCategory.MONSTER)
                                        .notInPeaceful()
                                        .sized(1.25F, 0.75F)
                                        .clientTrackingRange(5)
                                        .updateInterval(3)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id("entity_glyphid_scout"))));
        Registrar.registerLivingAttributes(GLYPHID_SCOUT, GlyphidScoutEntity::createAttributes);

        GLYPHID_NUCLEAR =
                Reg.entity(
                        "entity_glyphid_nuclear",
                        () ->
                                EntityType.Builder.<GlyphidNuclearEntity>of(
                                                GlyphidNuclearEntity::new, MobCategory.MONSTER)
                                        .notInPeaceful()
                                        .sized(2.5F, 1.75F)
                                        .clientTrackingRange(5)
                                        .updateInterval(3)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id(
                                                                "entity_glyphid_nuclear"))));
        Registrar.registerLivingAttributes(GLYPHID_NUCLEAR, GlyphidNuclearEntity::createAttributes);

        GlyphidDiggerEntity.register();
        GLYPHID_DIGGER = GlyphidDiggerEntity.TYPE;

        WaypointEntity.register();
        GLYPHID_WAYPOINT = WaypointEntity.TYPE;

        AcidBombEntity.register();
        ACID_BOMB = AcidBombEntity.TYPE;

        ParasiteMaggotEntity.register();
        PARASITE_MAGGOT = ParasiteMaggotEntity.TYPE;

        NUKE_TOREX =
                Reg.entity(
                        "nuke_torex",
                        () ->
                                EntityType.Builder.of(NukeTorexEntity::new, MobCategory.MISC)
                                        .noLootTable()
                                        .noSave()
                                        .fireImmune()
                                        .sized(1.0F, 1.0F)
                                        .clientTrackingRange(16)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id("nuke_torex"))));
        VORTEX =
                Reg.entity(
                        "entity_vortex",
                        () ->
                                EntityType.Builder.<VortexEntity>of(
                                                VortexEntity::new, MobCategory.MISC)
                                        .noLootTable()
                                        .fireImmune()
                                        .sized(0.6F, 1.8F)
                                        .clientTrackingRange(16)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id("entity_vortex"))));
        METEOR =
                Reg.entity(
                        "entity_meteor",
                        () ->
                                EntityType.Builder.<MeteorEntity>of(
                                                MeteorEntity::new, MobCategory.MISC)
                                        .noLootTable()
                                        .fireImmune()
                                        .sized(4F, 4F)
                                        .clientTrackingRange(16)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id("entity_meteor"))));

        MISSILE_GENERIC = standard("missile_generic", () -> MissileVariants.GENERIC);
        MISSILE_INCENDIARY = standard("missile_incendiary", () -> MissileVariants.INCENDIARY);
        MISSILE_DECOY = standard("missile_decoy", () -> MissileVariants.DECOY);
        MISSILE_BUSTER = standard("missile_buster", () -> MissileVariants.BUSTER);
        MISSILE_STRONG = standard("missile_strong", () -> MissileVariants.STRONG);
        MISSILE_INCENDIARY_STRONG =
                standard("missile_incendiary_strong", () -> MissileVariants.INCENDIARY_STRONG);
        MISSILE_BUSTER_STRONG =
                standard("missile_buster_strong", () -> MissileVariants.BUSTER_STRONG);
        MISSILE_BURST = standard("missile_burst", () -> MissileVariants.BURST);

        MISSILE_TEST =
                ModEntities.<EntityMissileTest>missile("missile_test", EntityMissileTest::new);
        MISSILE_MICRO =
                ModEntities.<EntityMissileMicro>missile("missile_micro", EntityMissileMicro::new);
        MISSILE_SCHRABIDIUM =
                ModEntities.<EntityMissileSchrabidium>missile(
                        "missile_schrabidium", EntityMissileSchrabidium::new);
        MISSILE_BHOLE =
                ModEntities.<EntityMissileBHole>missile("missile_bhole", EntityMissileBHole::new);
        MISSILE_TAINT =
                ModEntities.<EntityMissileTaint>missile("missile_taint", EntityMissileTaint::new);
        MISSILE_EMP = ModEntities.<EntityMissileEMP>missile("missile_emp", EntityMissileEMP::new);

        MISSILE_CLUSTER = standard("missile_cluster", () -> MissileVariants.CLUSTER);
        MISSILE_CLUSTER_STRONG =
                standard("missile_cluster_strong", () -> MissileVariants.CLUSTER_STRONG);
        MISSILE_EMP_STRONG = standard("missile_emp_strong", () -> MissileVariants.EMP_STRONG);
        MISSILE_RAIN = standard("missile_rain", () -> MissileVariants.RAIN);
        MISSILE_DRILL = standard("missile_drill", () -> MissileVariants.DRILL);
        MISSILE_VOLCANO = standard("missile_volcano", () -> MissileVariants.VOLCANO);

        MISSILE_CUSTOM =
                ModEntities.<CustomMissile>missile("entity_custom_missile", CustomMissile::new);
        MISSILE_SHUTTLE = standard("entity_missile_shuttle", () -> MissileVariants.SHUTTLE);
        MISSILE_INFERNO = standard("missile_inferno", () -> MissileVariants.INFERNO);
        MISSILE_NUCLEAR = standard("missile_nuclear", () -> MissileVariants.NUCLEAR);
        MISSILE_NUCLEAR_CLUSTER = standard("missile_nuclear_cluster", () -> MissileVariants.MIRV);
        MISSILE_STEALTH = standard("missile_stealth", () -> MissileVariants.STEALTH);
        MISSILE_DOOMSDAY =
                Reg.entity(
                        "missile_doomsday",
                        () ->
                                EntityType.Builder.<StandardMissile>of(
                                                (type, level) ->
                                                        new StandardMissile(
                                                                type,
                                                                level,
                                                                MissileVariants.DOOMSDAY),
                                                MobCategory.MISC)
                                        .noLootTable()
                                        .sized(1.5F, 1.5F)
                                        .clientTrackingRange(63)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id("missile_doomsday"))));
        MISSILE_DOOMSDAY_RUSTED =
                Reg.entity(
                        "missile_doomsday_rusted",
                        () ->
                                EntityType.Builder.<StandardMissile>of(
                                                (type, level) ->
                                                        new StandardMissile(
                                                                type,
                                                                level,
                                                                MissileVariants.DOOMSDAY_RUSTED),
                                                MobCategory.MISC)
                                        .noLootTable()
                                        .sized(1.5F, 1.5F)
                                        .clientTrackingRange(63)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id(
                                                                "missile_doomsday_rusted"))));

        SHRAPNEL =
                Reg.entity(
                        "shrapnel",
                        () ->
                                EntityType.Builder.<ShrapnelEntity>of(
                                                ShrapnelEntity::new, MobCategory.MISC)
                                        .noLootTable()
                                        .noSave()
                                        .fireImmune()
                                        .sized(0.25F, 0.25F)
                                        .clientTrackingRange(63)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id("shrapnel"))));
        BULLET_MK4 =
                Reg.entity(
                        "bullet_mk4",
                        () ->
                                EntityType.Builder.<BulletBaseMK4Entity>of(
                                                BulletBaseMK4Entity::new, MobCategory.MISC)
                                        .noLootTable()
                                        .noSave()
                                        .fireImmune()
                                        .sized(0.5F, 0.5F)
                                        .clientTrackingRange(16)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id("bullet_mk4"))));
        BULLET_BEAM =
                Reg.entity(
                        "bullet_beam",
                        () ->
                                EntityType.Builder.<BulletBeamBaseEntity>of(
                                                BulletBeamBaseEntity::new, MobCategory.MISC)
                                        .noLootTable()
                                        .noSave()
                                        .fireImmune()
                                        .sized(0.5F, 0.5F)
                                        .clientTrackingRange(16)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id("bullet_beam"))));
        FIRE_LINGERING =
                Reg.entity(
                        "fire_lingering",
                        () ->
                                EntityType.Builder.<FireLingeringEntity>of(
                                                FireLingeringEntity::new, MobCategory.MISC)
                                        .noLootTable()
                                        .noSave()
                                        .fireImmune()
                                        .sized(0.5F, 0.5F)
                                        .clientTrackingRange(16)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id("fire_lingering"))));
        COIN =
                Reg.entity(
                        "coin",
                        () ->
                                EntityType.Builder.<CoinEntity>of(CoinEntity::new, MobCategory.MISC)
                                        .noLootTable()
                                        .noSave()
                                        .sized(1F, 1F)
                                        .clientTrackingRange(63)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id("coin"))));
        RUBBLE =
                Reg.entity(
                        "rubble",
                        () ->
                                EntityType.Builder.<RubbleEntity>of(
                                                RubbleEntity::new, MobCategory.MISC)
                                        .noLootTable()
                                        .sized(0.25F, 0.25F)
                                        .clientTrackingRange(63)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id("rubble"))));
        COG =
                Reg.entity(
                        "entity_stray_cog",
                        () ->
                                EntityType.Builder.<CogEntity>of(CogEntity::new, MobCategory.MISC)
                                        .noLootTable()
                                        .sized(1.0F, 1.0F)
                                        .clientTrackingRange(63)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id("entity_stray_cog"))));
        RBMK_DEBRIS =
                Reg.entity(
                        "rbmk_debris",
                        () ->
                                EntityType.Builder.<RBMKDebrisEntity>of(
                                                RBMKDebrisEntity::new, MobCategory.MISC)
                                        .noLootTable()
                                        .fireImmune()
                                        .sized(1.0F, 1.0F)
                                        .clientTrackingRange(63)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id("rbmk_debris"))));
        ZIRNOX_DEBRIS =
                Reg.entity(
                        "zirnox_debris",
                        () ->
                                EntityType.Builder.<ZirnoxDebrisEntity>of(
                                                ZirnoxDebrisEntity::new, MobCategory.MISC)
                                        .noLootTable()
                                        .fireImmune()
                                        .sized(1.0F, 1.0F)
                                        .clientTrackingRange(63)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id("zirnox_debris"))));
        SPEAR =
                Reg.entity(
                        "entity_spear",
                        () ->
                                EntityType.Builder.of(SpearEntity::new, MobCategory.MISC)
                                        .noLootTable()
                                        .fireImmune()
                                        .sized(2.0F, 10.0F)
                                        .clientTrackingRange(63)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id("entity_spear"))));
        WASTE_ITEM =
                Reg.entity(
                        "waste_item",
                        () ->
                                EntityType.Builder.<ItemWasteEntity>of(
                                                ItemWasteEntity::new, MobCategory.MISC)
                                        .sized(0.25F, 0.25F)
                                        .clientTrackingRange(7)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id("waste_item"))));
        MIST =
                Reg.entity(
                        "mist",
                        () ->
                                EntityType.Builder.<MistEntity>of(MistEntity::new, MobCategory.MISC)
                                        .noLootTable()
                                        .sized(1.0F, 1.0F)
                                        .clientTrackingRange(16)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id("mist"))));
        AGENT_ORANGE =
                Reg.entity(
                        "entity_agent_orange",
                        () ->
                                EntityType.Builder.<OrangeFXEntity>of(
                                                OrangeFXEntity::new, MobCategory.MISC)
                                        .noLootTable()
                                        .noSave()
                                        .sized(0.2F, 0.2F)
                                        .clientTrackingRange(63)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id("entity_agent_orange"))));
        CHLORINE_FX =
                Reg.entity(
                        "entity_chlorine_fx",
                        () ->
                                EntityType.Builder.<ChlorineFXEntity>of(
                                                ChlorineFXEntity::new, MobCategory.MISC)
                                        .noLootTable()
                                        .noSave()
                                        .sized(0.2F, 0.2F)
                                        .clientTrackingRange(63)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id("entity_chlorine_fx"))));
        CLOUD_FX =
                Reg.entity(
                        "entity_cloud_fx",
                        () ->
                                EntityType.Builder.<CloudFXEntity>of(
                                                CloudFXEntity::new, MobCategory.MISC)
                                        .noLootTable()
                                        .noSave()
                                        .sized(0.2F, 0.2F)
                                        .clientTrackingRange(63)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id("entity_cloud_fx"))));
        PINK_CLOUD_FX =
                Reg.entity(
                        "entity_pink_cloud_fx",
                        () ->
                                EntityType.Builder.<PinkCloudFXEntity>of(
                                                PinkCloudFXEntity::new, MobCategory.MISC)
                                        .noLootTable()
                                        .noSave()
                                        .sized(0.2F, 0.2F)
                                        .clientTrackingRange(63)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id("entity_pink_cloud_fx"))));
        FIREWORK_BALL =
                Reg.entity(
                        "entity_firework_ball",
                        () ->
                                EntityType.Builder.<FireworksEntity>of(
                                                FireworksEntity::new, MobCategory.MISC)
                                        .noLootTable()
                                        .sized(0.6F, 1.8F)
                                        .clientTrackingRange(63)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id("entity_firework_ball"))));

        CART_ORE =
                Reg.entity(
                        "entity_cart_ore",
                        () ->
                                EntityType.Builder.<MinecartOreEntity>of(
                                                MinecartOreEntity::new, MobCategory.MISC)
                                        .noLootTable()
                                        .sized(0.98F, 0.7F)
                                        .clientTrackingRange(16)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id("entity_cart_ore"))));
        CART_POWDER =
                Reg.entity(
                        "entity_cart_powder",
                        () ->
                                EntityType.Builder.<MinecartPowderEntity>of(
                                                MinecartPowderEntity::new, MobCategory.MISC)
                                        .noLootTable()
                                        .sized(0.98F, 0.7F)
                                        .clientTrackingRange(16)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id("entity_cart_powder"))));
        CART_SEMTEX =
                Reg.entity(
                        "entity_cart_semtex",
                        () ->
                                EntityType.Builder.<MinecartSemtexEntity>of(
                                                MinecartSemtexEntity::new, MobCategory.MISC)
                                        .noLootTable()
                                        .sized(0.98F, 0.7F)
                                        .clientTrackingRange(16)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id("entity_cart_semtex"))));
        CART_CRATE =
                Reg.entity(
                        "entity_cart_crate",
                        () ->
                                EntityType.Builder.<MinecartCrateEntity>of(
                                                MinecartCrateEntity::new, MobCategory.MISC)
                                        .noLootTable()
                                        .sized(0.98F, 0.7F)
                                        .clientTrackingRange(16)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id("entity_cart_crate"))));
        CART_DESTROYER =
                Reg.entity(
                        "entity_cart_destroyer",
                        () ->
                                EntityType.Builder.<MinecartDestroyerEntity>of(
                                                MinecartDestroyerEntity::new, MobCategory.MISC)
                                        .noLootTable()
                                        .sized(0.98F, 0.7F)
                                        .clientTrackingRange(16)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id("entity_cart_destroyer"))));
        CART_TEST =
                Reg.entity(
                        "entity_minecart_test",
                        () ->
                                EntityType.Builder.<MinecartTestEntity>of(
                                                MinecartTestEntity::new, MobCategory.MISC)
                                        .noLootTable()
                                        .sized(0.98F, 0.7F)
                                        .clientTrackingRange(63)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id("entity_minecart_test"))));
        BOAT_RUBBER =
                Reg.entity(
                        "entity_rubber_boat",
                        () ->
                                EntityType.Builder.<BoatRubberEntity>of(
                                                BoatRubberEntity::new, MobCategory.MISC)
                                        .noLootTable()
                                        .sized(1.375F, 0.5625F)
                                        .eyeHeight(0.5625F)
                                        .clientTrackingRange(16)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id("entity_rubber_boat"))));
        CARGO_TRAM =
                Reg.entity(
                        "entity_cargo_tram",
                        () ->
                                EntityType.Builder.<TrainCargoTram>of(
                                                TrainCargoTram::new, MobCategory.MISC)
                                        .noLootTable()
                                        .sized(5F, 2F)
                                        .clientTrackingRange(16)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id("entity_cargo_tram"))));
        CARGO_TRAM_TRAILER =
                Reg.entity(
                        "entity_cargo_tram_trailer",
                        () ->
                                EntityType.Builder.<TrainCargoTramTrailer>of(
                                                TrainCargoTramTrailer::new, MobCategory.MISC)
                                        .noLootTable()
                                        .sized(5F, 2F)
                                        .clientTrackingRange(16)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id(
                                                                "entity_cargo_tram_trailer"))));
        BOUNDING_DUMMY =
                Reg.entity(
                        "entity_bounding_dummy",
                        () ->
                                EntityType.Builder.<RailCarBaseEntity.BoundingBoxDummyEntity>of(
                                                RailCarBaseEntity.BoundingBoxDummyEntity::new,
                                                MobCategory.MISC)
                                        .noLootTable()
                                        .sized(1F, 1F)
                                        .clientTrackingRange(16)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id("entity_bounding_dummy"))));
        SEAT_DUMMY =
                Reg.entity(
                        "entity_seat_dummy",
                        () ->
                                EntityType.Builder.<RailCarRidableEntity.SeatDummyEntity>of(
                                                RailCarRidableEntity.SeatDummyEntity::new,
                                                MobCategory.MISC)
                                        .noLootTable()
                                        .sized(0.5F, 0.1F)
                                        .clientTrackingRange(16)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id("entity_seat_dummy"))));
        MOVING_ITEM =
                Reg.entity(
                        "entity_c_item",
                        () ->
                                EntityType.Builder.<MovingItemEntity>of(
                                                MovingItemEntity::new, MobCategory.MISC)
                                        .noLootTable()
                                        .sized(0.375F, 0.375F)
                                        .clientTrackingRange(63)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id("entity_c_item"))));
        MOVING_PACKAGE =
                Reg.entity(
                        "entity_c_package",
                        () ->
                                EntityType.Builder.<MovingPackageEntity>of(
                                                MovingPackageEntity::new, MobCategory.MISC)
                                        .noLootTable()
                                        .sized(0.5F, 0.5F)
                                        .clientTrackingRange(63)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id("entity_c_package"))));
        DELIVERY_DRONE =
                Reg.entity(
                        "entity_delivery_drone",
                        () ->
                                EntityType.Builder.<DeliveryDroneEntity>of(
                                                DeliveryDroneEntity::new, MobCategory.MISC)
                                        .noLootTable()
                                        .sized(0.75F, 0.75F)
                                        .clientTrackingRange(16)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id("entity_delivery_drone"))));
        REQUEST_DRONE =
                Reg.entity(
                        "entity_request_drone",
                        () ->
                                EntityType.Builder.<RequestDroneEntity>of(
                                                RequestDroneEntity::new, MobCategory.MISC)
                                        .noLootTable()
                                        .sized(0.75F, 0.75F)
                                        .clientTrackingRange(16)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id("entity_request_drone"))));
        FBI_DRONE =
                Reg.entity(
                        "entity_fbi_drone",
                        () ->
                                EntityType.Builder.<FBIDroneEntity>of(
                                                FBIDroneEntity::new, MobCategory.MONSTER)
                                        .notInPeaceful()
                                        .noLootTable()
                                        .fireImmune()
                                        .sized(0.6F, 1.8F)
                                        .clientTrackingRange(5)
                                        .updateInterval(3)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id("entity_fbi_drone"))));
        Registrar.registerLivingAttributes(FBI_DRONE, FBIDroneEntity::createAttributes);
        RAD_BEAST =
                Reg.entity(
                        "entity_radiation_blaze",
                        () ->
                                EntityType.Builder.<RADBeastEntity>of(
                                                RADBeastEntity::new, MobCategory.MONSTER)
                                        .notInPeaceful()
                                        .fireImmune()
                                        .sized(0.6F, 1.8F)
                                        .clientTrackingRange(5)
                                        .updateInterval(3)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id(
                                                                "entity_radiation_blaze"))));
        Registrar.registerLivingAttributes(RAD_BEAST, RADBeastEntity::createAttributes);
        MASK_MAN =
                Reg.entity(
                        "entity_mob_mask_man",
                        () ->
                                EntityType.Builder.<MaskManEntity>of(
                                                MaskManEntity::new, MobCategory.MONSTER)
                                        .notInPeaceful()
                                        .fireImmune()
                                        .sized(2.0F, 5.0F)
                                        .clientTrackingRange(5)
                                        .updateInterval(3)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id("entity_mob_mask_man"))));
        Registrar.registerLivingAttributes(MASK_MAN, MaskManEntity::createAttributes);
        UFO =
                Reg.entity(
                        "entity_ufo",
                        () ->
                                EntityType.Builder.<UFOEntity>of(
                                                UFOEntity::new, MobCategory.MONSTER)
                                        .notInPeaceful()
                                        .noLootTable()
                                        .fireImmune()
                                        .sized(15.0F, 4.0F)
                                        .clientTrackingRange(63)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id("entity_ufo"))));
        Registrar.registerLivingAttributes(UFO, UFOEntity::createAttributes);
        BALLS_O_TRON =
                Reg.entity(
                        "entity_balls_o_tron",
                        () ->
                                EntityType.Builder.<BOTPrimeHeadEntity>of(
                                                BOTPrimeHeadEntity::new, MobCategory.MONSTER)
                                        .notInPeaceful()
                                        .noLootTable()
                                        .fireImmune()
                                        .sized(3.0F, 3.0F)
                                        .eyeHeight(1.5F)
                                        .clientTrackingRange(63)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id("entity_balls_o_tron"))));
        Registrar.registerLivingAttributes(BALLS_O_TRON, BOTPrimeHeadEntity::createAttributes);
        BALLS_O_TRON_SEG =
                Reg.entity(
                        "entity_balls_o_tron_seg",
                        () ->
                                EntityType.Builder.<BOTPrimeBodyEntity>of(
                                                BOTPrimeBodyEntity::new, MobCategory.MONSTER)
                                        .notInPeaceful()
                                        .noLootTable()
                                        .fireImmune()
                                        .sized(2.0F, 2.0F)
                                        .eyeHeight(1.0F)
                                        .clientTrackingRange(63)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id(
                                                                "entity_balls_o_tron_seg"))));
        Registrar.registerLivingAttributes(BALLS_O_TRON_SEG, BOTPrimeBodyEntity::createAttributes);
        HUNTER_CHOPPER =
                Reg.entity(
                        "entity_mob_hunter_chopper",
                        () ->
                                EntityType.Builder.<HunterChopperEntity>of(
                                                HunterChopperEntity::new, MobCategory.MONSTER)
                                        .notInPeaceful()
                                        .noLootTable()
                                        .fireImmune()
                                        .sized(8.25F, 3.0F)
                                        .clientTrackingRange(5)
                                        .updateInterval(3)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id(
                                                                "entity_mob_hunter_chopper"))));
        Registrar.registerLivingAttributes(HUNTER_CHOPPER, HunterChopperEntity::createAttributes);
        TNT_NTM =
                Reg.entity(
                        "tnt",
                        () ->
                                EntityType.Builder.<TntNtmEntity>of(
                                                TntNtmEntity::new, MobCategory.MISC)
                                        .noLootTable()
                                        .fireImmune()
                                        .sized(0.98F, 0.98F)
                                        .eyeHeight(0.15F)
                                        .clientTrackingRange(63)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id("tnt"))));
        FALLING_BLOCK_NT =
                Reg.entity(
                        "entity_falling_block",
                        () ->
                                EntityType.Builder.of(FallingBlockNTEntity::new, MobCategory.MISC)
                                        .noLootTable()
                                        .sized(0.98F, 0.98F)
                                        .clientTrackingRange(63)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id("entity_falling_block"))));
        FALLING_BLOCK_BATCH =
                Reg.entity(
                        "entity_falling_block_batch",
                        () ->
                                EntityType.Builder.of(
                                                FallingBlockBatchEntity::new, MobCategory.MISC)
                                        .noLootTable()
                                        .sized(0.01F, 0.01F)
                                        .clientTrackingRange(63)
                                        .updateInterval(Integer.MAX_VALUE)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id(
                                                                "entity_falling_block_batch"))));
        FALLING_MULTIBLOCK =
                Reg.entity(
                        "entity_falling_multiblock",
                        () ->
                                EntityType.Builder.of(
                                                FallingMultiblockEntity::new, MobCategory.MISC)
                                        .noLootTable()
                                        .sized(0.98F, 0.98F)
                                        .clientTrackingRange(64)
                                        .updateInterval(20)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id(
                                                                "entity_falling_multiblock"))));

        C130 =
                Reg.entity(
                        "c130",
                        () ->
                                EntityType.Builder.of(C130Entity::new, MobCategory.MISC)
                                        .noLootTable()
                                        .sized(8.0F, 4.0F)
                                        .clientTrackingRange(63)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id("c130"))));
        BOMBER =
                Reg.entity(
                        "entity_bomber",
                        () ->
                                EntityType.Builder.of(BomberEntity::new, MobCategory.MISC)
                                        .noLootTable()
                                        .sized(8.0F, 4.0F)
                                        .clientTrackingRange(63)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id("entity_bomber"))));
        BOMBLET_ZETA =
                Reg.entity(
                        "entity_zeta",
                        () ->
                                EntityType.Builder.of(BombletZetaEntity::new, MobCategory.MISC)
                                        .noLootTable()
                                        .noSave()
                                        .sized(0.25F, 0.25F)
                                        .clientTrackingRange(63)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id("entity_zeta"))));
        GRENADE_UNIVERSAL =
                Reg.entity(
                        "grenade_universal",
                        () ->
                                EntityType.Builder.<GrenadeUniversalEntity>of(
                                                GrenadeUniversalEntity::new, MobCategory.MISC)
                                        .noLootTable()
                                        .sized(0.25F, 0.25F)
                                        .clientTrackingRange(16)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id("grenade_universal"))));
        WASTE_PEARL =
                Reg.entity(
                        "entity_waste_pearl",
                        () ->
                                EntityType.Builder.<WastePearlEntity>of(
                                                WastePearlEntity::new, MobCategory.MISC)
                                        .noLootTable()
                                        .sized(0.25F, 0.25F)
                                        .clientTrackingRange(63)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id("entity_waste_pearl"))));
        GRENADE_BOUNCY_GENERIC =
                Reg.entity(
                        "grenade_bouncy_generic",
                        () ->
                                EntityType.Builder.<GrenadeBouncyGenericEntity>of(
                                                GrenadeBouncyGenericEntity::new, MobCategory.MISC)
                                        .noLootTable()
                                        .sized(0.25F, 0.25F)
                                        .clientTrackingRange(16)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id(
                                                                "grenade_bouncy_generic"))));
        DISPERSER_CANISTER =
                Reg.entity(
                        "disperser_canister",
                        () ->
                                EntityType.Builder.<DisperserCanisterEntity>of(
                                                DisperserCanisterEntity::new, MobCategory.MISC)
                                        .noLootTable()
                                        .sized(0.25F, 0.25F)
                                        .clientTrackingRange(16)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id("disperser_canister"))));
        ITEM_BUOYANT =
                Reg.entity(
                        "item_buoyant",
                        () ->
                                EntityType.Builder.<ItemBuoyantEntity>of(
                                                ItemBuoyantEntity::new, MobCategory.MISC)
                                        .sized(0.25F, 0.25F)
                                        .clientTrackingRange(7)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id("item_buoyant"))));
        NUKE_EXPLOSION_MK3 =
                Reg.entity(
                        "nuke_mk3",
                        () ->
                                EntityType.Builder.of(NukeExplosionMK3Entity::new, MobCategory.MISC)
                                        .noLootTable()
                                        .fireImmune()
                                        .sized(1.0F, 1.0F)
                                        .clientTrackingRange(0)
                                        .updateInterval(Integer.MAX_VALUE)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id("nuke_mk3"))));
        CLOUD_FLEIJA =
                Reg.entity(
                        "cloud_fleija",
                        () ->
                                EntityType.Builder.of(CloudFleijaEntity::new, MobCategory.MISC)
                                        .noLootTable()
                                        .fireImmune()
                                        .sized(20.0F, 40.0F)
                                        .clientTrackingRange(32)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id("cloud_fleija"))));
        CLOUD_SOLINIUM =
                Reg.entity(
                        "cloud_solinium",
                        () ->
                                EntityType.Builder.of(CloudSoliniumEntity::new, MobCategory.MISC)
                                        .noLootTable()
                                        .fireImmune()
                                        .sized(20.0F, 40.0F)
                                        .clientTrackingRange(63)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id("cloud_solinium"))));
        CLOUD_RAINBOW =
                Reg.entity(
                        "cloud_rainbow",
                        () ->
                                EntityType.Builder.of(
                                                CloudFleijaRainbowEntity::new, MobCategory.MISC)
                                        .noLootTable()
                                        .fireImmune()
                                        .sized(20.0F, 40.0F)
                                        .clientTrackingRange(63)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id("cloud_rainbow"))));
        PARACHUTE_CRATE =
                Reg.entity(
                        "parachute_crate",
                        () ->
                                EntityType.Builder.of(ParachuteCrateEntity::new, MobCategory.MISC)
                                        .noLootTable()
                                        .fireImmune()
                                        .sized(1.0F, 1.0F)
                                        .clientTrackingRange(63)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id("parachute_crate"))));
        BOXCAR =
                Reg.entity(
                        "boxcar",
                        () ->
                                EntityType.Builder.of(BoxcarEntity::new, MobCategory.MISC)
                                        .noLootTable()
                                        .fireImmune()
                                        .sized(0.25F, 0.25F)
                                        .clientTrackingRange(63)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id("boxcar"))));
        TORPEDO =
                Reg.entity(
                        "torpedo",
                        () ->
                                EntityType.Builder.of(TorpedoEntity::new, MobCategory.MISC)
                                        .noLootTable()
                                        .fireImmune()
                                        .sized(0.25F, 0.25F)
                                        .clientTrackingRange(63)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id("torpedo"))));
        DUCHESS_GAMBIT =
                Reg.entity(
                        "duchessgambit",
                        () ->
                                EntityType.Builder.of(DuchessGambitEntity::new, MobCategory.MISC)
                                        .noLootTable()
                                        .fireImmune()
                                        .sized(0.25F, 0.25F)
                                        .clientTrackingRange(63)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id("duchessgambit"))));
        FALLING_BUILDING =
                Reg.entity(
                        "falling_building",
                        () ->
                                EntityType.Builder.of(BuildingEntity::new, MobCategory.MISC)
                                        .noLootTable()
                                        .fireImmune()
                                        .sized(0.25F, 0.25F)
                                        .clientTrackingRange(63)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id("falling_building"))));
        CHEMICAL =
                Reg.entity(
                        "chemical",
                        () ->
                                EntityType.Builder.<ChemicalEntity>of(
                                                ChemicalEntity::new, MobCategory.MISC)
                                        .noLootTable()
                                        .noSave()
                                        .fireImmune()
                                        .sized(0.25F, 0.25F)
                                        .clientTrackingRange(63)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id("chemical"))));

        BOBMAZON_DELIVERY =
                Reg.entity(
                        "entity_bobmazon_delivery",
                        () ->
                                EntityType.Builder.<BobmazonEntity>of(
                                                BobmazonEntity::new, MobCategory.MISC)
                                        .noLootTable()
                                        .sized(1.0F, 3.0F)
                                        .clientTrackingRange(63)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id(
                                                                "entity_bobmazon_delivery"))));

        SATELLITE_POD =
                Reg.entity(
                        "entity_miner_lander",
                        () ->
                                EntityType.Builder.<SatellitePod>of(
                                                SatellitePod::new, MobCategory.MISC)
                                        .noLootTable()
                                        .fireImmune()
                                        .sized(0.95F, 3.75F)
                                        .clientTrackingRange(63)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id("entity_miner_lander"))));

        MISSILE_ANTI =
                Reg.entity(
                        "entity_missile_anti",
                        () ->
                                EntityType.Builder.<MissileAntiBallisticEntity>of(
                                                MissileAntiBallisticEntity::new, MobCategory.MISC)
                                        .noLootTable()
                                        .sized(1.5F, 1.5F)
                                        .clientTrackingRange(63)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id("entity_missile_anti"))));

        BLACK_HOLE =
                Reg.entity(
                        "entity_black_hole",
                        () ->
                                EntityType.Builder.<BlackHoleEntity>of(
                                                BlackHoleEntity::new, MobCategory.MISC)
                                        .noLootTable()
                                        .fireImmune()
                                        .sized(0.6F, 1.8F)
                                        .clientTrackingRange(16)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id("entity_black_hole"))));

        DIGAMMA_QUASAR =
                Reg.entity(
                        "entity_digamma_quasar",
                        () ->
                                EntityType.Builder.<QuasarEntity>of(
                                                QuasarEntity::new, MobCategory.MISC)
                                        .noLootTable()
                                        .sized(0.6F, 1.8F)
                                        .clientTrackingRange(16)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id("entity_digamma_quasar"))));

        EMP_BLAST =
                Reg.entity(
                        "entity_emp_blast",
                        () ->
                                EntityType.Builder.<EMPBlastEntity>of(
                                                EMPBlastEntity::new, MobCategory.MISC)
                                        .noLootTable()
                                        .sized(1.5F, 1.5F)
                                        .clientTrackingRange(63)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id("entity_emp_blast"))));

        EMP_LOGIC =
                Reg.entity(
                        "entity_emp_logic",
                        () ->
                                EntityType.Builder.<EMPEntity>of(EMPEntity::new, MobCategory.MISC)
                                        .noLootTable()
                                        .sized(0.6F, 1.8F)
                                        .clientTrackingRange(63)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id("entity_emp_logic"))));

        BALEFIRE =
                Reg.entity(
                        "entity_balefire",
                        () ->
                                EntityType.Builder.<BalefireEntity>of(
                                                BalefireEntity::new, MobCategory.MISC)
                                        .noLootTable()
                                        .sized(0.6F, 1.8F)
                                        .clientTrackingRange(63)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id("entity_balefire"))));

        LASER_BLAST =
                Reg.entity(
                        "entity_laser_blast",
                        () ->
                                EntityType.Builder.<DeathBlastEntity>of(
                                                DeathBlastEntity::new, MobCategory.MISC)
                                        .noLootTable()
                                        .sized(0.6F, 1.8F)
                                        .clientTrackingRange(63)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id("entity_laser_blast"))));
        ORBITAL_LASER =
                Reg.entity(
                        "entity_orbital_laser",
                        () ->
                                EntityType.Builder.<OrbitalLaserEntity>of(
                                                OrbitalLaserEntity::new, MobCategory.MISC)
                                        .noLootTable()
                                        .sized(0.6F, 1.8F)
                                        .clientTrackingRange(63)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id("entity_orbital_laser"))));

        TOM_BUST =
                Reg.entity(
                        "entity_tom_bust",
                        () ->
                                EntityType.Builder.<TomBlastEntity>of(
                                                TomBlastEntity::new, MobCategory.MISC)
                                        .noLootTable()
                                        .sized(0.6F, 1.8F)
                                        .clientTrackingRange(63)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id("entity_tom_bust"))));

        RAGING_VORTEX =
                Reg.entity(
                        "entity_raging_vortex",
                        () ->
                                EntityType.Builder.<RagingVortexEntity>of(
                                                RagingVortexEntity::new, MobCategory.MISC)
                                        .noLootTable()
                                        .fireImmune()
                                        .sized(0.6F, 1.8F)
                                        .clientTrackingRange(16)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id("entity_raging_vortex"))));

        MOONSTONE_BLAST =
                Reg.entity(
                        "entity_moonstone_blast",
                        () ->
                                EntityType.Builder.<CloudTomEntity>of(
                                                CloudTomEntity::new, MobCategory.MISC)
                                        .noLootTable()
                                        .fireImmune()
                                        .sized(1.0F, 4.0F)
                                        .clientTrackingRange(63)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id(
                                                                "entity_moonstone_blast"))));

        ARTILLERY_SHELL =
                Reg.entity(
                        "entity_artillery_shell",
                        () ->
                                EntityType.Builder.<ArtilleryShellEntity>of(
                                                ArtilleryShellEntity::new, MobCategory.MISC)
                                        .noLootTable()
                                        .sized(0.5F, 0.5F)
                                        .clientTrackingRange(63)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id(
                                                                "entity_artillery_shell"))));

        HIMARS =
                Reg.entity(
                        "entity_himars",
                        () ->
                                EntityType.Builder.<ArtilleryRocketEntity>of(
                                                ArtilleryRocketEntity::new, MobCategory.MISC)
                                        .noLootTable()
                                        .sized(0.25F, 0.25F)
                                        .clientTrackingRange(63)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id("entity_himars"))));

        BEAM_BOMB =
                Reg.entity(
                        "entity_beam_bomb",
                        () ->
                                EntityType.Builder.<B92BeamEntity>of(
                                                B92BeamEntity::new, MobCategory.MISC)
                                        .noLootTable()
                                        .sized(0.5F, 0.5F)
                                        .clientTrackingRange(63)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id("entity_beam_bomb"))));

        BULLET =
                Reg.entity(
                        "entity_bullet",
                        () ->
                                EntityType.Builder.<BulletEntity>of(
                                                BulletEntity::new, MobCategory.MISC)
                                        .noLootTable()
                                        .sized(0.5F, 0.5F)
                                        .clientTrackingRange(16)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id("entity_bullet"))));

        BURNING_FOEQ =
                Reg.entity(
                        "entity_burning_foeq",
                        () ->
                                EntityType.Builder.<BurningFOEQEntity>of(
                                                BurningFOEQEntity::new, MobCategory.MISC)
                                        .noLootTable()
                                        .sized(0.25F, 0.25F)
                                        .clientTrackingRange(63)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id("entity_burning_foeq"))));

        FALLING_BOMB =
                Reg.entity(
                        "entity_falling_bomb",
                        () ->
                                EntityType.Builder.<FallingNukeEntity>of(
                                                FallingNukeEntity::new, MobCategory.MISC)
                                        .noLootTable()
                                        .sized(0.98F, 0.98F)
                                        .clientTrackingRange(63)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id("entity_falling_bomb"))));

        SIEGE_LASER =
                Reg.entity(
                        "entity_siege_laser",
                        () ->
                                EntityType.Builder.<SiegeLaserEntity>of(
                                                SiegeLaserEntity::new, MobCategory.MISC)
                                        .noLootTable()
                                        .sized(0.25F, 0.25F)
                                        .clientTrackingRange(63)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id("entity_siege_laser"))));

        STRAY_SAW =
                Reg.entity(
                        "entity_stray_saw",
                        () ->
                                EntityType.Builder.<SawbladeEntity>of(
                                                SawbladeEntity::new, MobCategory.MISC)
                                        .noLootTable()
                                        .sized(1.0F, 1.0F)
                                        .clientTrackingRange(63)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id("entity_stray_saw"))));

        TOM_THE_MOONSTONE =
                Reg.entity(
                        "entity_tom_the_moonstone",
                        () ->
                                EntityType.Builder.<TomEntity>of(TomEntity::new, MobCategory.MISC)
                                        .noLootTable()
                                        .sized(0.25F, 0.25F)
                                        .clientTrackingRange(63)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id(
                                                                "entity_tom_the_moonstone"))));

        SOYUZ =
                Reg.entity(
                        "entity_soyuz",
                        () ->
                                EntityType.Builder.of(SoyuzEntity::new, MobCategory.MISC)
                                        .noLootTable()
                                        .fireImmune()
                                        .sized(5.0F, 50.0F)
                                        .clientTrackingRange(63)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id("entity_soyuz"))));
        SOYUZ_CAPSULE =
                Reg.entity(
                        "entity_soyuz_capsule",
                        () ->
                                EntityType.Builder.of(SoyuzCapsule::new, MobCategory.MISC)
                                        .noLootTable()
                                        .fireImmune()
                                        .sized(1.0F, 1.0F)
                                        .clientTrackingRange(63)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id("entity_soyuz_capsule"))));
        CHOPPER_MINE =
                Reg.entity(
                        "entity_chopper_mine",
                        () ->
                                EntityType.Builder.<ChopperMineEntity>of(
                                                ChopperMineEntity::new, MobCategory.MISC)
                                        .noLootTable()
                                        .fireImmune()
                                        .sized(12.0F, 12.0F)
                                        .clientTrackingRange(63)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id("entity_chopper_mine"))));
    }

    private static void spawn(
            RegistryHandle<? extends EntityType<?>> type,
            BiomeTarget biomes,
            int weight,
            int min,
            int max) {
        BiomeInjection.addSpawn(biomes, NO_DEFAULT_MONSTERS, type, weight, min, max);
    }

    private static TagKey<Biome> convention(String path) {
        return TagKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath("c", path));
    }

    private static <T extends Missile> Reg.EntityHandle<T> missile(
            String name, EntityType.EntityFactory<T> factory) {
        return Reg.entity(
                name,
                () ->
                        EntityType.Builder.of(factory, MobCategory.MISC)
                                .noLootTable()
                                .sized(1.5F, 1.5F)
                                .clientTrackingRange(63)
                                .updateInterval(1)
                                .build(
                                        ResourceKey.create(
                                                Registries.ENTITY_TYPE, RegistryUtil.id(name))));
    }

    private static Reg.EntityHandle<StandardMissile> standard(
            String name, Supplier<MissileVariant> variant) {
        return missile(name, (type, level) -> new StandardMissile(type, level, variant.get()));
    }
}
