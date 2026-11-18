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

package com.hbm.api.client;

import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import java.util.Arrays;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.renderer.oit.OitRenderPassProvider;
import net.minecraft.client.renderer.oit.OitStage;
import net.minecraft.world.phys.Vec3;

public final class CloudLayers {

    public interface Renderer {
        void prepare(
                int color,
                CloudStatus status,
                float bottomY,
                int range,
                Vec3 camera,
                long gameTime,
                float partialTicks);

        void render(CloudStatus status, RenderPass pass);

        void renderOit(
                CloudStatus status,
                OitStage stage,
                GpuTextureView mainDepth,
                OitRenderPassProvider.Parameters parameters);

        default void reload() {}

        default void markForRebuild() {}

        default void endFrame() {}

        default void close() {}
    }

    private static Renderer[] renderers = new Renderer[0];

    private CloudLayers() {}

    public static void register(Renderer renderer) {
        renderers = Arrays.copyOf(renderers, renderers.length + 1);
        renderers[renderers.length - 1] = renderer;
    }

    public static void prepare(
            int color,
            CloudStatus status,
            float bottomY,
            int range,
            Vec3 camera,
            long gameTime,
            float partialTicks) {
        for (Renderer renderer : renderers)
            renderer.prepare(color, status, bottomY, range, camera, gameTime, partialTicks);
    }

    public static void render(CloudStatus status, RenderPass pass) {
        for (Renderer renderer : renderers) renderer.render(status, pass);
    }

    public static void renderOit(
            CloudStatus status,
            OitStage stage,
            GpuTextureView mainDepth,
            OitRenderPassProvider.Parameters parameters) {
        for (Renderer renderer : renderers)
            renderer.renderOit(status, stage, mainDepth, parameters);
    }

    public static void reload() {
        for (Renderer renderer : renderers) renderer.reload();
    }

    public static void markForRebuild() {
        for (Renderer renderer : renderers) renderer.markForRebuild();
    }

    public static void endFrame() {
        for (Renderer renderer : renderers) renderer.endFrame();
    }

    public static void close() {
        for (Renderer renderer : renderers) renderer.close();
    }
}
