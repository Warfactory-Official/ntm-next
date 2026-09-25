// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.machine.BlockEntitySteamEngine;
import com.hbm.util.Facing;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import java.util.function.Consumer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

public final class SteamEngineVisual extends HbmDynamicBlockEntityVisual<BlockEntitySteamEngine>
        implements ShaderLightVisual {
    private static final String[] NAMES = {"Flywheel", "Shaft", "Transmission", "Piston"};
    private static final HFRWavefrontObject MODEL = ResourceManager.steam_engine;
    private static final Material MATERIAL =
            SimpleMaterial.builderOf(MeshPart.litCutout(ResourceManager.steam_engine_tex))
                    .backfaceCulling(false)
                    .build();
    private static final MeshPart[] MOVING_PARTS = buildMovingParts();
    private final AABB bodyBounds;
    private final TransformedInstance[] instances = new TransformedInstance[NAMES.length];
    private final Matrix4f basePose = new Matrix4f();
    private final Matrix4f instancePose = new Matrix4f();
    private final Matrix4f[] partPoses = {
        new Matrix4f(), new Matrix4f(), new Matrix4f(), new Matrix4f()
    };
    private final double[] lightBounds = new double[6];
    private @Nullable AABB lastLightBounds;
    private double lastRotor = Double.NaN;

    public SteamEngineVisual(
            VisualizationContext context, BlockEntitySteamEngine blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        Matrix4f bodyLocal =
                new Matrix4f()
                        .translation(.5F, 0F, .5F)
                        .rotateY(
                                (Facing.yaw(
                                                BlockMultiblockCore.coreFacing(
                                                        blockEntity.getBlockState()),
                                                270))
                                        * Mth.DEG_TO_RAD)
                        .translate(2F, 0F, 0F);
        bodyBounds = LightBounds.of(MODEL, "Base", bodyLocal, pos);
        basePose.set(bodyLocal);
        for (int i = 0; i < NAMES.length; i++)
            instances[i] =
                    instancerProvider()
                            .instancer(InstanceTypes.TRANSFORMED, MOVING_PARTS[i].model())
                            .createInstance();
        updateMovingParts(partialTick);
    }

    private static MeshPart[] buildMovingParts() {
        var parts = new MeshPart[NAMES.length];
        for (int i = 0; i < parts.length; i++)
            parts[i] =
                    MeshPart.obj(MODEL.groups[MODEL.partId(NAMES[i])], MODEL.smoothing(), MATERIAL);
        return parts;
    }

    public static void initModels() {}

    @Override
    protected void frame(Context context) {
        updateMovingParts(context.partialTick());
    }

    public void updateMovingParts(float partialTick) {
        double rot =
                blockEntity.lastRotor + (blockEntity.rotor - blockEntity.lastRotor) * partialTick;
        if (Double.doubleToLongBits(rot) == Double.doubleToLongBits(lastRotor)) return;
        lastRotor = rot;

        partPoses[0]
                .set(basePose)
                .translate(2F, 1.375F, 0F)
                .rotateZ(-((float) rot) * Mth.DEG_TO_RAD)
                .translate(-2F, -1.375F, 0F);
        write(0, partPoses[0]);

        partPoses[1]
                .set(basePose)
                .translate(0F, 1.375F, -.5F)
                .rotateX(((float) (rot * 2D)) * Mth.DEG_TO_RAD)
                .translate(0F, -1.375F, .5F);
        write(1, partPoses[1]);

        double sin = Math.sin(rot * Math.PI / 180D) * .25D - .25D;
        double cos = Math.cos(rot * Math.PI / 180D) * .25D;
        double ang = Math.acos(cos / 1.875D);
        partPoses[2]
                .set(basePose)
                .translate((float) sin, (float) cos, 0F)
                .translate(2.25F, 1.375F, 0F)
                .rotateZ(-((float) (ang * 180D / Math.PI - 90D)) * Mth.DEG_TO_RAD)
                .translate(-2.25F, -1.375F, 0F);
        write(2, partPoses[2]);

        double cath = Math.sqrt(3.515625D - (cos * cos) / 2D);
        partPoses[3].set(basePose).translate((float) (1.875D - cath + sin), 0F, 0F);
        write(3, partPoses[3]);
        LightBounds.resetBounds(lightBounds, bodyBounds);
        for (int i = 0; i < partPoses.length; i++)
            LightBounds.includeLightBounds(lightBounds, MOVING_PARTS[i].model(), partPoses[i], pos);
        lastLightBounds = LightBounds.sections(lightSections, lightBounds, lastLightBounds);
    }

    private void write(int index, Matrix4f pose) {
        instancePose.translation(visualPos.getX(), visualPos.getY(), visualPos.getZ()).mul(pose);
        instances[index].setTransform(instancePose).light(0).setChanged();
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return bodyBounds.minmax(
                new AABB(
                                pos.getX() - 5,
                                pos.getY(),
                                pos.getZ() - 5,
                                pos.getX() + 6,
                                pos.getY() + 3,
                                pos.getZ() + 6)
                        .inflate(1));
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        for (var instance : instances) consumer.accept(instance);
    }

    @Override
    protected void _delete() {
        for (var instance : instances) instance.delete();
    }
}
