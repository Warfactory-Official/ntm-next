// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.weapon.grenade;

import com.hbm.entity.effect.EntityCloudFleija;
import com.hbm.entity.effect.EntityFireLingering;
import com.hbm.entity.grenade.EntityGrenadeUniversal;
import com.hbm.entity.logic.EntityNukeExplosionMK3;
import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.entity.projectile.EntityBulletBeamBase;
import com.hbm.explosion.ExplosionNukeGeneric;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.*;
import com.hbm.handler.threading.TargetPoint;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.factory.Lego;
import com.hbm.items.weapon.sedna.factory.XFactoryCatapult;
import com.hbm.packet.toclient.HazePayload;
import com.hbm.packet.toclient.MukePayload;
import com.hbm.packet.toclient.PlasmaBlastPayload;
import com.hbm.platform.Services;
import com.hbm.saveddata.satellites.SatelliteDetector;
import com.hbm.sound.ModSounds;
import com.hbm.util.DamageClass;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class ItemGrenadeFilling extends Item {

    public static BulletConfig fragmentation;
    public static BulletConfig pellets;
    public static BulletConfig pellets_heavy;
    public static BulletConfig laser;

    public final EnumGrenadeFilling type;

    public ItemGrenadeFilling(Item.Properties properties, EnumGrenadeFilling type) {
        super(properties);
        this.type = type;

        if (fragmentation != null) return;
        fragmentation =
                new BulletConfig()
                        .setLife(3)
                        .setThresholdNegation(5F)
                        .setRicochetAngle(90)
                        .setRicochetCount(2);
        pellets =
                new BulletConfig()
                        .setLife(100)
                        .setGrav(0.04D)
                        .setVel(1.5F)
                        .setOnImpact(
                                (bullet, impact) -> {
                                    if (bullet.tickCount < 2) return;
                                    Lego.tinyExplode(bullet, impact, 1.5F);
                                    bullet.discard();
                                });
        pellets_heavy =
                new BulletConfig()
                        .setLife(100)
                        .setGrav(0.04D)
                        .setVel(1.5F)
                        .setOnImpact(
                                (bullet, impact) -> {
                                    if (bullet.tickCount < 2) return;
                                    Lego.standardExplode(bullet, impact, 5F);
                                    bullet.discard();
                                });
        laser =
                new BulletConfig()
                        .setBeam()
                        .setupDamageClass(DamageClass.LASER)
                        .setLife(3)
                        .setRenderRotations(false)
                        .setThresholdNegation(10F)
                        .setOnBeamImpact(BulletConfig.LAMBDA_STANDARD_BEAM_HIT);
    }

    public static void explode(EntityGrenadeUniversal grenade) {
        switch (grenade.getFilling()) {
            case POWDER -> standardExplode(grenade, 5F, 10F, 5F, 0F);
            case HE -> standardExplode(grenade, 7.5F, 25F, 10F, 0.1F);
            case DEMO -> explodeDemo(grenade);
            case INC -> explodeIncendiary(grenade);
            case WP -> explodeWhitePhosphorus(grenade);
            case CLUSTER -> explodeCluster(grenade);
            case EMP -> explodeEmp(grenade);
            case PLASMA ->
                    explodeStandardEnergy(grenade, 50F, 5F, DamageClass.PLASMA, 0.5F, 1F, 0.5F, 4F);
            case LASER -> explodeLaser(grenade);
            case CLUSTER_HEAVY -> explodeClusterHeavy(grenade);
            case NUCLEAR -> explodeNuclear(grenade);
            case NUCLEAR_DEMO -> explodeNuclearDemo(grenade);
            case SCHRAB -> explodeSchrabidium(grenade);
        }
    }

    private static void explodeDemo(EntityGrenadeUniversal grenade) {
        ExplosionVNT vnt =
                new ExplosionVNT(
                        grenade.level(),
                        grenade.getX(),
                        grenade.getY(),
                        grenade.getZ(),
                        5F,
                        grenade.getThrower());
        vnt.setBlockAllocator(new BlockAllocatorStandard());
        vnt.setBlockProcessor(new BlockProcessorStandard());
        vnt.setEntityProcessor(new EntityProcessorCrossSmooth(1, 10F));
        vnt.setPlayerProcessor(new PlayerProcessorStandard());
        vnt.setSFX(new ExplosionEffectWeapon(10, 2.5F, 1F));
        vnt.explode();
    }

    private static void explodeCluster(EntityGrenadeUniversal grenade) {
        standardExplode(grenade, 7.5F, 15F, 10F, 0.1F);
        int frags = 30;
        if (grenade.getShell() == ItemGrenadeShell.EnumGrenadeShell.FRAG) frags *= 1.25;
        for (int i = 0; i < frags; i++) {
            EntityBulletBaseMK4 bullet =
                    new EntityBulletBaseMK4(
                            grenade.level(),
                            pellets,
                            15F,
                            0F,
                            grenade.level().getRandom().nextFloat() * 2F * (float) Math.PI,
                            (grenade.level().getRandom().nextFloat() * 0.5F + 0.5F)
                                    * (float) Math.PI);
            bullet.setPos(grenade.getX(), grenade.getY() + 0.05D, grenade.getZ());
            bullet.setDeltaMovement(bullet.getDeltaMovement().multiply(0.5D, 0.75D, 0.5D));
            grenade.level().addFreshEntity(bullet);
        }
    }

    private static void explodeClusterHeavy(EntityGrenadeUniversal grenade) {
        standardExplode(grenade, 7.5F, 15F, 10F, 0.1F);
        for (int i = 0; i < 15; i++) {
            EntityBulletBaseMK4 bullet =
                    new EntityBulletBaseMK4(
                            grenade.level(),
                            pellets_heavy,
                            30F,
                            0F,
                            grenade.level().getRandom().nextFloat() * 2F * (float) Math.PI,
                            (grenade.level().getRandom().nextFloat() * 0.5F + 0.5F)
                                    * (float) Math.PI);
            bullet.setPos(grenade.getX(), grenade.getY() + 0.05D, grenade.getZ());
            bullet.setDeltaMovement(bullet.getDeltaMovement().multiply(0.5D, 1.25D, 0.5D));
            grenade.level().addFreshEntity(bullet);
        }
    }

    private static void explodeIncendiary(EntityGrenadeUniversal grenade) {
        Level level = grenade.level();
        standardExplode(grenade, 3F, 10F, 0F, 0F);
        EntityFireLingering fire =
                new EntityFireLingering(level)
                        .setArea(6, 2)
                        .setDuration(200)
                        .setType(EntityFireLingering.TYPE_DIESEL);
        fire.setPos(grenade.getX(), grenade.getY(), grenade.getZ());
        level.addFreshEntity(fire);
        igniteAround(grenade, 2);
    }

    private static void explodeWhitePhosphorus(EntityGrenadeUniversal grenade) {
        Level level = grenade.level();
        standardExplode(grenade, 3F, 10F, 0F, 0F);
        EntityFireLingering fire =
                new EntityFireLingering(level)
                        .setArea(6, 2)
                        .setDuration(600)
                        .setType(EntityFireLingering.TYPE_PHOSPHORUS);
        fire.setPos(grenade.getX(), grenade.getY(), grenade.getZ());
        level.addFreshEntity(fire);
        igniteAround(grenade, 3);
        if (!(level instanceof ServerLevel server)) return;
        for (int i = 0; i < 3; i++) {
            Services.NETWORK.sendToAllAround(
                    new HazePayload(
                            grenade.getX() + server.getRandom().nextGaussian() * 4D,
                            grenade.getY(),
                            grenade.getZ() + server.getRandom().nextGaussian() * 4D),
                    new TargetPoint(server, grenade.getX(), grenade.getY(), grenade.getZ(), 150));
        }
    }

    private static void igniteAround(EntityGrenadeUniversal grenade, int radius) {
        if (!(grenade.level() instanceof ServerLevel server)) return;
        BlockPos origin = BlockPos.containing(grenade.getX(), grenade.getY(), grenade.getZ());
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx++)
            for (int dy = -radius; dy <= radius; dy++)
                for (int dz = -radius; dz <= radius; dz++) {
                    pos.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    if (!server.getBlockState(pos).isAir()) continue;
                    for (Direction direction : Direction.VALUES) {
                        BlockPos neighbor = pos.relative(direction);
                        if (Services.PLATFORM.isFlammable(
                                server,
                                neighbor,
                                server.getBlockState(neighbor),
                                direction.getOpposite())) {
                            server.setBlock(pos, BaseFireBlock.getState(server, pos), 3);
                            break;
                        }
                    }
                }
    }

    private static void explodeEmp(EntityGrenadeUniversal grenade) {
        explodeStandardEnergy(grenade, 15F, 3F, DamageClass.ELECTRIC, 0.5F, 0.5F, 1F, 3F);
        ExplosionNukeGeneric.empBlast(
                grenade.level(),
                BlockPos.containing(grenade.getX(), grenade.getY(), grenade.getZ()),
                5);
    }

    private static void explodeLaser(EntityGrenadeUniversal grenade) {
        tinyExplode(grenade, 2F, 5F);

        double x = grenade.getX();
        double y = grenade.getY() + 0.125D;
        double z = grenade.getZ();

        double range = 15D;
        List<LivingEntity> potentialTargets =
                grenade.level()
                        .getEntitiesOfClass(
                                LivingEntity.class, new AABB(x, y, z, x, y, z).inflate(range));
        Collections.shuffle(potentialTargets);

        for (LivingEntity target : potentialTargets) {
            if (target == grenade.getThrower()) continue;

            Vec3 delta =
                    new Vec3(
                            target.getX() - x,
                            target.getY() + target.getBbHeight() / 2F - y,
                            target.getZ() - z);
            if (delta.length() > range) continue;
            EntityBulletBeamBase sub = new EntityBulletBeamBase(grenade.level(), laser, 30F);
            sub.thrower = grenade.getThrower();
            sub.setPos(x, y, z);
            sub.setRotationsFromVector(delta);
            sub.performHitscanExternal(delta.length());
            grenade.level().addFreshEntity(sub);
        }
    }

    private static void explodeNuclear(EntityGrenadeUniversal grenade) {
        ExplosionVNT vnt =
                new ExplosionVNT(
                        grenade.level(), grenade.getX(), grenade.getY(), grenade.getZ(), 10F);
        vnt.setEntityProcessor(new EntityProcessorCrossSmooth(2, 100).withRangeMod(1.5F));
        vnt.setPlayerProcessor(new PlayerProcessorStandard());
        vnt.explode();
        XFactoryCatapult.incrementRad(
                grenade.level(), grenade.getX(), grenade.getY(), grenade.getZ(), 1F);
        spawnMush(grenade);
    }

    private static void explodeNuclearDemo(EntityGrenadeUniversal grenade) {
        ExplosionVNT vnt =
                new ExplosionVNT(
                        grenade.level(), grenade.getX(), grenade.getY(), grenade.getZ(), 10F);
        vnt.setBlockAllocator(new BlockAllocatorStandard(64));
        vnt.setBlockProcessor(new BlockProcessorStandard().withBlockEffect(new BlockMutatorFire()));
        vnt.setEntityProcessor(new EntityProcessorCrossSmooth(2, 50).withRangeMod(1.5F));
        vnt.setPlayerProcessor(new PlayerProcessorStandard());
        vnt.explode();
        XFactoryCatapult.incrementRad(
                grenade.level(), grenade.getX(), grenade.getY(), grenade.getZ(), 1.5F);
        spawnMush(grenade);
    }

    private static void explodeSchrabidium(EntityGrenadeUniversal grenade) {
        Level level = grenade.level();
        EntityNukeExplosionMK3 ex =
                EntityNukeExplosionMK3.statFacFleija(
                        level, grenade.getX(), grenade.getY(), grenade.getZ(), 20);

        if (!ex.isRemoved()) {
            level.playSound(
                    null,
                    grenade.getX(),
                    grenade.getY(),
                    grenade.getZ(),
                    SoundEvents.GENERIC_EXPLODE,
                    SoundSource.BLOCKS,
                    100.0F,
                    level.getRandom().nextFloat() * 0.1F + 0.9F);
            level.addFreshEntity(ex);
            level.addFreshEntity(
                    EntityCloudFleija.statFac(
                            level, 20, grenade.getX(), grenade.getY(), grenade.getZ()));
        }
    }

    public static void spawnMush(EntityGrenadeUniversal grenade) {
        if (grenade.level() instanceof ServerLevel server) {
            SatelliteDetector.reportEvent(
                    server,
                    SatelliteDetector.DURATION_LOW,
                    SatelliteDetector.BurstIntensity.LOW,
                    grenade.getX(),
                    grenade.getZ());
        }
        grenade.level()
                .playSound(
                        null,
                        grenade.getX(),
                        grenade.getY(),
                        grenade.getZ(),
                        ModSounds.GUN_MINI_NUKE_EXPLOSION.get(),
                        SoundSource.BLOCKS,
                        15.0F,
                        1.0F);
        if (!(grenade.level() instanceof ServerLevel server)) return;

        Services.NETWORK.sendToAllAround(
                new MukePayload(
                        grenade.getX(),
                        grenade.getY() + 0.5D,
                        grenade.getZ(),
                        false,
                        server.getRandom().nextInt(100) == 0),
                new TargetPoint(server, grenade.getX(), grenade.getY(), grenade.getZ(), 250));
    }

    public static void explodeStandardEnergy(
            EntityGrenadeUniversal grenade,
            float damage,
            float range,
            DamageClass damageClass,
            float r,
            float g,
            float b,
            float scale) {
        ExplosionVNT vnt =
                new ExplosionVNT(
                        grenade.level(),
                        grenade.getX(),
                        grenade.getY(),
                        grenade.getZ(),
                        range,
                        grenade.getThrower());
        vnt.setEntityProcessor(
                new EntityProcessorCrossSmooth(1, damage).setDamageClass(damageClass));
        vnt.setPlayerProcessor(new PlayerProcessorStandard());
        vnt.explode();
        grenade.level()
                .playSound(
                        null,
                        grenade.getX(),
                        grenade.getY(),
                        grenade.getZ(),
                        ModSounds.UFO_BLAST.get(),
                        SoundSource.BLOCKS,
                        5F,
                        0.9F + grenade.level().getRandom().nextFloat() * 0.2F);

        grenade.level()
                .playSound(
                        null,
                        grenade.getX(),
                        grenade.getY(),
                        grenade.getZ(),
                        SoundEvents.FIREWORK_ROCKET_BLAST,
                        SoundSource.BLOCKS,
                        5F,
                        0.5F);

        if (!(grenade.level() instanceof ServerLevel server)) return;
        float yaw = server.getRandom().nextFloat() * 180F;
        for (int i = 0; i < 3; i++) {
            Services.NETWORK.sendToAllAround(
                    new PlasmaBlastPayload(
                            grenade.getX(),
                            grenade.getY() + 0.125D,
                            grenade.getZ(),
                            r,
                            g,
                            b,
                            -60F + 60F * i,
                            yaw,
                            scale),
                    new TargetPoint(server, grenade.getX(), grenade.getY(), grenade.getZ(), 100));
        }
    }

    public static void standardExplode(
            EntityGrenadeUniversal grenade,
            float range,
            float damage,
            float threshold,
            float resistance) {
        ExplosionVNT vnt =
                new ExplosionVNT(
                        grenade.level(),
                        grenade.getX(),
                        grenade.getY(),
                        grenade.getZ(),
                        range,
                        grenade.getThrower());
        vnt.setEntityProcessor(
                new EntityProcessorCrossSmooth(1, damage).setupPiercing(threshold, resistance));
        vnt.setPlayerProcessor(new PlayerProcessorStandard());
        vnt.setSFX(new ExplosionEffectWeapon(10, 2.5F, 1F));
        vnt.explode();
    }

    public static void tinyExplode(EntityGrenadeUniversal grenade, float range, float damage) {
        ExplosionVNT vnt =
                new ExplosionVNT(
                        grenade.level(),
                        grenade.getX(),
                        grenade.getY(),
                        grenade.getZ(),
                        range,
                        grenade.getThrower());
        vnt.setEntityProcessor(new EntityProcessorCrossSmooth(0.5, damage).setKnockback(0.25D));
        vnt.setPlayerProcessor(new PlayerProcessorStandard());
        vnt.setSFX(new ExplosionEffectTiny());
        vnt.explode();
    }

    public static void standardFragmentation(EntityGrenadeUniversal grenade, float frags) {
        if (grenade.getShell() == ItemGrenadeShell.EnumGrenadeShell.FRAG) frags *= 1.5F;
        for (int i = 0; i < frags; i++) {
            EntityBulletBaseMK4 bullet =
                    new EntityBulletBaseMK4(
                            grenade.level(),
                            fragmentation,
                            10F,
                            0F,
                            grenade.level().getRandom().nextFloat() * 2F * (float) Math.PI,
                            (grenade.level().getRandom().nextFloat() - 0.5F)
                                    * 2F
                                    * (float) Math.PI);
            bullet.setPos(grenade.getX(), grenade.getY() + 0.05D, grenade.getZ());
            grenade.level().addFreshEntity(bullet);
        }
    }

    public enum EnumGrenadeFilling {
        POWDER(0x424242, 0x939176),
        HE(0x595533, 0xA49D62),
        DEMO(0x595533, 0xDD4029),
        INC(0x5A5A5A, 0xFF5F21),
        WP(0xDCDCDC, 0xFF5F21),
        CLUSTER(0x5A5A5A, 0xFFC711),
        EMP(0x93A1AC, 0x00FFFF),
        PLASMA(0x655B2C, 0x4CFF00),
        LASER(0x493A3A, 0xFF0000),
        CLUSTER_HEAVY(0x5A5A5A, 0xFF5F21),
        NUCLEAR(0xDFD7A8, 0xA49D62),
        NUCLEAR_DEMO(0xDFD7A8, 0xDD4029),
        SCHRAB(0x00BDBD, 0x000000);

        private final int bodyColor;
        private final int labelColor;

        EnumGrenadeFilling(int bodyColor, int labelColor) {
            this.bodyColor = bodyColor;
            this.labelColor = labelColor;
        }

        public int bodyColor() {
            return bodyColor;
        }

        public int labelColor() {
            return labelColor;
        }
    }
}
