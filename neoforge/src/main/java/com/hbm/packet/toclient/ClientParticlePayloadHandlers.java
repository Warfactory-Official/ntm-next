// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.packet.toclient;

import com.hbm.blocks.ModBlocks;
import com.hbm.client.ClientEffects;
import com.hbm.client.VanishedEntities;
import com.hbm.client.particle.ParticleHadron;
import com.hbm.items.ModItems;
import com.hbm.particle.*;
import com.hbm.sound.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SimpleAnimatedParticle;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

final class ClientParticlePayloadHandlers {

    private ClientParticlePayloadHandlers() {}

    static void handle(AmatFlashPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.level instanceof ClientLevel level)) return;
        mc.particleEngine.add(
                new ParticleAmatFlash(level, payload.x, payload.y, payload.z, payload.scale));
    }

    static void handle(BlockDustBurstPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.level instanceof ClientLevel level)) return;
        BlockState state = Block.BLOCK_STATE_REGISTRY.byId(payload.stateId);
        if (state == null) return;
        RandomSource rand = level.getRandom();
        for (int i = 0; i < payload.count; i++) {
            ClientEffects.spawnBlockDust(
                    level,
                    payload.x,
                    payload.y,
                    payload.z,
                    rand.nextGaussian() * payload.motion,
                    rand.nextGaussian() * payload.motion,
                    rand.nextGaussian() * payload.motion,
                    state);
        }
    }

    static void handle(BlockDestroyPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.level instanceof ClientLevel level)) return;
        BlockState state = Block.BLOCK_STATE_REGISTRY.byId(payload.stateId);
        if (state == null) return;
        level.addDestroyBlockEffect(new BlockPos(payload.x, payload.y, payload.z), state);
    }

    static void handle(RbmkJetPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.level instanceof ClientLevel level)) return;
        mc.particleEngine.add(
                payload.maxAge == 0
                        ? new ParticleRBMKSteam(level, payload.x, payload.y, payload.z)
                        : new ParticleRBMKFlame(
                                level, payload.x, payload.y, payload.z, payload.maxAge));
    }

    static void handle(BlackPowderPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.level instanceof ClientLevel level)) return;
        RandomSource rand = level.getRandom();
        Vec3 heading = new Vec3(payload.hx, payload.hy, payload.hz).normalize();

        for (int i = 0; i < payload.cloudCount; i++) {
            ParticleBlackPowderSmoke fx =
                    new ParticleBlackPowderSmoke(
                            level, payload.x, payload.y, payload.z, payload.cloudScale);
            double speedMult = 0.85 + rand.nextDouble() * 0.3;
            fx.setParticleSpeed(
                    heading.x * payload.cloudSpeedMult * speedMult + rand.nextGaussian() * 0.05,
                    heading.y * payload.cloudSpeedMult * speedMult + rand.nextGaussian() * 0.05,
                    heading.z * payload.cloudSpeedMult * speedMult + rand.nextGaussian() * 0.05);
            mc.particleEngine.add(fx);
        }

        for (int i = 0; i < payload.sparkCount; i++) {
            double speedMult = 0.85 + rand.nextDouble() * 0.3;
            level.addParticle(
                    HbmParticles.BLACK_POWDER_SPARK.get(),
                    true,
                    false,
                    payload.x,
                    payload.y,
                    payload.z,
                    heading.x * payload.sparkSpeedMult * speedMult + rand.nextGaussian() * 0.02,
                    heading.y * payload.sparkSpeedMult * speedMult + rand.nextGaussian() * 0.02,
                    heading.z * payload.sparkSpeedMult * speedMult + rand.nextGaussian() * 0.02);
        }
    }

    static void handle(SparkBurstPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.level instanceof ClientLevel level)) return;
        RandomSource rand = level.getRandom();

        for (int i = 0; i < payload.count; i++) {
            mc.particleEngine.add(
                    new ParticleSpark(
                                    level,
                                    payload.x,
                                    payload.y,
                                    payload.z,
                                    rand.nextGaussian() * 0.05,
                                    0.05,
                                    rand.nextGaussian() * 0.05)
                            .makeSmall(payload.small));
        }
        if (mc.particleEngine.createParticle(
                        HbmParticles.HADRON.get(), payload.x, payload.y, payload.z, 0D, 0D, 0D)
                instanceof ParticleHadron hadron) hadron.makeSmall(payload.small);
    }

    static void handle(DebugParticlePayload payload) {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.level instanceof ClientLevel level)) return;

        Particle fx =
                switch (payload.mode) {
                    case DebugParticlePayload.MODE_TEXT ->
                            new ParticleText(
                                            level,
                                            payload.x,
                                            payload.y,
                                            payload.z,
                                            payload.color,
                                            payload.text)
                                    .setScale(payload.scale);
                    case DebugParticlePayload.MODE_LETTER -> {
                        for (int i = 0; i < 50; i++) {
                            Particle spark =
                                    mc.particleEngine.createParticle(
                                            ParticleTypes.FIREWORK,
                                            payload.x,
                                            payload.y,
                                            payload.z,
                                            0.4 * level.getRandom().nextGaussian(),
                                            0.4 * level.getRandom().nextGaussian(),
                                            0.4 * level.getRandom().nextGaussian());
                            if (spark instanceof SimpleAnimatedParticle animated)
                                animated.setColor(payload.color);
                        }
                        yield new ParticleLetter(
                                level,
                                payload.x,
                                payload.y,
                                payload.z,
                                payload.color,
                                payload.text.isEmpty() ? ' ' : payload.text.charAt(0));
                    }
                    case DebugParticlePayload.MODE_DRONE_LINE ->
                            showsDroneNetwork(mc)
                                    ? new ParticleDebugLine(
                                            level,
                                            payload.x,
                                            payload.y,
                                            payload.z,
                                            payload.mx,
                                            payload.my,
                                            payload.mz,
                                            payload.color)
                                    : null;
                    default -> null;
                };
        if (fx != null) mc.particleEngine.add(fx);
    }

    private static boolean showsDroneNetwork(Minecraft mc) {
        if (mc.player == null) return false;
        ItemStack held = mc.player.getMainHandItem();
        return ModItems.DRONE.typeOf(held) != null
                || held.is(ModItems.DRONE_LINKER.get())
                || held.is(ModBlocks.DRONE_CRATE_PROVIDER.get().asItem())
                || held.is(ModBlocks.DRONE_CRATE_REQUESTER.get().asItem())
                || held.is(ModBlocks.DRONE_DOCK.get().asItem())
                || held.is(ModBlocks.DRONE_WAYPOINT.get().asItem())
                || held.is(ModBlocks.DRONE_WAYPOINT_REQUEST.get().asItem());
    }

    static void handle(AshesPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.level instanceof ClientLevel level)) return;
        Entity entity = level.getEntity(payload.entityId);
        if (entity == null) return;
        RandomSource rand = level.getRandom();

        VanishedEntities.vanish(payload.entityId);
        float spread = entity.getBbWidth() + payload.scale * 2;
        for (int i = 0; i < payload.count; i++) {
            double px = entity.getX() + spread * (rand.nextDouble() - 0.5);
            double py = entity.getY() + entity.getBbHeight() * rand.nextDouble();
            double pz = entity.getZ() + spread * (rand.nextDouble() - 0.5);
            mc.particleEngine.add(new ParticleAshes(level, px, py, pz, payload.scale));
            level.addParticle(ParticleTypes.FLAME, px, py, pz, 0D, 0D, 0D);
        }
    }

    static void handle(VomitPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.level instanceof ClientLevel level)) return;
        if (!(level.getEntity(payload.entityId) instanceof LivingEntity entity)) return;
        RandomSource rand = level.getRandom();

        int count = payload.count / (mc.options.particles().get().ordinal() + 1);

        double x = entity.getX(), y = entity.getEyeY(), z = entity.getZ();
        Vec3 look = entity.getLookAngle();
        for (int i = 0; i < count; i++) {
            if (payload.mode == VomitPayload.MODE_SMOKE) {
                Particle fx =
                        mc.particleEngine.createParticle(
                                ParticleTypes.SMOKE,
                                x,
                                y,
                                z,
                                (look.x + rand.nextGaussian() * 0.1) * 0.05,
                                (look.y + rand.nextGaussian() * 0.1) * 0.05,
                                (look.z + rand.nextGaussian() * 0.1) * 0.05);
                if (fx != null) {
                    fx.scale(0.2F);
                    fx.setLifetime(10 + rand.nextInt(10));
                }
                continue;
            }
            double mx = (look.x + rand.nextGaussian() * 0.2) * 0.2;
            double my = (look.y + rand.nextGaussian() * 0.2) * 0.2;
            double mz = (look.z + rand.nextGaussian() * 0.2) * 0.2;
            BlockState state =
                    payload.mode == VomitPayload.MODE_BLOOD
                            ? Blocks.REDSTONE_BLOCK.defaultBlockState()
                            : rand.nextBoolean()
                                    ? Blocks.DYED_TERRACOTTA.lime().defaultBlockState()
                                    : Blocks.DYED_TERRACOTTA.green().defaultBlockState();
            Particle fx =
                    mc.particleEngine.createParticle(
                            new BlockParticleOption(ParticleTypes.BLOCK, state),
                            x,
                            y,
                            z,
                            mx,
                            my,
                            mz);
            if (fx != null) {
                fx.setParticleSpeed(mx, my, mz);
                fx.setLifetime(150 + rand.nextInt(50));
            }
        }
    }

    static void handle(ProperJoltPayload payload) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        player.animateHurt(0F);
        player.hurtTime = payload.time;
        player.hurtDuration = payload.maxTime;
    }

    static void handle(SweatPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.level instanceof ClientLevel level)) return;
        if (!(level.getEntity(payload.entityId) instanceof LivingEntity entity)) return;
        BlockState state = Block.BLOCK_STATE_REGISTRY.byId(payload.stateId);
        if (state == null) return;
        RandomSource rand = level.getRandom();
        AABB box = entity.getBoundingBox();

        for (int i = 0; i < payload.count; i++) {
            double x = box.minX - 0.2 + (box.getXsize() + 0.4) * rand.nextDouble();
            double y = box.minY + (box.getYsize() + 0.2) * rand.nextDouble();
            double z = box.minZ - 0.2 + (box.getZsize() + 0.4) * rand.nextDouble();
            Particle fx =
                    mc.particleEngine.createParticle(
                            new BlockParticleOption(ParticleTypes.BLOCK, state),
                            x,
                            y,
                            z,
                            0D,
                            0D,
                            0D);
            if (fx != null) {
                fx.setParticleSpeed(0D, 0D, 0D);
                fx.setLifetime(150 + rand.nextInt(50));
            }
        }
    }

    static void handle(SkeletonPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.level instanceof ClientLevel level)) return;
        if (!(level.getEntity(payload.entityId) instanceof LivingEntity living)) return;
        VanishedEntities.vanish(payload.entityId);
        SkeletonBones.Bone[] bones = SkeletonBones.of(living);
        if (bones == null) return;
        RandomSource rand = level.getRandom();
        boolean skeletal = SkeletonBones.isSkeletal(living);

        for (SkeletonBones.Bone bone : bones) {
            if (payload.gib && rand.nextBoolean() && !skeletal) continue;

            ParticleSkeleton fx =
                    new ParticleSkeleton(
                            level,
                            bone.x(),
                            bone.y(),
                            bone.z(),
                            payload.brightness,
                            payload.brightness,
                            payload.brightness,
                            bone.part());
            fx.setInitialRotation(bone.yaw(), bone.pitch());
            if (payload.gib) {

                if (skeletal) fx.makeGibKeepingTexture();
                else fx.makeGib();
                fx.setParticleSpeed(
                        rand.nextGaussian() * payload.force,
                        (rand.nextGaussian() + 1) * payload.force,
                        rand.nextGaussian() * payload.force);
            }
            mc.particleEngine.add(fx);
        }
    }

    static void handle(GibletPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.level instanceof ClientLevel level)) return;
        VanishedEntities.vanish(payload.entityId);
        Entity entity = level.getEntity(payload.entityId);
        if (entity == null) return;
        RandomSource rand = level.getRandom();

        int gW = (int) (entity.getBbWidth() / 0.25F);
        int gH = (int) (entity.getBbHeight() / 0.25F);
        int count = (int) (gW * 1.5 * gH);
        if (payload.countDivisor > 0)
            count = (int) Math.ceil(count / (double) payload.countDivisor);

        double mult = rand.nextInt(15) == 0 ? 10D : 1D;
        for (int i = 0; i < count; i++) {
            mc.particleEngine.add(
                    new ParticleGiblet(
                            level,
                            payload.x,
                            payload.y,
                            payload.z,
                            rand.nextGaussian() * 0.25 * mult,
                            rand.nextDouble() * mult,
                            rand.nextGaussian() * 0.25 * mult,
                            payload.gibType));
        }
    }

    static void handle(CasingEjectPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.level instanceof ClientLevel level)) return;

        SpentCasing casingConfig = SpentCasing.casingMap.get(payload.casing);
        if (casingConfig == null) return;

        ParticleSpentCasing casing =
                new ParticleSpentCasing(
                        level,
                        payload.x,
                        payload.y,
                        payload.z,
                        payload.mX,
                        payload.mY,
                        payload.mZ,
                        payload.mPitch,
                        payload.mYaw,
                        casingConfig,
                        payload.smoking,
                        payload.smokeLife,
                        payload.smokeLift,
                        payload.nodeLife);
        casing.prevRotationYaw = casing.rotationYaw = payload.yaw;
        casing.prevRotationPitch = casing.rotationPitch = payload.pitch;
        mc.particleEngine.add(casing);
    }

    static void handle(PlasmaBlastPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.level instanceof ClientLevel level)) return;

        ParticlePlasmaBlast cloud =
                new ParticlePlasmaBlast(
                        level,
                        payload.x,
                        payload.y,
                        payload.z,
                        payload.r,
                        payload.g,
                        payload.b,
                        payload.pitch,
                        payload.yaw);
        cloud.setScale(payload.scale);
        mc.particleEngine.add(cloud);
    }

    static void handle(HazePayload payload) {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.level instanceof ClientLevel level)) return;
        mc.particleEngine.add(new ParticleHaze(level, payload.x, payload.y, payload.z));
    }

    static void handle(ExplosionStandardPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.level instanceof ClientLevel level)) return;
        RandomSource rand = level.getRandom();
        double x = payload.x, y = payload.y, z = payload.z;
        level.addParticle(
                payload.size >= 2F ? ParticleTypes.EXPLOSION_EMITTER : ParticleTypes.EXPLOSION,
                x,
                y,
                z,
                1D,
                0D,
                0D);
        for (BlockPos pos : payload.blocks) {
            double oX = pos.getX() + rand.nextFloat();
            double oY = pos.getY() + rand.nextFloat();
            double oZ = pos.getZ() + rand.nextFloat();
            double dX = oX - x, dY = oY - y, dZ = oZ - z;
            double delta = Mth.sqrt((float) (dX * dX + dY * dY + dZ * dZ));
            dX /= delta;
            dY /= delta;
            dZ /= delta;
            double mod = 0.5D / (delta / payload.size + 0.1D);
            mod *= rand.nextFloat() * rand.nextFloat() + 0.3F;
            dX *= mod;
            dY *= mod;
            dZ *= mod;
            level.addParticle(
                    ParticleTypes.POOF, (oX + x) / 2D, (oY + y) / 2D, (oZ + z) / 2D, dX, dY, dZ);
            level.addParticle(ParticleTypes.SMOKE, oX, oY, oZ, dX, dY, dZ);
        }
    }

    static void handle(SmokeShockPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.level instanceof ClientLevel level)) return;
        RandomSource rand = level.getRandom();
        int count = Math.max(1, payload.count);

        if (payload.mode == SmokeShockPayload.MODE_BURST) {

            float angle = rand.nextInt(360) * Mth.DEG_TO_RAD;
            float step = Mth.TWO_PI / count;
            for (int i = 0; i < count; i++) {
                ClientEffects.spawnGasFlame(
                        level,
                        payload.x,
                        payload.y,
                        payload.z,
                        Math.cos(angle) * payload.strength,
                        0D,
                        Math.sin(angle) * payload.strength,
                        6.5F);
                angle += step;
            }
            return;
        }
        if (payload.mode == SmokeShockPayload.MODE_RADIAL) {
            double spread = 1 + count / 50;
            for (int i = 0; i < count; i++) {
                ParticleExSmoke fx = new ParticleExSmoke(level, payload.x, payload.y, payload.z);
                double mY = rand.nextGaussian() * spread;
                double mX = rand.nextGaussian() * spread;
                fx.setParticleSpeed(mX, mY, rand.nextGaussian() * spread);
                mc.particleEngine.add(fx);
            }
            return;
        }
        if (payload.mode == SmokeShockPayload.MODE_FOAM) {

            for (int i = 0; i < count; i++) {
                ParticleFoam fx = new ParticleFoam(level, payload.x, payload.y, payload.z);
                fx.setLifetime(50);
                fx.setParticleSpeed(0, 0, 0);
                mc.particleEngine.add(fx);
            }
            return;
        }
        if (payload.mode == SmokeShockPayload.MODE_SHOCK) {

            float angle = rand.nextInt(360) * Mth.DEG_TO_RAD;
            float step = Mth.TWO_PI / count;
            for (int i = 0; i < count; i++) {
                ParticleExSmoke fx = new ParticleExSmoke(level, payload.x, payload.y, payload.z);
                fx.setParticleSpeed(
                        Math.cos(angle) * payload.strength, 0, Math.sin(angle) * payload.strength);
                mc.particleEngine.add(fx);
                angle += step;
            }
        } else {

            for (int i = 0; i < count; i++) {
                ParticleExSmoke fx = new ParticleExSmoke(level, payload.x, payload.y, payload.z);
                double mY = rand.nextGaussian() * (1 + (count / 100));
                if (rand.nextBoolean()) mY = Math.abs(mY);
                fx.setParticleSpeed(
                        rand.nextGaussian() * (1 + (count / 150)),
                        mY,
                        rand.nextGaussian() * (1 + (count / 150)));
                mc.particleEngine.add(fx);
            }
        }
    }

    static void handle(MukePayload payload) {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.level instanceof ClientLevel level)) return;
        LocalPlayer player = mc.player;
        if (player == null) return;
        RandomSource rand = level.getRandom();

        double x = payload.x, y = payload.y, z = payload.z;

        if (!payload.tinytot) {
            mc.particleEngine.add(new ParticleMukeWave(level, x, y, z));
            mc.particleEngine.add(new ParticleMukeFlash(level, x, y, z, payload.balefire));
        } else {

            mc.particleEngine.add(new ParticleMukeWave(level, x, y, z));

            for (double d = 0.0D; d <= 1.6D; d += 0.1) {
                mc.particleEngine.add(
                        new ParticleMukeCloud(
                                level,
                                x,
                                y,
                                z,
                                rand.nextGaussian() * 0.05,
                                d + rand.nextGaussian() * 0.02,
                                rand.nextGaussian() * 0.05));
            }
            for (int i = 0; i < 50; i++) {
                mc.particleEngine.add(
                        new ParticleMukeCloud(
                                level,
                                x,
                                y + 0.5,
                                z,
                                rand.nextGaussian() * 0.5,
                                rand.nextInt(5) == 0 ? 0.02 : 0,
                                rand.nextGaussian() * 0.5));
            }
            for (int i = 0; i < 15; i++) {
                double ix = rand.nextGaussian() * 0.2;
                double iz = rand.nextGaussian() * 0.2;

                if (ix * ix + iz * iz > 0.75) {
                    ix *= 0.5;
                    iz *= 0.5;
                }

                double iy = 1.6 + (rand.nextDouble() * 2 - 1) * (0.75 - (ix * ix + iz * iz)) * 0.5;

                mc.particleEngine.add(
                        new ParticleMukeCloud(
                                level, x, y, z, ix, iy + rand.nextGaussian() * 0.02, iz));
            }
        }

        player.animateHurt(0F);
        player.hurtTime = 15;
        player.hurtDuration = 15;
    }

    static void handle(ExplosionSmallPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.level instanceof ClientLevel level)) return;
        Player player = mc.player;
        if (player == null) return;
        RandomSource rand = level.getRandom();

        double x = payload.x, y = payload.y, z = payload.z;

        float dist = (float) player.getEyePosition().distanceTo(new Vec3(x, y, z));
        float soundRange = 200F;
        if (dist <= soundRange) {
            var sound =
                    dist <= soundRange * 0.4
                            ? ModSounds.WEAPON_EXPLOSION_SMALL_NEAR
                            : ModSounds.WEAPON_EXPLOSION_SMALL_FAR;
            mc.getSoundManager()
                    .playDelayed(
                            new SimpleSoundInstance(
                                    sound.get(),
                                    SoundSource.BLOCKS,
                                    100F,
                                    0.9F + rand.nextFloat() * 0.2F,
                                    RandomSource.create(),
                                    x,
                                    y,
                                    z),
                            (int) (dist / ExplosionSmallPayload.SPEED_OF_SOUND));
        }

        for (int i = 0; i < payload.cloudCount; i++) {
            mc.particleEngine.add(
                    new ParticleExplosionSmall(
                            level, x, y, z, payload.cloudScale, payload.cloudSpeedMult));
        }

        BlockState dustState = null;
        BlockPos center = BlockPos.containing(x, y, z);
        for (Direction dir : Direction.VALUES) {
            BlockState state = level.getBlockState(center.relative(dir));
            if (!state.isAir()) {
                dustState = state;
                break;
            }
        }

        if (dustState != null)
            for (int i = 0; i < payload.debris; i++) {
                double mx = rand.nextGaussian() * 0.2;
                double my = 0.5F + rand.nextDouble() * 0.7;
                double mz = rand.nextGaussian() * 0.2;
                Particle fx =
                        mc.particleEngine.createParticle(
                                new BlockParticleOption(ParticleTypes.BLOCK, dustState),
                                x,
                                y + 0.1,
                                z,
                                mx,
                                my,
                                mz);
                if (fx != null) {
                    fx.setParticleSpeed(mx, my, mz);
                    fx.scale(2F);
                    fx.setLifetime(50 + rand.nextInt(20));
                }
            }
    }
}
