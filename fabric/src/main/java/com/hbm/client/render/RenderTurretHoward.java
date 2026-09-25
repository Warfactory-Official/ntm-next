// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.main.ResourceManager;
import com.hbm.tileentity.turret.BlockEntityTurretHoward;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

public class RenderTurretHoward extends RenderTurretBase<BlockEntityTurretHoward> {

    private static final int CARRIAGE = ResourceManager.turret_howard.partId("Carriage");
    private static final int BODY = ResourceManager.turret_howard.partId("Body");
    private static final int BARRELS_TOP = ResourceManager.turret_howard.partId("BarrelsTop");
    private static final int BARRELS_BOTTOM = ResourceManager.turret_howard.partId("BarrelsBottom");

    @Override
    protected void emit(BlockEntityTurretHoward turret, State state, TurretFrame frame) {
        connectors(frame, turret.connectorMask);
        Matrix4f body =
                mount(
                        frame,
                        state,
                        ResourceManager.turret_howard,
                        CARRIAGE,
                        ResourceManager.turret_carriage_ciws_tex,
                        2.25F,
                        ResourceManager.turret_howard,
                        BODY,
                        ResourceManager.turret_howard_tex);

        float spin = Mth.lerp(state.partialTicks, turret.lastSpin, turret.spin);

        TurretFrame.Part top =
                frame.push()
                        .obj(ResourceManager.turret_howard, BARRELS_TOP)
                        .texture(ResourceManager.turret_howard_barrels_tex);
        top.pose
                .set(body)
                .translate(0F, 2.5F, 0F)
                .rotateX(-spin * Mth.DEG_TO_RAD)
                .translate(0F, -2.5F, 0F);

        TurretFrame.Part bottom =
                frame.push()
                        .obj(ResourceManager.turret_howard, BARRELS_BOTTOM)
                        .texture(ResourceManager.turret_howard_barrels_tex);
        bottom.pose
                .set(body)
                .translate(0F, 2F, 0F)
                .rotateX(spin * Mth.DEG_TO_RAD)
                .translate(0F, -2F, 0F);
    }
}
