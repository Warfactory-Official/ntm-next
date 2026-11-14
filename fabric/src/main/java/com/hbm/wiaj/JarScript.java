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

package com.hbm.wiaj;

import com.hbm.wiaj.actors.JarActor;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.util.Mth;

public final class JarScript {
    public final WorldInAJar world;
    public final List<JarScene> scenes = new ArrayList<>();
    public final Map<Integer, JarActor> actors = new LinkedHashMap<>();
    public JarScene currentScene;
    public int sceneNumber;
    public double lastRotationYaw = -45, rotationYaw = -45;
    public double lastRotationPitch = -30, rotationPitch = -30;
    public double lastOffsetX, offsetX;
    public double lastOffsetY, offsetY;
    public double lastOffsetZ, offsetZ;
    public double lastZoom = 1, zoom = 1;
    public float interp;
    public int ticksElapsed;
    private boolean paused;
    private boolean firstTick = true;

    public JarScript(WorldInAJar world) {
        this.world = world;
    }

    public JarScript addScene(JarScene scene) {
        if (currentScene == null) currentScene = scene;
        scenes.add(scene);
        return this;
    }

    public void run() {
        if (!paused) advanceTick();
    }

    public void advanceTick() {
        if (firstTick) firstTick = false;
        else ticksElapsed++;
        lastRotationPitch = rotationPitch;
        lastRotationYaw = rotationYaw;
        lastOffsetX = offsetX;
        lastOffsetY = offsetY;
        lastOffsetZ = offsetZ;
        lastZoom = zoom;
        if (currentScene != null) {
            for (JarActor actor : actors.values()) actor.tick(currentScene);
            tickScene();
        }
    }

    public void tickScene() {
        currentScene.tick();
        if (currentScene.currentAction == null) {
            sceneNumber++;
            if (sceneNumber < scenes.size()) {
                currentScene = scenes.get(sceneNumber);
                currentScene.reset();
            } else {
                currentScene = null;
            }
        }
    }

    public void pause() {
        paused = true;
    }

    public void unpause() {
        paused = false;
    }

    public boolean isPaused() {
        return paused;
    }

    public void reset() {
        actors.clear();
        world.nuke();
        currentScene = scenes.getFirst();
        sceneNumber = 0;
        ticksElapsed = 0;
        firstTick = true;
        lastOffsetX = offsetX = 0;
        lastOffsetY = offsetY = 0;
        lastOffsetZ = offsetZ = 0;
        lastZoom = zoom = 1;
        lastRotationYaw = rotationYaw = -45;
        lastRotationPitch = rotationPitch = -30;
        for (JarScene scene : scenes) scene.reset();
    }

    private void fastForward(int target) {
        reset();
        int i = 0;
        while (sceneNumber < target && currentScene != null && i < 10_000) {
            advanceTick();
            i++;
        }
        if (i > 0 && currentScene != null) advanceTick();
    }

    public void rewindOne() {
        fastForward(Math.max(0, sceneNumber - 1));
    }

    public void forwardOne() {
        fastForward(Math.min(scenes.size(), sceneNumber + 1));
    }

    public double yaw() {
        return Mth.lerp(interp, lastRotationYaw, rotationYaw);
    }

    public double pitch() {
        return Mth.lerp(interp, lastRotationPitch, rotationPitch);
    }

    public double offsetX() {
        return Mth.lerp(interp, lastOffsetX, offsetX);
    }

    public double offsetY() {
        return Mth.lerp(interp, lastOffsetY, offsetY);
    }

    public double offsetZ() {
        return Mth.lerp(interp, lastOffsetZ, offsetZ);
    }

    public double zoom() {
        return Mth.lerp(interp, lastZoom, zoom);
    }
}
