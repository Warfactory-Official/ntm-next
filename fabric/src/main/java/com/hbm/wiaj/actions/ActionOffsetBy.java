// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.wiaj.actions;

import com.hbm.wiaj.JarScene;
import com.hbm.wiaj.WorldInAJar;

public final class ActionOffsetBy implements IJarAction {
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
    public int getDuration() {
        return time;
    }

    @Override
    public void act(WorldInAJar world, JarScene scene) {
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
