// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.blocks.machine.pile.BlockPileDevice;
import com.hbm.main.ResourceManager;
import com.hbm.tileentity.machine.pile.BlockEntityPileLoader;
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

public final class PileLoaderVisual extends HbmDynamicBlockEntityVisual<BlockEntityPileLoader>
        implements ShaderLightVisual {
    private static final int LEVER = ResourceManager.pile_loader.partId("Lever");
    private static final int SLIDER = ResourceManager.pile_loader.partId("Slider");
    private static final int ROD = ResourceManager.pile_loader.partId("Rod");
    private static final MeshPart LEVER_PART = part(LEVER);
    private static final MeshPart SLIDER_PART = part(SLIDER);
    private static final MeshPart ROD_PART = part(ROD);

    private final TransformedInstance lever;
    private final TransformedInstance slider;
    private final TransformedInstance rod;
    private final Matrix4f basePose = new Matrix4f();
    private final Matrix4f pose = new Matrix4f();
    private final Matrix4f world = new Matrix4f();
    private float lastExtension = Float.NaN;
    private boolean lastRod;

    private static MeshPart part(int id) {
        return MeshPart.obj(
                ResourceManager.pile_loader.groups[id],
                ResourceManager.pile_loader.smoothing(),
                MeshPart.litCutout(ResourceManager.pile_loader_tex));
    }

    public PileLoaderVisual(
            VisualizationContext context, BlockEntityPileLoader blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        basePose.translation(.5F, 0F, .5F)
                .rotateY(
                        Facing.yaw(blockState.getValue(BlockPileDevice.FACING), 90)
                                * Mth.DEG_TO_RAD);
        lever =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, LEVER_PART.model())
                        .createInstance();
        slider =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, SLIDER_PART.model())
                        .createInstance();
        rod =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, ROD_PART.model())
                        .createInstance();
        updateMovingParts(partialTick);
    }

    @Override
    protected void frame(Context context) {
        updateMovingParts(context.partialTick());
    }

    private void updateMovingParts(float partialTick) {
        float extension =
                (float)
                        (blockEntity.lastExtension
                                + (blockEntity.extension - blockEntity.lastExtension)
                                        * partialTick);
        boolean hasRod = !blockEntity.getItem(0).isEmpty();
        if (extension == lastExtension && hasRod == lastRod) return;
        lastExtension = extension;
        lastRod = hasRod;

        pose.set(basePose)
                .translate(-.1875F, .5F, 0F)
                .rotateZ(extension * Mth.HALF_PI)
                .translate(.1875F, -.5F, 0F);
        world.translation(visualPos.getX(), visualPos.getY(), visualPos.getZ()).mul(pose);
        lever.setTransform(world).light(0).setChanged();

        pose.set(basePose).translate(extension * -.5F, 0F, 0F);
        world.translation(visualPos.getX(), visualPos.getY(), visualPos.getZ()).mul(pose);
        slider.setTransform(world).light(0).setChanged();
        rod.setTransform(world).light(0);
        rod.setVisible(hasRod);
        rod.setChanged();
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return new AABB(pos).inflate(2);
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        consumer.accept(lever);
        consumer.accept(slider);
        if (lastRod) consumer.accept(rod);
    }

    @Override
    protected void _delete() {
        lever.delete();
        slider.delete();
        rod.delete();
    }
}
