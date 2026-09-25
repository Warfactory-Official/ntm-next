// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.entity.ModEntities;
import com.hbm.entity.missile.EntityMissileAntiBallistic;
import com.hbm.entity.missile.EntityMissileBaseNT;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public final class MissileVisuals {
    private MissileVisuals() {}

    private static <T extends EntityMissileBaseNT> ObjEntityVisual.Pose<T> flight() {
        return (pose, entity, partialTick) -> {
            float yaw = Mth.rotLerp(partialTick, entity.renderYawO, entity.renderYaw);
            pose.rotateY((yaw - 90F) * Mth.DEG_TO_RAD);
            pose.rotateZ(
                    Mth.lerp(partialTick, entity.renderPitchO, entity.renderPitch)
                            * Mth.DEG_TO_RAD);
            pose.rotateY((90F - yaw) * Mth.DEG_TO_RAD);
            switch (entity.getFacing()) {
                case 2 -> pose.rotateY(90F * Mth.DEG_TO_RAD);
                case 4 -> pose.rotateY(180F * Mth.DEG_TO_RAD);
                case 3 -> pose.rotateY(270F * Mth.DEG_TO_RAD);
                case 5 -> pose.rotateY(0F * Mth.DEG_TO_RAD);
                default -> {}
            }
        };
    }

    private static <T extends EntityMissileBaseNT> ObjEntityVisual.Spec<T> hull(
            HFRWavefrontObject mesh, Identifier texture, float scale, String... parts) {
        return ObjEntityVisual.Spec.<T>of(mesh, texture, parts).scale(scale).pose(flight());
    }

    private static <T extends EntityMissileBaseNT> ObjEntityVisual.Spec<T> v2(Identifier texture) {
        return hull(ResourceManager.missileV2, texture, 1F, "Cylinder");
    }

    private static <T extends EntityMissileBaseNT> ObjEntityVisual.Spec<T> strong(
            Identifier texture) {
        return hull(ResourceManager.missileStrong, texture, 1.5F, "Circle");
    }

    private static <T extends EntityMissileBaseNT> ObjEntityVisual.Spec<T> huge(
            Identifier texture) {
        return hull(ResourceManager.missileHuge, texture, 1F, "Circle");
    }

    private static <T extends EntityMissileBaseNT> ObjEntityVisual.Spec<T> atlas(
            Identifier texture) {
        return hull(ResourceManager.missileNuclear, texture, 1F, "Circle.002_Circle.003");
    }

    private static <T extends EntityMissileBaseNT> ObjEntityVisual.Spec<T> micro(
            Identifier texture) {
        return hull(ResourceManager.missileMicro, texture, 1F, "Circle");
    }

    public static void register() {
        ObjEntityVisual.register(
                ModEntities.MISSILE_GENERIC.get(), () -> v2(ResourceManager.missileV2_HE_tex));
        ObjEntityVisual.register(
                ModEntities.MISSILE_INCENDIARY.get(), () -> v2(ResourceManager.missileV2_IN_tex));
        ObjEntityVisual.register(
                ModEntities.MISSILE_DECOY.get(), () -> v2(ResourceManager.missileV2_decoy_tex));
        ObjEntityVisual.register(
                ModEntities.MISSILE_BUSTER.get(), () -> v2(ResourceManager.missileV2_BU_tex));
        ObjEntityVisual.register(
                ModEntities.MISSILE_CLUSTER.get(), () -> v2(ResourceManager.missileV2_CL_tex));

        ObjEntityVisual.register(
                ModEntities.MISSILE_STRONG.get(),
                () -> strong(ResourceManager.missileStrong_HE_tex));
        ObjEntityVisual.register(
                ModEntities.MISSILE_INCENDIARY_STRONG.get(),
                () -> strong(ResourceManager.missileStrong_IN_tex));
        ObjEntityVisual.register(
                ModEntities.MISSILE_BUSTER_STRONG.get(),
                () -> strong(ResourceManager.missileStrong_BU_tex));
        ObjEntityVisual.register(
                ModEntities.MISSILE_CLUSTER_STRONG.get(),
                () -> strong(ResourceManager.missileStrong_CL_tex));
        ObjEntityVisual.register(
                ModEntities.MISSILE_EMP_STRONG.get(),
                () -> strong(ResourceManager.missileStrong_EMP_tex));

        ObjEntityVisual.register(
                ModEntities.MISSILE_BURST.get(), () -> huge(ResourceManager.missileHuge_HE_tex));
        ObjEntityVisual.register(
                ModEntities.MISSILE_INFERNO.get(), () -> huge(ResourceManager.missileHuge_IN_tex));
        ObjEntityVisual.register(
                ModEntities.MISSILE_RAIN.get(), () -> huge(ResourceManager.missileHuge_CL_tex));
        ObjEntityVisual.register(
                ModEntities.MISSILE_DRILL.get(), () -> huge(ResourceManager.missileHuge_BU_tex));

        ObjEntityVisual.register(
                ModEntities.MISSILE_NUCLEAR.get(), () -> atlas(ResourceManager.missileNuclear_tex));
        ObjEntityVisual.register(
                ModEntities.MISSILE_NUCLEAR_CLUSTER.get(),
                () -> atlas(ResourceManager.missileMIRV_tex));
        ObjEntityVisual.register(
                ModEntities.MISSILE_VOLCANO.get(), () -> atlas(ResourceManager.missileVolcano_tex));
        ObjEntityVisual.register(
                ModEntities.MISSILE_DOOMSDAY.get(),
                () -> atlas(ResourceManager.missileDoomsday_tex));
        ObjEntityVisual.register(
                ModEntities.MISSILE_DOOMSDAY_RUSTED.get(),
                () -> atlas(ResourceManager.missileDoomsdayRusted_tex));

        ObjEntityVisual.register(
                ModEntities.MISSILE_MICRO.get(), () -> micro(ResourceManager.missileMicro_tex));
        ObjEntityVisual.register(
                ModEntities.MISSILE_TAINT.get(),
                () -> micro(ResourceManager.missileMicroTaint_tex));
        ObjEntityVisual.register(
                ModEntities.MISSILE_BHOLE.get(),
                () -> micro(ResourceManager.missileMicroBHole_tex));
        ObjEntityVisual.register(
                ModEntities.MISSILE_SCHRABIDIUM.get(),
                () -> micro(ResourceManager.missileMicroSchrab_tex));
        ObjEntityVisual.register(
                ModEntities.MISSILE_EMP.get(), () -> micro(ResourceManager.missileMicroEMP_tex));
        ObjEntityVisual.register(
                ModEntities.MISSILE_TEST.get(), () -> micro(ResourceManager.missileMicroTest_tex));

        ObjEntityVisual.register(
                ModEntities.MISSILE_STEALTH.get(),
                () ->
                        hull(
                                ResourceManager.missileStealth,
                                ResourceManager.missileStealth_tex,
                                1F,
                                "Cylinder"));
        ObjEntityVisual.register(
                ModEntities.MISSILE_SHUTTLE.get(),
                () ->
                        hull(
                                ResourceManager.missileShuttle,
                                ResourceManager.missileShuttle_tex,
                                1F,
                                "Cube_Cube.001",
                                "Cylinder.003",
                                "Cylinder.002",
                                "Cylinder.001",
                                "Cylinder"));

        ObjEntityVisual.register(
                ModEntities.MISSILE_ANTI.get(),
                () ->
                        ObjEntityVisual.Spec.<EntityMissileAntiBallistic>of(
                                        ResourceManager.missileABM,
                                        ResourceManager.missileAA_tex,
                                        "Circle")
                                .pose(
                                        (pose, entity, partialTick) -> {
                                            float yaw =
                                                    Mth.rotLerp(
                                                            partialTick,
                                                            entity.yRotO,
                                                            entity.getYRot());
                                            pose.rotateY((yaw - 90F) * Mth.DEG_TO_RAD);
                                            pose.rotateZ(
                                                    Mth.lerp(
                                                                    partialTick,
                                                                    entity.xRotO,
                                                                    entity.getXRot())
                                                            * Mth.DEG_TO_RAD);
                                            pose.rotateY((90F - yaw) * Mth.DEG_TO_RAD);
                                        }));
    }
}
