// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.missile;

import com.hbm.advancement.DetonationTrigger;
import com.hbm.advancement.HbmCriteria;
import com.hbm.client.ClientEffects;
import com.hbm.entity.ModEntities;
import com.hbm.explosion.ExplosionLarge;
import com.hbm.interfaces.StoredItems;
import com.hbm.items.ISatChip;
import com.hbm.items.ModItems;
import com.hbm.lib.ModDamageTypes;
import com.hbm.saveddata.satellites.Satellite;
import com.hbm.sound.ModSounds;
import com.hbm.util.ChunkUtil;
import java.util.Arrays;
import java.util.List;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class EntitySoyuz extends Entity implements StoredItems {

    private static final EntityDataAccessor<Integer> SKIN =
            SynchedEntityData.defineId(EntitySoyuz.class, EntityDataSerializers.INT);

    double acceleration = 0.00D;
    public int mode;
    public int targetX;
    public int targetZ;
    boolean memed = false;

    private final ItemStack[] payload = new ItemStack[18];

    @Override
    public void visitStoredItems(Visitor visitor) {
        for (int i = 0; i < payload.length; i++) {
            if (visitor.visit(payload[i]) && payload[i].isEmpty()) payload[i] = ItemStack.EMPTY;
        }
    }

    public EntitySoyuz(EntityType<? extends EntitySoyuz> type, Level level) {
        super(type, level);
        Arrays.fill(this.payload, ItemStack.EMPTY);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(SKIN, 0);
    }

    @Override
    public void tick() {

        Vec3 motion = getDeltaMovement();
        if (motion.y < 2.0D) {
            acceleration += 0.00025D;
            motion = new Vec3(motion.x, motion.y + acceleration, motion.z);
            setDeltaMovement(motion);
        }

        this.xo = this.xOld = getX();
        this.yo = this.yOld = getY();
        this.zo = this.zOld = getZ();
        setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);

        if (level() instanceof ServerLevel server) {

            List<Entity> list =
                    level().getEntities(
                                    this,
                                    new AABB(
                                            getX() - 5,
                                            getY() - 15,
                                            getZ() - 5,
                                            getX() + 5,
                                            getY(),
                                            getZ() + 5));

            for (Entity e : list) {
                e.igniteForSeconds(15);

                e.hurtServer(
                        server, level().damageSources().source(ModDamageTypes.EXHAUST), 100.0F);

                if (e instanceof Player) {
                    if (!memed) {
                        memed = true;
                        level().playSound(
                                        null,
                                        getX(),
                                        getY(),
                                        getZ(),
                                        ModSounds.ALARM_SOYUZED.get(),
                                        SoundSource.NEUTRAL,
                                        100.0F,
                                        1.0F);
                    }

                    if (e instanceof ServerPlayer serverPlayer) {
                        HbmCriteria.detonation(serverPlayer, DetonationTrigger.Kind.SOYUZ_EXHAUST);
                    }
                }
            }
        }

        if (level().isClientSide()) {
            spawnExhaust(getX(), getY(), getZ());
            spawnExhaust(getX() + 2.75, getY(), getZ());
            spawnExhaust(getX() - 2.75, getY(), getZ());
            spawnExhaust(getX(), getY(), getZ() + 2.75);
            spawnExhaust(getX(), getY(), getZ() - 2.75);
        }

        if (getY() > 600 && !level().isClientSide()) {
            deployPayload();
        }
    }

    private void spawnExhaust(double x, double y, double z) {
        double width = random.nextDouble() * 0.25 - 0.5;
        ClientEffects.spawnRocketFlame(
                level(),
                x + random.nextGaussian() * width,
                y,
                z + random.nextGaussian() * width,
                1.0F,
                0.0D,
                -0.75D + random.nextDouble() * 0.5D,
                0.0D,
                300 + random.nextInt(50));
    }

    private void deployPayload() {

        if (mode == 0) {

            ItemStack load = this.payload[0];

            if (!load.isEmpty()) {

                if (load.is(ModItems.FLAME_PONY.get())) {
                    ExplosionLarge.spawnTracers(level(), getX(), getY(), getZ(), 25);
                    for (Player p : level().players()) {
                        if (p instanceof ServerPlayer serverPlayer)
                            HbmCriteria.orbit(serverPlayer, load);
                    }
                }

                if (load.getItem() instanceof ISatChip
                        && level() instanceof ServerLevel orbitLevel) {
                    Satellite.orbit(
                            orbitLevel, load, ISatChip.getFreqS(load), getX(), getY(), getZ());
                }
            }
        }

        if (mode == 1) {

            EntitySoyuzCapsule capsule =
                    new EntitySoyuzCapsule(ModEntities.SOYUZ_CAPSULE.get(), level());
            capsule.payload = this.payload;
            capsule.soyuz = this.getSkin();
            capsule.setPos(targetX + 0.5, 600, targetZ + 0.5);

            if (level() instanceof ServerLevel server) {
                ChunkUtil.loadForEntity(server, new ChunkPos(targetX >> 4, targetZ >> 4));
            }

            level().addFreshEntity(capsule);
        }

        this.discard();
    }

    public void setSat(ItemStack stack) {
        this.payload[0] = stack == null ? ItemStack.EMPTY : stack;
    }

    public void setPayload(List<ItemStack> payload) {
        for (int i = 0; i < payload.size() && i < this.payload.length; i++) {
            ItemStack stack = payload.get(i);
            this.payload[i] = stack == null ? ItemStack.EMPTY : stack;
        }
    }

    public void setSkin(int i) {
        entityData.set(SKIN, i);
    }

    public int getSkin() {
        return entityData.get(SKIN);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {

        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 500000;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        setSkin(input.getIntOr("skin", 0));
        targetX = input.getIntOr("targetX", targetX);
        targetZ = input.getIntOr("targetZ", targetZ);
        mode = input.getIntOr("mode", mode);

        List<ItemStack> list =
                input.read("items", ItemStack.OPTIONAL_CODEC.listOf()).orElse(List.of());
        for (int i = 0; i < list.size() && i < payload.length; i++) {
            payload[i] = list.get(i);
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putInt("skin", getSkin());
        output.putInt("targetX", targetX);
        output.putInt("targetZ", targetZ);
        output.putInt("mode", mode);

        output.store("items", ItemStack.OPTIONAL_CODEC.listOf(), List.of(payload));
    }
}
