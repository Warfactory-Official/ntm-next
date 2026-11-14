/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.render.item.weapon.sedna;

import com.hbm.client.render.WeaponRenderTypes;
import com.hbm.main.ResourceManager;
import com.hbm.render.item.weapon.sedna.WeaponItemRenderer;
import com.hbm.render.loader.UnitQuad;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import org.joml.Matrix4f;

public enum MuzzleFlash {
    NORMAL("normal", ResourceManager.flash_plume, false),
    GAP("gap", ResourceManager.flash_plume, true),
    LASER("laser", ResourceManager.laser_flash, false),
    FIREBALL("fireball", ResourceManager.flash_plume, true);

    private final String kind;
    private final Identifier texture;
    private final boolean depth;

    MuzzleFlash(String kind, Identifier texture, boolean depth) {
        this.kind = kind;
        this.texture = texture;
        this.depth = depth;
    }

    public static MuzzleFlash byName(String kind) {
        for (MuzzleFlash flash : values()) if (flash.kind.equals(kind)) return flash;
        throw new IllegalStateException("Unknown gun muzzle kind: " + kind);
    }

    public Identifier texture() {
        return texture;
    }

    public boolean depth() {
        return depth;
    }

    public double fire(long lastShot, int duration) {
        return WeaponItemRenderer.flashFire(
                lastShot,
                switch (this) {
                    case GAP -> WeaponItemRenderer.GAP_FLASH;
                    case FIREBALL -> AberratorItemRenderer.FIREBALL_FLASH;
                    case NORMAL, LASER -> duration;
                });
    }

    public void quads(double fire, double length, Matrix4f scratch, UnitQuad.Visitor visitor) {
        switch (this) {
            case NORMAL -> WeaponItemRenderer.muzzleFlashQuads(fire, length, scratch, visitor);
            case GAP -> WeaponItemRenderer.gapFlashQuads(fire, scratch, visitor);
            case LASER -> WeaponItemRenderer.laserFlashQuads(fire, length, scratch, visitor);
            case FIREBALL -> AberratorItemRenderer.fireballQuads(fire, scratch, visitor);
        }
    }

    public int argb(int color) {
        if (this != LASER) return -1;
        return ARGB.colorFromFloat(
                1F,
                ((color >> 16) & 0xFF) / 255F,
                ((color >> 8) & 0xFF) / 255F,
                (color & 0xFF) / 255F);
    }

    public RenderType renderType() {
        return depth
                ? WeaponRenderTypes.flashLitDepth(texture)
                : WeaponRenderTypes.flashLit(texture);
    }

    public void submit(
            SubmitNodeCollector collector,
            PoseStack pose,
            long lastShot,
            int duration,
            double length,
            int color) {
        double fire = fire(lastShot, duration);
        if (Double.isNaN(fire)) return;
        int argb = argb(color);
        collector.submitCustomGeometry(
                pose,
                renderType(),
                (p, tess) -> {
                    PoseStack.Pose scratch = new PoseStack.Pose();
                    quads(
                            fire,
                            length,
                            new Matrix4f(),
                            (quad, m) ->
                                    quad.emit(
                                            p,
                                            scratch,
                                            tess,
                                            m,
                                            WeaponItemRenderer.FLASH_LIGHT,
                                            argb));
                });
    }
}
