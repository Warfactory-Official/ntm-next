// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.client.render.RenderCog;
import com.hbm.entity.ModEntities;
import com.hbm.entity.projectile.EntityArtilleryRocket;
import com.hbm.entity.projectile.EntityArtilleryShell;
import com.hbm.entity.projectile.EntityCog;
import com.hbm.entity.projectile.EntityFallingNuke;
import com.hbm.entity.projectile.EntitySawblade;
import com.hbm.entity.projectile.EntityTorpedo;
import com.hbm.items.weapon.ItemAmmoHIMARS.HIMARSRocketType;
import com.hbm.main.ResourceManager;
import com.hbm.util.GameTime;
import dev.engine_room.flywheel.lib.visualization.SimpleEntityVisualizer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

public final class OrdnanceVisuals {
    private OrdnanceVisuals() {}

    public static void initModels() {
        Models.init();
    }

    public static void register() {

        ObjEntityVisual.register(
                ModEntities.ARTILLERY_SHELL.get(),
                () ->
                        ObjEntityVisual.Spec.<EntityArtilleryShell>of(
                                        ResourceManager.projectiles,
                                        ResourceManager.grenade_tex,
                                        "Grenade")
                                .pose(
                                        (pose, entity, partialTick) -> {
                                            pose.rotateY(
                                                    (Mth.rotLerp(
                                                                            partialTick,
                                                                            entity.yRotO,
                                                                            entity.getYRot())
                                                                    - 90F)
                                                            * Mth.DEG_TO_RAD);
                                            pose.rotateZ(
                                                    (Mth.rotLerp(
                                                                            partialTick,
                                                                            entity.xRotO,
                                                                            entity.getXRot())
                                                                    - 90F)
                                                            * Mth.DEG_TO_RAD);
                                            pose.scale(2.5F, 5F, 2.5F);
                                        })
                                .unfogged());

        ObjEntityVisual.register(
                ModEntities.TORPEDO.get(),
                () ->
                        ObjEntityVisual.Spec.<EntityTorpedo>of(
                                        ResourceManager.torpedo,
                                        ResourceManager.torpedo_tex,
                                        "Cylinder")
                                .pose(
                                        (pose, entity, partialTick) ->
                                                pose.rotateX(
                                                        Math.min(
                                                                        85F,
                                                                        (entity.tickCount
                                                                                        + partialTick)
                                                                                * 3F)
                                                                * Mth.DEG_TO_RAD)));

        ObjEntityVisual.register(
                ModEntities.FALLING_BOMB.get(),
                () ->
                        ObjEntityVisual.Spec.<EntityFallingNuke>of(
                                        ResourceManager.lil_boy,
                                        ResourceManager.custom_nuke_tex,
                                        "Cylinder_Cylinder.002")
                                .pose(
                                        (pose, entity, partialTick) -> {
                                            float pitch =
                                                    Mth.lerp(
                                                            partialTick,
                                                            entity.xRotO,
                                                            entity.getXRot());
                                            pose.rotateZ(
                                                    (pitch < -80F ? 0F : pitch) * Mth.DEG_TO_RAD);
                                        }));

        ObjEntityVisual.register(
                ModEntities.STRAY_SAW.get(),
                () ->
                        ObjEntityVisual.Spec.<EntitySawblade>of(
                                        ResourceManager.sawmill,
                                        ResourceManager.sawmill_tex,
                                        "Blade")
                                .pose(
                                        (pose, entity, partialTick) ->
                                                disc(
                                                        pose,
                                                        entity.getOrientation(),
                                                        RenderCog.BLADE_SPIN_PERIOD)));

        SimpleEntityVisualizer.builder(ModEntities.BOMBLET_ZETA.get())
                .factory(BombletZetaVisual::new)
                .apply();
        SimpleEntityVisualizer.builder(ModEntities.GRENADE_BOUNCY_GENERIC.get())
                .factory(GenericGrenadeVisual::new)
                .apply();
        SimpleEntityVisualizer.builder(ModEntities.DISPERSER_CANISTER.get())
                .factory(GenericGrenadeVisual::new)
                .apply();

        SimpleEntityVisualizer.builder(ModEntities.COG.get())
                .factory(
                        (ctx, entity, partialTick) ->
                                new ObjEntityVisual<>(
                                        ctx,
                                        entity,
                                        partialTick,
                                        Models.COG[RenderCog.steel(entity) == 0 ? 0 : 1]))
                .apply();

        SimpleEntityVisualizer.builder(ModEntities.HIMARS.get())
                .factory(
                        (ctx, entity, partialTick) ->
                                new ObjEntityVisual<>(
                                        ctx,
                                        entity,
                                        partialTick,
                                        Models.HIMARS[entity.getRocket().ordinal()]))
                .apply();

        SimpleEntityVisualizer.builder(ModEntities.CHOPPER_MINE.get())
                .factory(ChopperMineVisual::new)
                .apply();

        SimpleEntityVisualizer.builder(ModEntities.SHRAPNEL.get())
                .factory(
                        (ctx, entity, partialTick) ->
                                new ShrapnelVisual<>(ctx, entity, partialTick))
                .apply();

        SimpleEntityVisualizer.builder(ModEntities.FIREWORK_BALL.get())
                .factory(
                        (ctx, entity, partialTick) ->
                                new ShrapnelVisual<>(ctx, entity, partialTick))
                .apply();

        SimpleEntityVisualizer.builder(ModEntities.BULLET.get())
                .factory(LegacyBulletVisual::new)
                .apply();
    }

    private static ObjEntityVisual.Spec<EntityCog> cog(boolean steel) {
        Identifier texture =
                !steel ? ResourceManager.stirling_tex : ResourceManager.stirling_steel_tex;
        return ObjEntityVisual.Spec.<EntityCog>of(ResourceManager.stirling, texture, "Cog")
                .pose(
                        (pose, cog, partialTick) ->
                                disc(pose, cog.getOrientation(), RenderCog.COG_SPIN_PERIOD));
    }

    private static ObjEntityVisual.Spec<EntityArtilleryRocket> himars(HIMARSRocketType rocket) {
        String part = Math.floorMod(rocket.modelType, 2) == 1 ? "RocketSingle" : "RocketStandard";
        return ObjEntityVisual.Spec.<EntityArtilleryRocket>of(
                        ResourceManager.turret_himars, rocket.texture, part)
                .pose(
                        (pose, entity, partialTick) -> {
                            pose.rotateY(
                                    (Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot()) - 90F)
                                            * Mth.DEG_TO_RAD);
                            pose.rotateZ(
                                    (Mth.lerp(partialTick, entity.xRotO, entity.getXRot()) - 90F)
                                            * Mth.DEG_TO_RAD);
                            pose.rotateY(90F * Mth.DEG_TO_RAD);
                            pose.rotateX(90F * Mth.DEG_TO_RAD);
                        })
                .unfogged();
    }

    private static void disc(Matrix4f pose, int orientation, long spinPeriod) {
        switch (orientation % 6) {
            case 5 -> pose.rotateY(90F * Mth.DEG_TO_RAD);
            case 2 -> pose.rotateY(180F * Mth.DEG_TO_RAD);
            case 4 -> pose.rotateY(270F * Mth.DEG_TO_RAD);
            default -> {}
        }
        pose.translate(0F, 0F, -1F);
        if (orientation < RenderCog.LANDED) {
            pose.rotateZ(-(GameTime.now() % spinPeriod) / RenderCog.SPIN_DIVISOR * Mth.DEG_TO_RAD);
        }
        pose.translate(0F, -1.375F, 0F);
    }

    private static final class Models {
        static final ObjEntityVisual.Prepared<EntityCog>[] COG =
                new ObjEntityVisual.Prepared[] {
                    ObjEntityVisual.prepare(cog(false)), ObjEntityVisual.prepare(cog(true))
                };
        static final ObjEntityVisual.Prepared<EntityArtilleryRocket>[] HIMARS = rockets();

        static void init() {}

        private static ObjEntityVisual.Prepared<EntityArtilleryRocket>[] rockets() {
            var types = HIMARSRocketType.values();
            var models = new ObjEntityVisual.Prepared[types.length];
            for (int i = 0; i < models.length; i++)
                models[i] = ObjEntityVisual.prepare(himars(types[i]));
            return models;
        }
    }
}
