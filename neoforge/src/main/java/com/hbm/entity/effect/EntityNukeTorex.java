// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.effect;

import com.hbm.client.TorexClientFX;
import com.hbm.entity.ModEntities;
import com.hbm.platform.Services;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

public class EntityNukeTorex extends Entity {

    public static final int firstCondenseHeight = 130;
    public static final int secondCondenseHeight = 170;
    public static final int maxCloudlets = 20_000;

    public static final double nr1 = 2.5, ng1 = 1.3, nb1 = 0.4;
    public static final double nr2 = 0.1, ng2 = 0.075, nb2 = 0.05;
    public static final double br1 = 1, bg1 = 2, bb1 = 0.5;
    public static final double br2 = 0.1, bg2 = 0.1, bb2 = 0.1;
    private static final EntityDataAccessor<Float> SCALE =
            SynchedEntityData.defineId(EntityNukeTorex.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Byte> TYPE =
            SynchedEntityData.defineId(EntityNukeTorex.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Long> BIRTH =
            SynchedEntityData.defineId(EntityNukeTorex.class, EntityDataSerializers.LONG);
    public final List<Cloudlet> cloudlets = new ArrayList<>();
    public double coreHeight = 3;
    public double convectionHeight = 3;
    public double torusWidth = 3;
    public double rollerSize = 1;
    public double heat = 1;
    public double lastSpawnY = -1;
    public int maxAge = 1000;

    private int simAge;
    public float humidity = -1;

    public boolean didPlaySound = false;
    public boolean didShake = false;

    public int lastRenderSortTick = -1;

    public EntityNukeTorex(EntityType<? extends EntityNukeTorex> type, Level level) {
        super(type, level);
    }

    public static EntityNukeTorex statFac(Level level, double x, double y, double z, float scale) {
        EntityNukeTorex torex = new EntityNukeTorex(ModEntities.NUKE_TOREX.get(), level);
        torex.setScale(Mth.clamp(scale * 0.01F, 0.25F, 5F));
        torex.setPos(x, y, z);
        level.addFreshEntity(torex);
        return torex;
    }

    public static EntityNukeTorex statFacBale(
            Level level, double x, double y, double z, float scale) {
        EntityNukeTorex torex = statFac(level, x, y, z, scale);
        torex.setTorexType(1);
        return torex;
    }

    public static EntityNukeTorex statFacFleija(
            Level level, double x, double y, double z, float scale) {
        return statFac(level, x, y, z, scale * 0.75F);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(SCALE, 1.0F);
        builder.define(TYPE, (byte) 0);
        builder.define(BIRTH, Long.MIN_VALUE);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        float s = input.getFloatOr("Scale", 0F);
        if (s > 0F) setScale(Mth.clamp(s, 0.25F, 5F));
        setTorexType(input.getByteOr("Type", (byte) 0));
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {}

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {

        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distSq) {
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            tickClient();
            return;
        }
        if (this.entityData.get(BIRTH) == Long.MIN_VALUE) {
            this.entityData.set(BIRTH, level().getGameTime() - this.tickCount);
        }
        if (age() > maxAge) this.discard();
    }

    public int age() {
        long birth = this.entityData.get(BIRTH);
        return birth == Long.MIN_VALUE
                ? this.tickCount
                : (int) Math.max(1L, level().getGameTime() - birth);
    }

    private static final int CATCHUP_UPDATES = 200_000;

    private void tickClient() {
        if (this.tickCount == 1) {
            this.setScale((float) this.getScaleVal());
            this.simAge = 0;
        }
        int target = age();

        for (int spent = 0; simAge < target && spent < CATCHUP_UPDATES; ) {
            simAge++;
            spent += simulateTick(simAge, simAge == target);
        }
    }

    private int simulateTick(int age, boolean live) {
        double s = this.getScaleVal();
        double cs = 1.5;
        if (live) TorexClientFX.hudFlash(this);

        if (humidity == -1) {

            humidity = Services.PLATFORM.biomeDownfall(level().getBiome(blockPosition()).value());
        }

        if (lastSpawnY == -1) lastSpawnY = getY() - 3;

        int spawnTarget =
                Math.max(
                        level().getHeight(
                                                Heightmap.Types.MOTION_BLOCKING,
                                                (int) Math.floor(getX()),
                                                (int) Math.floor(getZ()))
                                - 3,
                        1);
        double moveSpeed = 0.5D;
        if (Math.abs(spawnTarget - lastSpawnY) < moveSpeed) {
            lastSpawnY = spawnTarget;
        } else {
            lastSpawnY += moveSpeed * Math.signum(spawnTarget - lastSpawnY);
        }

        RandomSource rand = this.random;

        double range = (torusWidth - rollerSize) * 0.5;
        double simSpeed = getSimulationSpeed();
        int lifetime = Math.min((age * age) + 200, maxAge - age + 200);
        int toSpawn =
                (int)
                        (0.6
                                * Math.min(
                                        Math.max(0, maxCloudlets - cloudlets.size()),
                                        Math.ceil(
                                                10
                                                        * simSpeed
                                                        * simSpeed
                                                        * Math.min(1, 1200 / (double) lifetime))));
        for (int i = 0; i < toSpawn; i++) {
            double x = getX() + rand.nextGaussian() * range;
            double z = getZ() + rand.nextGaussian() * range;
            Cloudlet cloud =
                    new Cloudlet(
                            x,
                            lastSpawnY,
                            z,
                            (float) (rand.nextDouble() * 2D * Math.PI),
                            0,
                            lifetime);
            cloud.setScale(
                    (float) (Math.sqrt(s) * 3 + age * 0.0025 * s),
                    (float) (Math.sqrt(s) * 3 + age * 0.0025 * 6 * cs * s));
            cloudlets.add(cloud);
        }

        if (live && age < 120 * s) {
            TorexClientFX.skyFlash(level());
        }

        if (age < 150) {
            int cloudCount = Math.min(age * 2, 100);
            int shockLife = Math.max(400 - age * 20, 50);
            for (int i = 0; i < cloudCount; i++) {
                double radius = (age + rand.nextDouble() * 2) * 1.5;
                float rot = (float) (Math.PI * 2 * rand.nextDouble());
                Vec3 vec = new Vec3(radius, 0, 0).yRot(rot);
                int gh =
                        level().getHeight(
                                        Heightmap.Types.MOTION_BLOCKING,
                                        (int) (vec.x + getX()) + 1,
                                        (int) (vec.z + getZ()));
                cloudlets.add(
                        new Cloudlet(
                                        vec.x + getX(),
                                        gh,
                                        vec.z + getZ(),
                                        rot,
                                        0,
                                        shockLife,
                                        TorexType.SHOCK)
                                .setScale((float) s * 5F, (float) s * 2F)
                                .setMotion(Mth.clamp(0.25 * age - 5, 0, 1)));
            }
            if (live) TorexClientFX.shockwaveArrival(this);
        }

        if (age < 200) {
            lifetime *= s;
            for (int i = 0; i < 2; i++) {
                Cloudlet cloud =
                        new Cloudlet(
                                getX(),
                                getY() + coreHeight,
                                getZ(),
                                (float) (rand.nextDouble() * 2D * Math.PI),
                                0,
                                lifetime,
                                TorexType.RING);
                cloud.setScale(
                        (float) (Math.sqrt(s) * cs + age * 0.0015 * s),
                        (float) (Math.sqrt(s) * cs + age * 0.0015 * 6 * cs * s));
                cloudlets.add(cloud);
            }
        }

        if (this.humidity > 0 && age < 220) {
            spawnCondensationClouds(age, this.humidity, firstCondenseHeight, 80, 4, s, cs);
            spawnCondensationClouds(age, this.humidity, secondCondenseHeight, 80, 2, s, cs);
        }

        cloudlets.removeIf(cloud -> cloud.isDead);
        for (Cloudlet cloud : cloudlets) cloud.update();

        coreHeight += 0.15;
        torusWidth += 0.05;
        rollerSize = torusWidth * 0.35;
        convectionHeight = coreHeight + rollerSize;

        int maxHeat = (int) (50 * s * s);
        heat = maxHeat - Math.pow((maxHeat * age) / (double) maxAge, 0.6);
        return cloudlets.size();
    }

    private void spawnCondensationClouds(
            int age, float humidity, int height, int count, int spreadAngle, double s, double cs) {
        if ((getY() + age) <= height) return;
        RandomSource rand = this.random;
        int outer = (int) (5 * humidity * count / (double) spreadAngle);
        for (int i = 0; i < outer; i++) {
            for (int j = 1; j < spreadAngle; j++) {
                float angle = (float) (Math.PI * 2 * rand.nextDouble());
                double rotZ =
                        Math.acos((height - getY()) / (double) age)
                                + Math.toRadians(
                                        humidity
                                                * humidity
                                                * 90
                                                * j
                                                * (0.1 * rand.nextDouble() - 0.05));
                Vec3 vec = new Vec3(0, age, 0).zRot((float) rotZ).yRot(angle);
                Cloudlet cloud =
                        new Cloudlet(
                                getX() + vec.x,
                                getY() + vec.y,
                                getZ() + vec.z,
                                angle,
                                0,
                                (int) ((20 + age / 10) * (1 + rand.nextDouble() * 0.1)),
                                TorexType.CONDENSATION);
                cloud.setScale(3F * (float) (cs * s), 4F * (float) (cs * s));
                cloudlets.add(cloud);
            }
        }
    }

    public EntityNukeTorex setScale(float scale) {
        if (!level().isClientSide()) entityData.set(SCALE, scale);
        this.coreHeight *= scale;
        this.convectionHeight *= scale;
        this.torusWidth *= scale;
        this.rollerSize *= scale;
        this.maxAge = (int) (45 * SharedConstants.TICKS_PER_SECOND * scale);
        return this;
    }

    public double getScaleVal() {
        return entityData.get(SCALE);
    }

    public byte getTorexType() {
        return entityData.get(TYPE);
    }

    public EntityNukeTorex setTorexType(int type) {
        entityData.set(TYPE, (byte) type);
        return this;
    }

    public double getSimulationSpeed() {
        int simSlow = maxAge / 4;
        int life = level().isClientSide() ? simAge : age();
        if (life > maxAge) return 0D;
        if (life > simSlow) return 1D - ((double) (life - simSlow) / (double) (maxAge - simSlow));
        return 1.0D;
    }

    public float getAlpha() {
        int fadeOut = maxAge * 3 / 4;
        int life = age();
        if (life > fadeOut) return 1F - (float) (life - fadeOut) / (float) (maxAge - fadeOut);
        return 1.0F;
    }

    public enum TorexType {
        STANDARD,
        RING,
        CONDENSATION,
        SHOCK
    }

    public class Cloudlet {

        private static final double MOTION_CONVECTION_MULT = 0.5F;
        private static final double MOTION_LIFT_MULT = 0.625F;
        private static final double MOTION_RING_MULT = 0.5F;
        private static final double MOTION_CONDENSATION_MULT = 1F;
        private static final double MOTION_SHOCKWAVE_MULT = 1F;
        public double posX, posY, posZ;
        public double prevPosX, prevPosY, prevPosZ;
        public double motionX, motionY, motionZ;
        public int age;
        public int cloudletLife;
        public float angle;
        public boolean isDead = false;
        public float rangeMod = 1.0F;
        public float colorMod = 1.0F;
        public double colorR, colorG, colorB;
        public double prevColorR, prevColorG, prevColorB;
        public double renderSortDistanceSq;
        public TorexType type;
        public float startingScale = 3F;
        public float growingScale = 5F;

        private double computedMotionX;
        private double computedMotionY;
        private double computedMotionZ;
        private double motionMult = 1F;

        public Cloudlet(double posX, double posY, double posZ, float angle, int age, int maxAge) {
            this(posX, posY, posZ, angle, age, maxAge, TorexType.STANDARD);
        }

        public Cloudlet(
                double posX,
                double posY,
                double posZ,
                float angle,
                int age,
                int maxAge,
                TorexType type) {
            this.posX = posX;
            this.posY = posY;
            this.posZ = posZ;
            this.age = age;
            this.cloudletLife = maxAge;
            this.angle = angle;
            RandomSource rand = EntityNukeTorex.this.random;
            this.rangeMod = 0.3F + rand.nextFloat() * 0.7F;
            this.colorMod = 0.8F + rand.nextFloat() * 0.2F;
            this.type = type;
            this.updateColor();
        }

        private void update() {
            age++;
            if (age > cloudletLife) this.isDead = true;

            this.prevPosX = this.posX;
            this.prevPosY = this.posY;
            this.prevPosZ = this.posZ;

            double simDeltaX = EntityNukeTorex.this.getX() - this.posX;
            double simDeltaZ = EntityNukeTorex.this.getZ() - this.posZ;
            double simPosX =
                    EntityNukeTorex.this.getX()
                            + Math.sqrt(simDeltaX * simDeltaX + simDeltaZ * simDeltaZ);

            switch (this.type) {
                case STANDARD -> {
                    getConvectionMotion(simPosX);
                    double convX = this.computedMotionX;
                    double convY = this.computedMotionY;
                    double convZ = this.computedMotionZ;
                    getLiftMotion(simPosX);
                    double factor =
                            Mth.clamp(
                                    (this.posY - EntityNukeTorex.this.getY())
                                            / EntityNukeTorex.this.coreHeight,
                                    0,
                                    1);
                    double inverseFactor = 1D - factor;
                    this.motionX = convX * factor + this.computedMotionX * inverseFactor;
                    this.motionY = convY * factor + this.computedMotionY * inverseFactor;
                    this.motionZ = convZ * factor + this.computedMotionZ * inverseFactor;
                }
                case RING -> {
                    getRingMotion(simPosX);
                    this.motionX = this.computedMotionX;
                    this.motionY = this.computedMotionY;
                    this.motionZ = this.computedMotionZ;
                }
                case CONDENSATION -> {
                    getCondensationMotion();
                    this.motionX = this.computedMotionX;
                    this.motionY = this.computedMotionY;
                    this.motionZ = this.computedMotionZ;
                }
                case SHOCK -> {
                    getShockwaveMotion();
                    this.motionX = this.computedMotionX;
                    this.motionY = this.computedMotionY;
                    this.motionZ = this.computedMotionZ;
                }
            }

            double mult = this.motionMult * getSimulationSpeed();
            this.posX += this.motionX * mult;
            this.posY += this.motionY * mult;
            this.posZ += this.motionZ * mult;

            this.updateColor();
        }

        private void getCondensationMotion() {
            double speed = MOTION_CONDENSATION_MULT * EntityNukeTorex.this.getScaleVal() * 0.125D;
            setNormalizedMotion(
                    this.posX - EntityNukeTorex.this.getX(),
                    0D,
                    this.posZ - EntityNukeTorex.this.getZ(),
                    speed);
        }

        private void getShockwaveMotion() {
            double speed = MOTION_SHOCKWAVE_MULT * EntityNukeTorex.this.getScaleVal() * 0.25D;
            setNormalizedMotion(
                    this.posX - EntityNukeTorex.this.getX(),
                    0D,
                    this.posZ - EntityNukeTorex.this.getZ(),
                    speed);
        }

        private void getRingMotion(double simPosX) {
            if (simPosX > EntityNukeTorex.this.getX() + torusWidth * 2) {
                setComputedMotion(0D, 0D, 0D);
                return;
            }
            double torusPosX = EntityNukeTorex.this.getX() + torusWidth;
            double torusPosY = EntityNukeTorex.this.getY() + coreHeight * 0.5D;
            double deltaX = torusPosX - simPosX;
            double deltaY = torusPosY - this.posY;
            double roller = EntityNukeTorex.this.rollerSize * this.rangeMod * 0.25D;
            double dist = Math.sqrt(deltaX * deltaX + deltaY * deltaY) / roller - 1D;
            double func = 1D - Math.exp(-dist);
            float angle = (float) (func * Math.PI * 0.5D);
            double rotX = -deltaX / dist;
            double rotY = -deltaY / dist;
            float sin = Mth.sin(angle);
            float cos = Mth.cos(angle);
            double rotatedX = rotX * cos + rotY * sin;
            double rotatedY = rotY * cos - rotX * sin;
            setNormalizedMotion(
                    torusPosX + rotatedX - simPosX,
                    torusPosY + rotatedY - this.posY,
                    0D,
                    MOTION_RING_MULT * 0.5D);
            rotateComputedMotionAroundY();
        }

        private void getConvectionMotion(double simPosX) {
            if (simPosX > EntityNukeTorex.this.getX() + torusWidth * 2) {
                setComputedMotion(0D, 0D, 0D);
                return;
            }
            double torusPosX = EntityNukeTorex.this.getX() + torusWidth;
            double torusPosY = EntityNukeTorex.this.getY() + coreHeight;
            double deltaX = torusPosX - simPosX;
            double deltaY = torusPosY - this.posY;
            double roller = EntityNukeTorex.this.rollerSize * this.rangeMod;
            double dist = Math.sqrt(deltaX * deltaX + deltaY * deltaY) / roller - 1D;
            double func = 1D - Math.exp(-dist);
            float angle = (float) (func * Math.PI * 0.5D);
            double rotX = -deltaX / dist;
            double rotY = -deltaY / dist;
            float sin = Mth.sin(angle);
            float cos = Mth.cos(angle);
            double rotatedX = rotX * cos + rotY * sin;
            double rotatedY = rotY * cos - rotX * sin;
            setNormalizedMotion(
                    torusPosX + rotatedX - simPosX,
                    torusPosY + rotatedY - this.posY,
                    0D,
                    MOTION_CONVECTION_MULT);
            rotateComputedMotionAroundY();
        }

        private void getLiftMotion(double simPosX) {
            double scale =
                    Mth.clamp(1D - (simPosX - (EntityNukeTorex.this.getX() + torusWidth)), 0, 1)
                            * MOTION_LIFT_MULT;
            setNormalizedMotion(
                    EntityNukeTorex.this.getX() - this.posX,
                    (EntityNukeTorex.this.getY() + convectionHeight) - this.posY,
                    EntityNukeTorex.this.getZ() - this.posZ,
                    scale);
        }

        private void setComputedMotion(double x, double y, double z) {
            this.computedMotionX = x;
            this.computedMotionY = y;
            this.computedMotionZ = z;
        }

        private void setNormalizedMotion(double x, double y, double z, double speed) {
            double lengthSq = x * x + y * y + z * z;
            if (lengthSq < 1.0E-8D) {
                setComputedMotion(0D, 0D, 0D);
                return;
            }
            double scale = speed / Math.sqrt(lengthSq);
            setComputedMotion(x * scale, y * scale, z * scale);
        }

        private void rotateComputedMotionAroundY() {
            float cos = Mth.cos(this.angle);
            float sin = Mth.sin(this.angle);
            double mx = this.computedMotionX;
            double mz = this.computedMotionZ;
            this.computedMotionX = mx * cos + mz * sin;
            this.computedMotionZ = mz * cos - mx * sin;
        }

        private void updateColor() {
            this.prevColorR = this.colorR;
            this.prevColorG = this.colorG;
            this.prevColorB = this.colorB;

            double exX = EntityNukeTorex.this.getX();
            double exY = EntityNukeTorex.this.getY() + EntityNukeTorex.this.coreHeight;
            double exZ = EntityNukeTorex.this.getZ();

            double distX = exX - posX;
            double distY = exY - posY;
            double distZ = exZ - posZ;
            double distSq = distX * distX + distY * distY + distZ * distZ;
            distSq /=
                    this.type == TorexType.SHOCK
                            ? EntityNukeTorex.this.heat * 3
                            : EntityNukeTorex.this.heat;

            double col = 2D / Math.max(distSq, 1);
            byte type = EntityNukeTorex.this.getTorexType();

            if (type == 0) {
                this.colorR = nr2 + (nr1 - nr2) * col;
                this.colorG = ng2 + (ng1 - ng2) * col;
                this.colorB = nb2 + (nb1 - nb2) * col;
            } else {
                this.colorR = br2 + (br1 - br2) * col;
                this.colorG = bg2 + (bg1 - bg2) * col;
                this.colorB = bb2 + (bb1 - bb2) * col;
            }
        }

        public float getAlpha() {
            float alpha =
                    (1F - ((float) age / (float) cloudletLife)) * EntityNukeTorex.this.getAlpha();
            if (this.type == TorexType.CONDENSATION) alpha *= 0.25F;
            return Mth.clamp(alpha, 0.0001F, 1F);
        }

        public float getScale() {
            return startingScale + ((float) age / (float) cloudletLife) * growingScale;
        }

        public Cloudlet setScale(float start, float grow) {
            this.startingScale = start;
            this.growingScale = grow;
            return this;
        }

        public Cloudlet setMotion(double mult) {
            this.motionMult = mult;
            return this;
        }
    }
}
