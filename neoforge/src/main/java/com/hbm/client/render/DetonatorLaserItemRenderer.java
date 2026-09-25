// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.client.render.flywheel.DetonatorLaserItemVisual;
import com.hbm.client.render.flywheel.ItemVisuals;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.UnitQuad;
import com.hbm.util.GameTime;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.engine_room.flywheel.api.visual.ItemStackVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

public final class DetonatorLaserItemRenderer
        implements SpecialModelRenderer<ItemDisplayContext>,
                ItemVisuals.Factory<ItemDisplayContext> {
    public static final int PART_MAIN = ResourceManager.detonator_laser.partId("Main");
    public static final int PART_LIGHTS = ResourceManager.detonator_laser.partId("Lights");
    public static final int LIGHTS_COLOR = 0xFFFF0000;
    public static final int BEAM_COLOR = 0xFFFFFF00;
    public static final int TEXT_COLOR = 0xFFFF0000;
    public static final float READOUT_STEP = 12.5F;

    private static final float PX = 0.0625F;
    private static final int SUB = 32;
    private static final double AMPLITUDE = 0.075D;
    private static final UnitQuad BEAM = UnitQuad.of(new float[8], 1F, 0F, 0F);

    private final float[] bounds = ResourceManager.detonator_laser.boundsExcluding(Set.of());

    private static void beam(SubmitNodeCollector collector, PoseStack pose, long time) {
        collector.submitCustomGeometry(
                pose,
                DetonatorLaserRenderTypes.LIGHTS,
                (p, buffer) -> {
                    PoseStack.Pose scratch = new PoseStack.Pose();
                    beamQuads(
                            time,
                            new Matrix4f(),
                            (quad, m) ->
                                    quad.emit(
                                            p,
                                            scratch,
                                            buffer,
                                            m,
                                            LightCoordsUtil.FULL_BRIGHT,
                                            BEAM_COLOR));
                });
    }

    public static void beamQuads(long time, Matrix4f scratch, UnitQuad.Visitor visitor) {

        double phase = time / -100.0D;
        double length = (double) (PX * 8F) / SUB;
        for (int i = 0; i < SUB; i++) {
            double h0 = Math.sin(i * 0.5D + phase) * AMPLITUDE;
            double h1 = Math.sin((i + 1) * 0.5D + phase) * AMPLITUDE;
            float z0 = (float) (length * i);
            float z1 = (float) (length * (i + 1));
            if (UnitQuad.matrix(
                    0F,
                    (float) (-PX * 0.25D + h1),
                    z1,
                    0F,
                    (float) (PX * 0.25D + h1),
                    z1,
                    0F,
                    (float) (PX * 0.25D + h0),
                    z0,
                    0F,
                    (float) (-PX * 0.25D + h0),
                    z0,
                    scratch)) {
                visitor.accept(BEAM, scratch.translateLocal(0.5626F, PX * 18F, -PX * 14F));
            }
        }
    }

    private static void readouts(SubmitNodeCollector collector, PoseStack pose, long time) {
        pose.pushPose();
        readoutPose(pose.last());
        for (String line : readoutLines(time)) {

            collector.submitText(
                    pose,
                    0F,
                    0F,
                    FormattedCharSequence.forward(line, Style.EMPTY),
                    false,
                    Font.DisplayMode.NORMAL,
                    LightCoordsUtil.FULL_BRIGHT,
                    TEXT_COLOR,
                    0,
                    0);
            pose.translate(0F, READOUT_STEP, 0F);
        }
        pose.popPose();
    }

    public static void readoutPose(PoseStack.Pose pose) {
        float f3 = 0.01F;
        pose.translate(0.5625F, 1.3125F, 0.875F);
        pose.scale(f3, -f3, f3);
        pose.rotate(Axis.YP.rotationDegrees(90F));

        pose.translate(3F, -2F, 0.2F);
    }

    public static String[] readoutLines(long time) {
        Random rand = new Random(time / 500L);
        String[] lines = new String[3];
        for (int i = 0; i < lines.length; i++)
            lines[i] = Integer.toString(rand.nextInt(900000) + 100000);
        return lines;
    }

    @Override
    public @Nullable ItemDisplayContext extractArgument(ItemStack stack) {
        return ItemDisplayContext.NONE;
    }

    @Override
    public void submit(
            @Nullable ItemDisplayContext context,
            PoseStack pose,
            SubmitNodeCollector collector,
            int light,
            int overlay,
            boolean foil,
            int outline) {

        collector.submitCustomGeometry(
                pose,
                RenderTypes.entityCutoutCull(ResourceManager.detonator_laser_tex),
                (p, buffer) ->
                        ResourceManager.detonator_laser.renderPart(
                                p, buffer, light, -1, PART_MAIN));

        long time = GameTime.now();

        collector.submitCustomGeometry(
                pose,
                DetonatorLaserRenderTypes.LIGHTS,
                (p, buffer) ->
                        ResourceManager.detonator_laser.renderPart(
                                p, buffer, LightCoordsUtil.FULL_BRIGHT, LIGHTS_COLOR, PART_LIGHTS));

        beam(collector, pose, time);
        readouts(collector, pose, time);
    }

    @Override
    public ItemStackVisual createVisual(
            VisualizationContext ctx,
            @Nullable ItemDisplayContext argument,
            ItemStack stack,
            ItemDisplayContext context,
            @Nullable ItemOwner owner) {
        return new DetonatorLaserItemVisual(ctx);
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        for (int x = 0; x < 2; x++)
            for (int y = 0; y < 2; y++)
                for (int z = 0; z < 2; z++) {
                    output.accept(
                            new Vector3f(bounds[x * 3], bounds[y * 3 + 1], bounds[z * 3 + 2]));
                }
    }

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
                    new DetonatorLaserItemRenderer(),
                    (stack, display, owner) -> display,
                    true,
                    DynamicSpecialWrapper.properties(context, base));
        }

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }
    }
}
