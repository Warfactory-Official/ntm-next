// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.main.ResourceManager;
import com.hbm.tileentity.machine.BlockEntityMachineLargeTurbine;
import com.hbm.util.Facing;
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
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

public final class LargeTurbineVisual
        extends HbmDynamicBlockEntityVisual<BlockEntityMachineLargeTurbine>
        implements ShaderLightVisual {
    private static final Material BLADES_MATERIAL =
            SimpleMaterial.builderOf(Materials.CUTOUT_NO_CULL)
                    .texture(ResourceManager.turbofan_blades_tex)
                    .mipmap(false)
                    .cutout(CutoutShaders.ONE_TENTH)
                    .light(LightShaders.SMOOTH)
                    .ambientOcclusion(false)
                    .cardinalLightingMode(CardinalLightingMode.CHUNK)
                    .build();
    private static final MeshPart BLADES_PART =
            MeshPart.obj(
                    ResourceManager.turbine.groups[ResourceManager.turbine.partId("Blades")],
                    false,
                    BLADES_MATERIAL);

    private final TransformedInstance blades;
    private final AABB bodyBounds;
    private final Matrix4f basePose = new Matrix4f();
    private final Matrix4f bladePose = new Matrix4f();
    private final Matrix4f instancePose = new Matrix4f();
    private float lastRotor = Float.NaN;

    public LargeTurbineVisual(
            VisualizationContext context,
            BlockEntityMachineLargeTurbine blockEntity,
            float partialTick) {
        super(context, blockEntity, partialTick);
        blades =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, BLADES_PART.model())
                        .createInstance();
        basePose.identity()
                .translate(.5F, 0F, .5F)
                .rotateY(
                        Facing.yaw(BlockMultiblockCore.coreFacing(blockState), 180)
                                * Mth.DEG_TO_RAD)
                .translate(0F, 0F, -1F);
        bodyBounds = LightBounds.of(ResourceManager.turbine, "Body", basePose, pos);
        writeFrame(partialTick);
    }

    public static void initModels() {}

    @Override
    protected void frame(Context context) {
        writeFrame(context.partialTick());
    }

    private void writeFrame(float partialTick) {
        float rotor = Mth.lerp(partialTick, blockEntity.lastRotor, blockEntity.rotor);
        if (rotor == lastRotor) return;
        bladePose
                .set(basePose)
                .translate(0F, 1F, 0F)
                .rotateZ(rotor * Mth.DEG_TO_RAD)
                .translate(0F, -1F, 0F);
        instancePose
                .translation(visualPos.getX(), visualPos.getY(), visualPos.getZ())
                .mul(bladePose);
        blades.setTransform(instancePose).light(0).setChanged();
        lastRotor = rotor;
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return bodyBounds.minmax(
                new AABB(
                                pos.getX() - 8,
                                pos.getY() - 1,
                                pos.getZ() - 8,
                                pos.getX() + 9,
                                pos.getY() + 6,
                                pos.getZ() + 9)
                        .inflate(1));
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        consumer.accept(blades);
    }

    @Override
    protected void _delete() {
        blades.delete();
    }
}
