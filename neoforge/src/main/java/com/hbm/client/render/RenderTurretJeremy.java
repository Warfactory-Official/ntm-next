// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.main.ResourceManager;
import com.hbm.tileentity.turret.BlockEntityTurretJeremy;

public class RenderTurretJeremy extends RenderTurretBase<BlockEntityTurretJeremy> {

    private static final int CARRIAGE = ResourceManager.turret_chekhov.partId("Carriage");
    private static final int GUN = ResourceManager.turret_jeremy.partId("Gun");

    @Override
    protected void emit(BlockEntityTurretJeremy turret, State state, TurretFrame frame) {
        connectors(frame, turret.connectorMask);
        mount(
                frame,
                state,
                ResourceManager.turret_chekhov,
                CARRIAGE,
                ResourceManager.turret_carriage_tex,
                1.5F,
                ResourceManager.turret_jeremy,
                GUN,
                ResourceManager.turret_jeremy_tex);
    }
}
