// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.main.ResourceManager;
import com.hbm.tileentity.turret.BlockEntityTurretChekhov;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

public class RenderTurretChekhov<T extends BlockEntityTurretChekhov> extends RenderTurretBase<T> {

    private static final int CARRIAGE = ResourceManager.turret_chekhov.partId("Carriage");
    private static final int BODY = ResourceManager.turret_chekhov.partId("Body");
    private static final int BARRELS = ResourceManager.turret_chekhov.partId("Barrels");

    private final Identifier carriageTex;

    public RenderTurretChekhov(Identifier carriageTex) {
        this.carriageTex = carriageTex;
    }

    @Override
    protected void emit(T turret, State state, TurretFrame frame) {
        connectors(frame, turret.connectorMask);
        Matrix4f body =
                mount(
                        frame,
                        state,
                        ResourceManager.turret_chekhov,
                        CARRIAGE,
                        carriageTex,
                        1.5F,
                        ResourceManager.turret_chekhov,
                        BODY,
                        ResourceManager.turret_chekhov_tex);

        float spin = Mth.lerp(state.partialTicks, turret.lastSpin, turret.spin);
        TurretFrame.Part barrels =
                frame.push()
                        .obj(ResourceManager.turret_chekhov, BARRELS)
                        .texture(ResourceManager.turret_chekhov_barrels_tex);
        barrels.pose
                .set(body)
                .translate(0F, 1.5F, 0F)
                .rotateX(-spin * Mth.DEG_TO_RAD)
                .translate(0F, -1.5F, 0F);
    }
}
