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

package com.hbm.client.gui;

import com.hbm.client.render.FlatCutout;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.CardinalLighting;
import org.joml.Quaternionfc;

public final class PwrSliceRenderer extends PictureInPictureRenderer<PwrSliceRenderState> {

    private final QuadInstance quad = new QuadInstance();
    private int guiScale;

    @Override
    public Class<PwrSliceRenderState> getRenderStateClass() {
        return PwrSliceRenderState.class;
    }

    @Override
    protected String getTextureLabel() {
        return "PWR construction slice";
    }

    @Override
    protected float getTranslateY(int height, int guiScale) {
        return (height / guiScale / 2 - 36) * guiScale;
    }

    @Override
    public void prepare(
            PwrSliceRenderState state,
            GuiRenderState gui,
            FeatureRenderDispatcher features,
            int guiScale) {
        this.guiScale = guiScale;
        super.prepare(state, gui, features, guiScale);
        if (!state.job().beginCapture(state.slice())) return;
        int width = texture.getWidth(0);
        int height = texture.getHeight(0);
        var frame = Minecraft.getInstance().gameRenderer.mainRenderTarget();
        int outputWidth = Math.min(width, frame.width);
        int outputHeight = Math.min(height, frame.height);
        int stride = texture.getFormat().blockSize();
        GpuBuffer buffer =
                RenderSystem.getDevice()
                        .createBuffer(
                                () -> "PWR slice readback",
                                GpuBuffer.USAGE_MAP_READ | GpuBuffer.USAGE_COPY_DST,
                                (long) width * height * stride);
        RenderSystem.getDevice()
                .createCommandEncoder()
                .copyTextureToBuffer(
                        texture,
                        buffer,
                        0,
                        () -> {
                            try (buffer;
                                    var view = buffer.map(true, false)) {
                                NativeImage image =
                                        new NativeImage(outputWidth, outputHeight, false);
                                for (int y = 0; y < outputHeight; y++) {
                                    for (int x = 0; x < outputWidth; x++) {
                                        image.setPixelABGR(
                                                x,
                                                y,
                                                view.data()
                                                        .getInt(
                                                                (x + (height - 1 - y) * width)
                                                                        * stride));
                                    }
                                }
                                state.job().write(state.slice(), image);
                            }
                        },
                        0);
    }

    @Override
    protected void renderToTexture(
            PwrSliceRenderState state, PoseStack pose, SubmitNodeCollector collector) {
        var data = state.job().data;
        pose.translate((state.x1() / 2 - state.x1() / 2F) / 24F, 0, -400D / (24D * guiScale));
        pose.scale(-1, -1, 0.5F / guiScale);
        pose.rotate((Quaternionfc) Axis.XP.rotationDegrees(-30));
        pose.rotate((Quaternionfc) Axis.YP.rotationDegrees(225));
        boolean across = data.direction().getAxis() == Direction.Axis.X;
        pose.translate(
                (across ? data.sizeX() : data.sizeZ()) / -2D,
                data.sizeY() / -2D,
                (across ? data.sizeZ() : data.sizeX()) / -2D);
        var models = Minecraft.getInstance().getModelManager().getBlockStateModelSet();
        for (int i = 0; i < data.positions().length; i++) {
            if (BlockPos.getY(data.positions()[i]) - data.min().getY() != state.slice()) continue;
            var block = data.states()[i].rotate(data.rotation());
            List<BlockStateModelPart> parts = new ArrayList<>();
            models.get(block)
                    .collectParts(
                            RandomSource.create(block.getSeed(BlockPos.of(data.positions()[i]))),
                            parts);
            pose.pushPose();
            pose.translate(data.displayX(i), 0, data.displayZ(i));
            collector.submitCustomGeometry(
                    pose,
                    FlatCutout.of(TextureAtlas.LOCATION_BLOCKS),
                    (matrix, vertices) -> {
                        quad.setLightCoords(LightCoordsUtil.FULL_BRIGHT);
                        quad.setOverlayCoords(OverlayTexture.NO_OVERLAY);
                        for (BlockStateModelPart part : parts) {
                            for (Direction side : Direction.VALUES) {
                                for (BakedQuad face : part.getQuads(side))
                                    emit(matrix, vertices, face);
                            }
                            for (BakedQuad face : part.getQuads(null)) emit(matrix, vertices, face);
                        }
                    });
            pose.popPose();
        }
    }

    private void emit(PoseStack.Pose pose, VertexConsumer vertices, BakedQuad face) {
        Direction override = face.materialInfo().shadeDirectionOverride();
        int shade =
                (int)
                        (255F
                                * CardinalLighting.DEFAULT.byFace(
                                        override == null ? face.direction() : override));
        quad.setColor(0xFF000000 | shade << 16 | shade << 8 | shade);
        vertices.putBakedQuad(pose, face, quad);
    }
}
