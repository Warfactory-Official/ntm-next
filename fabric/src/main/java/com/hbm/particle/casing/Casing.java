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

package com.hbm.particle.casing;

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.jspecify.annotations.Nullable;

public record Casing(
        String name,
        CasingShape shape,
        float scaleX,
        float scaleY,
        float scaleZ,
        int[] tints,
        float bounceYaw,
        float bouncePitch,
        int lifetime) {

    public static final int BRASS = 0xEBC35E;
    public static final int EQUESTRIAN = 0x957BA0;
    public static final int GAUGE_12 = 0x757575;
    public static final int GAUGE_4 = 0xD8D8D8;
    public static final int MAGNUM_44 = 0x3E3E3E;
    public static final int INCH_16 = 0xD89128;
    public static final int INCH_16_PHOSPHORUS = 0xC8C8C8;
    public static final int INCH_16_NUCLEAR = 0x495443;
    public static final int GRENADE_40MM = 0x515151;

    public static final float MESH_SCALE = 0.05F;

    private static final ConcurrentHashMap<String, Casing> NAMED = new ConcurrentHashMap<>();

    public static Template template(CasingShape shape) {
        return new Template(shape);
    }

    public static @Nullable Casing named(String name) {
        return NAMED.get(name);
    }

    public static Casing register(Casing casing) {
        NAMED.put(casing.name, casing);
        return casing;
    }

    public int tint(int part) {
        return tints[part];
    }

    public int tintCount() {
        return tints.length;
    }

    public CasingImpactSound plink() {
        return CasingImpactSound.of(shape, scaleX, scaleY, scaleZ);
    }

    public float width() {
        return 2F * MESH_SCALE * Math.max(scaleX, scaleZ);
    }

    public float height() {
        return MESH_SCALE * scaleY;
    }

    public float meshLift() {
        return -height() / 4F + scaleY * 0.01F;
    }

    public float smokeHalfWidth() {
        return scaleX * 0.5F * MESH_SCALE;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Casing casing
                && name.equals(casing.name)
                && shape == casing.shape
                && Float.compare(scaleX, casing.scaleX) == 0
                && Float.compare(scaleY, casing.scaleY) == 0
                && Float.compare(scaleZ, casing.scaleZ) == 0
                && Arrays.equals(tints, casing.tints)
                && Float.compare(bounceYaw, casing.bounceYaw) == 0
                && Float.compare(bouncePitch, casing.bouncePitch) == 0
                && lifetime == casing.lifetime;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                name,
                shape,
                scaleX,
                scaleY,
                scaleZ,
                Arrays.hashCode(tints),
                bounceYaw,
                bouncePitch,
                lifetime);
    }

    @Override
    public String toString() {
        return "Casing["
                + name
                + ", "
                + shape
                + ", "
                + scaleX
                + "/"
                + scaleY
                + "/"
                + scaleZ
                + ", "
                + Arrays.toString(tints)
                + ", bounce "
                + bounceYaw
                + "/"
                + bouncePitch
                + ", "
                + lifetime
                + "t]";
    }

    public static final class Template {
        private final CasingShape shape;
        private float scaleX = 1F;
        private float scaleY = 1F;
        private float scaleZ = 1F;
        private int[] tints = {};
        private float bounceYaw = 1F;
        private float bouncePitch = 1F;
        private int lifetime = 240;

        private Template(CasingShape shape) {
            this.shape = shape;
        }

        public Template scale(float scale) {
            return scale(scale, scale, scale);
        }

        public Template scale(float x, float y, float z) {
            scaleX = x;
            scaleY = y;
            scaleZ = z;
            return this;
        }

        public Template tints(int... tints) {
            this.tints = tints;
            return this;
        }

        public Template bounce(float yaw, float pitch) {
            bounceYaw = yaw;
            bouncePitch = pitch;
            return this;
        }

        public Template lifetime(int ticks) {
            lifetime = ticks;
            return this;
        }

        public Template copy() {
            var copy = new Template(shape);
            copy.scaleX = scaleX;
            copy.scaleY = scaleY;
            copy.scaleZ = scaleZ;
            copy.tints = tints;
            copy.bounceYaw = bounceYaw;
            copy.bouncePitch = bouncePitch;
            copy.lifetime = lifetime;
            return copy;
        }

        public Casing build(String name) {
            return new Casing(
                    name, shape, scaleX, scaleY, scaleZ, tints, bounceYaw, bouncePitch, lifetime);
        }

        public Casing register(String name) {
            return Casing.register(build(name));
        }
    }
}
