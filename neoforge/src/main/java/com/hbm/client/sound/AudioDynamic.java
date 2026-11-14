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

package com.hbm.client.sound;

import com.hbm.blocks.multiblock.MultiblockSurface;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;

public class AudioDynamic extends AbstractTickableSoundInstance {

    public float maxVolume = 1F;
    public float range = 10F;
    public int keepAlive;
    public int timeSinceKA;
    public boolean shouldExpire;
    public @Nullable Entity parentEntity;
    long machineCore = MultiblockSurface.NO_CORE;

    protected AudioDynamic(SoundEvent sound, SoundSource category) {
        super(sound, category, RandomSource.create());
        looping = true;
        attenuation = SoundInstance.Attenuation.NONE;
    }

    public void setPosition(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public void attachTo(Entity entity) {
        parentEntity = entity;
    }

    @Override
    public void tick() {
        LocalPlayer player = Minecraft.getInstance().player;

        if (parentEntity != null && player != parentEntity) {
            setPosition(
                    (float) parentEntity.getX(),
                    (float) parentEntity.getY(),
                    (float) parentEntity.getZ());
        }

        if (player != null && player != parentEntity) {
            volume = volumeAtDistance(distanceTo(player));
        } else {
            if (player != null && player == parentEntity) {
                setPosition(
                        (float) parentEntity.getX(),
                        (float) parentEntity.getY() + 10F,
                        (float) parentEntity.getZ());
            }
            volume = maxVolume;
        }

        if (shouldExpire) {
            if (timeSinceKA > keepAlive) {
                stop();
            }
            timeSinceKA++;
        }
    }

    private float distanceTo(LocalPlayer player) {
        double dx = x - player.getX();
        double dy = y - player.getEyeY();
        double dz = z - player.getZ();
        return (float) Mth.length(dx, dy, dz);
    }

    public void start() {
        SoundManager manager = Minecraft.getInstance().getSoundManager();
        if (!manager.isActive(this)) {
            manager.play(this);
        }
    }

    public void stopSound() {
        stop();
        Minecraft.getInstance().getSoundManager().stop(this);
    }

    public void setVolume(float volume) {
        maxVolume = volume;
    }

    public void setRange(float range) {
        this.range = range;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

    public void setLooping(boolean looping) {
        this.looping = looping;
    }

    public void setKeepAlive(int keepAlive) {
        this.keepAlive = keepAlive;
        shouldExpire = true;
    }

    public void keepAlive() {
        timeSinceKA = 0;
    }

    public float volumeAtDistance(float distance) {
        return (distance / range) * -maxVolume + maxVolume;
    }

    public boolean isPlaying() {
        return !isStopped() && Minecraft.getInstance().getSoundManager().isActive(this);
    }

    public long machineCore() {
        return machineCore;
    }

    public float rawPitch() {
        return pitch;
    }
}
