// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.logic;

import com.hbm.entity.ModEntities;
import com.hbm.entity.item.EntityParachuteCrate;
import com.hbm.itempool.ItemPool;
import com.hbm.itempool.ItemPools;
import com.hbm.sound.AudioSystem;
import com.hbm.sound.AudioWrapper;
import com.hbm.sound.ModSounds;
import com.hbm.util.EnumUtil;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

public class EntityC130 extends EntityPlaneBase {

    public C130PayloadType payload = C130PayloadType.SUPPLIES;
    protected AudioWrapper audio;

    public EntityC130(EntityType<? extends EntityC130> type, Level level) {
        super(type, level);
    }

    @Override
    public void tick() {
        super.tick();

        if (level().isClientSide()) {
            if (this.health > 0) {
                if (audio == null || !audio.isPlaying()) {
                    audio =
                            AudioSystem.getLoopedSound(
                                    ModSounds.ENTITY_BOMBER_LOOP.get(),
                                    SoundSource.NEUTRAL,
                                    (float) getX(),
                                    (float) getY(),
                                    (float) getZ(),
                                    2F,
                                    250F,
                                    1F,
                                    20);
                    if (audio != null) audio.startSound();
                }
                if (audio != null) {
                    audio.keepAlive();
                    audio.updatePosition((float) getX(), (float) getY(), (float) getZ());
                }
            } else {
                if (audio != null && audio.isPlaying()) {
                    audio.stopSound();
                    audio = null;
                }
            }
        }

        if (!level().isClientSide()
                && this.tickCount == this.getLifetime() / 2
                && this.health > 0) {
            EntityParachuteCrate crate =
                    new EntityParachuteCrate(ModEntities.PARACHUTE_CRATE.get(), level());
            Vec3 motion = getDeltaMovement();
            crate.setPos(getX() - motion.x * 7, getY() - 10, getZ() - motion.z * 7);

            if (this.payload == C130PayloadType.SUPPLIES) {
                for (int i = 0; i < 5; i++)
                    crate.items.add(ItemPool.getStack(ItemPools.POOL_SUPPLIES, this.random));
            }
            if (this.payload == C130PayloadType.WEAPONS) {
                int amount = 1 + random.nextInt(2);
                for (int i = 0; i < amount; i++)
                    crate.items.add(ItemPool.getStack(ItemPools.POOL_WEAPONS, this.random));
                for (int i = 0; i < 6; i++)
                    crate.items.add(ItemPool.getStack(ItemPools.POOL_AMMO, this.random));
            }

            level().addFreshEntity(crate);
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.payload = EnumUtil.grabEnumSafely(C130PayloadType.class, input.getIntOr("payload", 0));
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("payload", this.payload.ordinal());
    }

    public void fac(Level world, double x, double y, double z, C130PayloadType payload) {
        Vec3 vector =
                new Vec3(
                                world.getRandom().nextDouble() - 0.5,
                                0,
                                world.getRandom().nextDouble() - 0.5)
                        .normalize()
                        .multiply(2, 0, 2);

        this.payload = payload;

        snapTo(x - vector.x * 100, y + 100, z - vector.z * 100, 0.0F, 0.0F);
        setDeltaMovement(vector.x, 0, vector.z);
        rotation();
        forceSpawnChunk();
    }

    public enum C130PayloadType {
        SUPPLIES,
        WEAPONS,
        A_FUCKING_FUEL_TRUCK
    }
}
