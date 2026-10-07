// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.api.client;

import java.util.Arrays;
import net.minecraft.client.CloudStatus;
import net.minecraft.world.phys.Vec3;

public final class CloudLayers {

    public interface Renderer {
        void render(
                int color,
                CloudStatus status,
                float bottomY,
                int range,
                Vec3 camera,
                long gameTime,
                float partialTicks);

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

    public static void render(
            int color,
            CloudStatus status,
            float bottomY,
            int range,
            Vec3 camera,
            long gameTime,
            float partialTicks) {
        for (Renderer renderer : renderers)
            renderer.render(color, status, bottomY, range, camera, gameTime, partialTicks);
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
