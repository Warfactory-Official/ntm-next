// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.wiaj.actions;

import com.hbm.wiaj.JarScene;
import com.hbm.wiaj.WorldInAJar;

public final class ActionSetZoom implements IJarAction {
    private final int time;
    private final double zoom;

    public ActionSetZoom(double zoom, int time) {
        this.zoom = zoom / (time + 1);
        this.time = time;
    }

    @Override
    public int getDuration() {
        return time;
    }

    @Override
    public void act(WorldInAJar world, JarScene scene) {
        if (time == 0) scene.script.lastZoom = scene.script.zoom = zoom;
        else scene.script.zoom += zoom;
    }
}
