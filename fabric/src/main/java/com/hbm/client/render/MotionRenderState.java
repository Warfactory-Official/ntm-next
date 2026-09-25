// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

public interface MotionRenderState {

    boolean hbm$onGround();

    double hbm$motionY();

    void hbm$setMotion(boolean onGround, double motionY);
}
