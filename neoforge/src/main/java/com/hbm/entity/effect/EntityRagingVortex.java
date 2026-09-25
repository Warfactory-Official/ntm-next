// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.effect;

import com.hbm.entity.ModEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class EntityRagingVortex extends EntityBlackHole {

    private int timer;

    public EntityRagingVortex(EntityType<? extends EntityRagingVortex> type, Level level) {
        super(type, level);
    }

    public EntityRagingVortex(Level level) {
        this(ModEntities.RAGING_VORTEX.get(), level);
    }

    public EntityRagingVortex(Level level, float size) {
        this(level);
        setSize(size);
    }

    @Override
    public void tick() {

        if (level() instanceof ServerLevel server) {

            timer++;

            if (timer >= 20) timer -= 20;

            float pulse = (float) (Math.sin(timer) * Math.PI / 20D) * 0.35F;

            float dec = 0.0F;

            if (random.nextInt(100) == 0) {
                dec = 0.1F;

                server.explode(
                        this,
                        null,
                        null,
                        getX(),
                        getY(),
                        getZ(),
                        10F,
                        false,
                        Level.ExplosionInteraction.NONE);
            }

            setSize(getSize() - pulse - dec);
            if (getSize() <= 0F) {
                discard();
                return;
            }
        }

        super.tick();
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        timer = input.getIntOr("vortexTimer", 0);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("vortexTimer", timer);
    }
}
