// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

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

    public static boolean replace(SkyRenderer vanilla, boolean disc) {
        Renderer renderer = claimed;
        SkyRenderState state = claimedState;
        if (renderer == null || state == null || drawing) return false;
        if (disc) {
            drawing = true;
            try {
                renderer.render(vanilla, state);
            } finally {
                drawing = false;
            }
        }
        return true;
    }

    public static void close() {
        for (Renderer renderer : renderers) renderer.close();
    }
}
