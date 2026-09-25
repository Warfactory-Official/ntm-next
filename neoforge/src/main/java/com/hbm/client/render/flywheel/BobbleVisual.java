// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.blocks.generic.BlockBobble.BobbleType;
import com.hbm.blocks.generic.BlockBobble;
import com.hbm.client.render.RenderBobble;
import com.hbm.tileentity.BlockEntityBobble;
import com.hbm.util.GameTime;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.material.Transparency;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.network.chat.Component;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

public final class BobbleVisual extends HbmDynamicBlockEntityVisual<BlockEntityBobble>
        implements ShaderLightVisual {
    private static final Prop[] PROPS = new Prop[BobbleType.values().length];
    private static final VarHandle PROP = MethodHandles.arrayElementVarHandle(Prop[].class);
    private List<BobbleDraws.Draw> draws = List.of();
    private final ArrayList<TransformedInstance> instances = new ArrayList<>();
    private final Matrix4f instancePose = new Matrix4f();
    private final Matrix4f anchor = new Matrix4f();
    private final WorldText label =
            new WorldText(instancerProvider(), WorldText.Style.NORMAL, true);
    private final Matrix4f labelBase = new Matrix4f();
    private final WorldText.Posing labelPosing =
            (width, out) -> RenderBobble.labelWidth(out.set(labelBase), width);
    private final WorldItem prop = new WorldItem(visualizationContext, level, pos);
    private final Matrix4f propLocal = new Matrix4f();
    private final Matrix4f propPose = new Matrix4f();
    private @Nullable Matrix4f propPivot;
    private @Nullable BobbleType lastType;
    private long lastTime = Long.MIN_VALUE;
    private boolean animated;

    public BobbleVisual(
            VisualizationContext context, BlockEntityBobble blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        updateMovingParts(partialTick);
    }

    public static void initModels() {}

    public static boolean vanillaNeeded(BlockEntityBobble be) {
        int i = be.type.ordinal();
        Prop prop = (Prop) PROP.getAcquire(PROPS, i);

        if (prop == null || prop.stack().getPrototype() != prop.components()) {
            ItemStack stack = RenderBobble.prop(be.type);
            prop = new Prop(stack, stack.getPrototype());
            PROP.setRelease(PROPS, i, prop);
        }
        return !WorldItem.draws(prop.stack(), ItemDisplayContext.NONE);
    }

    @Override
    protected void frame(Context context) {
        updateMovingParts(context.partialTick());
    }

    public void updateMovingParts(float partialTick) {
        BobbleType type = blockEntity.type;
        if (type != lastType) {
            lastType = type;
            rebuild(type);
            lastTime = Long.MIN_VALUE;
        }
        if (animated) {
            long time = GameTime.now();
            if (time != lastTime) {
                lastTime = time;
                wobble(time);
            }
        }
        label.write(labelPosing);
        prop.write(propPose, partialTick);
    }

    private void wobble(long time) {
        int shine = RenderBobble.shine(time);
        for (int i = 0; i < draws.size(); i++) {
            BobbleDraws.Draw draw = draws.get(i);
            TransformedInstance instance = instances.get(i);
            if (draw.pivot() != null) {
                instance.setTransform(
                                BobbleDraws.bob(
                                        instancePose, anchor, draw.pivot(), time, draw.pose()))
                        .setChanged();
            } else if (draw.shine()) {
                instance.colorArgb(shine).setChanged();
            }
        }
        if (propPivot != null) BobbleDraws.bob(propPose, anchor, propPivot, time, propLocal);
    }

    private void rebuild(BobbleType type) {
        for (TransformedInstance instance : instances) instance.delete();
        instances.clear();
        int rotation = blockState.getValue(BlockBobble.ROTATION);
        PoseStack pose = new PoseStack();
        pose.translate(.5D, 0D, .5D);
        pose.scale(.25F, .25F, .25F);
        pose.mulPose(Axis.YN.rotationDegrees(22.5F * rotation + 90F));
        BobbleDraws built = new BobbleDraws(type, pose);
        draws = built.draws;
        propLocal.set(built.propLocal);
        propPivot = built.propPivot;
        anchor.translation(visualPos.getX(), visualPos.getY(), visualPos.getZ());
        labelBase.set(anchor).mul(built.labelLocal);
        label.set(Component.literal(type.label), 0F, 0F, RenderBobble.labelColor(type));
        prop.set(RenderBobble.prop(type), ItemDisplayContext.NONE);
        if (propPivot == null) propPose.set(anchor).mul(propLocal);
        animated = false;
        for (BobbleDraws.Draw draw : draws) {
            TransformedInstance instance =
                    instancerProvider()
                            .instancer(InstanceTypes.TRANSFORMED, draw.part().model())
                            .createInstance();
            instance.colorArgb(draw.color()).light(draw.light());
            if (draw.pivot() == null)
                instance.setTransform(instancePose.set(anchor).mul(draw.pose()));
            instance.setChanged();
            instances.add(instance);
            animated |= draw.pivot() != null || draw.shine();
        }
    }

    private static boolean solid(BobbleDraws.Draw draw) {
        Material material = draw.part().model().meshes().getFirst().material();
        return material.transparency() == Transparency.OPAQUE
                && material.useLight()
                && draw.light() != LightCoordsUtil.FULL_BRIGHT;
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return new AABB(pos).inflate(2);
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        for (int i = 0; i < draws.size(); i++)
            if (solid(draws.get(i))) consumer.accept(instances.get(i));
    }

    @Override
    protected void _delete() {
        for (TransformedInstance instance : instances) instance.delete();
        instances.clear();
        label.delete();
        prop.delete();
    }

    private record Prop(ItemStack stack, DataComponentMap components) {}
}
