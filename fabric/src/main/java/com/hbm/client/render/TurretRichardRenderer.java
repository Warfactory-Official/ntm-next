/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: HbmMods (The Bobcat)
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) HbmMods (The Bobcat). All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only,
 * as published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this code. If not, see <https://www.gnu.org/licenses/>.
 *
 * Under GPLv3 §7(c) and §7(e), these additional terms apply to LGPL-covered
 * material authored by movblock:
 *
 * - the license grants no rights to the name "NTM NEXT";
 * - a modified version must not be named NTM NEXT or presented as the
 *   official release, and must be marked as different from it.
 *
 * Accurately identifying a work as derived from NTM NEXT does not violate
 * these additional terms, provided it is clearly distinguished from the
 * official release and does not imply endorsement. Ports are encouraged to
 * acknowledge NTM NEXT as their upstream project and provide a link to it.
 */

package com.hbm.client.render;

import com.hbm.blockentity.turret.TurretRichardBlockEntity;
import com.hbm.main.ResourceManager;
import org.joml.Matrix4f;

public class TurretRichardRenderer extends TurretBaseRenderer<TurretRichardBlockEntity> {

    private static final int CARRIAGE = ResourceManager.turret_chekhov.partId("Carriage");
    private static final int LAUNCHER = ResourceManager.turret_richard.partId("Launcher");
    private static final int MISSILE = ResourceManager.turret_richard.partId("MissileLoaded");

    @Override
    protected void emit(TurretRichardBlockEntity turret, State state, TurretFrame frame) {
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
        for (int i = 0; i < turret.magazine(); i++) {
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
