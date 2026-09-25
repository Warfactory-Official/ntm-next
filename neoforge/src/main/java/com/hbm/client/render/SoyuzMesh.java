// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.main.ResourceManager;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public final class SoyuzMesh {

    private static final int MAINENGINES = 7;
    private static final int BOOSTER = 9;
    private static final int BOOSTERSIDE = 10;

    private static final int[] SINGLE =
            ResourceManager.soyuz.partIds(
                    "EngineBlock",
                    "BottomStage",
                    "TopStage",
                    "Payload",
                    "PayloadBlocks",
                    "LES",
                    "LESThrusters",
                    "MainEngines",
                    "SideEngines");
    private static final int MEMENTO = ResourceManager.soyuz.partId("Memento");
    private static final int[] BOOSTERS =
            ResourceManager.soyuz.partIds(
                    "Booster.000", "Booster.001", "Booster.002", "Booster.003");
    private static final int[] BOOSTER_ENGINES =
            ResourceManager.soyuz.partIds(
                    "BoosterEngines.000",
                    "BoosterEngines.001",
                    "BoosterEngines.002",
                    "BoosterEngines.003");
    private static final int[] BOOSTER_SIDES =
            ResourceManager.soyuz.partIds(
                    "BoosterSide.000", "BoosterSide.001", "BoosterSide.002", "BoosterSide.003");

    private SoyuzMesh() {}

    public static boolean hasSkin(int skin) {
        return skin >= 0 && skin < ResourceManager.soyuz_skin_tex.length;
    }

    public static int wrapSkin(int skin) {
        return Mth.positiveModulo(skin, ResourceManager.soyuz_skin_tex.length);
    }

    public static void submit(
            PoseStack poseStack, SubmitNodeCollector collector, int light, int skinIndex) {
        Identifier[] skin = ResourceManager.soyuz_skin_tex[skinIndex];

        for (int i = 0; i < SINGLE.length; i++)
            part(poseStack, collector, light, skin[i], SINGLE[i]);
        part(poseStack, collector, light, ResourceManager.soyuz_memento_tex, MEMENTO);
        for (int booster : BOOSTERS) part(poseStack, collector, light, skin[BOOSTER], booster);
        for (int engine : BOOSTER_ENGINES)
            part(poseStack, collector, light, skin[MAINENGINES], engine);
        for (int side : BOOSTER_SIDES) part(poseStack, collector, light, skin[BOOSTERSIDE], side);
    }

    private static void part(
            PoseStack poseStack,
            SubmitNodeCollector collector,
            int light,
            Identifier texture,
            int part) {
        collector.submitCustomGeometry(
                poseStack,
                RenderTypes.entityCutoutCull(texture),
                (pose, buffer) -> ResourceManager.soyuz.renderPart(pose, buffer, light, -1, part));
    }
}
