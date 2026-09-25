// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.machine.BlockEntityCharger;
import com.hbm.util.Facing;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.material.CardinalLightingMode;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.material.CutoutShaders;
import dev.engine_room.flywheel.lib.material.LightShaders;
import dev.engine_room.flywheel.lib.material.Materials;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.model.SingleMeshModel;
import java.util.function.Consumer;
import net.minecraft.core.Direction;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

public final class ChargerVisual extends HbmDynamicBlockEntityVisual<BlockEntityCharger>
        implements ShaderLightVisual {
    private static final int LIGHT = ResourceManager.charger.partId("Light");
    private static final int SLIDE = ResourceManager.charger.partId("Slide");
    private static final int LEFT = ResourceManager.charger.partId("Left");
    private static final int RIGHT = ResourceManager.charger.partId("Right");
    private static final float TILT = 10F;
    private static final float PIVOT_X = -.34375F;
    private static final float PIVOT_Y = .25F;
    private static final float DROP = .25F;
    private static final float HINGE_Y = .28F;
    private static final float SWIVEL = 30F;
    private static final int LAMP = 0xFFFFBF00;
    private static final Material SLIDE_MATERIAL =
            SimpleMaterial.builderOf(Materials.CUTOUT_NO_CULL)
                    .texture(ResourceManager.charger_tex)
                    .mipmap(false)
                    .cutout(CutoutShaders.ONE_TENTH)
                    .light(LightShaders.SMOOTH)
                    .ambientOcclusion(false)
                    .cardinalLightingMode(CardinalLightingMode.OFF)
                    .backfaceCulling(false)
                    .build();
    private static final Material LAMP_MATERIAL =
            SimpleMaterial.builderOf(Materials.CUTOUT_NO_CULL)
                    .texture(ResourceManager.white_tex)
                    .mipmap(false)
                    .cutout(CutoutShaders.ONE_TENTH)
                    .useLight(false)
                    .useOverlay(false)
                    .cardinalLightingMode(CardinalLightingMode.OFF)
                    .ambientOcclusion(false)
                    .backfaceCulling(false)
                    .build();
    private static final HFRWavefrontObject MODEL = ResourceManager.charger;
    private static final Material BODY_MATERIAL = MeshPart.litCutout(ResourceManager.charger_tex);
    private static final Model LAMP_PART =
            new SingleMeshModel(
                    PackedQuadMesh.of(MODEL.groups[LIGHT], MODEL.smoothing()), LAMP_MATERIAL);
    private static final MeshPart[] PARTS = {
        MeshPart.obj(MODEL.groups[SLIDE], MODEL.smoothing(), SLIDE_MATERIAL),
        MeshPart.obj(MODEL.groups[LEFT], MODEL.smoothing(), BODY_MATERIAL),
        MeshPart.obj(MODEL.groups[RIGHT], MODEL.smoothing(), BODY_MATERIAL)
    };
    private final AABB bodyBounds;
    private final TransformedInstance[] instances;
    private final Matrix4f[] local = {
        new Matrix4f(), new Matrix4f(), new Matrix4f(), new Matrix4f()
    };
    private final Matrix4f world = new Matrix4f();
    private float lastExtend = Float.NaN;
    private float lastSwivel = Float.NaN;
    private boolean initialized;

    public ChargerVisual(
            VisualizationContext context, BlockEntityCharger blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        Direction bodyFacing = blockState.getValue(HorizontalDirectionalBlock.FACING);
        Matrix4f bodyLocal =
                new Matrix4f()
                        .translation(.5F, 0F, .5F)
                        .rotateY(Facing.yaw(bodyFacing, 90) * Mth.DEG_TO_RAD);
        AABB bodyExtent = new AABB(pos);
        for (int i = 0; i < MODEL.groups.length; i++)
            if (i != LIGHT && i != SLIDE && i != LEFT && i != RIGHT)
                bodyExtent = bodyExtent.minmax(LightBounds.of(MODEL.groups[i], bodyLocal, pos));
        bodyBounds = bodyExtent;
        instances =
                new TransformedInstance[] {
                    instancerProvider()
                            .instancer(InstanceTypes.TRANSFORMED, PARTS[0].model())
                            .createInstance(),
                    instancerProvider()
                            .instancer(InstanceTypes.TRANSFORMED, PARTS[1].model())
                            .createInstance(),
                    instancerProvider()
                            .instancer(InstanceTypes.TRANSFORMED, PARTS[2].model())
                            .createInstance(),
                    instancerProvider()
                            .instancer(InstanceTypes.TRANSFORMED, LAMP_PART)
                            .createInstance()
                };
        local[3].set(bodyLocal);
        writeLamp(local[3]);
        updateMovingParts(partialTick);
    }

    public static void initModels() {}

    @Override
    protected void frame(Context context) {
        updateMovingParts(context.partialTick());
    }

    public void updateMovingParts(float partialTick) {
        double time =
                Mth.lerp(partialTick, blockEntity.lastUsingTicks, blockEntity.usingTicks)
                        / (double) BlockEntityCharger.DELAY;
        float extend = (float) Math.min(1D, time * 2D);
        float swivel = (float) Math.max(0D, (time - .5D) * 2D);
        boolean extendChanged = !initialized || extend != lastExtend;
        boolean swivelChanged = !initialized || swivel != lastSwivel;
        if (extendChanged) {
            local[0].set(local[3])
                    .translate(PIVOT_X, PIVOT_Y, 0F)
                    .rotateZ(TILT * Mth.DEG_TO_RAD)
                    .translate(-PIVOT_X, -PIVOT_Y, 0F)
                    .translate(0F, -DROP * extend, 0F);
            write(0, local[0]);
            lastExtend = extend;
        }
        if (extendChanged || swivelChanged) {
            local[1].set(local[0])
                    .translate(0F, HINGE_Y, 0F)
                    .rotateX(swivel * SWIVEL * Mth.DEG_TO_RAD)
                    .translate(0F, -HINGE_Y, 0F);
            local[2].set(local[0])
                    .translate(0F, HINGE_Y, 0F)
                    .rotateX(-swivel * SWIVEL * Mth.DEG_TO_RAD)
                    .translate(0F, -HINGE_Y, 0F);
            write(1, local[1]);
            write(2, local[2]);
            lastSwivel = swivel;
        }
        initialized = true;
    }

    private void write(int index, Matrix4f pose) {
        TransformedInstance instance = instances[index];
        world.translation(visualPos.getX(), visualPos.getY(), visualPos.getZ()).mul(pose);
        instance.setTransform(world)
                .light(index == 0 ? LightCoordsUtil.FULL_BRIGHT : 0)
                .setChanged();
    }

    private void writeLamp(Matrix4f pose) {
        TransformedInstance lamp = instances[3];
        world.translation(visualPos.getX(), visualPos.getY(), visualPos.getZ()).mul(pose);
        lamp.setTransform(world).light(LightCoordsUtil.FULL_BRIGHT).colorArgb(LAMP).setChanged();
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return bodyBounds.minmax(
                new AABB(
                                pos.getX() - 1,
                                pos.getY(),
                                pos.getZ() - 1,
                                pos.getX() + 2,
                                pos.getY() + 2,
                                pos.getZ() + 2)
                        .inflate(1));
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        consumer.accept(instances[1]);
        consumer.accept(instances[2]);
    }

    @Override
    protected void _delete() {
        for (TransformedInstance instance : instances) instance.delete();
    }
}
