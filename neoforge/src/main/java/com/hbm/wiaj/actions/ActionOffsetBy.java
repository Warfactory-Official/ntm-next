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

package com.hbm.wiaj.actions;

import com.hbm.wiaj.JarScene;
import com.hbm.wiaj.WorldInAJar;

public final class ActionOffsetBy implements SceneAction {
    private final int time;
    private final double motionX;
    private final double motionY;
    private final double motionZ;

    public ActionOffsetBy(double x, double y, double z, int time) {
        motionX = x / (time + 1);
        motionY = y / (time + 1);
        motionZ = z / (time + 1);
        this.time = time;
    }

    @Override
    public int duration() {
        return time;
    }

    @Override
    public void perform(WorldInAJar world, JarScene scene) {
        scene.script.offsetX += motionX;
        scene.script.offsetY += motionY;
        scene.script.offsetZ += motionZ;
        if (time == 0) {
            scene.script.lastOffsetX = scene.script.offsetX;
            scene.script.lastOffsetY = scene.script.offsetY;
            scene.script.lastOffsetZ = scene.script.offsetZ;
        }
    }
}
