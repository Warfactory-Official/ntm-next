// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.client.model.Meshes;
import com.hbm.lib.Library;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.machine.BlockEntityCargoElevator;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import java.util.Arrays;
import java.util.function.Consumer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

public final class CargoElevatorVisual extends HbmDynamicBlockEntityVisual<BlockEntityCargoElevator>
        implements ShaderLightVisual {
    private static final HFRWavefrontObject MODEL =
            Meshes.load(Library.id("models/machines/elevator.obj"));
    private static final Material MATERIAL =
            MeshPart.litCutout(Library.id("textures/block/models/machines/elevator.png"));
    private static final MeshPart PLATFORM_PART =
            MeshPart.obj(MODEL.groups[MODEL.partId("Platform")], MODEL.smoothing(), MATERIAL);
    private static final MeshPart PISTON_PART =
            MeshPart.obj(MODEL.groups[MODEL.partId("Piston")], MODEL.smoothing(), MATERIAL);

    private final AABB bodyBounds;
    private final TransformedInstance platform;
    private TransformedInstance[] pistons = new TransformedInstance[0];
    private final Matrix4f platformPose = new Matrix4f();
    private final Matrix4f pistonPose = new Matrix4f();
    private final Matrix4f world = new Matrix4f();
    private @Nullable AABB lastLightBounds;
    private double lastExtension = Double.NaN;
    private int lastHeight;
    private boolean lastRenderPlatform;
    private boolean initialized;

    public CargoElevatorVisual(
            VisualizationContext context, BlockEntityCargoElevator blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        Matrix4f bodyLocal = new Matrix4f().translation(.5F, 0F, .5F);
        bodyBounds =
                LightBounds.of(MODEL, "Base", bodyLocal, pos)
                        .minmax(LightBounds.of(MODEL, "Guides", bodyLocal, pos));
        platform =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, PLATFORM_PART.model())
                        .createInstance();
        lastHeight = blockEntity.height;
        updateMovingParts(partialTick);
    }

    public static void initModels() {}

    @Override
    protected void trackExtent() {
        if (blockEntity.height == lastHeight) return;
        lastHeight = blockEntity.height;
        lastLightBounds =
                LightBounds.sections(lightSections, getRenderBoundingBox(), lastLightBounds);
        refreshVisibleBounds();
    }

    @Override
    protected void frame(Context context) {
        updateMovingParts(context.partialTick());
    }

    public void updateMovingParts(float partialTick) {
        double extension = Mth.lerp(partialTick, blockEntity.prevExtension, blockEntity.extension);
        boolean renderPlatform = blockEntity.renderPlatform;
        if (initialized && extension == lastExtension && renderPlatform == lastRenderPlatform)
            return;
        lastExtension = extension;
        lastRenderPlatform = renderPlatform;
        int count = (int) Math.ceil(extension + 1D);
        if (count > pistons.length) {
            int from = pistons.length;
            pistons = Arrays.copyOf(pistons, count);
            for (int i = from; i < count; i++)
                pistons[i] =
                        instancerProvider()
                                .instancer(InstanceTypes.TRANSFORMED, PISTON_PART.model())
                                .createInstance();
        }
        platform.setVisible(renderPlatform);
        if (renderPlatform) {
            platformPose.identity().translate(.5F, (float) extension, .5F);
            world.translation(visualPos.getX(), visualPos.getY(), visualPos.getZ())
                    .mul(platformPose);
            platform.setTransform(world).light(0).setChanged();
        }
        for (int i = 0; i < pistons.length; i++) {
            TransformedInstance piston = pistons[i];
            boolean visible = renderPlatform && i < count;
            piston.setVisible(visible);
            if (!visible) continue;
            pistonPose.identity().translate(.5F, (float) extension - i, .5F);
            world.translation(visualPos.getX(), visualPos.getY(), visualPos.getZ()).mul(pistonPose);
            piston.setTransform(world).light(0).setChanged();
        }
        initialized = true;
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return bodyBounds.minmax(
                new AABB(
                                pos.getX() - 1,
                                pos.getY(),
                                pos.getZ() - 1,
                                pos.getX() + 2,
                                pos.getY() + blockEntity.height + 1,
                                pos.getZ() + 2)
                        .inflate(1));
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        consumer.accept(platform);
        for (TransformedInstance piston : pistons) consumer.accept(piston);
    }

    @Override
    protected void _delete() {
        platform.delete();
        for (TransformedInstance piston : pistons) piston.delete();
    }
}
