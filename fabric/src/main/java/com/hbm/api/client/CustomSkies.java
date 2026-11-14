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

import java.util.Arrays;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import org.jspecify.annotations.Nullable;

public final class CustomSkies {

    public interface Renderer {
        boolean extract(ClientLevel level, float partialTicks, Camera camera, SkyRenderState state);

        void render(SkyRenderer vanilla, SkyRenderState state);

        default void close() {}
    }

    private static Renderer[] renderers = new Renderer[0];
    private static @Nullable Renderer claimed;
    private static @Nullable SkyRenderState claimedState;
    private static boolean drawing;

    private CustomSkies() {}

    public static void register(Renderer renderer) {
        renderers = Arrays.copyOf(renderers, renderers.length + 1);
        renderers[renderers.length - 1] = renderer;
    }

    public static boolean extract(
            ClientLevel level, float partialTicks, Camera camera, SkyRenderState state) {
        claimed = null;
        claimedState = null;
        for (Renderer renderer : renderers) {
            if (renderer.extract(level, partialTicks, camera, state)) {
                claimed = renderer;
                claimedState = state;
                return true;
            }
        }
        return false;
    }

    public static boolean render(SkyRenderer vanilla, SkyRenderState state) {
        Renderer renderer = claimed;
        if (renderer == null || claimedState != state || drawing) return false;
        drawing = true;
        try {
            renderer.render(vanilla, state);
        } finally {
            drawing = false;
        }
        return true;
    }

    public static void close() {
        for (Renderer renderer : renderers) renderer.close();
    }
}
