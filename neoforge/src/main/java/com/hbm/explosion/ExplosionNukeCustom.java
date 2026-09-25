// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.explosion;

import com.hbm.config.BombConfig;
import com.hbm.entity.ModEntities;
import com.hbm.entity.effect.EntityCloudFleija;
import com.hbm.entity.effect.EntityCloudFleijaRainbow;
import com.hbm.entity.effect.EntityNukeTorex;
import com.hbm.entity.logic.EntityBalefire;
import com.hbm.entity.logic.EntityNukeExplosionMK3;
import com.hbm.entity.logic.EntityNukeExplosionMK5;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;

public final class ExplosionNukeCustom {

    public static final int MAX_TNT = 150;
    public static final int MAX_NUKE = 200;
    public static final int MAX_HYDRO = 350;
    public static final int MAX_AMAT = 350;
    public static final int MAX_SCHRAB = 250;

    public static final int MAX_DIRTY = 100;

    private ExplosionNukeCustom() {}

    public static void explodeCustom(
            Level level,
            double x,
            double y,
            double z,
            float tnt,
            float nuke,
            float hydro,
            float amat,
            float dirty,
            float schrab,
            float euph) {

        dirty = Math.min(dirty, MAX_DIRTY);

        if (euph > 0) {

            EntityNukeExplosionMK3 ex =
                    new EntityNukeExplosionMK3(ModEntities.NUKE_EXPLOSION_MK3.get(), level);
            ex.setPos(x, y, z);
            ex.destructionRange = 150;
            ex.speed = BombConfig.blastSpeed;
            ex.coefficient = 1.0F;

            level.addFreshEntity(ex);

            level.playSound(
                    null,
                    x,
                    y,
                    z,
                    SoundEvents.GENERIC_EXPLODE.value(),
                    SoundSource.BLOCKS,
                    100000.0F,
                    1.0F);

            level.addFreshEntity(EntityCloudFleijaRainbow.statFac(level, 50, x, y, z));

        } else if (schrab > 0) {

            schrab += amat / 2 + hydro / 4 + nuke / 8 + tnt / 16;
            schrab = Math.min(schrab, MAX_SCHRAB);

            EntityNukeExplosionMK3 ex =
                    EntityNukeExplosionMK3.statFacFleija(
                            level, x + 0.5, y + 0.5, z + 0.5, (int) schrab);
            if (!ex.isRemoved()) {
                level.addFreshEntity(ex);
                level.addFreshEntity(
                        EntityCloudFleija.statFac(level, (int) schrab, x + 0.5, y + 0.5, z + 0.5));
            }

        } else if (amat > 0) {

            amat += hydro / 2 + nuke / 4 + tnt / 8;
            amat = Math.min(amat, MAX_AMAT);

            EntityBalefire bf = new EntityBalefire(ModEntities.BALEFIRE.get(), level);
            bf.setPos(x + 0.5, y + 0.5, z + 0.5);
            bf.destructionRange = (int) amat;
            level.addFreshEntity(bf);
            EntityNukeTorex.statFacBale(level, x + 0.5, y + 5, z + 0.5, amat);

        } else if (hydro > 0) {

            hydro += nuke / 2 + tnt / 4;
            hydro = Math.min(hydro, MAX_HYDRO);
            dirty *= 0.25F;

            level.addFreshEntity(
                    EntityNukeExplosionMK5.statFac(level, (int) hydro, x + 0.5, y + 0.5, z + 0.5)
                            .moreFallout((int) dirty));
            EntityNukeTorex.statFac(level, x + 0.5, y + 5, z + 0.5, hydro);

        } else if (nuke > 0) {

            nuke += tnt / 2;
            nuke = Math.min(nuke, MAX_NUKE);

            level.addFreshEntity(
                    EntityNukeExplosionMK5.statFac(level, (int) nuke, x + 0.5, y + 5, z + 0.5)
                            .moreFallout((int) dirty));
            EntityNukeTorex.statFac(level, x + 0.5, y + 5, z + 0.5, nuke);

        } else if (tnt >= 75) {

            tnt = Math.min(tnt, MAX_TNT);

            level.addFreshEntity(
                    EntityNukeExplosionMK5.statFacNoRad(
                            level, (int) tnt, x + 0.5, y + 0.5, z + 0.5));
            EntityNukeTorex.statFac(level, x + 0.5, y + 5, z + 0.5, tnt);

        } else if (tnt > 0) {

            ExplosionLarge.explode(level, x + 0.5, y + 0.5, z + 0.5, tnt, true, true, true);
        }
    }
}
