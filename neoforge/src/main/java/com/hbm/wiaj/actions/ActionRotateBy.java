// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.wiaj.actions;

import com.hbm.wiaj.JarScene;
import com.hbm.wiaj.WorldInAJar;

public final class ActionRotateBy implements IJarAction {
    private final int time;
    private final double velYaw;
    private final double velPitch;

    public ActionRotateBy(double yaw, double pitch, int time) {
        velYaw = yaw / (time + 1);
        velPitch = pitch / (time + 1);
        this.time = time;
    }

    @Override
    public int getDuration() {
        return time;
    }

    @Override
    public void act(WorldInAJar world, JarScene scene) {
        scene.script.rotationPitch += velPitch;
        scene.script.rotationYaw += velYaw;
    }
}
