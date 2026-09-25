// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.client.render.flywheel.ChainsawItemVisual;
import com.hbm.client.render.flywheel.ItemVisuals;
import com.hbm.items.tool.ItemToolAbilityFueled;
import com.hbm.main.ResourceManager;
import com.hbm.render.anim.HbmAnimations;
import com.hbm.util.GameTime;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.engine_room.flywheel.api.visual.ItemStackVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

public final class ChainsawItemRenderer
        implements SpecialModelRenderer<ChainsawItemRenderer.Argument>,
                ItemVisuals.Factory<ChainsawItemRenderer.Argument> {

    public static final int SAW = ResourceManager.chainsaw.partId("Saw");
    public static final int TOOTH = ResourceManager.chainsaw.partId("Tooth");
    public static final int TEETH = 20;
    private static final double IDLE_RUN = 0.0625D;
    private static final double BEND = 0.25D * Math.PI;
    private static final int SWEEP_STEPS = 256;

    private final List<Vector3fc> extents = extentsOf();

    private static List<Vector3fc> extentsOf() {
        float[] saw = ResourceManager.chainsaw.boundsOfParts(SAW);
        float[] tooth = ResourceManager.chainsaw.boundsOfParts(TOOTH);
        Vector3f min = new Vector3f(saw[0], saw[1], saw[2]);
        Vector3f max = new Vector3f(saw[3], saw[4], saw[5]);
        PoseStack.Pose root = new PoseStack().last();
        PoseStack.Pose pose = root.copy();
        double first = forward(0, 0.0D);
        double last = forward(TEETH - 1, 0.25D);
        Vector3f corner = new Vector3f();
        for (int step = 0; step <= SWEEP_STEPS; step++) {
            pose.set(root);
            tooth(pose, first + (last - first) * step / SWEEP_STEPS);
            for (int x = 0; x < 2; x++)
                for (int y = 0; y < 2; y++)
                    for (int z = 0; z < 2; z++) {
                        pose.pose()
                                .transformPosition(
                                        tooth[x * 3], tooth[y * 3 + 1], tooth[z * 3 + 2], corner);
                        min.min(corner);
                        max.max(corner);
                    }
        }
        List<Vector3fc> corners = new ArrayList<>(8);
        for (int x = 0; x < 2; x++)
            for (int y = 0; y < 2; y++)
                for (int z = 0; z < 2; z++) {
                    corners.add(
                            new Vector3f(
                                    x == 0 ? min.x : max.x,
                                    y == 0 ? min.y : max.y,
                                    z == 0 ? min.z : max.z));
                }
        return List.copyOf(corners);
    }

    public static double forward(int tooth, double run) {
        return tooth * 0.25D + run - 2.0625D;
    }

    public static double run(boolean running, long now) {
        return running ? now % 100L * 0.25D / 100.0D : IDLE_RUN;
    }

    public static void swing(PoseStack.Pose pose) {
        double[] rot = HbmAnimations.getRelevantTransformation("SWING_ROT");
        double[] trans = HbmAnimations.getRelevantTransformation("SWING_TRANS");
        pose.translate((float) trans[0], (float) trans[1], (float) trans[2]);
        pose.rotate(Axis.ZP.rotationDegrees((float) rot[2]));
        pose.rotate(Axis.YP.rotationDegrees((float) rot[1]));
        pose.rotate(Axis.XP.rotationDegrees((float) rot[0]));
    }

    public static void tooth(PoseStack.Pose pose, double forward) {
        pose.translate(0.0F, 0.0F, 1.9375F);
        pose.translate(0.0F, 0.375F, 0.5625F);
        double angle = Mth.clamp(forward, 0.0D, BEND);
        pose.rotate(Axis.XP.rotationDegrees((float) (angle * 180.0D / BEND)));
        pose.translate(0.0F, -0.375F, -0.5625F);
        if (forward < 0.0D) pose.translate(0.0F, 0.0F, (float) forward);
        if (forward > BEND) pose.translate(0.0F, 0.0F, (float) (forward - BEND));
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        extents.forEach(output);
    }

    @Override
    public @Nullable Argument extractArgument(ItemStack stack) {
        return null;
    }

    @Override
    public void submit(
            @Nullable Argument argument,
            PoseStack pose,
            SubmitNodeCollector collector,
            int light,
            int overlay,
            boolean foil,
            int outline) {
        if (argument == null) return;

        RenderType type = RenderTypes.entityCutoutCull(ResourceManager.chainsaw_tex);

        pose.pushPose();
        if (argument.swings()) swing(pose.last());

        double run = run(argument.running(), GameTime.now());
        collector.submitCustomGeometry(
                pose,
                type,
                (p, buffer) -> {
                    ResourceManager.chainsaw.renderPart(p, buffer, light, -1, SAW);
                    PoseStack.Pose tooth = p.copy();
                    for (int i = 0; i < TEETH; i++) {
                        tooth.set(p);
                        tooth(tooth, forward(i, run));
                        ResourceManager.chainsaw.renderPart(tooth, buffer, light, -1, TOOTH);
                    }
                });
        pose.popPose();
    }

    @Override
    public ItemStackVisual createVisual(
            VisualizationContext ctx,
            @Nullable Argument argument,
            ItemStack stack,
            ItemDisplayContext context,
            @Nullable ItemOwner owner) {
        return new ChainsawItemVisual(
                ctx, argument != null && argument.swings(), argument != null && argument.running());
    }

    public record Argument(boolean swings, boolean running) {}

    public record Unbaked(Identifier base) implements ItemModel.Unbaked {
        public static final MapCodec<Unbaked> MAP_CODEC =
                RecordCodecBuilder.mapCodec(
                        instance ->
                                instance.group(
                                                Identifier.CODEC
                                                        .fieldOf("base")
                                                        .forGetter(Unbaked::base))
                                        .apply(instance, Unbaked::new));

        @Override
        public void resolveDependencies(ResolvableModel.Resolver resolver) {
            resolver.markDependency(base);
        }

        @Override
        public ItemModel bake(ItemModel.BakingContext context, Matrix4fc transformation) {
            return new DynamicSpecialWrapper<>(
                    new ChainsawItemRenderer(),
                    (stack, display, owner) ->
                            new Argument(
                                    HandPass.local(display, owner)
                                            && !Minecraft.getInstance().player.isBlocking(),
                                    ((ItemToolAbilityFueled) stack.getItem()).canOperate(stack)),
                    true,
                    DynamicSpecialWrapper.properties(context, base));
        }

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }
    }
}
