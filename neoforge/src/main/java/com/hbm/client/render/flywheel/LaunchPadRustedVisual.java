// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.GroupObject;
import com.hbm.tileentity.bomb.BlockEntityLaunchPadRusted;
import com.hbm.util.Facing;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import java.util.function.Consumer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

public final class LaunchPadRustedVisual
        extends HbmDynamicBlockEntityVisual<BlockEntityLaunchPadRusted>
        implements ShaderLightVisual {
    private static final MeshPart[] MISSILE_PARTS =
            MeshPart.objParts(
                    ResourceManager.missileNuclear,
                    MeshPart.litCutout(ResourceManager.missileDoomsdayRusted_tex));
    private final TransformedInstance[] instances;
    private final AABB bodyBounds;
    private final Matrix4f missileBasePose = new Matrix4f();
    private final Matrix4f instancePose = new Matrix4f();
    private final double[] lightBoundsAccumulator = new double[6];
    private @Nullable AABB lastLightBounds;
    private boolean lastShown;
    private boolean initialized;

    public LaunchPadRustedVisual(
            VisualizationContext context,
            BlockEntityLaunchPadRusted blockEntity,
            float partialTick) {
        super(context, blockEntity, partialTick);
        float yaw = Facing.yaw(BlockMultiblockCore.coreFacing(blockState), 90) * Mth.DEG_TO_RAD;
        Matrix4f bodyLocal = new Matrix4f().translation(.5F, 0F, .5F).rotateY(yaw);
        AABB extent = new AABB(pos);
        for (GroupObject group : ResourceManager.launch_pad_silo.groups)
            extent = extent.minmax(LightBounds.of(group, bodyLocal, pos));
        bodyBounds = extent;
        missileBasePose.translation(.5F, 1F, .5F).rotateY(yaw);
        instances = new TransformedInstance[MISSILE_PARTS.length];
        for (int i = 0; i < MISSILE_PARTS.length; i++)
            instances[i] =
                    instancerProvider()
                            .instancer(InstanceTypes.TRANSFORMED, MISSILE_PARTS[i].model())
                            .createInstance();
        writeFrame();
    }

    public static void initModels() {}

    @Override
    protected void frame(Context context) {
        writeFrame();
    }

    private void writeFrame() {
        boolean shown = blockEntity.missileLoaded;
        if (initialized && shown == lastShown) return;
        lastShown = shown;
        instancePose
                .translation(visualPos.getX(), visualPos.getY(), visualPos.getZ())
                .mul(missileBasePose);
        for (var instance : instances) {
            instance.setVisible(shown);
            if (shown) instance.setTransform(instancePose).light(0).setChanged();
        }
        LightBounds.resetBounds(lightBoundsAccumulator, bodyBounds);
        if (shown)
            for (MeshPart part : MISSILE_PARTS)
                LightBounds.includeLightBounds(
                        lightBoundsAccumulator, part.model(), missileBasePose, pos);
        lastLightBounds =
                LightBounds.sections(lightSections, lightBoundsAccumulator, lastLightBounds);
        initialized = true;
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return bodyBounds.minmax(
                new AABB(
                                pos.getX() - 2,
                                pos.getY(),
                                pos.getZ() - 2,
                                pos.getX() + 3,
                                pos.getY() + 15,
                                pos.getZ() + 3)
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
