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

package com.hbm.handler;

import net.minecraft.world.phys.Vec3;

public class CasingEjector implements Cloneable {

    private Vec3 posOffset = Vec3.ZERO;
    private Vec3 initialMotion = Vec3.ZERO;
    private float randomYaw;
    private float randomPitch;

    public CasingEjector setOffset(double x, double y, double z) {
        this.posOffset = new Vec3(x, y, z);
        return this;
    }

    public CasingEjector setMotion(double x, double y, double z) {
        this.initialMotion = new Vec3(x, y, z);
        return this;
    }

    public CasingEjector setAngleRange(float yaw, float pitch) {
        this.randomYaw = yaw;
        this.randomPitch = pitch;
        return this;
    }

    public Vec3 getOffset() {
        return posOffset;
    }

    public Vec3 getMotion() {
        return initialMotion;
    }

    public float getYawFactor() {
        return randomYaw;
    }

    public float getPitchFactor() {
        return randomPitch;
    }

    @Override
    public CasingEjector clone() {
        try {
            return (CasingEjector) super.clone();
        } catch (CloneNotSupportedException e) {
            return new CasingEjector();
        }
    }
}
