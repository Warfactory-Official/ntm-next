// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.main.ResourceManager;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKAutoloader;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import java.util.function.Consumer;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

public final class RBMKAutoloaderVisual
        extends HbmDynamicBlockEntityVisual<BlockEntityRBMKAutoloader>
        implements ShaderLightVisual {
    private static final int PISTON = ResourceManager.rbmk_autoloader.partId("Piston");
    private static final MeshPart PISTON_PART =
            MeshPart.obj(
                    ResourceManager.rbmk_autoloader.groups[PISTON],
                    ResourceManager.rbmk_autoloader.smoothing(),
                    MeshPart.litCutout(ResourceManager.rbmk_autoloader_tex));
    private final TransformedInstance piston;
    private final Matrix4f pose = new Matrix4f();
    private final Matrix4f world = new Matrix4f();
    private double lastPiston = Double.NaN;
    private final AABB rawBodyBounds;
    private final double[] lightBounds = new double[6];
    private @Nullable AABB lastLightBounds;

    public RBMKAutoloaderVisual(
            VisualizationContext context,
            BlockEntityRBMKAutoloader blockEntity,
            float partialTick) {
        super(context, blockEntity, partialTick);
        rawBodyBounds =
                LightBounds.of(
                        ResourceManager.rbmk_autoloader,
                        "Base",
                        new Matrix4f().translation(.5F, 0F, .5F),
                        pos);
        piston =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, PISTON_PART.model())
                        .createInstance();
        updateMovingParts(partialTick);
    }

    public static void initModels() {}

    @Override
    protected void frame(Context context) {
        updateMovingParts(context.partialTick());
    }

    public void updateMovingParts(float partialTick) {
        double pistonValue =
                blockEntity.lastPiston
                        + (blockEntity.renderPiston - blockEntity.lastPiston) * partialTick;
        if (Double.doubleToLongBits(pistonValue) == Double.doubleToLongBits(lastPiston)) return;
        lastPiston = pistonValue;
        pose.identity().translate(.5F, 4F - (float) pistonValue * 4F, .5F);
        world.translation(visualPos.getX(), visualPos.getY(), visualPos.getZ()).mul(pose);
        piston.setTransform(world).light(0).setChanged();
        LightBounds.resetBounds(lightBounds, rawBodyBounds);
        LightBounds.includeLightBounds(lightBounds, PISTON_PART.model(), pose, pos);
        lastLightBounds = LightBounds.sections(lightSections, lightBounds, lastLightBounds);
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return rawBodyBounds.minmax(
                new AABB(
                                pos.getX() - 1,
                                pos.getY(),
                                pos.getZ() - 1,
                                pos.getX() + 2,
                                pos.getY() + 9,
                                pos.getZ() + 2)
                        .inflate(1));
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        consumer.accept(piston);
    }

    @Override
    protected void _delete() {
        piston.delete();
    }
}
