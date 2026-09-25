// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.mob.glyphid;

import com.hbm.blocks.ModBlocks;
import com.hbm.entity.logic.EntityWaypoint;
import com.hbm.entity.mob.EntityParasiteMaggot;
import com.hbm.entity.mob.glyphid.GlyphidStats.StatBundle;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.BlockAllocatorStandard;
import com.hbm.explosion.vanillant.standard.BlockMutatorDebris;
import com.hbm.explosion.vanillant.standard.BlockProcessorStandard;
import com.hbm.explosion.vanillant.standard.EntityProcessorStandard;
import com.hbm.explosion.vanillant.standard.PlayerProcessorStandard;
import com.hbm.handler.threading.TargetPoint;
import com.hbm.main.Polaroid;
import com.hbm.main.ResourceManager;
import com.hbm.packet.toclient.MukePayload;
import com.hbm.platform.Services;
import com.hbm.sound.ModSounds;
import com.hbm.util.MobUtil;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

public class EntityGlyphidNuclear extends EntityGlyphid {

    public int deathTicks;
    private boolean sentRetreatSignal;

    public EntityGlyphidNuclear(EntityType<? extends EntityGlyphidNuclear> type, Level level) {
        super(type, level);
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return EntityGlyphid.createAttributes()
                .add(Attributes.MAX_HEALTH, GlyphidStats.getStats().getNuclear().health)
                .add(
                        Attributes.MOVEMENT_SPEED,
                        MobUtil.oldAiWalkSpeed(GlyphidStats.getStats().getNuclear().speed))
                .add(Attributes.ATTACK_DAMAGE, GlyphidStats.getStats().getNuclear().damage);
    }

    @Override
    protected EntityDimensions getDefaultDimensions(Pose pose) {
        return EntityDimensions.fixed(2.5F, 1.75F);
    }

    @Override
    public Identifier getSkin() {
        return ResourceManager.glyphid_nuclear_tex;
    }

    @Override
    public double getGlyphidScale() {
        return 2D;
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getAttribute(Attributes.MAX_HEALTH)
                .setBaseValue(GlyphidStats.getStats().getNuclear().health);
        getAttribute(Attributes.MOVEMENT_SPEED)
                .setBaseValue(MobUtil.oldAiWalkSpeed(GlyphidStats.getStats().getNuclear().speed));
        getAttribute(Attributes.ATTACK_DAMAGE)
                .setBaseValue(GlyphidStats.getStats().getNuclear().damage);
    }

    @Override
    public StatBundle getStats() {
        return GlyphidStats.getStats().statsNuclear;
    }

    @Override
    public boolean isNuclearType() {
        return true;
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);

        if (tickCount % 20 == 0) {

            if (isAtDestination() && getCurrentTask() == TASK_FOLLOW) {
                setCurrentTask(TASK_IDLE, null);
            }

            if (getCurrentTask() == TASK_BUILD_HIVE && getTarget() == null) {
                addEffect(new MobEffectInstance(MobEffects.SPEED, 10 * 20, 3));
            }

            if (getCurrentTask() == TASK_TERRAFORM) {
                setHealth(0F);
            }
        }
    }

    @Override
    public void communicate(int task, @Nullable EntityWaypoint waypoint) {
        int radius = waypoint != null ? waypoint.radius : 4;
        AABB bb = new AABB(getX(), getY(), getZ(), getX(), getY(), getZ()).inflate(radius);

        List<Entity> bugs = level().getEntities(this, bb);
        for (Entity e : bugs) {
            if (e instanceof EntityGlyphid bug && bug.isScoutType()) {
                if (bug.getCurrentTask() != task) {
                    bug.setCurrentTask(task, waypoint);
                }
            }
        }
    }

    @Override
    public boolean isArmorBroken(float amount) {
        return random.nextInt(100) <= Math.min(Math.pow(amount * 0.12, 2), 100);
    }

    @Override
    public boolean doesInfectedSpawnMaggots() {
        return false;
    }

    @Override
    protected void tickDeath() {
        ++deathTicks;

        if (!sentRetreatSignal) {
            communicate(TASK_INITIATE_RETREAT, null);
            sentRetreatSignal = true;
        }

        if (deathTicks == 90) {
            int radius = 8;
            AABB bb = new AABB(getX(), getY(), getZ(), getX(), getY(), getZ()).inflate(radius);

            List<Entity> bugs = level().getEntities(this, bb);
            for (Entity e : bugs) {
                if (e instanceof EntityGlyphid) {
                    addEffect(new MobEffectInstance(MobEffects.HEALTH_BOOST, 20, 6));
                    addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 15 * 20, 1));
                }
            }
        }

        if (deathTicks == 100) {

            if (level() instanceof ServerLevel server) {
                ExplosionVNT vnt = new ExplosionVNT(server, getX(), getY(), getZ(), 25, this);

                if (subtype() == TYPE_INFECTED) {

                    if (EntityParasiteMaggot.TYPE != null) {
                        int count = 15 + random.nextInt(6);
                        for (int k = 0; k < count; k++) {
                            float f = ((float) (k % 2) - 0.5F) * 0.5F;
                            float f1 = ((float) (k / 2) - 0.5F) * 0.5F;
                            EntityParasiteMaggot maggot =
                                    new EntityParasiteMaggot(
                                            EntityParasiteMaggot.TYPE.get(), server);
                            maggot.snapTo(
                                    getX() + f,
                                    getY() + 0.5D,
                                    getZ() + f1,
                                    random.nextFloat() * 360.0F,
                                    0F);
                            maggot.setDeltaMovement(f, 0D, f1);
                            maggot.hurtMarked = true;
                            server.addFreshEntity(maggot);
                        }
                    }

                } else {
                    vnt.setBlockAllocator(new BlockAllocatorStandard(24));
                    vnt.setBlockProcessor(
                            new BlockProcessorStandard()
                                    .withBlockEffect(
                                            new BlockMutatorDebris(
                                                    ModBlocks.VOLCANIC_LAVA_BLOCK
                                                            .get()
                                                            .defaultBlockState()))
                                    .setNoDrop());
                }

                vnt.setEntityProcessor(new EntityProcessorStandard());
                vnt.setPlayerProcessor(new PlayerProcessorStandard());
                vnt.explode();

                server.playSound(
                        null,
                        getX(),
                        getY(),
                        getZ(),
                        ModSounds.GUN_MINI_NUKE_EXPLOSION.get(),
                        SoundSource.BLOCKS,
                        15.0F,
                        1.0F);

                boolean balefire = Polaroid.isBalefireDay() || random.nextInt(100) == 0;
                Services.NETWORK.sendToAllAround(
                        new MukePayload(getX(), getY() + 0.5D, getZ(), false, balefire),
                        new TargetPoint(server, getX(), getY(), getZ(), 250));
            }

            discard();
        } else if (!level().isClientSide() && deathTicks % 10 == 0) {
            level().playSound(
                            null,
                            getX(),
                            getY(),
                            getZ(),
                            ModSounds.FSTBMB_PING.get(),
                            SoundSource.BLOCKS,
                            5.0F,
                            1.0F);
        }
    }
}
