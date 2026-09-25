// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.blocks.machine.BlastDoor;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.machine.BlockEntityBlastDoor;
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
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

public final class BlastDoorVisual extends HbmDynamicBlockEntityVisual<BlockEntityBlastDoor>
        implements ShaderLightVisual {
    private static final int SLIDERS = 4;
    private static final float EXTEND = 5F;
    private static final Material MATERIAL =
            SimpleMaterial.builderOf(Materials.CUTOUT_NO_CULL)
                    .texture(ResourceManager.blast_door_tooth_tex)
                    .mipmap(false)
                    .cutout(CutoutShaders.ONE_TENTH)
                    .light(LightShaders.SMOOTH)
                    .ambientOcclusion(false)
                    .cardinalLightingMode(CardinalLightingMode.CHUNK)
                    .backfaceCulling(false)
                    .build();
    private static final Material SLIDER_MATERIAL =
            SimpleMaterial.builderOf(Materials.CUTOUT_NO_CULL)
                    .texture(ResourceManager.blast_door_slider_tex)
                    .mipmap(false)
                    .cutout(CutoutShaders.ONE_TENTH)
                    .light(LightShaders.SMOOTH)
                    .ambientOcclusion(false)
                    .cardinalLightingMode(CardinalLightingMode.CHUNK)
                    .backfaceCulling(false)
                    .build();
    private static final HFRWavefrontObject tooth = ResourceManager.blast_door_tooth;
    private static final HFRWavefrontObject slider = ResourceManager.blast_door_slider;
    private static final HFRWavefrontObject base = ResourceManager.blast_door_base;
    private static final HFRWavefrontObject block = ResourceManager.blast_door_block;
    private static final MeshPart TOOTH_PART =
            MeshPart.obj(tooth.groups[0], tooth.smoothing(), MATERIAL);
    private static final MeshPart SLIDER_PART =
            MeshPart.obj(slider.groups[0], slider.smoothing(), SLIDER_MATERIAL);
    private static final MeshPart[] MOVING_PARTS = {
        TOOTH_PART, SLIDER_PART, SLIDER_PART, SLIDER_PART, SLIDER_PART
    };
    private final TransformedInstance[] instances;
    private final AABB bodyBounds;
    private final Matrix4f basePose = new Matrix4f();
    private final Matrix4f[] local = {
        new Matrix4f(), new Matrix4f(), new Matrix4f(), new Matrix4f(), new Matrix4f()
    };
    private final Matrix4f world = new Matrix4f();
    private float lastRamp = Float.NaN;

    public BlastDoorVisual(
            VisualizationContext context, BlockEntityBlastDoor blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        Matrix4f bodyBase = new Matrix4f().translation(.5F, 0F, .5F).rotateY(180F * Mth.DEG_TO_RAD);
        if (blockEntity.getBlockState().getValue(BlockMultiblockCore.FACING).getAxis()
                == Direction.Axis.Z) bodyBase.rotateY(90F * Mth.DEG_TO_RAD);
        basePose.set(bodyBase);
        Matrix4f blockLocal = new Matrix4f(bodyBase).translate(0F, 3F, 0F);
        bodyBounds =
                LightBounds.of(base.groups[0], bodyBase, pos)
                        .minmax(LightBounds.of(block.groups[0], blockLocal, pos));
        instances = new TransformedInstance[MOVING_PARTS.length];
        for (int i = 0; i < MOVING_PARTS.length; i++)
            instances[i] =
                    instancerProvider()
                            .instancer(InstanceTypes.TRANSFORMED, MOVING_PARTS[i].model())
                            .createInstance();
        updateMovingParts(partialTick);
    }

    public static void initModels() {}

    @Override
    protected void frame(Context context) {
        updateMovingParts(context.partialTick());
    }

    public void updateMovingParts(float partialTick) {
        float ramp = ramp(partialTick);
        if (ramp == lastRamp) return;
        local[0].set(basePose).translate(0F, 5F - ramp, 0F);
        for (int i = 0; i < SLIDERS; i++) local[i + 1].set(local[0]).translate(0F, i, 0F);
        write(0, local[0], true);
        for (int i = 0; i < SLIDERS; i++) write(i + 1, local[i + 1], ramp > i + 1);
        lastRamp = ramp;
    }

    private float ramp(float partialTick) {
        if (blockEntity.state == BlockEntityBlastDoor.STATE_OPEN) return 0F;
        if (blockEntity.state != BlockEntityBlastDoor.STATE_MOVING) return EXTEND;
        float travelled =
                Mth.clamp(
                        (blockEntity.timer + partialTick) / BlockEntityBlastDoor.TRAVEL_TICKS,
                        0F,
                        1F);
        return EXTEND * (blockEntity.isOpening ? 1F - travelled : travelled);
    }

    private void write(int index, Matrix4f pose, boolean visible) {
        TransformedInstance instance = instances[index];
        instance.setVisible(visible);
        if (!visible) return;
        world.translation(visualPos.getX(), visualPos.getY(), visualPos.getZ()).mul(pose);
        instance.setTransform(world).light(0).setChanged();
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return bodyBounds.minmax(
                new AABB(
                                pos.getX(),
                                pos.getY(),
                                pos.getZ(),
                                pos.getX() + 1,
                                pos.getY() + BlastDoor.LEAF_TOP + 2,
                                pos.getZ() + 1)
                        .inflate(1));
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        for (TransformedInstance instance : instances) consumer.accept(instance);
    }

    @Override
    protected void _delete() {
        for (TransformedInstance instance : instances) instance.delete();
    }
}
