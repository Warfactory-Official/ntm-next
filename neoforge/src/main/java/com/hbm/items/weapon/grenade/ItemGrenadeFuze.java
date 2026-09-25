// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.weapon.grenade;

import com.hbm.entity.grenade.EntityGrenadeUniversal;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class ItemGrenadeFuze extends Item {

    public final EnumGrenadeFuze type;

    public ItemGrenadeFuze(Item.Properties properties, EnumGrenadeFuze type) {
        super(properties);
        this.type = type;
    }

    public static void updateTick(EntityGrenadeUniversal grenade) {
        switch (grenade.getFuze()) {
            case S3 -> {
                if (grenade.getTimer() >= 60) grenade.explode();
            }
            case S7 -> {
                if (grenade.getTimer() >= 140) grenade.explode();
            }
            case S15 -> {
                if (grenade.getTimer() >= 300) grenade.explode();
            }
            case AIRBURST -> airburst(grenade);
            case IMPACT -> {}
        }
    }

    public static void onImpact(EntityGrenadeUniversal grenade, HitResult impact) {
        switch (grenade.getFuze()) {
            case IMPACT -> {
                if (grenade.getTimer() >= 10) {
                    Vec3 hit = impact.getLocation();
                    grenade.setPos(hit.x, hit.y, hit.z);
                    grenade.explode();
                }
            }
            case S3, S7, S15, AIRBURST -> {}
        }
    }

    private static void airburst(EntityGrenadeUniversal grenade) {
        if (grenade.getTimer() < 30) return;

        HitResult trace =
                grenade.level()
                        .clip(
                                new ClipContext(
                                        grenade.position(),
                                        grenade.position().add(0D, -10D, 0D),
                                        ClipContext.Block.OUTLINE,
                                        ClipContext.Fluid.NONE,
                                        grenade));
        if (trace.getType() == HitResult.Type.BLOCK) grenade.explode();
    }

    public enum EnumGrenadeFuze {
        S3(0x000000),
        S7(0x404040),
        S15(0x808080),
        IMPACT(0xE36C17),
        AIRBURST(0x56A137);

        private final int bandColor;

        EnumGrenadeFuze(int bandColor) {
            this.bandColor = bandColor;
        }

        public int bandColor() {
            return bandColor;
        }
    }
}
