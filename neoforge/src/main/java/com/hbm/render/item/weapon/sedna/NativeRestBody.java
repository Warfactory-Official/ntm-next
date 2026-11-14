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

import com.hbm.client.render.FlatCutout;
import com.hbm.client.render.WeaponRenderTypes;
import com.hbm.main.ResourceManager;
import com.hbm.render.item.weapon.sedna.WeaponItemRenderer;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.render.loader.UnitQuad;
import com.hbm.util.GameTime;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionfc;
import org.joml.Vector3fc;

public final class NativeRestBody implements RestBody {
    private final PoseStack pose;
    private final SubmitNodeCollector collector;
    private final int light;
    private final PoseStack.Pose scratch = new PoseStack.Pose();

    public NativeRestBody(PoseStack pose, SubmitNodeCollector collector, int light) {
        this.pose = pose;
        this.collector = collector;
        this.light = light;
    }

    @Override
    public void part(
            Matrix4fc at,
            HFRWavefrontObject model,
            int part,
            Identifier texture,
            int color,
            boolean fullBright) {
        pose.pushPose();
        pose.mulPose(at);
        WeaponItemRenderer.submitPart(
                collector,
                pose,
                RenderTypes.entityCutout(texture),
                model,
                part,
                fullBright ? WeaponItemRenderer.FULL_BRIGHT : light,
                color);
        pose.popPose();
    }

    @Override
    public void quad(Matrix4fc at, Matrix4fc corners, UnitQuad quad, Identifier texture) {
        pose.pushPose();
        pose.mulPose(at);
        Matrix4f cornered = new Matrix4f(corners);
        collector.submitCustomGeometry(
                pose,
                FlatCutout.of(texture),
                (p, buffer) -> quad.emit(p, scratch, buffer, cornered, light, -1));
        pose.popPose();
    }

    @Override
    public void balefireGlint(Matrix4fc at, HFRWavefrontObject model, int part) {
        pose.pushPose();
        pose.mulPose(at);
        for (int layer = 0; layer < 3; layer++) {
            WeaponItemRenderer.submitPart(
                    collector,
                    pose,
                    WeaponRenderTypes.balefireGlint(ResourceManager.glint_bf_tex, layer),
                    model,
                    part,
                    light,
                    WeaponRenderTypes.BALEFIRE_GLINT_TINT);
        }
        pose.popPose();
    }

    @Override
    public void text(Matrix4fc before, Vector3fc shift, Matrix4fc after, String text, int color) {
        float half = Minecraft.getInstance().font.width(text) / 2;
        pose.pushPose();
        pose.mulPose(before);
        pose.translate(shift.x() * half, shift.y() * half, shift.z() * half);
        pose.mulPose(after);
        collector.submitText(
                pose,
                0,
                0,
                Component.literal(text).getVisualOrderText(),
                false,
                Font.DisplayMode.NORMAL,
                WeaponItemRenderer.FULL_BRIGHT,
                color,
                0,
                0);
        pose.popPose();
    }

    @Override
    public void spinZ(Matrix4fc pivot, double msPerDegree, Consumer<RestBody> turned) {
        pose.pushPose();
        pose.mulPose(pivot);
        pose.rotate(
                (Quaternionfc)
                        Axis.ZP.rotationDegrees((float) (GameTime.now() / msPerDegree % 360D)));
        turned.accept(this);
        pose.popPose();
    }
}
