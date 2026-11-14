/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) Contributors to Hbm's Nuclear Tech Mod. All rights reserved.
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

package com.hbm.render.anim;

import com.hbm.config.GunVisualConfig;

public class BusAnimationKeyframe {

    public final double value;
    public final int authoredDuration;
    public final int duration;
    public final IType interpolationType;
    public final EType easingType;
    public double leftX;
    public double leftY;
    public HType leftType;
    public double rightX;
    public double rightY;
    public HType rightType;
    public double amplitude;
    public double period;
    public double back;

    public BusAnimationKeyframe(double value, int duration) {
        this(value, duration, IType.LINEAR);
    }

    public BusAnimationKeyframe(double value, int duration, IType interpolation) {
        this(value, duration, interpolation, EType.AUTO);
    }

    public BusAnimationKeyframe(double value, int duration, IType interpolation, EType easing) {
        this.value = value;
        authoredDuration = duration;
        this.duration = (int) (duration / Math.max(0.001D, GunVisualConfig.animationSpeed));
        interpolationType = interpolation;
        easingType = easing;
    }

    public enum IType {
        CONSTANT,
        LINEAR,
        SIN_UP,
        SIN_DOWN,
        SIN_FULL,
        BEZIER,
        SINE,
        QUAD,
        CUBIC,
        QUART,
        QUINT,
        EXPO,
        CIRC,
        BOUNCE,
        ELASTIC,
        BACK,
    }

    public enum EType {
        AUTO,
        EASE_IN,
        EASE_OUT,
        EASE_IN_OUT,
    }

    public enum HType {
        FREE,
        ALIGNED,
        VECTOR,
        AUTO,
        AUTO_CLAMPED,
    }
}
