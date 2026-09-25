// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.items.weapon.ItemAmmoHIMARS.HIMARSRocketType;
import com.hbm.items.weapon.ItemAmmoHIMARS;
import com.hbm.main.ResourceManager;
import com.hbm.tileentity.turret.BlockEntityTurretHIMARS;
import net.minecraft.util.Mth;

public class RenderTurretHIMARS extends RenderTurretBase<BlockEntityTurretHIMARS> {

    private static final int CARRIAGE = ResourceManager.turret_himars.partId("Carriage");
    private static final int LAUNCHER = ResourceManager.turret_himars.partId("Launcher");
    private static final int CRANE = ResourceManager.turret_himars.partId("Crane");
    private static final int TUBE_STANDARD = ResourceManager.turret_himars.partId("TubeStandard");
    private static final int TUBE_SINGLE = ResourceManager.turret_himars.partId("TubeSingle");
    private static final int CAP_SINGLE = ResourceManager.turret_himars.partId("CapSingle");
    private static final int[] CAPS_STANDARD =
            ResourceManager.turret_himars.partIds(
                    "CapStandard6",
                    "CapStandard5",
                    "CapStandard4",
                    "CapStandard3",
                    "CapStandard2",
                    "CapStandard1");

    @Override
    protected void emit(BlockEntityTurretHIMARS turret, State state, TurretFrame frame) {
        TurretFrame.Part carriage =
                frame.push()
                        .obj(ResourceManager.turret_himars, CARRIAGE)
                        .texture(ResourceManager.turret_himars_tex);
        carriage.pose.rotateY((state.yaw - 180F) * Mth.DEG_TO_RAD);

        TurretFrame.Part launcher =
                frame.push()
                        .obj(ResourceManager.turret_himars, LAUNCHER)
                        .texture(ResourceManager.turret_himars_tex);
        launcher.pose
                .set(carriage.pose)
                .translate(0F, 2.25F, 2F)
                .rotateX(state.pitch * Mth.DEG_TO_RAD)
                .translate(0F, -2.25F, -2F);

        double travel = Mth.lerp(state.partialTicks, turret.lastCrane, turret.crane) * -5D;
        TurretFrame.Part crane =
                frame.push()
                        .obj(ResourceManager.turret_himars, CRANE)
                        .texture(ResourceManager.turret_himars_tex);
        crane.pose.set(launcher.pose).translate(0F, 0F, (float) travel);

        if (turret.typeLoaded < 0) return;
        HIMARSRocketType type = ItemAmmoHIMARS.byIndex(turret.typeLoaded);

        if (type.modelType == 0) {
            frame.push()
                    .obj(ResourceManager.turret_himars, TUBE_STANDARD)
                    .texture(type.texture)
                    .pose
                    .set(crane.pose);
            for (int i = 0; i < turret.ammo && i < CAPS_STANDARD.length; i++) {
                frame.push()
                        .obj(ResourceManager.turret_himars, CAPS_STANDARD[i])
                        .texture(type.texture)
                        .pose
                        .set(crane.pose);
            }
        }

        if (type.modelType == 1) {
            frame.push()
                    .obj(ResourceManager.turret_himars, TUBE_SINGLE)
                    .texture(type.texture)
                    .pose
                    .set(crane.pose);
            if (turret.hasAmmo()) {
                frame.push()
                        .obj(ResourceManager.turret_himars, CAP_SINGLE)
                        .texture(type.texture)
                        .pose
                        .set(crane.pose);
            }
        }
    }
}
