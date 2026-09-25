// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.client.render.RenderLantern;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.TileEntityLantern;
import com.hbm.util.GameTime;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.material.CardinalLightingMode;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.material.CutoutShaders;
import dev.engine_room.flywheel.lib.material.LightShaders;
import dev.engine_room.flywheel.lib.material.Materials;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import java.util.function.Consumer;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

public final class LanternVisual extends HbmDynamicBlockEntityVisual<TileEntityLantern> {
    private static final HFRWavefrontObject MODEL = ResourceManager.lantern;
    private static final MeshPart LIGHT_PART =
            MeshPart.obj(
                    MODEL.groups[RenderLantern.LIGHT_PART],
                    MODEL.smoothing(),
                    SimpleMaterial.builderOf(Materials.CUTOUT_NO_CULL)
                            .texture(ResourceManager.white_tex)
                            .mipmap(false)
                            .cutout(CutoutShaders.ONE_TENTH)
                            .light(LightShaders.NONE)
                            .useLight(false)
                            .useOverlay(false)
                            .ambientOcclusion(false)
                            .cardinalLightingMode(CardinalLightingMode.OFF)
                            .build());
    private final TransformedInstance light;
    private final AABB rawBodyBounds;
    private int lastColor;
    private boolean initialized;

    public LanternVisual(
            VisualizationContext context, TileEntityLantern blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        var basePose = new Matrix4f().translation(.5F, 0F, .5F);
        rawBodyBounds = new AABB(pos).minmax(LightBounds.of(MODEL, "Lantern", basePose, pos));
        light =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, LIGHT_PART.model())
                        .createInstance();
        light.setTransform(
                        new Matrix4f()
                                .translation(visualPos.getX(), visualPos.getY(), visualPos.getZ())
                                .mul(basePose))
                .light(LightCoordsUtil.FULL_BRIGHT);
        writeFrame();
    }

    public static void initModels() {}

    @Override
    protected void frame(Context context) {
        writeFrame();
    }

    private void writeFrame() {
        int color = RenderLantern.flicker(GameTime.now());
        if (initialized && color == lastColor) return;
        light.colorArgb(color).setChanged();
        lastColor = color;
        initialized = true;
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return rawBodyBounds.minmax(
                new AABB(
                                pos.getX(),
                                pos.getY(),
                                pos.getZ(),
                                pos.getX() + 1,
                                pos.getY() + 6,
                                pos.getZ() + 1)
                        .inflate(1));
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {}

    @Override
    protected void _delete() {
        light.delete();
    }
}
