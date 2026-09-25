// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.client.render.FramedItem;
import com.hbm.client.render.RenderPistonInserter;
import com.hbm.main.ResourceManager;
import com.hbm.tileentity.machine.BlockEntityPistonInserter;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import java.util.function.Consumer;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

public final class PistonInserterVisual
        extends HbmDynamicBlockEntityVisual<BlockEntityPistonInserter>
        implements ShaderLightVisual {
    private static final double STROKE = .9375D;
    private static final MeshPart PISTON_PART =
            MeshPart.obj(
                    ResourceManager.piston_inserter
                            .groups[ResourceManager.piston_inserter.partId("Piston")],
                    ResourceManager.piston_inserter.smoothing(),
                    MeshPart.litCutout(ResourceManager.piston_inserter_tex));
    private final AABB frameBounds;
    private final Direction facing;
    private final TransformedInstance piston;
    private final Matrix4f pose = new Matrix4f();
    private final Matrix4f basePose = new Matrix4f();
    private final Matrix4f world = new Matrix4f();
    private float lastExtend = Float.NaN;
    private final WorldItem item = new WorldItem(visualizationContext, level, pos);
    private final PoseStack itemPoses = new PoseStack();

    public PistonInserterVisual(
            VisualizationContext context,
            BlockEntityPistonInserter blockEntity,
            float partialTick) {
        super(context, blockEntity, partialTick);
        facing = blockEntity.facing();
        Matrix4f framePose = new Matrix4f().translation(.5F, .5F, .5F);
        rotate(framePose, facing);
        framePose.translate(0F, -.5F, 0F);
        frameBounds = LightBounds.of(ResourceManager.piston_inserter, "Frame", framePose, pos);
        basePose.set(framePose);
        piston =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, PISTON_PART.model())
                        .createInstance();
        updateMovingParts(partialTick);
        updateItem(partialTick);
    }

    public static void initModels() {}

    public static boolean vanillaNeeded(BlockEntityPistonInserter be) {
        return !WorldItem.drawsFramed(be.getItem(0));
    }

    private static void rotate(Matrix4f pose, Direction facing) {
        switch (facing) {
            case DOWN -> pose.rotateX(180F * Mth.DEG_TO_RAD);
            case UP -> {}
            case NORTH -> pose.rotateX(-90F * Mth.DEG_TO_RAD).rotateY(180F * Mth.DEG_TO_RAD);
            case SOUTH -> pose.rotateX(90F * Mth.DEG_TO_RAD);
            case WEST -> pose.rotateZ(90F * Mth.DEG_TO_RAD).rotateY(-90F * Mth.DEG_TO_RAD);
            case EAST -> pose.rotateZ(-90F * Mth.DEG_TO_RAD).rotateY(90F * Mth.DEG_TO_RAD);
        }
    }

    @Override
    protected void frame(Context context) {
        updateMovingParts(context.partialTick());
        updateItem(context.partialTick());
    }

    private void updateItem(float partialTick) {
        ItemStack stack = blockEntity.getItem(0);
        FramedItem.Arm arm = item.setFramed(stack, level);
        if (arm == null) return;
        itemPoses.setIdentity();
        itemPoses.translate(visualPos.getX(), visualPos.getY(), visualPos.getZ());
        RenderPistonInserter.pistonPose(itemPoses, facing, lastExtend);
        RenderPistonInserter.itemPose(itemPoses, stack.getItem() instanceof BlockItem, arm);
        item.write(itemPoses.last().pose(), partialTick);
    }

    public void updateMovingParts(float partialTick) {
        float extend =
                (float)
                        ((blockEntity.lastExtend
                                        + (blockEntity.renderExtend - blockEntity.lastExtend)
                                                * partialTick)
                                / BlockEntityPistonInserter.MAX_EXTEND);
        if (extend == lastExtend) return;
        pose.set(basePose).translate(0F, extend * (float) STROKE, 0F);
        world.translation(visualPos.getX(), visualPos.getY(), visualPos.getZ()).mul(pose);
        piston.setTransform(world).light(0).setChanged();
        lastExtend = extend;
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return frameBounds.minmax(
                new AABB(pos)
                        .expandTowards(facing.getStepX(), facing.getStepY(), facing.getStepZ())
                        .inflate(1));
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        consumer.accept(piston);
    }

    @Override
    protected void _delete() {
        piston.delete();
        item.delete();
    }
}
