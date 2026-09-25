// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.client.render.FramedItem;
import com.hbm.client.render.RenderPress;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.GroupObject;
import com.hbm.tileentity.machine.BlockEntityMachinePress;
import com.mojang.blaze3d.vertex.PoseStack;
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

public final class PressVisual extends HbmDynamicBlockEntityVisual<BlockEntityMachinePress>
        implements ShaderLightVisual {
    private static final MeshPart[] HEAD =
            MeshPart.objParts(
                    ResourceManager.press_head, MeshPart.litCutout(ResourceManager.press_head_tex));
    private final AABB bodyBounds;
    private final TransformedInstance[] heads;
    private final Matrix4f localPose = new Matrix4f();
    private final Matrix4f headBasePose = new Matrix4f();
    private final Matrix4f instancePose = new Matrix4f();
    private final double[] lightBoundsAccumulator = new double[6];
    private @Nullable AABB lastLightBounds;
    private float lastLift = Float.NaN;
    private final WorldItem item = new WorldItem(visualizationContext, level, pos);
    private final PoseStack itemPoses = new PoseStack();

    public PressVisual(
            VisualizationContext context, BlockEntityMachinePress blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        var bodyLocal = new Matrix4f().translation(.5F, 0F, .5F).rotateY(Mth.PI);
        headBasePose.translation(.5F, 0F, .5F).scale(.99F, 1F, .99F);
        AABB extent = new AABB(pos);
        for (GroupObject group : ResourceManager.press_body.groups)
            extent = extent.minmax(LightBounds.of(group, bodyLocal, pos));
        bodyBounds = extent;
        heads = new TransformedInstance[HEAD.length];
        for (int i = 0; i < HEAD.length; i++)
            heads[i] =
                    instancerProvider()
                            .instancer(InstanceTypes.TRANSFORMED, HEAD[i].model())
                            .createInstance();
        updateMovingParts(partialTick);
        updateItem(partialTick);
    }

    public static void initModels() {}

    public static boolean vanillaNeeded(BlockEntityMachinePress be) {
        return !WorldItem.drawsFramed(be.getItem(BlockEntityMachinePress.SLOT_INPUT));
    }

    @Override
    protected void frame(Context context) {
        updateMovingParts(context.partialTick());
        updateItem(context.partialTick());
    }

    private void updateItem(float partialTick) {
        FramedItem.Arm arm =
                item.setFramed(blockEntity.getItem(BlockEntityMachinePress.SLOT_INPUT), level);
        if (arm == null) return;
        itemPoses.setIdentity();
        itemPoses.translate(visualPos.getX(), visualPos.getY(), visualPos.getZ());
        RenderPress.PRESS_ITEM.apply(itemPoses, 0F);
        FramedItem.decoArm(itemPoses, arm);
        item.write(itemPoses.last().pose(), partialTick);
    }

    public void updateMovingParts(float partialTick) {
        double progress =
                (blockEntity.lastPress
                                + (blockEntity.renderPress - blockEntity.lastPress) * partialTick)
                        / (double) BlockEntityMachinePress.MAX_PROGRESS;
        float lift = (float) (Mth.clamp(1D - progress, 0D, 1D) * .875D);
        if (lift == lastLift) return;
        lastLift = lift;
        localPose.set(headBasePose).translate(0F, lift, 0F);
        instancePose
                .translation(visualPos.getX(), visualPos.getY(), visualPos.getZ())
                .mul(localPose);
        LightBounds.resetBounds(lightBoundsAccumulator, bodyBounds);
        for (int i = 0; i < heads.length; i++) {
            heads[i].setTransform(instancePose).light(0).setChanged();
            LightBounds.includeLightBounds(lightBoundsAccumulator, HEAD[i].model(), localPose, pos);
        }
        lastLightBounds =
                LightBounds.sections(lightSections, lightBoundsAccumulator, lastLightBounds);
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return bodyBounds.minmax(new AABB(pos).expandTowards(0, 2, 0).inflate(1));
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        for (var head : heads) consumer.accept(head);
    }

    @Override
    protected void _delete() {
        for (var head : heads) head.delete();
        item.delete();
    }
}
