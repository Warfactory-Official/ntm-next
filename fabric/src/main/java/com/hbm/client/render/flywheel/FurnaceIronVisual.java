// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.machine.BlockEntityFurnaceIron;
import com.hbm.util.Facing;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.material.CardinalLightingMode;
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

public final class FurnaceIronVisual extends HbmDynamicBlockEntityVisual<BlockEntityFurnaceIron>
        implements ShaderLightVisual {
    private static final HFRWavefrontObject MODEL = ResourceManager.furnace_iron;
    private static final MeshPart ON_PART =
            MeshPart.obj(
                    MODEL.groups[MODEL.partId("On")],
                    MODEL.smoothing(),
                    SimpleMaterial.builderOf(Materials.CUTOUT)
                            .texture(ResourceManager.furnace_iron_tex)
                            .mipmap(false)
                            .cutout(CutoutShaders.ONE_TENTH)
                            .light(LightShaders.NONE)
                            .cardinalLightingMode(CardinalLightingMode.OFF)
                            .ambientOcclusion(false)
                            .backfaceCulling(false)
                            .build());
    private static final MeshPart OFF_PART =
            MeshPart.obj(
                    MODEL.groups[MODEL.partId("Off")],
                    MODEL.smoothing(),
                    MeshPart.litCutout(ResourceManager.furnace_iron_tex));

    private final AABB bodyBounds;
    private final TransformedInstance on;
    private final TransformedInstance off;
    private final Matrix4f basePose = new Matrix4f();
    private final Matrix4f instancePose = new Matrix4f();
    private final double[] lightBounds = new double[6];
    private @Nullable AABB lastLightBounds;
    private boolean lastOn;
    private boolean initialized;

    public FurnaceIronVisual(
            VisualizationContext context, BlockEntityFurnaceIron blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        basePose.translate(.5F, 0F, .5F)
                .rotateY(
                        Facing.yaw(BlockMultiblockCore.coreFacing(blockState), 180)
                                * Mth.DEG_TO_RAD)
                .translate(-.5F, 0F, -.5F);
        bodyBounds = LightBounds.of(MODEL, "Main", basePose, pos);
        on =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, ON_PART.model())
                        .createInstance();
        off =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, OFF_PART.model())
                        .createInstance();
        updateMovingParts(partialTick);
    }

    public static void initModels() {}

    @Override
    protected void frame(Context context) {
        updateMovingParts(context.partialTick());
    }

    public void updateMovingParts(float partialTick) {
        boolean onState = blockEntity.wasOn;
        if (initialized && onState == lastOn) return;
        instancePose
                .translation(visualPos.getX(), visualPos.getY(), visualPos.getZ())
                .mul(basePose);
        if (onState) {
            on.setVisible(true);
            on.setTransform(instancePose).light(LightCoordsUtil.FULL_BRIGHT).setChanged();
            off.setVisible(false);
        } else {
            on.setVisible(false);
            off.setVisible(true);
            off.setTransform(instancePose).light(0).setChanged();
        }
        LightBounds.resetBounds(lightBounds, bodyBounds);
        if (!onState) LightBounds.includeLightBounds(lightBounds, OFF_PART.model(), basePose, pos);
        lastLightBounds = LightBounds.sections(lightSections, lightBounds, lastLightBounds);
        lastOn = onState;
        initialized = true;
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return bodyBounds.minmax(new AABB(pos).expandTowards(1, 3, 1).inflate(1));
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        consumer.accept(off);
    }

    @Override
    protected void _delete() {
        on.delete();
        off.delete();
    }
}
