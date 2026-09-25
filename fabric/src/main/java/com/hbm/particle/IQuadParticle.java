// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.particle;

public interface IQuadParticle {

    double interpX(float pt);

    double interpY(float pt);

    double interpZ(float pt);

    float size(float pt);

    int argb(float pt);

    default float spin(float pt) {
        return 0F;
    }

    int light(float pt);

    default float u0() {
        return 0F;
    }

    default float u1() {
        return 1F;
    }

    default float v0() {
        return 0F;
    }

    default float v1() {
        return 1F;
    }
}
