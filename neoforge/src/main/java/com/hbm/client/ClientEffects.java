// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client;

import com.hbm.client.particle.ContrailVisuals;
import com.hbm.client.particle.DebrisVisuals;
import com.hbm.client.particle.ParticleGasFlame;
import com.hbm.client.particle.ParticleRadiationFog;
import com.hbm.client.particle.RadFogVisuals;
import com.hbm.client.particle.RocketFlameVisuals;
import com.hbm.packet.toclient.JetpackParticlePayload;
import com.hbm.particle.CoolingTowerParticleOptions;
import com.hbm.particle.DebrisChunk;
import com.hbm.particle.DebrisMesh;
import com.hbm.particle.HbmEffectNT;
import com.hbm.particle.HbmParticles;
import com.hbm.particle.ParticleContrail;
import com.hbm.particle.ParticleDeadLeaf;
import com.hbm.particle.ParticleDebris;
import com.hbm.particle.ParticleExSmoke;
import com.hbm.particle.ParticleMukeWave;
import com.hbm.particle.ParticleRocketFlame;
import com.hbm.sound.ModSounds;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

public final class ClientEffects {

    private ClientEffects() {}

    public static void spawnGasFlame(
            Level level,
            double x,
            double y,
            double z,
            double motionX,
            double motionY,
            double motionZ,
            float scale) {
        if (level instanceof ClientLevel client) {
            ParticleGasFlame.spawn(client, x, y, z, motionX, motionY, motionZ, scale);
        }
    }

    public static void spawn(
            HbmEffectNT effect, Level level, double x, double y, double z, float scale) {
        switch (effect) {
            case RadFog -> spawnRadFog(level, x, y, z);
            case ExplosionSmall -> {
                spawnMushroom(level, x, y, z, 10, 2F, 0.5F, 25F, 150F);
                spawnDebris(level, x, y, z, 5, 8, 20, 0.75F, 1F, -2F);
            }
            case ExplosionStandard -> {
                spawnMushroom(level, x, y, z, 15, 5F, 1F, 45F, 200F);
                spawnDebris(level, x, y, z, 10, 16, 50, 1F, 3F, -2F);
            }
            case ExplosionLarge -> {
                spawnMushroom(level, x, y, z, 30, 6.5F, 2F, 65F, 350F);
                spawnDebris(level, x, y, z, 25, 16, 50, 1.25F, 3F, -2F);
            }

            case RBMKMush -> spawnRBMKMush(level, x, y, z, scale);
            case GasFlame -> spawnGasFlame(level, x, y, z, 0.05D);
            case GasFlameFlare -> spawnGasFlame(level, x, y, z, 0.15D);
            case GasFlameVent -> spawnGasFlame(level, x, y, z, 0D);
            case AnnihilatorFlame -> spawnAnnihilatorFlame(level, x, y, z);

            case Hadron ->
                    level.addParticle(HbmParticles.HADRON.get(), true, false, x, y, z, 0D, 0D, 0D);
            case DeadLeaf -> spawnDeadLeaf(level, x, y, z);
        }
    }

    private static void spawnDeadLeaf(Level level, double x, double y, double z) {
        if (!(level instanceof ClientLevel client)) return;
        Minecraft.getInstance().particleEngine.add(new ParticleDeadLeaf(client, x, y, z));
    }

    private static void spawnGasFlame(Level level, double x, double y, double z, double spread) {
        RandomSource rand = level.getRandom();
        level.addParticle(
                HbmParticles.GAS_FLAME.get(),
                true,
                false,
                x,
                y,
                z,
                rand.nextGaussian() * spread,
                0.2D,
                rand.nextGaussian() * spread);
    }

    public static void spawnPlaneFlame(Level level, double x, double y, double z) {
        level.addParticle(HbmParticles.GAS_FLAME.get(), true, false, x, y, z, 0.0D, 0.1D, 0.0D);
    }

    private static void spawnAnnihilatorFlame(Level level, double x, double y, double z) {
        RandomSource rand = level.getRandom();
        level.addParticle(
                HbmParticles.GAS_FLAME.get(),
                true,
                false,
                x,
                y,
                z,
                rand.nextGaussian() * 0.05D,
                0.1D,
                rand.nextGaussian() * 0.05D);
    }

    public static void radiationAura(int count) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;
        RandomSource rand = player.level().getRandom();
        for (int i = 0; i < count; i++) {
            Particle fx =
                    mc.particleEngine.createParticle(
                            ParticleTypes.MYCELIUM,
                            player.getX() + rand.nextGaussian() * 4,
                            player.getEyeY() + rand.nextGaussian() * 2,
                            player.getZ() + rand.nextGaussian() * 4,
                            0D,
                            0D,
                            0D);
            if (fx == null) continue;
            if (fx instanceof SingleQuadParticle quad) quad.setColor(0F, 0.75F, 1F);
            fx.setParticleSpeed(rand.nextGaussian(), rand.nextGaussian(), rand.nextGaussian());
        }
    }

    public static void craterAura(Player player) {
        if (player != Minecraft.getInstance().player) return;
        RandomSource rand = player.getRandom();
        for (int i = 0; i < 3; i++) {
            player.level()
                    .addParticle(
                            ParticleTypes.MYCELIUM,
                            player.getX() + rand.nextGaussian() * 3,
                            player.getEyeY() + rand.nextGaussian() * 2,
                            player.getZ() + rand.nextGaussian() * 3,
                            0D,
                            0D,
                            0D);
        }
    }

    public static void spawnColorDust(
            Level level,
            double x,
            double y,
            double z,
            double mx,
            double my,
            double mz,
            float r,
            float g,
            float b) {
        Minecraft mc = Minecraft.getInstance();
        Particle fx =
                mc.particleEngine.createParticle(
                        new BlockParticleOption(
                                ParticleTypes.BLOCK, Blocks.WOOL.white().defaultBlockState()),
                        x,
                        y,
                        z,
                        mx,
                        my + 0.2,
                        mz);
        if (fx != null) {
            fx.setParticleSpeed(mx, my + 0.2, mz);
            if (fx instanceof SingleQuadParticle quad) quad.setColor(r, g, b);
            fx.setLifetime(10 + level.getRandom().nextInt(20));
        }
    }

    public static void spawnJetpackPlume(Level level, int playerId, int mode) {
        if (!(level.getEntity(playerId) instanceof Player player)) return;
        if (mode == JetpackParticlePayload.REGULAR || mode == JetpackParticlePayload.VECTOR) {
            spawnFueledJetpackPlume(level, player, mode == JetpackParticlePayload.VECTOR);
            return;
        }
        if (mode != JetpackParticlePayload.DNS && mode != JetpackParticlePayload.BJ) {
            throw new IllegalArgumentException();
        }
        ParticleStatus setting = Minecraft.getInstance().options.particles().get();
        if (setting == ParticleStatus.MINIMAL) return;
        boolean bj = mode == JetpackParticlePayload.BJ;

        float angle = (float) -Math.toRadians(player.yBodyRot);
        double sin = Math.sin(angle);
        double cos = Math.cos(angle);
        double ox = 0.125D * cos;
        double oz = -0.125D * sin;

        double ix = player.getX();
        double iy;
        double iz = player.getZ();
        if (bj) {
            ix += -0.3125D * sin;
            iz += -0.3125D * cos;

            iy = player.getY() + 1.62D + 0.12D - 0.9375D;
        } else {

            iy = player.getY();
        }

        if (setting == ParticleStatus.ALL)
            spawnJetpackGroundWash(level, new Vec3(ix, iy, iz), new Vec3(0D, -1D, 0D));

        Vec3 motion = player.getDeltaMovement();
        float r = bj ? 0.8F : 0.01F;
        float g = bj ? 0.5F : 1.0F;
        float b = 1.0F;
        spawnReddust(ix + ox, iy, iz + oz, motion, r, g, b);
        spawnReddust(ix - ox, iy, iz - oz, motion, r, g, b);
    }

    private static void spawnReddust(
            double x, double y, double z, Vec3 motion, float r, float g, float b) {
        Particle fx =
                Minecraft.getInstance()
                        .particleEngine
                        .createParticle(
                                new DustParticleOptions(ARGB.colorFromFloat(1F, r, g, b), 1F),
                                x,
                                y,
                                z,
                                0D,
                                0D,
                                0D);
        if (fx != null) fx.setParticleSpeed(motion.x, motion.y, motion.z);
    }

    private static void spawnFueledJetpackPlume(Level level, Player player, boolean vectorized) {
        Minecraft mc = Minecraft.getInstance();
        ParticleStatus setting = mc.options.particles().get();
        if (setting == ParticleStatus.MINIMAL) return;

        float angle = (float) -Math.toRadians(player.yBodyRot);
        Vec3 back = new Vec3(0D, 0D, -0.25D).yRot(angle);
        Vec3 offset = new Vec3(0.125D, 0D, 0D).yRot(angle);
        Vec3 origin =
                new Vec3(player.getX() + back.x, player.getEyeY() - 1D, player.getZ() + back.z);
        Vec3 exhaust = vectorized ? player.getLookAngle().scale(-0.1D) : new Vec3(0D, -0.2D, 0D);
        if (setting == ParticleStatus.ALL) {
            spawnJetpackGroundWash(level, origin, exhaust.normalize());
        }

        Vec3 motion = player.getDeltaMovement().add(exhaust.scale(2D));
        for (int side = 1; side >= -1; side -= 2) {
            Vec3 at = origin.add(offset.scale(side));
            mc.particleEngine.createParticle(
                    ParticleTypes.FLAME,
                    at.x,
                    at.y,
                    at.z,
                    Mth.clamp(motion.x, -5D, 5D),
                    Mth.clamp(motion.y, -5D, 5D),
                    Mth.clamp(motion.z, -5D, 5D));
        }
        if (setting == ParticleStatus.ALL) {
            for (int side = 1; side >= -1; side -= 2) {
                Vec3 at = origin.add(offset.scale(side));
                mc.particleEngine.createParticle(
                        ParticleTypes.SMOKE,
                        at.x,
                        at.y,
                        at.z,
                        Mth.clamp(motion.x, -10D, 10D),
                        Mth.clamp(motion.y, -10D, 10D),
                        Mth.clamp(motion.z, -10D, 10D));
            }
        }
    }

    private static void spawnJetpackGroundWash(Level level, Vec3 from, Vec3 direction) {
        Vec3 to = from.add(direction.scale(10D));
        BlockHitResult hit =
                level.clip(
                        new ClipContext(
                                from,
                                to,
                                ClipContext.Block.COLLIDER,
                                ClipContext.Fluid.NONE,
                                CollisionContext.empty()));
        if (hit.getType() != HitResult.Type.BLOCK || hit.getDirection() != Direction.UP) return;

        BlockState state = level.getBlockState(hit.getBlockPos());
        double distance = from.distanceTo(hit.getLocation());
        double speed = 0.75D - distance * 0.075D;
        RandomSource rand = level.getRandom();
        Vec3 velocity = new Vec3(speed, 0D, 0D);
        for (int i = 0; i < 10 - distance; i++) {
            velocity = velocity.yRot(rand.nextFloat() * (float) Math.PI * 2F);
            Particle fx =
                    Minecraft.getInstance()
                            .particleEngine
                            .createParticle(
                                    new BlockParticleOption(ParticleTypes.BLOCK, state),
                                    hit.getLocation().x,
                                    hit.getLocation().y + 0.1D,
                                    hit.getLocation().z,
                                    velocity.x,
                                    0.1D,
                                    velocity.z);
            if (fx != null) fx.setParticleSpeed(velocity.x, 0.1D, velocity.z);
        }
    }

    public static void spawnBlockDust(
            Level level,
            double x,
            double y,
            double z,
            double mx,
            double my,
            double mz,
            BlockState state) {
        Minecraft mc = Minecraft.getInstance();
        Particle fx =
                mc.particleEngine.createParticle(
                        new BlockParticleOption(ParticleTypes.BLOCK, state),
                        x,
                        y,
                        z,
                        mx,
                        my + 0.2,
                        mz);
        if (fx != null) {
            fx.setParticleSpeed(mx, my + 0.2, mz);
            fx.setLifetime(50 + level.getRandom().nextInt(50));
        }
    }

    private static void spawnMushroom(
            Level level,
            double x,
            double y,
            double z,
            int cloudCount,
            float cloudScale,
            float cloudSpeedMult,
            float waveScale,
            float soundRange) {
        RandomSource rand = level.getRandom();
        Minecraft mc = Minecraft.getInstance();
        float distance = (float) mc.player.getEyePosition().distanceTo(new Vec3(x, y, z));
        if (distance <= soundRange) {
            var sound =
                    distance <= soundRange * 0.4
                            ? ModSounds.WEAPON_EXPLOSION_LARGE_NEAR
                            : ModSounds.WEAPON_EXPLOSION_LARGE_FAR;

            mc.getSoundManager()
                    .playDelayed(
                            new SimpleSoundInstance(
                                    sound.get(),
                                    SoundSource.BLOCKS,
                                    1000F,
                                    0.9F + rand.nextFloat() * 0.2F,
                                    rand,
                                    x,
                                    y,
                                    z),
                            (int) (distance / (17.15D * 0.5D)));
        }
        if (level instanceof ClientLevel client) {
            Minecraft.getInstance()
                    .particleEngine
                    .add(
                            new ParticleMukeWave(client, x, y + 2, z)
                                    .setup(waveScale, (int) (25F * waveScale / 45F)));
        }
        for (int i = 0; i < cloudCount; i++) {
            spawnRocketFlame(
                    level,
                    x,
                    y,
                    z,
                    cloudScale,
                    rand.nextGaussian() * 0.5D * cloudSpeedMult,
                    rand.nextDouble() * 3D * cloudSpeedMult,
                    rand.nextGaussian() * 0.5D * cloudSpeedMult,
                    70 + rand.nextInt(20));
        }
    }

    public static void spawnExtBlockDust(double x, double y, double z, BlockState state) {
        spawnExtBlockDust(Minecraft.getInstance().level, x, y, z, 0D, -0.2D, 0D, state);
    }

    public static void spawnExtBlockDust(
            Level level,
            double x,
            double y,
            double z,
            double mx,
            double my,
            double mz,
            BlockState state) {
        Minecraft mc = Minecraft.getInstance();
        Particle fx =
                mc.particleEngine.createParticle(
                        new BlockParticleOption(ParticleTypes.BLOCK, state),
                        x,
                        y,
                        z,
                        mx,
                        my + 0.2D,
                        mz);
        if (fx == null) return;
        fx.setParticleSpeed(mx, my + 0.2D, mz);
        fx.setLifetime(10 + level.getRandom().nextInt(20));
        ((SingleQuadParticle) fx).setColor(0.8F, 0.8F, 0.8F);
    }

    public static void spawnNoClipSmoke(double x, double y, double z, int lifetime) {
        Particle smoke =
                Minecraft.getInstance()
                        .particleEngine
                        .createParticle(ParticleTypes.SMOKE, x, y, z, 0, 0, 0);
        if (smoke == null) return;
        smoke.hasPhysics = false;
        smoke.setLifetime(lifetime);
    }

    private static void spawnDebris(
            Level level,
            double x,
            double y,
            double z,
            int count,
            int size,
            int retry,
            float velocity,
            float deviation,
            float verticalOffset) {
        if (!(level instanceof ClientLevel client)) return;
        RandomSource rand = level.getRandom();
        for (int c = 0; c < count; c++) {
            double oX = rand.nextGaussian() * deviation;
            double oZ = rand.nextGaussian() * deviation;
            BlockPos centre =
                    BlockPos.containing(x + oX + 0.5, y + verticalOffset + 0.5, z + oZ + 0.5);
            float pitch = (float) -Math.toRadians(45 + rand.nextFloat() * 25);
            float yaw = (float) (rand.nextDouble() * Math.PI * 2);
            double horizontal = velocity * Mth.cos(pitch);
            double mY = -velocity * Mth.sin(pitch);
            double mX = horizontal * Mth.cos(yaw);
            double mZ = -horizontal * Mth.sin(yaw);
            DebrisChunk chunk = ParticleDebris.carve(client, centre, size, retry, rand);

            Minecraft mc = Minecraft.getInstance();
            CompletableFuture.supplyAsync(() -> DebrisMesh.bake(chunk), Util.backgroundExecutor())
                    .whenComplete(
                            (meshes, error) ->
                                    mc.execute(
                                            () -> {
                                                if (error != null)
                                                    throw new IllegalStateException(error);
                                                if (mc.level != client) return;
                                                ParticleDebris debris =
                                                        new ParticleDebris(
                                                                client, x, y, z, mX, mY, mZ, chunk,
                                                                meshes);
                                                mc.particleEngine.add(debris);
                                                if (DebrisVisuals.tryInstance(client, debris))
                                                    debris.markInstanced();
                                            }));
        }
    }

    private static void spawnRBMKMush(Level level, double x, double y, double z, float scale) {
        level.addParticle(HbmParticles.RBMK_MUSH.get(), true, false, x, y, z, 0.0D, scale, 0.0D);
        level.addParticle(ParticleTypes.EXPLOSION_EMITTER, true, false, x, y, z, 0.0D, 0.0D, 0.0D);
    }

    private static void spawnRadFog(Level level, double x, double y, double z) {
        if (RadFogVisuals.trySpawnInstanced(level, x, y, z)) return;
        spawnRadFogVanilla(level, x, y, z);
    }

    private static void spawnRadFogVanilla(Level level, double x, double y, double z) {
        ParticleRadiationFog.spawnCluster(level, x, y, z);
    }

    public static void radialDigamma(Level level, double x, double y, double z, int count) {
        count = Math.max(1, count);
        RandomSource rand = level.getRandom();
        float angle = rand.nextFloat() * Mth.TWO_PI;
        float step = Mth.TWO_PI / count;
        for (int i = 0; i < count; i++) {
            level.addParticle(
                    HbmParticles.DIGAMMA_SMOKE.get(),
                    true,
                    false,
                    x,
                    y,
                    z,
                    Math.cos(angle) * 2.0,
                    0.0,
                    Math.sin(angle) * 2.0);
            angle += step;
        }
    }

    public static void mistTower(Level level, double x, double y, double z, int color) {
        CoolingTowerParticleOptions opts =
                new CoolingTowerParticleOptions.Builder()
                        .setBaseScale(0.75F)
                        .setMaxScale(2F)
                        .setLift(0.5F)
                        .setLife(50 + level.getRandom().nextInt(10))
                        .setColor(color)
                        .build();
        level.addParticle(opts, x, y, z, 0, 0, 0);
    }

    public static void spawnRocketFlame(
            Level level,
            double x,
            double y,
            double z,
            float scale,
            double mX,
            double mY,
            double mZ,
            int maxAge) {
        if (RocketFlameVisuals.trySpawnInstanced(level, x, y, z, scale, mX, mY, mZ, maxAge)) return;
        if (!(level instanceof ClientLevel client)) return;
        ParticleRocketFlame fx =
                new ParticleRocketFlame(client, x, y, z).setScale(scale).setMaxAge(maxAge).noClip();
        fx.setParticleSpeed(mX, mY, mZ);
        Minecraft.getInstance().particleEngine.add(fx);
    }

    public static void spawnSmokeShockRand(
            Level level, double x, double y, double z, int count, double strength) {
        if (!(level instanceof ClientLevel client)) return;
        RandomSource rand = client.getRandom();
        double angle = rand.nextInt(360);
        double step = 360 / count;

        for (int i = 0; i < count; i++) {
            double r = rand.nextDouble();
            ParticleExSmoke fx = new ParticleExSmoke(client, x, y, z);
            fx.setParticleSpeed(Math.cos(angle) * strength * r, 0, Math.sin(angle) * strength * r);
            Minecraft.getInstance().particleEngine.add(fx);
            angle += step;
        }
    }

    public static void spawnContrail(Level level, double x, double y, double z, Contrail type) {
        if (ContrailVisuals.trySpawnInstanced(level, x, y, z, type.r, type.g, type.b, type.scale))
            return;
        if (!(level instanceof ClientLevel client)) return;
        Minecraft.getInstance()
                .particleEngine
                .add(new ParticleContrail(client, x, y, z, type.r, type.g, type.b, type.scale));
    }

    public enum Contrail {
        ABM(0F, 0F, 0F, 1F),
        KEROSENE(0F, 0F, 0F, 1F),
        SOLID(0.3F, 0.2F, 0.05F, 1F),
        HYDROGEN(0.7F, 0.7F, 0.7F, 1F),
        BALEFIRE(0.2F, 0.7F, 0.2F, 1F);

        final float r;
        final float g;
        final float b;
        final float scale;

        Contrail(float r, float g, float b, float scale) {
            this.r = r;
            this.g = g;
            this.b = b;
            this.scale = scale;
        }
    }
}
