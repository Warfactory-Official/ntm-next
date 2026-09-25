// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity;

import com.hbm.entity.cart.EntityMinecartCrate;
import com.hbm.entity.cart.EntityMinecartDestroyer;
import com.hbm.entity.cart.EntityMinecartOre;
import com.hbm.entity.cart.EntityMinecartPowder;
import com.hbm.entity.cart.EntityMinecartSemtex;
import com.hbm.entity.effect.*;
import com.hbm.entity.grenade.EntityDisperserCanister;
import com.hbm.entity.grenade.EntityGrenadeBouncyGeneric;
import com.hbm.entity.grenade.EntityGrenadeUniversal;
import com.hbm.entity.grenade.EntityWastePearl;
import com.hbm.entity.item.EntityBoatRubber;
import com.hbm.entity.item.EntityDeliveryDrone;
import com.hbm.entity.item.EntityFallingBlockNT;
import com.hbm.entity.item.EntityFallingMultiblock;
import com.hbm.entity.item.EntityFireworks;
import com.hbm.entity.item.EntityItemBuoyant;
import com.hbm.entity.item.EntityItemWaste;
import com.hbm.entity.item.EntityMinecartTest;
import com.hbm.entity.item.EntityMovingItem;
import com.hbm.entity.item.EntityMovingItem;
import com.hbm.entity.item.EntityMovingPackage;
import com.hbm.entity.item.EntityMovingPackage;
import com.hbm.entity.item.EntityParachuteCrate;
import com.hbm.entity.item.EntityRequestDrone;
import com.hbm.entity.item.EntityTntNtm;
import com.hbm.entity.logic.EntityBalefire;
import com.hbm.entity.logic.EntityBomber;
import com.hbm.entity.logic.EntityC130;
import com.hbm.entity.logic.EntityDeathBlast;
import com.hbm.entity.logic.EntityEMP;
import com.hbm.entity.logic.EntityNukeExplosionMK3;
import com.hbm.entity.logic.EntityNukeExplosionMK5;
import com.hbm.entity.logic.EntityOrbitalLaser;
import com.hbm.entity.logic.EntityTomBlast;
import com.hbm.entity.logic.EntityWaypoint;
import com.hbm.entity.missile.EntityBobmazon;
import com.hbm.entity.missile.EntityMissileAntiBallistic;
import com.hbm.entity.missile.EntityMissileBaseNT;
import com.hbm.entity.missile.EntityMissileCustom;
import com.hbm.entity.missile.EntityMissileShuttle;
import com.hbm.entity.missile.EntityMissileStealth;
import com.hbm.entity.missile.EntityMissileTier0.EntityMissileBHole;
import com.hbm.entity.missile.EntityMissileTier0.EntityMissileEMP;
import com.hbm.entity.missile.EntityMissileTier0.EntityMissileMicro;
import com.hbm.entity.missile.EntityMissileTier0.EntityMissileSchrabidium;
import com.hbm.entity.missile.EntityMissileTier0.EntityMissileTaint;
import com.hbm.entity.missile.EntityMissileTier0.EntityMissileTest;
import com.hbm.entity.missile.EntityMissileTier1.EntityMissileBunkerBuster;
import com.hbm.entity.missile.EntityMissileTier1.EntityMissileCluster;
import com.hbm.entity.missile.EntityMissileTier1.EntityMissileDecoy;
import com.hbm.entity.missile.EntityMissileTier1.EntityMissileGeneric;
import com.hbm.entity.missile.EntityMissileTier1.EntityMissileIncendiary;
import com.hbm.entity.missile.EntityMissileTier2.EntityMissileBusterStrong;
import com.hbm.entity.missile.EntityMissileTier2.EntityMissileClusterStrong;
import com.hbm.entity.missile.EntityMissileTier2.EntityMissileEMPStrong;
import com.hbm.entity.missile.EntityMissileTier2.EntityMissileIncendiaryStrong;
import com.hbm.entity.missile.EntityMissileTier2.EntityMissileStrong;
import com.hbm.entity.missile.EntityMissileTier3.EntityMissileBurst;
import com.hbm.entity.missile.EntityMissileTier3.EntityMissileDrill;
import com.hbm.entity.missile.EntityMissileTier3.EntityMissileInferno;
import com.hbm.entity.missile.EntityMissileTier3.EntityMissileRain;
import com.hbm.entity.missile.EntityMissileTier4.EntityMissileDoomsday;
import com.hbm.entity.missile.EntityMissileTier4.EntityMissileDoomsdayRusted;
import com.hbm.entity.missile.EntityMissileTier4.EntityMissileMirv;
import com.hbm.entity.missile.EntityMissileTier4.EntityMissileNuclear;
import com.hbm.entity.missile.EntityMissileTier4.EntityMissileVolcano;
import com.hbm.entity.missile.EntitySatellitePod;
import com.hbm.entity.missile.EntitySoyuz;
import com.hbm.entity.missile.EntitySoyuzCapsule;
import com.hbm.entity.mob.EntityBlockSpider;
import com.hbm.entity.mob.EntityCreeperGold;
import com.hbm.entity.mob.EntityCreeperNuclear;
import com.hbm.entity.mob.EntityCreeperPhosgene;
import com.hbm.entity.mob.EntityCreeperTainted;
import com.hbm.entity.mob.EntityCreeperVolatile;
import com.hbm.entity.mob.EntityCyberCrab;
import com.hbm.entity.mob.EntityDuck;
import com.hbm.entity.mob.EntityDummy;
import com.hbm.entity.mob.EntityFBI;
import com.hbm.entity.mob.EntityFBIDrone;
import com.hbm.entity.mob.EntityGhost;
import com.hbm.entity.mob.EntityHunterChopper;
import com.hbm.entity.mob.EntityMaskMan;
import com.hbm.entity.mob.EntityParasiteMaggot;
import com.hbm.entity.mob.EntityPigeon;
import com.hbm.entity.mob.EntityPlasticBag;
import com.hbm.entity.mob.EntityQuackos;
import com.hbm.entity.mob.EntityRADBeast;
import com.hbm.entity.mob.EntityTaintCrab;
import com.hbm.entity.mob.EntityTeslaCrab;
import com.hbm.entity.mob.EntityUFO;
import com.hbm.entity.mob.EntityUndeadSoldier;
import com.hbm.entity.mob.botprime.EntityBOTPrimeBody;
import com.hbm.entity.mob.botprime.EntityBOTPrimeHead;
import com.hbm.entity.mob.glyphid.EntityGlyphid;
import com.hbm.entity.mob.glyphid.EntityGlyphidBehemoth;
import com.hbm.entity.mob.glyphid.EntityGlyphidBlaster;
import com.hbm.entity.mob.glyphid.EntityGlyphidBombardier;
import com.hbm.entity.mob.glyphid.EntityGlyphidBrawler;
import com.hbm.entity.mob.glyphid.EntityGlyphidBrenda;
import com.hbm.entity.mob.glyphid.EntityGlyphidDigger;
import com.hbm.entity.mob.glyphid.EntityGlyphidNuclear;
import com.hbm.entity.mob.glyphid.EntityGlyphidScout;
import com.hbm.entity.particle.EntityChlorineFX;
import com.hbm.entity.particle.EntityCloudFX;
import com.hbm.entity.particle.EntityOrangeFX;
import com.hbm.entity.particle.EntityPinkCloudFX;
import com.hbm.entity.projectile.EntityAcidBomb;
import com.hbm.entity.projectile.EntityArtilleryRocket;
import com.hbm.entity.projectile.EntityArtilleryShell;
import com.hbm.entity.projectile.EntityB92Beam;
import com.hbm.entity.projectile.EntityBombletZeta;
import com.hbm.entity.projectile.EntityBoxcar;
import com.hbm.entity.projectile.EntityBuilding;
import com.hbm.entity.projectile.EntityBullet;
import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.entity.projectile.EntityBulletBeamBase;
import com.hbm.entity.projectile.EntityBurningFOEQ;
import com.hbm.entity.projectile.EntityChemical;
import com.hbm.entity.projectile.EntityChopperMine;
import com.hbm.entity.projectile.EntityCog;
import com.hbm.entity.projectile.EntityCoin;
import com.hbm.entity.projectile.EntityDuchessGambit;
import com.hbm.entity.projectile.EntityFallingNuke;
import com.hbm.entity.projectile.EntityMeteor;
import com.hbm.entity.projectile.EntityRBMKDebris;
import com.hbm.entity.projectile.EntityRubble;
import com.hbm.entity.projectile.EntitySawblade;
import com.hbm.entity.projectile.EntityShrapnel;
import com.hbm.entity.projectile.EntitySiegeLaser;
import com.hbm.entity.projectile.EntityTom;
import com.hbm.entity.projectile.EntityTorpedo;
import com.hbm.entity.projectile.EntityZirnoxDebris;
import com.hbm.entity.train.EntityRailCarBase;
import com.hbm.entity.train.EntityRailCarRidable;
import com.hbm.entity.train.TrainCargoTram;
import com.hbm.entity.train.TrainCargoTramTrailer;
import com.hbm.lib.Library;
import com.hbm.platform.Services;
import com.hbm.registration.IRegistrar;
import com.hbm.registration.Reg;
import com.hbm.registration.RegistryHandle;
import com.hbm.tags.HbmEntityTypeTags;
import com.hbm.world.BiomeTarget;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.EntityTypeTags;
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

    public static RegistryHandle<EntityType<EntityDischargeBeam>> BEAM_DISCHARGE;
    public static RegistryHandle<EntityType<EntityNukeExplosionMK5>> NUKE_EXPLOSION_MK5;
    public static RegistryHandle<EntityType<EntityFalloutRain>> FALLOUT_RAIN;
    public static RegistryHandle<EntityType<EntityNukeTorex>> NUKE_TOREX;
    public static RegistryHandle<EntityType<EntityVortex>> VORTEX;
    public static RegistryHandle<EntityType<EntityUndeadSoldier>> UNDEAD_SOLDIER;
    public static RegistryHandle<EntityType<EntityFBI>> FBI;

    public static RegistryHandle<EntityType<EntityCreeperGold>> CREEPER_GOLD;
    public static RegistryHandle<EntityType<EntityCreeperVolatile>> CREEPER_VOLATILE;
    public static RegistryHandle<EntityType<EntityCreeperPhosgene>> CREEPER_PHOSGENE;
    public static RegistryHandle<EntityType<EntityCreeperTainted>> CREEPER_TAINTED;
    public static RegistryHandle<EntityType<EntityCreeperNuclear>> CREEPER_NUCLEAR;

    public static RegistryHandle<EntityType<EntityCyberCrab>> CYBER_CRAB;
    public static RegistryHandle<EntityType<EntityTeslaCrab>> TESLA_CRAB;
    public static RegistryHandle<EntityType<EntityTaintCrab>> TAINT_CRAB;

    public static RegistryHandle<EntityType<EntityDuck>> DUCK;
    public static RegistryHandle<EntityType<EntityGhost>> GHOST;
    public static RegistryHandle<EntityType<EntityDummy>> TEST_DUMMY;
    public static RegistryHandle<EntityType<EntityBlockSpider>> BLOCK_SPIDER;
    public static RegistryHandle<EntityType<EntityQuackos>> QUACKOS;
    public static RegistryHandle<EntityType<EntityPlasticBag>> PLASTIC_BAG;
    public static RegistryHandle<EntityType<EntityPigeon>> PIGEON;

    public static RegistryHandle<EntityType<EntityGlyphid>> GLYPHID;
    public static RegistryHandle<EntityType<EntityGlyphidBrawler>> GLYPHID_BRAWLER;
    public static RegistryHandle<EntityType<EntityGlyphidBehemoth>> GLYPHID_BEHEMOTH;
    public static RegistryHandle<EntityType<EntityGlyphidBrenda>> GLYPHID_BRENDA;
    public static RegistryHandle<EntityType<EntityGlyphidBombardier>> GLYPHID_BOMBARDIER;
    public static RegistryHandle<EntityType<EntityGlyphidBlaster>> GLYPHID_BLASTER;
    public static RegistryHandle<EntityType<EntityGlyphidScout>> GLYPHID_SCOUT;
    public static RegistryHandle<EntityType<EntityGlyphidNuclear>> GLYPHID_NUCLEAR;
    public static RegistryHandle<EntityType<EntityGlyphidDigger>> GLYPHID_DIGGER;
    public static RegistryHandle<EntityType<EntityWaypoint>> GLYPHID_WAYPOINT;
    public static RegistryHandle<EntityType<EntityAcidBomb>> ACID_BOMB;
    public static RegistryHandle<EntityType<EntityParasiteMaggot>> PARASITE_MAGGOT;

    public static RegistryHandle<EntityType<EntityMissileGeneric>> MISSILE_GENERIC;
    public static RegistryHandle<EntityType<EntityMissileIncendiary>> MISSILE_INCENDIARY;
    public static RegistryHandle<EntityType<EntityMissileDecoy>> MISSILE_DECOY;
    public static RegistryHandle<EntityType<EntityMissileBunkerBuster>> MISSILE_BUSTER;
    public static RegistryHandle<EntityType<EntityMissileStrong>> MISSILE_STRONG;
    public static RegistryHandle<EntityType<EntityMissileIncendiaryStrong>>
            MISSILE_INCENDIARY_STRONG;
    public static RegistryHandle<EntityType<EntityMissileBusterStrong>> MISSILE_BUSTER_STRONG;
    public static RegistryHandle<EntityType<EntityMissileBurst>> MISSILE_BURST;
    public static RegistryHandle<EntityType<EntityMissileInferno>> MISSILE_INFERNO;
    public static RegistryHandle<EntityType<EntityMissileNuclear>> MISSILE_NUCLEAR;
    public static RegistryHandle<EntityType<EntityMissileMirv>> MISSILE_NUCLEAR_CLUSTER;
    public static RegistryHandle<EntityType<EntityMissileStealth>> MISSILE_STEALTH;
    public static RegistryHandle<EntityType<EntityMissileCustom>> MISSILE_CUSTOM;
    public static RegistryHandle<EntityType<EntityMissileShuttle>> MISSILE_SHUTTLE;
    public static RegistryHandle<EntityType<EntityMissileTest>> MISSILE_TEST;
    public static RegistryHandle<EntityType<EntityMissileMicro>> MISSILE_MICRO;
    public static RegistryHandle<EntityType<EntityMissileSchrabidium>> MISSILE_SCHRABIDIUM;
    public static RegistryHandle<EntityType<EntityMissileBHole>> MISSILE_BHOLE;
    public static RegistryHandle<EntityType<EntityMissileTaint>> MISSILE_TAINT;
    public static RegistryHandle<EntityType<EntityMissileEMP>> MISSILE_EMP;
    public static RegistryHandle<EntityType<EntityMissileCluster>> MISSILE_CLUSTER;
    public static RegistryHandle<EntityType<EntityMissileClusterStrong>> MISSILE_CLUSTER_STRONG;
    public static RegistryHandle<EntityType<EntityMissileEMPStrong>> MISSILE_EMP_STRONG;
    public static RegistryHandle<EntityType<EntityMissileRain>> MISSILE_RAIN;
    public static RegistryHandle<EntityType<EntityMissileDrill>> MISSILE_DRILL;
    public static RegistryHandle<EntityType<EntityMissileVolcano>> MISSILE_VOLCANO;
    public static RegistryHandle<EntityType<EntityMissileDoomsday>> MISSILE_DOOMSDAY;
    public static RegistryHandle<EntityType<EntityMissileDoomsdayRusted>> MISSILE_DOOMSDAY_RUSTED;

    public static RegistryHandle<EntityType<EntityShrapnel>> SHRAPNEL;
    public static RegistryHandle<EntityType<EntityBulletBaseMK4>> BULLET_MK4;
    public static RegistryHandle<EntityType<EntityBulletBeamBase>> BULLET_BEAM;
    public static RegistryHandle<EntityType<EntityFireLingering>> FIRE_LINGERING;
    public static RegistryHandle<EntityType<EntityCoin>> COIN;
    public static RegistryHandle<EntityType<EntityRubble>> RUBBLE;
    public static RegistryHandle<EntityType<EntityCog>> COG;
    public static RegistryHandle<EntityType<EntityRBMKDebris>> RBMK_DEBRIS;
    public static RegistryHandle<EntityType<EntityZirnoxDebris>> ZIRNOX_DEBRIS;
    public static RegistryHandle<EntityType<EntitySpear>> SPEAR;
    public static RegistryHandle<EntityType<EntityItemWaste>> WASTE_ITEM;
    public static RegistryHandle<EntityType<EntityMist>> MIST;
    public static RegistryHandle<EntityType<EntityOrangeFX>> AGENT_ORANGE;
    public static RegistryHandle<EntityType<EntityFireworks>> FIREWORK_BALL;
    public static RegistryHandle<EntityType<EntityMinecartOre>> CART_ORE;
    public static RegistryHandle<EntityType<EntityMinecartCrate>> CART_CRATE;
    public static RegistryHandle<EntityType<TrainCargoTram>> CARGO_TRAM;
    public static RegistryHandle<EntityType<TrainCargoTramTrailer>> CARGO_TRAM_TRAILER;
    public static RegistryHandle<EntityType<EntityRailCarBase.BoundingBoxDummyEntity>>
            BOUNDING_DUMMY;
    public static RegistryHandle<EntityType<EntityRailCarRidable.SeatDummyEntity>> SEAT_DUMMY;
    public static RegistryHandle<EntityType<EntityMinecartDestroyer>> CART_DESTROYER;
    public static RegistryHandle<EntityType<EntityMinecartTest>> CART_TEST;
    public static RegistryHandle<EntityType<EntityBoatRubber>> BOAT_RUBBER;
    public static RegistryHandle<EntityType<EntityMovingItem>> MOVING_ITEM;
    public static RegistryHandle<EntityType<EntityMovingPackage>> MOVING_PACKAGE;
    public static RegistryHandle<EntityType<EntityDeliveryDrone>> DELIVERY_DRONE;
    public static RegistryHandle<EntityType<EntityRequestDrone>> REQUEST_DRONE;
    public static RegistryHandle<EntityType<EntityFBIDrone>> FBI_DRONE;
    public static RegistryHandle<EntityType<EntityRADBeast>> RAD_BEAST;
    public static RegistryHandle<EntityType<EntityMaskMan>> MASK_MAN;
    public static RegistryHandle<EntityType<EntityUFO>> UFO;
    public static RegistryHandle<EntityType<EntityBOTPrimeHead>> BALLS_O_TRON;
    public static RegistryHandle<EntityType<EntityBOTPrimeBody>> BALLS_O_TRON_SEG;
    public static RegistryHandle<EntityType<EntityHunterChopper>> HUNTER_CHOPPER;
    public static RegistryHandle<EntityType<EntityMinecartPowder>> CART_POWDER;
    public static RegistryHandle<EntityType<EntityMinecartSemtex>> CART_SEMTEX;
    public static RegistryHandle<EntityType<EntityChlorineFX>> CHLORINE_FX;
    public static RegistryHandle<EntityType<EntityCloudFX>> CLOUD_FX;
    public static RegistryHandle<EntityType<EntityPinkCloudFX>> PINK_CLOUD_FX;
    public static RegistryHandle<EntityType<EntityMeteor>> METEOR;

    public static RegistryHandle<EntityType<EntityTntNtm>> TNT_NTM;
    public static RegistryHandle<EntityType<EntityFallingBlockNT>> FALLING_BLOCK_NT;
    public static RegistryHandle<EntityType<EntityFallingMultiblock>> FALLING_MULTIBLOCK;

    public static RegistryHandle<EntityType<EntityC130>> C130;
    public static RegistryHandle<EntityType<EntityBomber>> BOMBER;
    public static RegistryHandle<EntityType<EntityBombletZeta>> BOMBLET_ZETA;
    public static RegistryHandle<EntityType<EntityGrenadeUniversal>> GRENADE_UNIVERSAL;
    public static RegistryHandle<EntityType<EntityGrenadeBouncyGeneric>> GRENADE_BOUNCY_GENERIC;
    public static RegistryHandle<EntityType<EntityDisperserCanister>> DISPERSER_CANISTER;
    public static RegistryHandle<EntityType<EntityWastePearl>> WASTE_PEARL;
    public static RegistryHandle<EntityType<EntityItemBuoyant>> ITEM_BUOYANT;
    public static RegistryHandle<EntityType<EntityNukeExplosionMK3>> NUKE_EXPLOSION_MK3;
    public static RegistryHandle<EntityType<EntityCloudFleija>> CLOUD_FLEIJA;
    public static RegistryHandle<EntityType<EntityCloudSolinium>> CLOUD_SOLINIUM;
    public static RegistryHandle<EntityType<EntityCloudFleijaRainbow>> CLOUD_RAINBOW;
    public static RegistryHandle<EntityType<EntityParachuteCrate>> PARACHUTE_CRATE;
    public static RegistryHandle<EntityType<EntityBoxcar>> BOXCAR;
    public static RegistryHandle<EntityType<EntityTorpedo>> TORPEDO;
    public static RegistryHandle<EntityType<EntityDuchessGambit>> DUCHESS_GAMBIT;
    public static RegistryHandle<EntityType<EntityBuilding>> FALLING_BUILDING;
    public static RegistryHandle<EntityType<EntityChemical>> CHEMICAL;

    public static RegistryHandle<EntityType<EntitySoyuz>> SOYUZ;
    public static RegistryHandle<EntityType<EntitySoyuzCapsule>> SOYUZ_CAPSULE;
    public static RegistryHandle<EntityType<EntitySatellitePod>> SATELLITE_POD;
    public static RegistryHandle<EntityType<EntityChopperMine>> CHOPPER_MINE;

    public static RegistryHandle<EntityType<EntityBobmazon>> BOBMAZON_DELIVERY;
    public static RegistryHandle<EntityType<EntityMissileAntiBallistic>> MISSILE_ANTI;
    public static RegistryHandle<EntityType<EntityBlackHole>> BLACK_HOLE;
    public static RegistryHandle<EntityType<EntityQuasar>> DIGAMMA_QUASAR;
    public static RegistryHandle<EntityType<EntityEMPBlast>> EMP_BLAST;
    public static RegistryHandle<EntityType<EntityEMP>> EMP_LOGIC;
    public static RegistryHandle<EntityType<EntityBalefire>> BALEFIRE;
    public static RegistryHandle<EntityType<EntityDeathBlast>> LASER_BLAST;
    public static RegistryHandle<EntityType<EntityOrbitalLaser>> ORBITAL_LASER;
    public static RegistryHandle<EntityType<EntityTomBlast>> TOM_BUST;
    public static RegistryHandle<EntityType<EntityRagingVortex>> RAGING_VORTEX;
    public static RegistryHandle<EntityType<EntityCloudTom>> MOONSTONE_BLAST;
    public static RegistryHandle<EntityType<EntityArtilleryShell>> ARTILLERY_SHELL;
    public static RegistryHandle<EntityType<EntityArtilleryRocket>> HIMARS;
    public static RegistryHandle<EntityType<EntityB92Beam>> BEAM_BOMB;
    public static RegistryHandle<EntityType<EntityBullet>> BULLET;
    public static RegistryHandle<EntityType<EntityBurningFOEQ>> BURNING_FOEQ;
    public static RegistryHandle<EntityType<EntityFallingNuke>> FALLING_BOMB;
    public static RegistryHandle<EntityType<EntitySiegeLaser>> SIEGE_LASER;
    public static RegistryHandle<EntityType<EntitySawblade>> STRAY_SAW;
    public static RegistryHandle<EntityType<EntityTom>> TOM_THE_MOONSTONE;

    private ModEntities() {}

    private static final TagKey<Biome> NO_DEFAULT_MONSTERS = convention("no_default_monsters");

    public static void register(IRegistrar r) {
        BEAM_DISCHARGE =
                Reg.entity(
                        "beam_discharge",
                        () ->
                                EntityType.Builder.of(EntityDischargeBeam::new, MobCategory.MISC)
                                        .noLootTable()
                                        .noSave()
                                        .fireImmune()
                                        .sized(0.5F, 0.5F)
                                        .clientTrackingRange(16)
                                        .updateInterval(Integer.MAX_VALUE)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id("beam_discharge"))));

        NUKE_EXPLOSION_MK5 =
                Reg.entity(
                        "nuke_mk5",
                        () ->
                                EntityType.Builder.of(EntityNukeExplosionMK5::new, MobCategory.MISC)
                                        .noLootTable()
                                        .fireImmune()
                                        .sized(1.0F, 1.0F)
                                        .clientTrackingRange(0)
                                        .updateInterval(Integer.MAX_VALUE)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id("nuke_mk5"))));
        FALLOUT_RAIN =
                Reg.entity(
                        "fallout_rain",
                        () ->
                                EntityType.Builder.of(EntityFalloutRain::new, MobCategory.MISC)
                                        .noLootTable()
                                        .fireImmune()
                                        .sized(1.0F, 1.0F)
                                        .clientTrackingRange(63)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id("fallout_rain"))));

        UNDEAD_SOLDIER =
                Reg.entity(
                        "entity_undead_soldier",
                        () ->
                                EntityType.Builder.of(EntityUndeadSoldier::new, MobCategory.MONSTER)
                                        .noLootTable()
                                        .sized(.6F, 1.95F)
                                        .clientTrackingRange(5)
                                        .updateInterval(3)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id("entity_undead_soldier"))));
        r.registerLivingAttributes(UNDEAD_SOLDIER, EntityUndeadSoldier::createAttributes);

        FBI =
                Reg.entity(
                        "entity_fbi",
                        () ->
                                EntityType.Builder.<EntityFBI>of(
                                                EntityFBI::new, MobCategory.MONSTER)
                                        .sized(0.6F, 1.8F)
                                        .fireImmune()
                                        .clientTrackingRange(5)
                                        .updateInterval(3)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id("entity_fbi"))));
        r.registerLivingAttributes(FBI, EntityFBI::createAttributes);

        CREEPER_GOLD =
                Reg.entity(
                        "entity_mob_gold_creeper",
                        () ->
                                EntityType.Builder.<EntityCreeperGold>of(
                                                EntityCreeperGold::new, MobCategory.MONSTER)
                                        .noLootTable()
                                        .sized(0.6F, 1.7F)
                                        .clientTrackingRange(5)
                                        .updateInterval(3)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id("entity_mob_gold_creeper"))));
        r.registerLivingAttributes(CREEPER_GOLD, Creeper::createAttributes);
        r.registerSpawnPlacement(
                CREEPER_GOLD,
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules);

        CREEPER_VOLATILE =
                Reg.entity(
                        "entity_mob_volatile_creeper",
                        () ->
                                EntityType.Builder.<EntityCreeperVolatile>of(
                                                EntityCreeperVolatile::new, MobCategory.MONSTER)
                                        .noLootTable()
                                        .sized(0.6F, 1.7F)
                                        .clientTrackingRange(5)
                                        .updateInterval(3)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id(
                                                                "entity_mob_volatile_creeper"))));
        r.registerLivingAttributes(CREEPER_VOLATILE, Creeper::createAttributes);
        r.registerSpawnPlacement(
                CREEPER_VOLATILE,
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules);

        CREEPER_PHOSGENE =
                Reg.entity(
                        "entity_mob_phosgene_creeper",
                        () ->
                                EntityType.Builder.<EntityCreeperPhosgene>of(
                                                EntityCreeperPhosgene::new, MobCategory.MONSTER)
                                        .noLootTable()
                                        .sized(0.6F, 1.7F)
                                        .clientTrackingRange(5)
                                        .updateInterval(3)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id(
                                                                "entity_mob_phosgene_creeper"))));
        r.registerLivingAttributes(CREEPER_PHOSGENE, Creeper::createAttributes);
        r.registerSpawnPlacement(
                CREEPER_PHOSGENE,
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules);

        CREEPER_TAINTED =
                Reg.entity(
                        "entity_mob_tainted_creeper",
                        () ->
                                EntityType.Builder.<EntityCreeperTainted>of(
                                                EntityCreeperTainted::new, MobCategory.MONSTER)
                                        .noLootTable()
                                        .sized(0.6F, 1.7F)
                                        .clientTrackingRange(5)
                                        .updateInterval(3)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id("entity_mob_tainted_creeper"))));
        r.registerLivingAttributes(CREEPER_TAINTED, EntityCreeperTainted::createAttributes);

        CREEPER_NUCLEAR =
                Reg.entity(
                        "entity_mob_nuclear_creeper",
                        () ->
                                EntityType.Builder.<EntityCreeperNuclear>of(
                                                EntityCreeperNuclear::new, MobCategory.MONSTER)
                                        .noLootTable()
                                        .sized(0.6F, 1.7F)
                                        .clientTrackingRange(5)
                                        .updateInterval(3)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id("entity_mob_nuclear_creeper"))));
        r.registerLivingAttributes(CREEPER_NUCLEAR, EntityCreeperNuclear::createAttributes);

        CYBER_CRAB =
                Reg.entity(
                        "entity_cyber_crab",
                        () ->
                                EntityType.Builder.<EntityCyberCrab>of(
                                                EntityCyberCrab::new, MobCategory.MONSTER)
                                        .noLootTable()
                                        .sized(0.75F, 0.35F)
                                        .clientTrackingRange(5)
                                        .updateInterval(3)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id("entity_cyber_crab"))));
        r.registerLivingAttributes(CYBER_CRAB, EntityCyberCrab::createAttributes);

        TESLA_CRAB =
                Reg.entity(
                        "entity_tesla_crab",
                        () ->
                                EntityType.Builder.<EntityTeslaCrab>of(
                                                EntityTeslaCrab::new, MobCategory.MONSTER)
                                        .noLootTable()
                                        .sized(0.75F, 1.25F)
                                        .clientTrackingRange(5)
                                        .updateInterval(3)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id("entity_tesla_crab"))));
        r.registerLivingAttributes(TESLA_CRAB, EntityTeslaCrab::createAttributes);

        TAINT_CRAB =
                Reg.entity(
                        "entity_taint_crab",
                        () ->
                                EntityType.Builder.<EntityTaintCrab>of(
                                                EntityTaintCrab::new, MobCategory.MONSTER)
                                        .noLootTable()
                                        .sized(1.25F, 1.25F)
                                        .clientTrackingRange(5)
                                        .updateInterval(3)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id("entity_taint_crab"))));
        r.registerLivingAttributes(TAINT_CRAB, EntityTaintCrab::createAttributes);

        DUCK =
                Reg.entity(
                        "entity_fucc_a_ducc",
                        () ->
                                EntityType.Builder.<EntityDuck>of(
                                                EntityDuck::new, MobCategory.CREATURE)
                                        .noLootTable()
                                        .sized(0.4F, 0.7F)
                                        .clientTrackingRange(5)
                                        .updateInterval(3)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id("entity_fucc_a_ducc"))));
        r.registerLivingAttributes(DUCK, Chicken::createAttributes);

        GHOST =
                Reg.entity(
                        "entity_ghost",
                        () ->
                                EntityType.Builder.<EntityGhost>of(
                                                EntityGhost::new, MobCategory.MONSTER)
                                        .noLootTable()
                                        .sized(0.6F, 1.8F)
                                        .clientTrackingRange(63)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id("entity_ghost"))));
        r.registerLivingAttributes(GHOST, EntityGhost::createAttributes);

        TEST_DUMMY =
                Reg.entity(
                        "entity_test_dummy",
                        () ->
                                EntityType.Builder.<EntityDummy>of(
                                                EntityDummy::new, MobCategory.CREATURE)
                                        .noLootTable()
                                        .sized(0.6F, 1.8F)
                                        .clientTrackingRange(5)
                                        .updateInterval(3)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id("entity_test_dummy"))));
        r.registerLivingAttributes(TEST_DUMMY, EntityDummy::createAttributes);

        BLOCK_SPIDER =
                Reg.entity(
                        "entity_taintcrawler",
                        () ->
                                EntityType.Builder.<EntityBlockSpider>of(
                                                EntityBlockSpider::new, MobCategory.MONSTER)
                                        .noLootTable()
                                        .sized(0.95F, 1.25F)
                                        .clientTrackingRange(63)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id("entity_taintcrawler"))));
        r.registerLivingAttributes(BLOCK_SPIDER, EntityBlockSpider::createAttributes);

        QUACKOS =
                Reg.entity(
                        "entity_elder_one",
                        () ->
                                EntityType.Builder.<EntityQuackos>of(
                                                EntityQuackos::new, MobCategory.CREATURE)
                                        .noLootTable()
                                        .sized(7.5F, 17.5F)
                                        .clientTrackingRange(5)
                                        .updateInterval(3)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id("entity_elder_one"))));
        r.registerLivingAttributes(QUACKOS, Chicken::createAttributes);

        PLASTIC_BAG =
                Reg.entity(
                        "entity_plastic_bag",
                        () ->
                                EntityType.Builder.<EntityPlasticBag>of(
                                                EntityPlasticBag::new, MobCategory.WATER_CREATURE)
                                        .noLootTable()
                                        .sized(0.45F, 0.45F)
                                        .clientTrackingRange(5)
                                        .updateInterval(3)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id("entity_plastic_bag"))));
        r.registerLivingAttributes(PLASTIC_BAG, EntityPlasticBag::createAttributes);

        r.registerSpawnPlacement(
                PLASTIC_BAG,
                SpawnPlacementTypes.IN_WATER,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) -> true);

        PIGEON =
                Reg.entity(
                        "entity_pigeon",
                        () ->
                                EntityType.Builder.<EntityPigeon>of(
                                                EntityPigeon::new, MobCategory.CREATURE)
                                        .noLootTable()
                                        .sized(0.5F, 1.0F)
                                        .clientTrackingRange(5)
                                        .updateInterval(3)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id("entity_pigeon"))));
        r.registerLivingAttributes(PIGEON, EntityPigeon::createAttributes);

        r.registerSpawnPlacement(
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

        Reg.EntityHandle<EntityGlyphid> grunt =
                Reg.entity(
                        "entity_glyphid",
                        () ->
                                EntityType.Builder.<EntityGlyphid>of(
                                                EntityGlyphid::new, MobCategory.MONSTER)
                                        .noLootTable()
                                        .sized(1.75F, 1F)
                                        .clientTrackingRange(5)
                                        .updateInterval(3)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id("entity_glyphid"))));
        GLYPHID = grunt;
        r.registerLivingAttributes(GLYPHID, EntityGlyphid::createAttributes);
        EntityGlyphidBrenda.GRUNT_TYPE = grunt;

        EntityGlyphidBrawler.register(r);
        GLYPHID_BRAWLER = EntityGlyphidBrawler.TYPE;

        GLYPHID_BEHEMOTH =
                Reg.entity(
                        "entity_glyphid_behemoth",
                        () ->
                                EntityType.Builder.<EntityGlyphidBehemoth>of(
                                                EntityGlyphidBehemoth::new, MobCategory.MONSTER)
                                        .noLootTable()
                                        .sized(2.5F, 1.5F)
                                        .clientTrackingRange(5)
                                        .updateInterval(3)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id("entity_glyphid_behemoth"))));
        r.registerLivingAttributes(GLYPHID_BEHEMOTH, EntityGlyphidBehemoth::createAttributes);

        GLYPHID_BRENDA =
                Reg.entity(
                        "entity_glyphid_brenda",
                        () ->
                                EntityType.Builder.<EntityGlyphidBrenda>of(
                                                EntityGlyphidBrenda::new, MobCategory.MONSTER)
                                        .noLootTable()
                                        .sized(2.5F, 1.75F)
                                        .clientTrackingRange(5)
                                        .updateInterval(3)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id("entity_glyphid_brenda"))));
        r.registerLivingAttributes(GLYPHID_BRENDA, EntityGlyphidBrenda::createAttributes);

        EntityGlyphidBombardier.register(r);
        GLYPHID_BOMBARDIER = EntityGlyphidBombardier.TYPE;

        EntityGlyphidBlaster.register(r);
        GLYPHID_BLASTER = EntityGlyphidBlaster.TYPE;

        GLYPHID_SCOUT =
                Reg.entity(
                        "entity_glyphid_scout",
                        () ->
                                EntityType.Builder.<EntityGlyphidScout>of(
                                                EntityGlyphidScout::new, MobCategory.MONSTER)
                                        .noLootTable()
                                        .sized(1.25F, 0.75F)
                                        .clientTrackingRange(5)
                                        .updateInterval(3)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id("entity_glyphid_scout"))));
        r.registerLivingAttributes(GLYPHID_SCOUT, EntityGlyphidScout::createAttributes);

        GLYPHID_NUCLEAR =
                Reg.entity(
                        "entity_glyphid_nuclear",
                        () ->
                                EntityType.Builder.<EntityGlyphidNuclear>of(
                                                EntityGlyphidNuclear::new, MobCategory.MONSTER)
                                        .noLootTable()
                                        .sized(2.5F, 1.75F)
                                        .clientTrackingRange(5)
                                        .updateInterval(3)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id("entity_glyphid_nuclear"))));
        r.registerLivingAttributes(GLYPHID_NUCLEAR, EntityGlyphidNuclear::createAttributes);

        EntityGlyphidDigger.register(r);
        GLYPHID_DIGGER = EntityGlyphidDigger.TYPE;

        EntityWaypoint.register(r);
        GLYPHID_WAYPOINT = EntityWaypoint.TYPE;

        EntityAcidBomb.register(r);
        ACID_BOMB = EntityAcidBomb.TYPE;

        EntityParasiteMaggot.register(r);
        PARASITE_MAGGOT = EntityParasiteMaggot.TYPE;

        NUKE_TOREX =
                Reg.entity(
                        "nuke_torex",
                        () ->
                                EntityType.Builder.of(EntityNukeTorex::new, MobCategory.MISC)
                                        .noLootTable()
                                        .noSave()
                                        .fireImmune()
                                        .sized(1.0F, 1.0F)
                                        .clientTrackingRange(16)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id("nuke_torex"))));
        VORTEX =
                Reg.entity(
                        "entity_vortex",
                        () ->
                                EntityType.Builder.<EntityVortex>of(
                                                EntityVortex::new, MobCategory.MISC)
                                        .noLootTable()
                                        .fireImmune()
                                        .sized(0.6F, 1.8F)
                                        .clientTrackingRange(16)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id("entity_vortex"))));

        METEOR =
                Reg.entity(
                        "entity_meteor",
                        () ->
                                EntityType.Builder.<EntityMeteor>of(
                                                EntityMeteor::new, MobCategory.MISC)
                                        .noLootTable()
                                        .fireImmune()
                                        .sized(4F, 4F)
                                        .clientTrackingRange(16)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id("entity_meteor"))));

        MISSILE_GENERIC =
                ModEntities.<EntityMissileGeneric>missile(
                        "missile_generic", EntityMissileGeneric::new);
        MISSILE_INCENDIARY =
                ModEntities.<EntityMissileIncendiary>missile(
                        "missile_incendiary", EntityMissileIncendiary::new);
        MISSILE_DECOY =
                ModEntities.<EntityMissileDecoy>missile("missile_decoy", EntityMissileDecoy::new);
        MISSILE_BUSTER =
                ModEntities.<EntityMissileBunkerBuster>missile(
                        "missile_buster", EntityMissileBunkerBuster::new);
        MISSILE_STRONG =
                ModEntities.<EntityMissileStrong>missile(
                        "missile_strong", EntityMissileStrong::new);
        MISSILE_INCENDIARY_STRONG =
                ModEntities.<EntityMissileIncendiaryStrong>missile(
                        "missile_incendiary_strong", EntityMissileIncendiaryStrong::new);
        MISSILE_BUSTER_STRONG =
                ModEntities.<EntityMissileBusterStrong>missile(
                        "missile_buster_strong", EntityMissileBusterStrong::new);
        MISSILE_BURST =
                ModEntities.<EntityMissileBurst>missile("missile_burst", EntityMissileBurst::new);

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

        MISSILE_CLUSTER =
                ModEntities.<EntityMissileCluster>missile(
                        "missile_cluster", EntityMissileCluster::new);
        MISSILE_CLUSTER_STRONG =
                ModEntities.<EntityMissileClusterStrong>missile(
                        "missile_cluster_strong", EntityMissileClusterStrong::new);
        MISSILE_EMP_STRONG =
                ModEntities.<EntityMissileEMPStrong>missile(
                        "missile_emp_strong", EntityMissileEMPStrong::new);
        MISSILE_RAIN =
                ModEntities.<EntityMissileRain>missile("missile_rain", EntityMissileRain::new);
        MISSILE_DRILL =
                ModEntities.<EntityMissileDrill>missile("missile_drill", EntityMissileDrill::new);
        MISSILE_VOLCANO =
                ModEntities.<EntityMissileVolcano>missile(
                        "missile_volcano", EntityMissileVolcano::new);

        MISSILE_CUSTOM =
                ModEntities.<EntityMissileCustom>missile(
                        "entity_custom_missile", EntityMissileCustom::new);
        MISSILE_SHUTTLE =
                ModEntities.<EntityMissileShuttle>missile(
                        "entity_missile_shuttle", EntityMissileShuttle::new);
        MISSILE_INFERNO =
                ModEntities.<EntityMissileInferno>missile(
                        "missile_inferno", EntityMissileInferno::new);
        MISSILE_NUCLEAR =
                ModEntities.<EntityMissileNuclear>missile(
                        "missile_nuclear", EntityMissileNuclear::new);
        MISSILE_NUCLEAR_CLUSTER =
                ModEntities.<EntityMissileMirv>missile(
                        "missile_nuclear_cluster", EntityMissileMirv::new);
        MISSILE_STEALTH =
                ModEntities.<EntityMissileStealth>missile(
                        "missile_stealth", EntityMissileStealth::new);
        MISSILE_DOOMSDAY =
                Reg.entity(
                        "missile_doomsday",
                        () ->
                                EntityType.Builder.<EntityMissileDoomsday>of(
                                                EntityMissileDoomsday::new, MobCategory.MISC)
                                        .noLootTable()
                                        .fireImmune()
                                        .sized(1.5F, 1.5F)
                                        .clientTrackingRange(63)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id("missile_doomsday"))));
        MISSILE_DOOMSDAY_RUSTED =
                Reg.entity(
                        "missile_doomsday_rusted",
                        () ->
                                EntityType.Builder.<EntityMissileDoomsdayRusted>of(
                                                EntityMissileDoomsdayRusted::new, MobCategory.MISC)
                                        .noLootTable()
                                        .fireImmune()
                                        .sized(1.5F, 1.5F)
                                        .clientTrackingRange(63)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id("missile_doomsday_rusted"))));

        SHRAPNEL =
                Reg.entity(
                        "shrapnel",
                        () ->
                                EntityType.Builder.<EntityShrapnel>of(
                                                EntityShrapnel::new, MobCategory.MISC)
                                        .noLootTable()
                                        .noSave()
                                        .fireImmune()
                                        .sized(0.25F, 0.25F)
                                        .clientTrackingRange(63)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id("shrapnel"))));
        BULLET_MK4 =
                Reg.entity(
                        "bullet_mk4",
                        () ->
                                EntityType.Builder.<EntityBulletBaseMK4>of(
                                                EntityBulletBaseMK4::new, MobCategory.MISC)
                                        .noLootTable()
                                        .noSave()
                                        .fireImmune()
                                        .sized(0.5F, 0.5F)
                                        .clientTrackingRange(16)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id("bullet_mk4"))));
        BULLET_BEAM =
                Reg.entity(
                        "bullet_beam",
                        () ->
                                EntityType.Builder.<EntityBulletBeamBase>of(
                                                EntityBulletBeamBase::new, MobCategory.MISC)
                                        .noLootTable()
                                        .noSave()
                                        .fireImmune()
                                        .sized(0.5F, 0.5F)
                                        .clientTrackingRange(16)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id("bullet_beam"))));
        FIRE_LINGERING =
                Reg.entity(
                        "fire_lingering",
                        () ->
                                EntityType.Builder.<EntityFireLingering>of(
                                                EntityFireLingering::new, MobCategory.MISC)
                                        .noLootTable()
                                        .noSave()
                                        .fireImmune()
                                        .sized(0.5F, 0.5F)
                                        .clientTrackingRange(16)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id("fire_lingering"))));
        COIN =
                Reg.entity(
                        "coin",
                        () ->
                                EntityType.Builder.<EntityCoin>of(EntityCoin::new, MobCategory.MISC)
                                        .noLootTable()
                                        .noSave()
                                        .sized(1F, 1F)
                                        .clientTrackingRange(63)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id("coin"))));
        RUBBLE =
                Reg.entity(
                        "rubble",
                        () ->
                                EntityType.Builder.<EntityRubble>of(
                                                EntityRubble::new, MobCategory.MISC)
                                        .noLootTable()
                                        .sized(0.25F, 0.25F)
                                        .clientTrackingRange(63)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id("rubble"))));

        COG =
                Reg.entity(
                        "entity_stray_cog",
                        () ->
                                EntityType.Builder.<EntityCog>of(EntityCog::new, MobCategory.MISC)
                                        .noLootTable()
                                        .sized(1.0F, 1.0F)
                                        .clientTrackingRange(63)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id("entity_stray_cog"))));

        RBMK_DEBRIS =
                Reg.entity(
                        "rbmk_debris",
                        () ->
                                EntityType.Builder.<EntityRBMKDebris>of(
                                                EntityRBMKDebris::new, MobCategory.MISC)
                                        .noLootTable()
                                        .fireImmune()
                                        .sized(1.0F, 1.0F)
                                        .clientTrackingRange(63)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id("rbmk_debris"))));
        ZIRNOX_DEBRIS =
                Reg.entity(
                        "zirnox_debris",
                        () ->
                                EntityType.Builder.<EntityZirnoxDebris>of(
                                                EntityZirnoxDebris::new, MobCategory.MISC)
                                        .noLootTable()
                                        .fireImmune()
                                        .sized(1.0F, 1.0F)
                                        .clientTrackingRange(63)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id("zirnox_debris"))));
        SPEAR =
                Reg.entity(
                        "entity_spear",
                        () ->
                                EntityType.Builder.of(EntitySpear::new, MobCategory.MISC)
                                        .noLootTable()
                                        .fireImmune()
                                        .sized(2.0F, 10.0F)
                                        .clientTrackingRange(63)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id("entity_spear"))));

        WASTE_ITEM =
                Reg.entity(
                        "waste_item",
                        () ->
                                EntityType.Builder.<EntityItemWaste>of(
                                                EntityItemWaste::new, MobCategory.MISC)
                                        .sized(0.25F, 0.25F)
                                        .clientTrackingRange(7)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id("waste_item"))));

        MIST =
                Reg.entity(
                        "mist",
                        () ->
                                EntityType.Builder.<EntityMist>of(EntityMist::new, MobCategory.MISC)
                                        .noLootTable()
                                        .sized(1.0F, 1.0F)
                                        .clientTrackingRange(16)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id("mist"))));
        AGENT_ORANGE =
                Reg.entity(
                        "entity_agent_orange",
                        () ->
                                EntityType.Builder.<EntityOrangeFX>of(
                                                EntityOrangeFX::new, MobCategory.MISC)
                                        .noLootTable()
                                        .noSave()
                                        .sized(0.2F, 0.2F)
                                        .clientTrackingRange(63)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id("entity_agent_orange"))));
        CHLORINE_FX =
                Reg.entity(
                        "entity_chlorine_fx",
                        () ->
                                EntityType.Builder.<EntityChlorineFX>of(
                                                EntityChlorineFX::new, MobCategory.MISC)
                                        .noLootTable()
                                        .noSave()
                                        .sized(0.2F, 0.2F)
                                        .clientTrackingRange(63)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id("entity_chlorine_fx"))));
        CLOUD_FX =
                Reg.entity(
                        "entity_cloud_fx",
                        () ->
                                EntityType.Builder.<EntityCloudFX>of(
                                                EntityCloudFX::new, MobCategory.MISC)
                                        .noLootTable()
                                        .noSave()
                                        .sized(0.2F, 0.2F)
                                        .clientTrackingRange(63)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id("entity_cloud_fx"))));
        PINK_CLOUD_FX =
                Reg.entity(
                        "entity_pink_cloud_fx",
                        () ->
                                EntityType.Builder.<EntityPinkCloudFX>of(
                                                EntityPinkCloudFX::new, MobCategory.MISC)
                                        .noLootTable()
                                        .noSave()
                                        .sized(0.2F, 0.2F)
                                        .clientTrackingRange(63)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id("entity_pink_cloud_fx"))));
        FIREWORK_BALL =
                Reg.entity(
                        "entity_firework_ball",
                        () ->
                                EntityType.Builder.<EntityFireworks>of(
                                                EntityFireworks::new, MobCategory.MISC)
                                        .noLootTable()
                                        .sized(0.6F, 1.8F)
                                        .clientTrackingRange(63)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id("entity_firework_ball"))));

        CART_ORE =
                Reg.entity(
                        "entity_cart_ore",
                        () ->
                                EntityType.Builder.<EntityMinecartOre>of(
                                                EntityMinecartOre::new, MobCategory.MISC)
                                        .noLootTable()
                                        .sized(0.98F, 0.7F)
                                        .clientTrackingRange(16)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id("entity_cart_ore"))));
        CART_POWDER =
                Reg.entity(
                        "entity_cart_powder",
                        () ->
                                EntityType.Builder.<EntityMinecartPowder>of(
                                                EntityMinecartPowder::new, MobCategory.MISC)
                                        .noLootTable()
                                        .sized(0.98F, 0.7F)
                                        .clientTrackingRange(16)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id("entity_cart_powder"))));
        CART_SEMTEX =
                Reg.entity(
                        "entity_cart_semtex",
                        () ->
                                EntityType.Builder.<EntityMinecartSemtex>of(
                                                EntityMinecartSemtex::new, MobCategory.MISC)
                                        .noLootTable()
                                        .sized(0.98F, 0.7F)
                                        .clientTrackingRange(16)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id("entity_cart_semtex"))));
        CART_CRATE =
                Reg.entity(
                        "entity_cart_crate",
                        () ->
                                EntityType.Builder.<EntityMinecartCrate>of(
                                                EntityMinecartCrate::new, MobCategory.MISC)
                                        .noLootTable()
                                        .sized(0.98F, 0.7F)
                                        .clientTrackingRange(16)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id("entity_cart_crate"))));
        CART_DESTROYER =
                Reg.entity(
                        "entity_cart_destroyer",
                        () ->
                                EntityType.Builder.<EntityMinecartDestroyer>of(
                                                EntityMinecartDestroyer::new, MobCategory.MISC)
                                        .noLootTable()
                                        .sized(0.98F, 0.7F)
                                        .clientTrackingRange(16)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id("entity_cart_destroyer"))));

        CART_TEST =
                Reg.entity(
                        "entity_minecart_test",
                        () ->
                                EntityType.Builder.<EntityMinecartTest>of(
                                                EntityMinecartTest::new, MobCategory.MISC)
                                        .noLootTable()
                                        .sized(0.98F, 0.7F)
                                        .clientTrackingRange(63)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id("entity_minecart_test"))));
        BOAT_RUBBER =
                Reg.entity(
                        "entity_rubber_boat",
                        () ->
                                EntityType.Builder.<EntityBoatRubber>of(
                                                EntityBoatRubber::new, MobCategory.MISC)
                                        .noLootTable()
                                        .sized(1.375F, 0.5625F)
                                        .eyeHeight(0.5625F)
                                        .clientTrackingRange(16)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id("entity_rubber_boat"))));
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
                                                        Library.id("entity_cargo_tram"))));
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
                                                        Library.id("entity_cargo_tram_trailer"))));

        BOUNDING_DUMMY =
                Reg.entity(
                        "entity_bounding_dummy",
                        () ->
                                EntityType.Builder.<EntityRailCarBase.BoundingBoxDummyEntity>of(
                                                EntityRailCarBase.BoundingBoxDummyEntity::new,
                                                MobCategory.MISC)
                                        .noLootTable()
                                        .sized(1F, 1F)
                                        .clientTrackingRange(16)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id("entity_bounding_dummy"))));
        SEAT_DUMMY =
                Reg.entity(
                        "entity_seat_dummy",
                        () ->
                                EntityType.Builder.<EntityRailCarRidable.SeatDummyEntity>of(
                                                EntityRailCarRidable.SeatDummyEntity::new,
                                                MobCategory.MISC)
                                        .noLootTable()
                                        .sized(0.5F, 0.1F)
                                        .clientTrackingRange(16)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id("entity_seat_dummy"))));
        MOVING_ITEM =
                Reg.entity(
                        "entity_c_item",
                        () ->
                                EntityType.Builder.<EntityMovingItem>of(
                                                EntityMovingItem::new, MobCategory.MISC)
                                        .noLootTable()
                                        .sized(0.375F, 0.375F)
                                        .clientTrackingRange(63)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id("entity_c_item"))));
        MOVING_PACKAGE =
                Reg.entity(
                        "entity_c_package",
                        () ->
                                EntityType.Builder.<EntityMovingPackage>of(
                                                EntityMovingPackage::new, MobCategory.MISC)
                                        .noLootTable()
                                        .sized(0.5F, 0.5F)
                                        .clientTrackingRange(63)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id("entity_c_package"))));
        DELIVERY_DRONE =
                Reg.entity(
                        "entity_delivery_drone",
                        () ->
                                EntityType.Builder.<EntityDeliveryDrone>of(
                                                EntityDeliveryDrone::new, MobCategory.MISC)
                                        .noLootTable()
                                        .sized(0.75F, 0.75F)
                                        .clientTrackingRange(16)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id("entity_delivery_drone"))));
        REQUEST_DRONE =
                Reg.entity(
                        "entity_request_drone",
                        () ->
                                EntityType.Builder.<EntityRequestDrone>of(
                                                EntityRequestDrone::new, MobCategory.MISC)
                                        .noLootTable()
                                        .sized(0.75F, 0.75F)
                                        .clientTrackingRange(16)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id("entity_request_drone"))));
        FBI_DRONE =
                Reg.entity(
                        "entity_fbi_drone",
                        () ->
                                EntityType.Builder.<EntityFBIDrone>of(
                                                EntityFBIDrone::new, MobCategory.MONSTER)
                                        .noLootTable()
                                        .fireImmune()
                                        .sized(0.6F, 1.8F)
                                        .clientTrackingRange(5)
                                        .updateInterval(3)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id("entity_fbi_drone"))));
        r.registerLivingAttributes(FBI_DRONE, EntityFBIDrone::createAttributes);
        RAD_BEAST =
                Reg.entity(
                        "entity_radiation_blaze",
                        () ->
                                EntityType.Builder.<EntityRADBeast>of(
                                                EntityRADBeast::new, MobCategory.MONSTER)
                                        .noLootTable()
                                        .fireImmune()
                                        .sized(0.6F, 1.8F)
                                        .clientTrackingRange(5)
                                        .updateInterval(3)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id("entity_radiation_blaze"))));
        r.registerLivingAttributes(RAD_BEAST, EntityRADBeast::createAttributes);
        MASK_MAN =
                Reg.entity(
                        "entity_mob_mask_man",
                        () ->
                                EntityType.Builder.<EntityMaskMan>of(
                                                EntityMaskMan::new, MobCategory.MONSTER)
                                        .noLootTable()
                                        .fireImmune()
                                        .sized(2.0F, 5.0F)
                                        .clientTrackingRange(5)
                                        .updateInterval(3)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id("entity_mob_mask_man"))));
        r.registerLivingAttributes(MASK_MAN, EntityMaskMan::createAttributes);
        UFO =
                Reg.entity(
                        "entity_ufo",
                        () ->
                                EntityType.Builder.<EntityUFO>of(
                                                EntityUFO::new, MobCategory.MONSTER)
                                        .noLootTable()
                                        .fireImmune()
                                        .sized(15.0F, 4.0F)
                                        .clientTrackingRange(63)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id("entity_ufo"))));
        r.registerLivingAttributes(UFO, EntityUFO::createAttributes);

        BALLS_O_TRON =
                Reg.entity(
                        "entity_balls_o_tron",
                        () ->
                                EntityType.Builder.<EntityBOTPrimeHead>of(
                                                EntityBOTPrimeHead::new, MobCategory.MONSTER)
                                        .noLootTable()
                                        .fireImmune()
                                        .sized(3.0F, 3.0F)
                                        .eyeHeight(1.5F)
                                        .clientTrackingRange(63)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id("entity_balls_o_tron"))));
        r.registerLivingAttributes(BALLS_O_TRON, EntityBOTPrimeHead::createAttributes);
        BALLS_O_TRON_SEG =
                Reg.entity(
                        "entity_balls_o_tron_seg",
                        () ->
                                EntityType.Builder.<EntityBOTPrimeBody>of(
                                                EntityBOTPrimeBody::new, MobCategory.MONSTER)
                                        .noLootTable()
                                        .fireImmune()
                                        .sized(2.0F, 2.0F)
                                        .eyeHeight(1.0F)
                                        .clientTrackingRange(63)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id("entity_balls_o_tron_seg"))));
        r.registerLivingAttributes(BALLS_O_TRON_SEG, EntityBOTPrimeBody::createAttributes);
        HUNTER_CHOPPER =
                Reg.entity(
                        "entity_mob_hunter_chopper",
                        () ->
                                EntityType.Builder.<EntityHunterChopper>of(
                                                EntityHunterChopper::new, MobCategory.MONSTER)
                                        .noLootTable()
                                        .fireImmune()
                                        .sized(8.25F, 3.0F)
                                        .clientTrackingRange(5)
                                        .updateInterval(3)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id("entity_mob_hunter_chopper"))));
        r.registerLivingAttributes(HUNTER_CHOPPER, EntityHunterChopper::createAttributes);
        TNT_NTM =
                Reg.entity(
                        "tnt",
                        () ->
                                EntityType.Builder.<EntityTntNtm>of(
                                                EntityTntNtm::new, MobCategory.MISC)
                                        .noLootTable()
                                        .fireImmune()
                                        .sized(0.98F, 0.98F)
                                        .eyeHeight(0.15F)
                                        .clientTrackingRange(63)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id("tnt"))));

        FALLING_BLOCK_NT =
                Reg.entity(
                        "entity_falling_block",
                        () ->
                                EntityType.Builder.of(EntityFallingBlockNT::new, MobCategory.MISC)
                                        .noLootTable()
                                        .sized(0.98F, 0.98F)
                                        .clientTrackingRange(63)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id("entity_falling_block"))));

        FALLING_MULTIBLOCK =
                Reg.entity(
                        "entity_falling_multiblock",
                        () ->
                                EntityType.Builder.of(
                                                EntityFallingMultiblock::new, MobCategory.MISC)
                                        .noLootTable()
                                        .sized(0.98F, 0.98F)
                                        .clientTrackingRange(64)
                                        .updateInterval(20)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id("entity_falling_multiblock"))));

        C130 =
                Reg.entity(
                        "c130",
                        () ->
                                EntityType.Builder.of(EntityC130::new, MobCategory.MISC)
                                        .noLootTable()
                                        .sized(8.0F, 4.0F)
                                        .clientTrackingRange(63)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id("c130"))));
        BOMBER =
                Reg.entity(
                        "entity_bomber",
                        () ->
                                EntityType.Builder.of(EntityBomber::new, MobCategory.MISC)
                                        .noLootTable()
                                        .sized(8.0F, 4.0F)
                                        .clientTrackingRange(63)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id("entity_bomber"))));
        BOMBLET_ZETA =
                Reg.entity(
                        "entity_zeta",
                        () ->
                                EntityType.Builder.of(EntityBombletZeta::new, MobCategory.MISC)
                                        .noLootTable()
                                        .noSave()
                                        .sized(0.25F, 0.25F)
                                        .clientTrackingRange(63)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id("entity_zeta"))));

        GRENADE_UNIVERSAL =
                Reg.entity(
                        "grenade_universal",
                        () ->
                                EntityType.Builder.<EntityGrenadeUniversal>of(
                                                EntityGrenadeUniversal::new, MobCategory.MISC)
                                        .noLootTable()
                                        .sized(0.25F, 0.25F)
                                        .clientTrackingRange(16)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id("grenade_universal"))));
        WASTE_PEARL =
                Reg.entity(
                        "entity_waste_pearl",
                        () ->
                                EntityType.Builder.<EntityWastePearl>of(
                                                EntityWastePearl::new, MobCategory.MISC)
                                        .noLootTable()
                                        .sized(0.25F, 0.25F)
                                        .clientTrackingRange(63)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id("entity_waste_pearl"))));
        GRENADE_BOUNCY_GENERIC =
                Reg.entity(
                        "grenade_bouncy_generic",
                        () ->
                                EntityType.Builder.<EntityGrenadeBouncyGeneric>of(
                                                EntityGrenadeBouncyGeneric::new, MobCategory.MISC)
                                        .noLootTable()
                                        .sized(0.25F, 0.25F)
                                        .clientTrackingRange(16)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id("grenade_bouncy_generic"))));
        DISPERSER_CANISTER =
                Reg.entity(
                        "disperser_canister",
                        () ->
                                EntityType.Builder.<EntityDisperserCanister>of(
                                                EntityDisperserCanister::new, MobCategory.MISC)
                                        .noLootTable()
                                        .sized(0.25F, 0.25F)
                                        .clientTrackingRange(16)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id("disperser_canister"))));

        ITEM_BUOYANT =
                Reg.entity(
                        "item_buoyant",
                        () ->
                                EntityType.Builder.<EntityItemBuoyant>of(
                                                EntityItemBuoyant::new, MobCategory.MISC)
                                        .sized(0.25F, 0.25F)
                                        .clientTrackingRange(7)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id("item_buoyant"))));

        NUKE_EXPLOSION_MK3 =
                Reg.entity(
                        "nuke_mk3",
                        () ->
                                EntityType.Builder.of(EntityNukeExplosionMK3::new, MobCategory.MISC)
                                        .noLootTable()
                                        .fireImmune()
                                        .sized(1.0F, 1.0F)
                                        .clientTrackingRange(0)
                                        .updateInterval(Integer.MAX_VALUE)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id("nuke_mk3"))));

        CLOUD_FLEIJA =
                Reg.entity(
                        "cloud_fleija",
                        () ->
                                EntityType.Builder.of(EntityCloudFleija::new, MobCategory.MISC)
                                        .noLootTable()
                                        .fireImmune()
                                        .sized(20.0F, 40.0F)
                                        .clientTrackingRange(32)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id("cloud_fleija"))));
        CLOUD_SOLINIUM =
                Reg.entity(
                        "cloud_solinium",
                        () ->
                                EntityType.Builder.of(EntityCloudSolinium::new, MobCategory.MISC)
                                        .noLootTable()
                                        .fireImmune()
                                        .sized(20.0F, 40.0F)
                                        .clientTrackingRange(63)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id("cloud_solinium"))));
        CLOUD_RAINBOW =
                Reg.entity(
                        "cloud_rainbow",
                        () ->
                                EntityType.Builder.of(
                                                EntityCloudFleijaRainbow::new, MobCategory.MISC)
                                        .noLootTable()
                                        .fireImmune()
                                        .sized(20.0F, 40.0F)
                                        .clientTrackingRange(63)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id("cloud_rainbow"))));
        PARACHUTE_CRATE =
                Reg.entity(
                        "parachute_crate",
                        () ->
                                EntityType.Builder.of(EntityParachuteCrate::new, MobCategory.MISC)
                                        .noLootTable()
                                        .fireImmune()
                                        .sized(1.0F, 1.0F)
                                        .clientTrackingRange(63)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id("parachute_crate"))));
        BOXCAR =
                Reg.entity(
                        "boxcar",
                        () ->
                                EntityType.Builder.of(EntityBoxcar::new, MobCategory.MISC)
                                        .noLootTable()
                                        .fireImmune()
                                        .sized(0.25F, 0.25F)
                                        .clientTrackingRange(63)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id("boxcar"))));
        TORPEDO =
                Reg.entity(
                        "torpedo",
                        () ->
                                EntityType.Builder.of(EntityTorpedo::new, MobCategory.MISC)
                                        .noLootTable()
                                        .fireImmune()
                                        .sized(0.25F, 0.25F)
                                        .clientTrackingRange(63)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id("torpedo"))));
        DUCHESS_GAMBIT =
                Reg.entity(
                        "duchessgambit",
                        () ->
                                EntityType.Builder.of(EntityDuchessGambit::new, MobCategory.MISC)
                                        .noLootTable()
                                        .fireImmune()
                                        .sized(0.25F, 0.25F)
                                        .clientTrackingRange(63)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id("duchessgambit"))));
        FALLING_BUILDING =
                Reg.entity(
                        "falling_building",
                        () ->
                                EntityType.Builder.of(EntityBuilding::new, MobCategory.MISC)
                                        .noLootTable()
                                        .fireImmune()
                                        .sized(0.25F, 0.25F)
                                        .clientTrackingRange(63)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id("falling_building"))));
        CHEMICAL =
                Reg.entity(
                        "chemical",
                        () ->
                                EntityType.Builder.<EntityChemical>of(
                                                EntityChemical::new, MobCategory.MISC)
                                        .noLootTable()
                                        .noSave()
                                        .fireImmune()
                                        .sized(0.25F, 0.25F)
                                        .clientTrackingRange(63)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id("chemical"))));

        BOBMAZON_DELIVERY =
                Reg.entity(
                        "entity_bobmazon_delivery",
                        () ->
                                EntityType.Builder.<EntityBobmazon>of(
                                                EntityBobmazon::new, MobCategory.MISC)
                                        .noLootTable()
                                        .sized(1.0F, 3.0F)
                                        .clientTrackingRange(63)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id("entity_bobmazon_delivery"))));

        SATELLITE_POD =
                Reg.entity(
                        "entity_miner_lander",
                        () ->
                                EntityType.Builder.<EntitySatellitePod>of(
                                                EntitySatellitePod::new, MobCategory.MISC)
                                        .noLootTable()
                                        .fireImmune()
                                        .sized(0.95F, 3.75F)
                                        .clientTrackingRange(63)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id("entity_miner_lander"))));

        MISSILE_ANTI =
                Reg.entity(
                        "entity_missile_anti",
                        () ->
                                EntityType.Builder.<EntityMissileAntiBallistic>of(
                                                EntityMissileAntiBallistic::new, MobCategory.MISC)
                                        .noLootTable()
                                        .sized(1.5F, 1.5F)
                                        .clientTrackingRange(63)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id("entity_missile_anti"))));

        BLACK_HOLE =
                Reg.entity(
                        "entity_black_hole",
                        () ->
                                EntityType.Builder.<EntityBlackHole>of(
                                                EntityBlackHole::new, MobCategory.MISC)
                                        .noLootTable()
                                        .fireImmune()
                                        .sized(0.6F, 1.8F)
                                        .clientTrackingRange(16)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id("entity_black_hole"))));

        DIGAMMA_QUASAR =
                Reg.entity(
                        "entity_digamma_quasar",
                        () ->
                                EntityType.Builder.<EntityQuasar>of(
                                                EntityQuasar::new, MobCategory.MISC)
                                        .noLootTable()
                                        .sized(0.6F, 1.8F)
                                        .clientTrackingRange(16)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id("entity_digamma_quasar"))));

        EMP_BLAST =
                Reg.entity(
                        "entity_emp_blast",
                        () ->
                                EntityType.Builder.<EntityEMPBlast>of(
                                                EntityEMPBlast::new, MobCategory.MISC)
                                        .noLootTable()
                                        .sized(1.5F, 1.5F)
                                        .clientTrackingRange(63)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id("entity_emp_blast"))));

        EMP_LOGIC =
                Reg.entity(
                        "entity_emp_logic",
                        () ->
                                EntityType.Builder.<EntityEMP>of(EntityEMP::new, MobCategory.MISC)
                                        .noLootTable()
                                        .sized(0.6F, 1.8F)
                                        .clientTrackingRange(63)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id("entity_emp_logic"))));

        BALEFIRE =
                Reg.entity(
                        "entity_balefire",
                        () ->
                                EntityType.Builder.<EntityBalefire>of(
                                                EntityBalefire::new, MobCategory.MISC)
                                        .noLootTable()
                                        .sized(0.6F, 1.8F)
                                        .clientTrackingRange(63)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id("entity_balefire"))));

        LASER_BLAST =
                Reg.entity(
                        "entity_laser_blast",
                        () ->
                                EntityType.Builder.<EntityDeathBlast>of(
                                                EntityDeathBlast::new, MobCategory.MISC)
                                        .noLootTable()
                                        .sized(0.6F, 1.8F)
                                        .clientTrackingRange(63)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id("entity_laser_blast"))));
        ORBITAL_LASER =
                Reg.entity(
                        "entity_orbital_laser",
                        () ->
                                EntityType.Builder.<EntityOrbitalLaser>of(
                                                EntityOrbitalLaser::new, MobCategory.MISC)
                                        .noLootTable()
                                        .sized(0.6F, 1.8F)
                                        .clientTrackingRange(63)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id("entity_orbital_laser"))));

        TOM_BUST =
                Reg.entity(
                        "entity_tom_bust",
                        () ->
                                EntityType.Builder.<EntityTomBlast>of(
                                                EntityTomBlast::new, MobCategory.MISC)
                                        .noLootTable()
                                        .sized(0.6F, 1.8F)
                                        .clientTrackingRange(63)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id("entity_tom_bust"))));

        RAGING_VORTEX =
                Reg.entity(
                        "entity_raging_vortex",
                        () ->
                                EntityType.Builder.<EntityRagingVortex>of(
                                                EntityRagingVortex::new, MobCategory.MISC)
                                        .noLootTable()
                                        .fireImmune()
                                        .sized(0.6F, 1.8F)
                                        .clientTrackingRange(16)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id("entity_raging_vortex"))));

        MOONSTONE_BLAST =
                Reg.entity(
                        "entity_moonstone_blast",
                        () ->
                                EntityType.Builder.<EntityCloudTom>of(
                                                EntityCloudTom::new, MobCategory.MISC)
                                        .noLootTable()
                                        .fireImmune()
                                        .sized(1.0F, 4.0F)
                                        .clientTrackingRange(63)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id("entity_moonstone_blast"))));

        ARTILLERY_SHELL =
                Reg.entity(
                        "entity_artillery_shell",
                        () ->
                                EntityType.Builder.<EntityArtilleryShell>of(
                                                EntityArtilleryShell::new, MobCategory.MISC)
                                        .noLootTable()
                                        .sized(0.5F, 0.5F)
                                        .clientTrackingRange(63)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id("entity_artillery_shell"))));

        HIMARS =
                Reg.entity(
                        "entity_himars",
                        () ->
                                EntityType.Builder.<EntityArtilleryRocket>of(
                                                EntityArtilleryRocket::new, MobCategory.MISC)
                                        .noLootTable()
                                        .sized(0.25F, 0.25F)
                                        .clientTrackingRange(63)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id("entity_himars"))));

        BEAM_BOMB =
                Reg.entity(
                        "entity_beam_bomb",
                        () ->
                                EntityType.Builder.<EntityB92Beam>of(
                                                EntityB92Beam::new, MobCategory.MISC)
                                        .noLootTable()
                                        .sized(0.5F, 0.5F)
                                        .clientTrackingRange(63)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id("entity_beam_bomb"))));

        BULLET =
                Reg.entity(
                        "entity_bullet",
                        () ->
                                EntityType.Builder.<EntityBullet>of(
                                                EntityBullet::new, MobCategory.MISC)
                                        .noLootTable()
                                        .sized(0.5F, 0.5F)
                                        .clientTrackingRange(16)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id("entity_bullet"))));

        BURNING_FOEQ =
                Reg.entity(
                        "entity_burning_foeq",
                        () ->
                                EntityType.Builder.<EntityBurningFOEQ>of(
                                                EntityBurningFOEQ::new, MobCategory.MISC)
                                        .noLootTable()
                                        .sized(0.25F, 0.25F)
                                        .clientTrackingRange(63)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id("entity_burning_foeq"))));

        FALLING_BOMB =
                Reg.entity(
                        "entity_falling_bomb",
                        () ->
                                EntityType.Builder.<EntityFallingNuke>of(
                                                EntityFallingNuke::new, MobCategory.MISC)
                                        .noLootTable()
                                        .sized(0.98F, 0.98F)
                                        .clientTrackingRange(63)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id("entity_falling_bomb"))));

        SIEGE_LASER =
                Reg.entity(
                        "entity_siege_laser",
                        () ->
                                EntityType.Builder.<EntitySiegeLaser>of(
                                                EntitySiegeLaser::new, MobCategory.MISC)
                                        .noLootTable()
                                        .sized(0.25F, 0.25F)
                                        .clientTrackingRange(63)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id("entity_siege_laser"))));

        STRAY_SAW =
                Reg.entity(
                        "entity_stray_saw",
                        () ->
                                EntityType.Builder.<EntitySawblade>of(
                                                EntitySawblade::new, MobCategory.MISC)
                                        .noLootTable()
                                        .sized(1.0F, 1.0F)
                                        .clientTrackingRange(63)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id("entity_stray_saw"))));

        TOM_THE_MOONSTONE =
                Reg.entity(
                        "entity_tom_the_moonstone",
                        () ->
                                EntityType.Builder.<EntityTom>of(EntityTom::new, MobCategory.MISC)
                                        .noLootTable()
                                        .sized(0.25F, 0.25F)
                                        .clientTrackingRange(63)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id("entity_tom_the_moonstone"))));

        SOYUZ =
                Reg.entity(
                        "entity_soyuz",
                        () ->
                                EntityType.Builder.of(EntitySoyuz::new, MobCategory.MISC)
                                        .noLootTable()
                                        .fireImmune()
                                        .sized(5.0F, 50.0F)
                                        .clientTrackingRange(63)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id("entity_soyuz"))));
        SOYUZ_CAPSULE =
                Reg.entity(
                        "entity_soyuz_capsule",
                        () ->
                                EntityType.Builder.of(EntitySoyuzCapsule::new, MobCategory.MISC)
                                        .noLootTable()
                                        .fireImmune()
                                        .sized(1.0F, 1.0F)
                                        .clientTrackingRange(63)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id("entity_soyuz_capsule"))));
        CHOPPER_MINE =
                Reg.entity(
                        "entity_chopper_mine",
                        () ->
                                EntityType.Builder.<EntityChopperMine>of(
                                                EntityChopperMine::new, MobCategory.MISC)
                                        .noLootTable()
                                        .fireImmune()
                                        .sized(12.0F, 12.0F)
                                        .clientTrackingRange(63)
                                        .updateInterval(1)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id("entity_chopper_mine"))));
    }

    private static void spawn(
            RegistryHandle<? extends EntityType<?>> type,
            BiomeTarget biomes,
            int weight,
            int min,
            int max) {
        Services.BIOME_MODIFIER.addSpawn(biomes, NO_DEFAULT_MONSTERS, type, weight, min, max);
    }

    private static TagKey<Biome> convention(String path) {
        return TagKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath("c", path));
    }

    private static <T extends EntityMissileBaseNT> Reg.EntityHandle<T> missile(
            String name, EntityType.EntityFactory<T> factory) {

        return Reg.entity(
                name,
                () ->
                        EntityType.Builder.of(factory, MobCategory.MISC)
                                .noLootTable()
                                .fireImmune()
                                .sized(1.5F, 1.5F)
                                .clientTrackingRange(63)
                                .updateInterval(1)
                                .build(
                                        ResourceKey.create(
                                                Registries.ENTITY_TYPE, Library.id(name))));
    }
}
