// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.main.ResourceManager;
import com.hbm.tileentity.turret.BlockEntityTurretRichard;
import org.joml.Matrix4f;

public class RenderTurretRichard extends RenderTurretBase<BlockEntityTurretRichard> {

    private static final int CARRIAGE = ResourceManager.turret_chekhov.partId("Carriage");
    private static final int LAUNCHER = ResourceManager.turret_richard.partId("Launcher");
    private static final int MISSILE = ResourceManager.turret_richard.partId("MissileLoaded");

    @Override
    protected void emit(BlockEntityTurretRichard turret, State state, TurretFrame frame) {
        connectors(frame, turret.connectorMask);
        Matrix4f launcher =
                mount(
                        frame,
                        state,
                        ResourceManager.turret_chekhov,
                        CARRIAGE,
                        ResourceManager.turret_carriage_tex,
                        1.5F,
                        ResourceManager.turret_richard,
                        LAUNCHER,
                        ResourceManager.turret_richard_tex);

        TurretFrame.Part previous = null;
        for (int i = 0; i < turret.loaded; i++) {
            TurretFrame.Part missile =
                    frame.push()
                            .obj(ResourceManager.turret_richard, MISSILE)
                            .texture(ResourceManager.turret_richard_tex);
            if (previous == null) {
                missile.pose.set(launcher).translate(0F, .375F, .1875F);
            } else {
                int last = i - 1;
                missile.pose.set(previous.pose);
                if (last == 2 || last == 6 || last == 9 || last == 13) {
                    missile.pose.translate(0F, -.1875F, .1875F * 2F + .09375F);
                } else {
                    missile.pose.translate(0F, 0F, -.1875F);
                }
            }
            previous = missile;
        }
    }
}
