// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.main.ResourceManager;
import com.hbm.tileentity.turret.BlockEntityTurretSentry;
import com.hbm.tileentity.turret.BlockEntityTurretSentryDamaged;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public class RenderTurretSentry extends RenderTurretBase<BlockEntityTurretSentry> {

    private static final int PIVOT = ResourceManager.turret_sentry.partId("Pivot");
    private static final int BODY = ResourceManager.turret_sentry.partId("Body");
    private static final int DRUM = ResourceManager.turret_sentry.partId("Drum");
    private static final int BARREL_L = ResourceManager.turret_sentry.partId("BarrelL");
    private static final int BARREL_R = ResourceManager.turret_sentry.partId("BarrelR");

    @Override
    protected void emit(BlockEntityTurretSentry turret, State state, TurretFrame frame) {
        boolean damaged = turret instanceof BlockEntityTurretSentryDamaged;
        Identifier tex =
                damaged
                        ? ResourceManager.turret_sentry_damaged_tex
                        : ResourceManager.turret_sentry_tex;

        TurretFrame.Part pivot =
                frame.push().obj(ResourceManager.turret_sentry, PIVOT).texture(tex);
        pivot.pose.rotateY(state.yaw * Mth.DEG_TO_RAD);

        TurretFrame.Part body = frame.push().obj(ResourceManager.turret_sentry, BODY).texture(tex);
        body.pose
                .set(pivot.pose)
                .translate(0F, 1.25F, 0F)
                .rotateX(-state.pitch * Mth.DEG_TO_RAD)
                .translate(0F, -1.25F, 0F);

        frame.push().obj(ResourceManager.turret_sentry, DRUM).texture(tex).pose.set(body.pose);

        double left =
                Mth.lerp(state.partialTicks, turret.lastBarrelLeftPos, turret.barrelLeftPos)
                        * -0.5D;
        frame.push()
                .obj(ResourceManager.turret_sentry, BARREL_L)
                .texture(tex)
                .pose
                .set(body.pose)
                .translate(0F, 0F, (float) left);

        TurretFrame.Part right =
                frame.push().obj(ResourceManager.turret_sentry, BARREL_R).texture(tex);
        right.pose.set(body.pose);
        if (damaged) {
            right.pose
                    .translate(0F, 1.5F, 0.5F)
                    .rotateX(25F * Mth.DEG_TO_RAD)
                    .translate(0F, -1.5F, -0.5F);
        } else {
            double recoil =
                    Mth.lerp(state.partialTicks, turret.lastBarrelRightPos, turret.barrelRightPos)
                            * -0.5D;
            right.pose.translate(0F, 0F, (float) recoil);
        }
    }
}
