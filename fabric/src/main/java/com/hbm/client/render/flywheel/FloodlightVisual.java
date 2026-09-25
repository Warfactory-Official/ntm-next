// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.blocks.machine.Floodlight;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.machine.BlockEntityFloodlight;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.material.CardinalLightingMode;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.material.CutoutShaders;
import dev.engine_room.flywheel.lib.material.LightShaders;
import dev.engine_room.flywheel.lib.material.Materials;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import java.util.function.Consumer;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

public final class FloodlightVisual extends HbmDynamicBlockEntityVisual<BlockEntityFloodlight>
        implements ShaderLightVisual {
    private static final HFRWavefrontObject MODEL = ResourceManager.floodlight;
    private static final Material BODY_MATERIAL =
            MeshPart.litCutout(ResourceManager.floodlight_tex);
    private static final MeshPart LIGHTS_PART =
            MeshPart.obj(MODEL.groups[MODEL.partId("Lights")], MODEL.smoothing(), BODY_MATERIAL);
    private static final MeshPart LAMPS_OFF_PART =
            MeshPart.obj(MODEL.groups[MODEL.partId("Lamps")], MODEL.smoothing(), BODY_MATERIAL);
    private static final Material LAMPS_ON_MATERIAL =
            SimpleMaterial.builderOf(Materials.CUTOUT)
                    .texture(ResourceManager.floodlight_tex)
                    .mipmap(false)
                    .cutout(CutoutShaders.ONE_TENTH)
                    .light(LightShaders.NONE)
                    .cardinalLightingMode(CardinalLightingMode.OFF)
                    .ambientOcclusion(false)
                    .backfaceCulling(false)
                    .build();
    private static final MeshPart LAMPS_ON_PART =
            MeshPart.obj(MODEL.groups[MODEL.partId("Lamps")], MODEL.smoothing(), LAMPS_ON_MATERIAL);

    private final TransformedInstance lights;
    private final TransformedInstance lampsOff;
    private final TransformedInstance lampsOn;
    private final Matrix4f basePose = new Matrix4f();
    private final Matrix4f localPose = new Matrix4f();
    private final Matrix4f instancePose = new Matrix4f();
    private final double[] lightBoundsAccumulator = new double[6];
    private final AABB rawBodyBounds;
    private int lastFacing = Integer.MIN_VALUE;
    private float lastRotation = Float.NaN;
    private boolean lastOn;
    private boolean initialized;
    private @Nullable AABB lastLightBounds;

    public FloodlightVisual(
            VisualizationContext context, BlockEntityFloodlight blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        int rawFacing = blockEntity.getBlockState().getValue(Floodlight.FACING);
        var rawBodyLocal = new Matrix4f().translate(.5F, .5F, .5F);
        switch (rawFacing) {
            case 0, 6 -> rawBodyLocal.rotateX((180F) * Mth.DEG_TO_RAD);
            case 1, 7 -> {}
            case 2 -> rawBodyLocal.rotateX((90F) * Mth.DEG_TO_RAD).rotateZ((180F) * Mth.DEG_TO_RAD);
            case 3 -> rawBodyLocal.rotateX((90F) * Mth.DEG_TO_RAD);
            case 4 -> rawBodyLocal.rotateX((90F) * Mth.DEG_TO_RAD).rotateZ((90F) * Mth.DEG_TO_RAD);
            case 5 -> rawBodyLocal.rotateX((90F) * Mth.DEG_TO_RAD).rotateZ((270F) * Mth.DEG_TO_RAD);
            default ->
                    throw new IllegalStateException("Unexpected floodlight facing: " + rawFacing);
        }
        rawBodyLocal.translate(0F, -.5F, 0F);
        if (rawFacing != 0 && rawFacing != 1) rawBodyLocal.rotateY((90F) * Mth.DEG_TO_RAD);
        basePose.set(rawBodyLocal);
        rawBodyBounds = LightBounds.of(MODEL, "Base", rawBodyLocal, pos);
        lights =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, LIGHTS_PART.model())
                        .createInstance();
        lampsOff =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, LAMPS_OFF_PART.model())
                        .createInstance();
        lampsOn =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, LAMPS_ON_PART.model())
                        .createInstance();
        updateMovingParts(partialTick);
    }

    public static void initModels() {}

    @Override
    protected void frame(Context context) {
        updateMovingParts(context.partialTick());
    }

    public void updateMovingParts(float partialTick) {
        int facing = blockEntity.getBlockState().getValue(Floodlight.FACING);
        float rotation = blockEntity.rotation;
        if (facing == 0 || facing == 6) rotation -= 90F;
        if (facing == 1 || facing == 7) rotation += 90F;
        boolean on = blockEntity.isOn;
        if (initialized
                && facing == lastFacing
                && Float.compare(rotation, lastRotation) == 0
                && on == lastOn) return;
        lastFacing = facing;
        lastRotation = rotation;
        lastOn = on;
        initialized = true;
        localPose
                .set(basePose)
                .translate(0F, .5F, 0F)
                .rotateZ((rotation) * Mth.DEG_TO_RAD)
                .translate(0F, -.5F, 0F);
        instancePose
                .translation(visualPos.getX(), visualPos.getY(), visualPos.getZ())
                .mul(localPose);
        lights.setTransform(instancePose).light(0).setChanged();

        if (on) {
            lampsOff.setVisible(false);
            lampsOn.setVisible(true);
            lampsOn.setTransform(instancePose).light(LightCoordsUtil.FULL_BRIGHT).setChanged();
        } else {
            lampsOff.setVisible(true);
            lampsOff.setTransform(instancePose).colorArgb(0xFF404040).light(0).setChanged();
            lampsOn.setVisible(false);
        }
        LightBounds.resetBounds(lightBoundsAccumulator, rawBodyBounds);
        LightBounds.includeLightBounds(lightBoundsAccumulator, LIGHTS_PART.model(), localPose, pos);
        if (!on)
            LightBounds.includeLightBounds(
                    lightBoundsAccumulator, LAMPS_OFF_PART.model(), localPose, pos);
        lastLightBounds =
                LightBounds.sections(lightSections, lightBoundsAccumulator, lastLightBounds);
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return rawBodyBounds.minmax(new AABB(pos).inflate(2));
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        consumer.accept(lights);
        consumer.accept(lampsOff);
    }

    @Override
    protected void _delete() {
        lights.delete();
        lampsOff.delete();
        lampsOn.delete();
    }
}
