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

package com.hbm.particle;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleGroup;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.ParticleGroupRenderState;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionfc;

public class TextParticleGroup extends ParticleGroup<Particle> {

    private static final int LIGHT = LightCoordsUtil.pack(15, 0);

    public TextParticleGroup(ParticleEngine engine) {
        super(engine);
    }

    @Override
    public ParticleGroupRenderState extractRenderState(
            Frustum frustum, Camera camera, float partialTicks) {
        List<Entry> out = new ArrayList<>(this.particles.size());
        Vec3 cam = camera.position();
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        float yaw = mc.player == null ? 0F : mc.player.getViewYRot(partialTicks);
        float pitch = mc.player == null ? 0F : mc.player.getViewXRot(partialTicks);

        for (Particle particle : this.particles) {
            String text;
            float scale;
            int color;
            boolean shadow;
            double px, py, pz;

            if (particle instanceof TextParticle fx) {
                text = fx.text();
                scale = fx.renderScale();
                color = fx.argb();
                shadow = fx.shadow();
                px = fx.interpX(partialTicks);
                py = fx.interpY(partialTicks);
                pz = fx.interpZ(partialTicks);
            } else if (particle instanceof LetterParticle fx) {
                text = fx.text();
                scale = fx.renderScale(partialTicks);
                color = fx.argb(partialTicks);
                shadow = fx.shadow();
                px = fx.interpX(partialTicks);
                py = fx.interpY(partialTicks);
                pz = fx.interpZ(partialTicks);
            } else {
                continue;
            }

            out.add(
                    new Entry(
                            (float) (px - cam.x),
                            (float) (py - cam.y),
                            (float) (pz - cam.z),
                            yaw,
                            pitch,
                            scale,
                            color,
                            shadow,
                            FormattedCharSequence.forward(text, Style.EMPTY),
                            -(int) (font.width(text) * 0.5F),
                            -(int) (font.lineHeight * 0.5F)));
        }

        return new State(out);
    }

    private record Entry(
            float px,
            float py,
            float pz,
            float yaw,
            float pitch,
            float scale,
            int color,
            boolean shadow,
            FormattedCharSequence text,
            int offsetX,
            int offsetY) {}

    private record State(List<Entry> entries) implements ParticleGroupRenderState {

        @Override
        public void submit(SubmitNodeCollector collector, CameraRenderState camera) {
            if (entries.isEmpty()) return;
            PoseStack pose = new PoseStack();

            for (Entry e : entries) {
                pose.pushPose();
                pose.translate(e.px, e.py, e.pz);
                pose.rotate((Quaternionfc) Axis.YP.rotationDegrees(-e.yaw));
                pose.rotate((Quaternionfc) Axis.XP.rotationDegrees(e.pitch));
                pose.scale(-e.scale, -e.scale, e.scale);

                collector.submitText(
                        pose,
                        e.offsetX,
                        e.offsetY,
                        e.text,
                        e.shadow,
                        Font.DisplayMode.NORMAL,
                        LIGHT,
                        e.color,
                        0,
                        0);
                pose.popPose();
            }
        }
    }
}
