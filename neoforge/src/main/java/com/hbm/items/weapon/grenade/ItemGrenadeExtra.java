// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.weapon.grenade;

import com.hbm.entity.grenade.EntityGrenadeUniversal;
import java.util.List;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class ItemGrenadeExtra extends Item {

    public final EnumGrenadeExtra type;

    public ItemGrenadeExtra(Item.Properties properties, EnumGrenadeExtra type) {
        super(properties);
        this.type = type;
    }

    public static void updateTick(EntityGrenadeUniversal grenade) {
        EnumGrenadeExtra extra = grenade.getExtra();
        if (extra == null) return;
        switch (extra) {
            case PROXY_FUZE -> proximityFuze(grenade);
            case GLUE, FRAG_SLEEVE, TRIPLEX -> {}
        }
    }

    public static void onImpact(EntityGrenadeUniversal grenade, HitResult impact) {
        EnumGrenadeExtra extra = grenade.getExtra();
        if (extra == null) return;
        switch (extra) {
            case GLUE -> glue(grenade, impact);
            case PROXY_FUZE, FRAG_SLEEVE, TRIPLEX -> {}
        }
    }

    public static void onExplode(EntityGrenadeUniversal grenade) {
        EnumGrenadeExtra extra = grenade.getExtra();
        if (extra == null) return;
        switch (extra) {
            case FRAG_SLEEVE -> ItemGrenadeFilling.standardFragmentation(grenade, 25F);
            case TRIPLEX -> spawnTriplex(grenade);
            case GLUE, PROXY_FUZE -> {}
        }
    }

    private static void glue(EntityGrenadeUniversal grenade, HitResult impact) {
        if (!(impact instanceof BlockHitResult blockHit)) return;
        Vec3 hit = blockHit.getLocation();
        grenade.setPos(hit.x, hit.y, hit.z);

        grenade.getStuck(blockHit.getBlockPos(), blockHit.getDirection().get3DDataValue());
    }

    private static void proximityFuze(EntityGrenadeUniversal grenade) {
        if (grenade.getTimer() < 10 || grenade.getTimer() % 3 != 0) return;
        List<LivingEntity> living =
                grenade.level()
                        .getEntitiesOfClass(
                                LivingEntity.class,
                                new AABB(
                                                grenade.getX(),
                                                grenade.getY(),
                                                grenade.getZ(),
                                                grenade.getX(),
                                                grenade.getY(),
                                                grenade.getZ())
                                        .inflate(10D));
        for (LivingEntity e : living) {
            if (e == grenade.getThrower()) continue;
            if (e.distanceTo(grenade) <= 10F) {
                grenade.explode();
                return;
            }
        }
    }

    private static void spawnTriplex(EntityGrenadeUniversal grenade) {
        ItemStack frag =
                ItemGrenadeUniversal.make(
                        grenade.getShell(),
                        grenade.getFilling(),
                        ItemGrenadeFuze.EnumGrenadeFuze.S3);

        double angle = grenade.level().getRandom().nextDouble() * Math.PI * 2D;

        for (int i = 0; i < 3; i++) {
            EntityGrenadeUniversal triplet =
                    new EntityGrenadeUniversal(grenade.level(), frag)
                            .setTrail(EntityGrenadeUniversal.TRAIL_TRIPLET);
            triplet.setPos(grenade.getX(), grenade.getY(), grenade.getZ());
            triplet.setThrower(grenade.getThrower());
            triplet.setDeltaMovement(Math.cos(angle) * 0.25D, 0.75D, -Math.sin(angle) * 0.25D);
            grenade.level().addFreshEntity(triplet);
            angle += Math.toRadians(120D);
        }
    }

    public enum EnumGrenadeExtra {
        GLUE,
        PROXY_FUZE,
        FRAG_SLEEVE,
        TRIPLEX
    }
}
