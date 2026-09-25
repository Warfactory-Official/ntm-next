// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.main.ResourceManager;
import com.hbm.tileentity.turret.BlockEntityTurretArty;
import net.minecraft.util.Mth;

public class RenderTurretArty extends RenderTurretBase<BlockEntityTurretArty> {

    private static final int CARRIAGE = ResourceManager.turret_arty.partId("Carriage");
    private static final int CANNON = ResourceManager.turret_arty.partId("Cannon");
    private static final int BARREL = ResourceManager.turret_arty.partId("Barrel");

    @Override
    protected void emit(BlockEntityTurretArty turret, State state, TurretFrame frame) {
        TurretFrame.Part carriage =
                frame.push()
                        .obj(ResourceManager.turret_arty, CARRIAGE)
                        .texture(ResourceManager.turret_arty_tex);
        carriage.pose.rotateY((state.yaw - 180F) * Mth.DEG_TO_RAD);

        TurretFrame.Part cannon =
                frame.push()
                        .obj(ResourceManager.turret_arty, CANNON)
                        .texture(ResourceManager.turret_arty_tex);
        cannon.pose
                .set(carriage.pose)
                .translate(0F, 3F, 0F)
                .rotateX(state.pitch * Mth.DEG_TO_RAD)
                .translate(0F, -3F, 0F);

        double barrel = Mth.lerp(state.partialTicks, turret.lastBarrelPos, turret.barrelPos) * 2.5D;
        frame.push()
                .obj(ResourceManager.turret_arty, BARREL)
                .texture(ResourceManager.turret_arty_tex)
                .pose
                .set(cannon.pose)
                .translate(0F, 0F, (float) barrel);
    }
}
