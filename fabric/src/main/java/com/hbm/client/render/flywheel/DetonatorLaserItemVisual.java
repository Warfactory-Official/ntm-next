// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.client.render.DetonatorLaserItemRenderer;
import com.hbm.main.ResourceManager;
import com.hbm.util.GameTime;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.PosedInstance;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.util.LightCoordsUtil;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

public final class DetonatorLaserItemVisual extends HbmItemVisual {
    private final TransformedInstance main;
    private final TransformedInstance lights;
    private final List<PosedInstance> beam = new ArrayList<>();
    private final WorldText[] readouts = new WorldText[3];
    private final Matrix4f scratch = new Matrix4f();
    private final Matrix4f world = new Matrix4f();
    private final Matrix3f normal = new Matrix3f();
    private final PoseStack.Pose text = new PoseStack.Pose();
    private int quad;

    public DetonatorLaserItemVisual(VisualizationContext ctx) {
        super(ctx);
        main =
                transformed(
                        ItemMaterials.part(
                                ResourceManager.detonator_laser,
                                DetonatorLaserItemRenderer.PART_MAIN,
                                ItemMaterials.cutout(ResourceManager.detonator_laser_tex, true)));
        Material lit = ItemMaterials.lights();
        lights =
                transformed(
                        ItemMaterials.part(
                                ResourceManager.detonator_laser,
                                DetonatorLaserItemRenderer.PART_LIGHTS,
                                lit));
        DetonatorLaserItemRenderer.beamQuads(
                0L,
                scratch,
                (unit, matrix) -> {
                    beam.add(posed(ItemMaterials.group(unit.group(), true, lit)));
                });
        for (int i = 0; i < readouts.length; i++)
            readouts[i] = new WorldText(instancers, WorldText.Style.NORMAL, true);
    }

    @Override
    protected void frame(Matrix4fc pose, int light, int overlay, float partialTick) {
        long now = GameTime.now();
        write(main, pose, -1, light);
        write(lights, pose, DetonatorLaserItemRenderer.LIGHTS_COLOR, LightCoordsUtil.FULL_BRIGHT);

        normal.set(pose).invert().transpose();
        quad = 0;
        DetonatorLaserItemRenderer.beamQuads(
                now,
                scratch,
                (unit, matrix) -> {
                    write(
                            beam.get(quad++),
                            world.set(pose).mul(matrix),
                            normal,
                            DetonatorLaserItemRenderer.BEAM_COLOR,
                            LightCoordsUtil.FULL_BRIGHT);
                });

        String[] lines = DetonatorLaserItemRenderer.readoutLines(now);
        text.setIdentity();
        text.pose().set(pose);
        DetonatorLaserItemRenderer.readoutPose(text);
        for (int i = 0; i < readouts.length; i++) {
            readouts[i].set(
                    Component.literal(lines[i]), 0F, 0F, DetonatorLaserItemRenderer.TEXT_COLOR);
            readouts[i].write(text.pose());
            text.translate(0F, DetonatorLaserItemRenderer.READOUT_STEP, 0F);
        }
    }

    @Override
    public void hide() {
        super.hide();
        for (WorldText readout : readouts) {
            readout.set(null, 0F, 0F, 0);
            readout.write(world);
        }
    }

    @Override
    public void delete() {
        super.delete();
        for (WorldText readout : readouts) readout.delete();
    }
}
