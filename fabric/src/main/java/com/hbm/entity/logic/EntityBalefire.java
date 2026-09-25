// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.logic;

import com.hbm.NuclearTech;
import com.hbm.explosion.ExplosionBalefire;
import com.hbm.explosion.ExplosionNukeGeneric;
import com.hbm.platform.Services;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class EntityBalefire extends EntityExplosionChunkloading {

    public int age = 0;
    public int destructionRange = 0;
    public ExplosionBalefire exp;
    public int speed = 1;
    public boolean did = false;

    public EntityBalefire(EntityType<? extends EntityBalefire> type, Level level) {
        super(type, level);
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.did) {
            if (!level().isClientSide() && Services.CONFIG.runtime().extendedLogging())
                NuclearTech.LOGGER.info(
                        "[NUKE] Initialized BF explosion at {} / {} / {} with strength {}!",
                        getX(),
                        getY(),
                        getZ(),
                        destructionRange);

            exp =
                    new ExplosionBalefire(
                            (int) getX(),
                            (int) getY(),
                            (int) getZ(),
                            level(),
                            this.destructionRange);

            this.did = true;
        }

        speed += 1;

        boolean flag = false;
        for (int i = 0; i < this.speed; i++) {
            flag = exp.update();

            if (flag) {
                this.discard();
            }
        }

        if (!flag) {
            ExplosionNukeGeneric.dealDamage(
                    level(), getX(), getY(), getZ(), this.destructionRange * 2);
        }

        age++;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        age = input.getIntOr("age", 0);
        destructionRange = input.getIntOr("destructionRange", 0);
        speed = input.getIntOr("speed", 1);
        did = input.getBooleanOr("did", false);

        exp =
                new ExplosionBalefire(
                        (int) getX(), (int) getY(), (int) getZ(), level(), this.destructionRange);
        exp.readFromNbt(input, "exp_");

        this.did = true;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("age", age);
        output.putInt("destructionRange", destructionRange);
        output.putInt("speed", speed);
        output.putBoolean("did", did);

        if (exp != null) exp.saveToNbt(output, "exp_");
    }
}
