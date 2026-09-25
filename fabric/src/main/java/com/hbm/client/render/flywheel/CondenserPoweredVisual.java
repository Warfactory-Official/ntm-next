// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.machine.BlockEntityCondenserPowered;
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

public final class CondenserPoweredVisual
        extends HbmDynamicBlockEntityVisual<BlockEntityCondenserPowered>
        implements ShaderLightVisual {
    private static final Material MATERIAL =
            SimpleMaterial.builderOf(Materials.CUTOUT_NO_CULL)
                    .texture(ResourceManager.condenser_tex)
                    .mipmap(false)
                    .cutout(CutoutShaders.ONE_TENTH)
                    .light(LightShaders.SMOOTH)
                    .ambientOcclusion(false)
                    .cardinalLightingMode(CardinalLightingMode.CHUNK)
                    .backfaceCulling(false)
                    .build();
    private static final HFRWavefrontObject MODEL = ResourceManager.condenser;
    private static final MeshPart[] PARTS = {
        MeshPart.obj(MODEL.groups[MODEL.partId("Fan1")], MODEL.smoothing(), MATERIAL),
        MeshPart.obj(MODEL.groups[MODEL.partId("Fan2")], MODEL.smoothing(), MATERIAL)
    };
    private final TransformedInstance[] instances = new TransformedInstance[PARTS.length];
    private final AABB bodyBounds;
    private final Matrix4f basePose = new Matrix4f();
    private final Matrix4f[] local = {new Matrix4f(), new Matrix4f()};
    private final Matrix4f world = new Matrix4f();
    private float lastFan = Float.NaN;

    public CondenserPoweredVisual(
            VisualizationContext context,
            BlockEntityCondenserPowered blockEntity,
            float partialTick) {
        super(context, blockEntity, partialTick);
        basePose.translation(.5F, 0F, .5F)
                .rotateY(
                        Facing.yaw(BlockMultiblockCore.coreFacing(blockState), 90)
                                * Mth.DEG_TO_RAD);
        bodyBounds = LightBounds.of(MODEL, "Condenser", basePose, pos);
        for (int i = 0; i < PARTS.length; i++)
            instances[i] =
                    instancerProvider()
                            .instancer(InstanceTypes.TRANSFORMED, PARTS[i].model())
                            .createInstance();
        updateMovingParts(partialTick);
    }

    public static void initModels() {}

    @Override
    protected void frame(Context context) {
        updateMovingParts(context.partialTick());
    }

    public void updateMovingParts(float partialTick) {
        float fan = Mth.lerp(partialTick, blockEntity.prevSpin, blockEntity.spin);
        if (fan == lastFan) return;
        local[0].set(basePose)
                .translate(0F, 1.5F, 0F)
                .rotateX(fan * Mth.DEG_TO_RAD)
                .translate(0F, -1.5F, 0F);
        local[1].set(basePose)
                .translate(0F, 1.5F, 0F)
                .rotateX(-fan * Mth.DEG_TO_RAD)
                .translate(0F, -1.5F, 0F);
        write(0, local[0]);
        write(1, local[1]);
        lastFan = fan;
    }

    private void write(int index, Matrix4f pose) {
        world.translation(visualPos.getX(), visualPos.getY(), visualPos.getZ()).mul(pose);
        instances[index].setTransform(world).light(0).setChanged();
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return bodyBounds.minmax(
                new AABB(
                                pos.getX() - 3,
                                pos.getY(),
                                pos.getZ() - 3,
                                pos.getX() + 4,
                                pos.getY() + 3,
                                pos.getZ() + 4)
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
