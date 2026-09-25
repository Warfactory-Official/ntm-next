// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.client.render.FramedItem;
import com.hbm.client.render.RenderPress;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.GroupObject;
import com.hbm.tileentity.machine.BlockEntityMachineEPress;
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

public final class ElectricPressVisual extends HbmDynamicBlockEntityVisual<BlockEntityMachineEPress>
        implements ShaderLightVisual {
    private static final MeshPart[] HEAD =
            MeshPart.objParts(
                    ResourceManager.epress_head,
                    MeshPart.litCutout(ResourceManager.epress_head_tex));

    private final AABB bodyBounds;
    private final TransformedInstance[] heads = new TransformedInstance[HEAD.length];
    private final Matrix4f localPose = new Matrix4f();
    private final Matrix4f headBasePose = new Matrix4f();
    private final Matrix4f instancePose = new Matrix4f();
    private final double[] lightBoundsAccumulator = new double[6];
    private float lastLift = Float.NaN;
    private @Nullable AABB lastLightBounds;
    private final float itemYaw;
    private final WorldItem item = new WorldItem(visualizationContext, level, pos);
    private final PoseStack itemPoses = new PoseStack();

    public ElectricPressVisual(
            VisualizationContext context, BlockEntityMachineEPress blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        itemYaw = 90F + RenderPress.facingYaw(BlockMultiblockCore.coreFacing(blockState));
        float bodyYaw = itemYaw * Mth.DEG_TO_RAD;
        Matrix4f bodyLocal = new Matrix4f().translation(.5F, 0F, .5F).rotateY(bodyYaw);
        headBasePose.translation(.5F, 1F, .5F).rotateY(bodyYaw);
        AABB bodyExtent = new AABB(pos);
        for (GroupObject group : ResourceManager.epress_body.groups)
            bodyExtent = bodyExtent.minmax(LightBounds.of(group, bodyLocal, pos));
        bodyBounds = bodyExtent;
        for (int i = 0; i < HEAD.length; i++)
            heads[i] =
                    instancerProvider()
                            .instancer(InstanceTypes.TRANSFORMED, HEAD[i].model())
                            .createInstance();
        updateMovingParts(partialTick);
        updateItem(partialTick);
    }

    public static void initModels() {}

    public static boolean vanillaNeeded(BlockEntityMachineEPress be) {
        return !WorldItem.drawsFramed(be.getItem(BlockEntityMachineEPress.SLOT_INPUT));
    }

    @Override
    protected void frame(Context context) {
        updateMovingParts(context.partialTick());
        updateItem(context.partialTick());
    }

    private void updateItem(float partialTick) {
        FramedItem.Arm arm =
                item.setFramed(blockEntity.getItem(BlockEntityMachineEPress.SLOT_INPUT), level);
        if (arm == null) return;
        itemPoses.setIdentity();
        itemPoses.translate(visualPos.getX(), visualPos.getY(), visualPos.getZ());
        RenderPress.EPRESS_ITEM.apply(itemPoses, itemYaw);
        FramedItem.decoArm(itemPoses, arm);
        item.write(itemPoses.last().pose(), partialTick);
    }

    public void updateMovingParts(float partialTick) {
        double progress =
                (blockEntity.lastPress
                                + (blockEntity.renderPress - blockEntity.lastPress) * partialTick)
                        / (double) BlockEntityMachineEPress.MAX_PROGRESS;
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
