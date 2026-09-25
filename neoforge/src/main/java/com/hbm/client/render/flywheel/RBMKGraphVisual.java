// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.blocks.machine.rbmk.RBMKMiniPanelBase;
import com.hbm.client.render.RenderRBMKGraph;
import com.hbm.main.ResourceManager;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKGraph.GraphUnit;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKGraph;
import com.hbm.util.BobMathUtil;
import com.hbm.util.Facing;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.material.CardinalLightingMode;
import dev.engine_room.flywheel.api.material.DepthTest;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.material.Transparency;
import dev.engine_room.flywheel.api.material.WriteMask;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.material.CutoutShaders;
import dev.engine_room.flywheel.lib.material.LightShaders;
import dev.engine_room.flywheel.lib.material.Materials;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.material.StandardMaterialShaders;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

public final class RBMKGraphVisual extends HbmDynamicBlockEntityVisual<BlockEntityRBMKGraph>
        implements ShaderLightVisual {
    private static final int GRAPHS = BlockEntityRBMKGraph.GRAPHS;
    private static final float LINE_WIDTH = 2F;
    private static final int PLOT_COLOR = ARGB.opaque(0x00FF00);
    private static final MeshPart SHELL_PART = buildShell();
    private static final Model LINE_MODEL = buildLine();
    private static final float TEXT_SCALE = (float) RenderRBMKGraph.LINE_SCALE;
    private static final float UPPER_SHIFT = (float) (-.03125D * 7 / RenderRBMKGraph.LINE_SCALE);
    private final TransformedInstance[] shells = new TransformedInstance[GRAPHS];
    private final ArrayList<TransformedInstance>[] plotLines;
    private final Matrix4f basePose = new Matrix4f();
    private final Matrix4f instancePose = new Matrix4f();
    private final Matrix4f linePose = new Matrix4f();
    private final Matrix4f plotPose = new Matrix4f();
    private final long[][] lastValues = new long[GRAPHS][];
    private final long[] lastMin = new long[GRAPHS], lastMax = new long[GRAPHS];
    private final boolean[] lastActive = new boolean[GRAPHS];
    private final boolean[] lastMinBound = new boolean[GRAPHS], lastMaxBound = new boolean[GRAPHS];
    private final boolean[] graphWritten = new boolean[GRAPHS];
    private final Vector3f from = new Vector3f();
    private final Vector3f to = new Vector3f();
    private final Quaternionf rotation = new Quaternionf();
    private final WorldText[] lowers = new WorldText[GRAPHS], uppers = new WorldText[GRAPHS];
    private final WorldText[] labels = new WorldText[GRAPHS];
    private final WorldText.Posing[] lowerPosings = new WorldText.Posing[GRAPHS];
    private final WorldText.Posing[] upperPosings = new WorldText.Posing[GRAPHS];
    private final WorldText.Posing[] labelPosings = new WorldText.Posing[GRAPHS];
    private final boolean[] textActive = new boolean[GRAPHS];
    private final long[] textLowest = new long[GRAPHS], textHighest = new long[GRAPHS];
    private final @Nullable String[] shownLabels = new String[GRAPHS];
    private float lastYaw;

    @SuppressWarnings("unchecked")
    public RBMKGraphVisual(
            VisualizationContext context, BlockEntityRBMKGraph blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        for (int i = 0; i < GRAPHS; i++)
            shells[i] =
                    instancerProvider()
                            .instancer(InstanceTypes.TRANSFORMED, SHELL_PART.model())
                            .createInstance();
        plotLines = new ArrayList[GRAPHS];
        for (int i = 0; i < GRAPHS; i++) plotLines[i] = new ArrayList<>();
        for (int i = 0; i < GRAPHS; i++) {
            int graph = i;
            lowers[i] = new WorldText(instancerProvider(), WorldText.Style.NORMAL, true);
            uppers[i] = new WorldText(instancerProvider(), WorldText.Style.NORMAL, true);
            labels[i] = new WorldText(instancerProvider(), WorldText.Style.NORMAL, true);
            lowerPosings[i] = (width, out) -> boundPose(out, graph).translate(-width, 0F, 0F);
            upperPosings[i] =
                    (width, out) ->
                            boundPose(out, graph)
                                    .translate(0F, UPPER_SHIFT, 0F)
                                    .translate(-width, 0F, 0F);
            labelPosings[i] =
                    (width, out) -> {
                        float scale = Math.min(.0125F, .75F / Math.max(width, 1));
                        graphPose(out, graph)
                                .translate(.01F, .3125F, 0F)
                                .scale(scale, -scale, scale)
                                .rotateY(90F * Mth.DEG_TO_RAD)
                                .translate(-width / 2, 0F, 0F);
                    };
        }
        writeFrame(partialTick);
        updateText();
    }

    public static void initModels() {}

    private static MeshPart buildShell() {
        var model = ResourceManager.rbmk_numitron;
        Material material =
                SimpleMaterial.builderOf(Materials.CUTOUT)
                        .texture(ResourceManager.rbmk_numitron_tex)
                        .mipmap(false)
                        .cutout(CutoutShaders.ONE_TENTH)
                        .light(LightShaders.SMOOTH)
                        .ambientOcclusion(false)
                        .cardinalLightingMode(CardinalLightingMode.CHUNK)
                        .backfaceCulling(true)
                        .build();
        return MeshPart.obj(model.groups[0], model.smoothing(), material);
    }

    private static Model buildLine() {
        Material material =
                SimpleMaterial.builder()
                        .texture(EffectVisuals.Shared.WHITE)
                        .mipmap(false)
                        .cutout(CutoutShaders.ONE_TENTH)
                        .shaders(StandardMaterialShaders.LINE)
                        .light(LightShaders.NONE)
                        .useLight(false)
                        .useOverlay(false)
                        .ambientOcclusion(false)
                        .cardinalLightingMode(CardinalLightingMode.OFF)
                        .backfaceCulling(false)
                        .transparency(Transparency.OPAQUE)
                        .writeMask(WriteMask.COLOR_DEPTH)
                        .depthTest(DepthTest.LEQUAL)
                        .build();
        return MeshPart.line(LINE_WIDTH, material);
    }

    private static float plotY(long value, long lowest, long highest, long range) {
        long flux = value;
        if (flux < lowest) flux = lowest;
        if (flux > highest) flux = highest;
        return (float) (.5D - .03125D + (flux - lowest) * .1875D / Math.max(range, 1L));
    }

    private static float plotZ(int index, int count) {
        return (float) (.375D - index * .75D / (count - 1));
    }

    private static long lowest(GraphUnit graph) {
        return graph.minBound ? graph.min : BobMathUtil.min(graph.values);
    }

    private static long highest(GraphUnit graph) {
        return graph.maxBound ? graph.max : BobMathUtil.max(graph.values);
    }

    private static void hide(List<TransformedInstance> instances) {
        for (var instance : instances) instance.setVisible(false);
    }

    @Override
    protected void frame(Context context) {
        writeFrame(context.partialTick());
        updateText();
    }

    private Matrix4f graphPose(Matrix4f out, int graph) {
        return out.translation(visualPos.getX(), visualPos.getY(), visualPos.getZ())
                .mul(basePose)
                .translate(.25F, graph * -.5F + .25F, 0F);
    }

    private Matrix4f boundPose(Matrix4f out, int graph) {
        return graphPose(out, graph)
                .translate(.032F, (float) (.5D - .03125D * 1.5D), (float) (-.375D + .03125D))
                .scale(TEXT_SCALE, -TEXT_SCALE, TEXT_SCALE)
                .rotateY(90F * Mth.DEG_TO_RAD);
    }

    private void updateText() {
        float y = -Minecraft.getInstance().font.lineHeight / 2;
        for (int i = 0; i < GRAPHS; i++) {
            GraphUnit graph = blockEntity.graphs[i];
            boolean active = graph.active;
            long lowest = active ? lowest(graph) : 0L;
            long highest = active ? highest(graph) : 0L;
            if (active != textActive[i] || lowest != textLowest[i] || highest != textHighest[i]) {
                textActive[i] = active;
                textLowest[i] = lowest;
                textHighest[i] = highest;
                lowers[i].set(
                        active ? Component.literal(Long.toString(lowest)) : null,
                        0F,
                        y,
                        PLOT_COLOR);
                uppers[i].set(
                        active ? Component.literal(Long.toString(highest)) : null,
                        0F,
                        y,
                        PLOT_COLOR);
            }
            String label =
                    active && graph.label != null && !graph.label.isEmpty() ? graph.label : null;
            if (!Objects.equals(label, shownLabels[i])) {
                shownLabels[i] = label;
                labels[i].set(label == null ? null : Component.literal(label), 0F, y, PLOT_COLOR);
            }
            lowers[i].write(lowerPosings[i]);
            uppers[i].write(upperPosings[i]);
            labels[i].write(labelPosings[i]);
        }
    }

    private void writeFrame(float partialTick) {
        Direction facing = blockEntity.getBlockState().getValue(RBMKMiniPanelBase.FACING);
        float yaw = Facing.yaw(facing, 90);
        basePose.identity().translate(.5F, 0F, .5F).rotateY(yaw * Mth.DEG_TO_RAD);
        for (int i = 0; i < GRAPHS; i++) {
            GraphUnit graph = blockEntity.graphs[i];
            if (graphWritten[i]
                    && lastYaw == yaw
                    && lastActive[i] == graph.active
                    && (!graph.active
                            || lastMin[i] == graph.min
                                    && lastMax[i] == graph.max
                                    && lastMinBound[i] == graph.minBound
                                    && lastMaxBound[i] == graph.maxBound
                                    && Arrays.equals(lastValues[i], graph.values))) continue;
            plotPose.set(basePose).translate(.25F, i * -.5F + .25F, 0F);
            shells[i].setVisible(graph.active);
            if (graph.active) {
                instancePose
                        .translation(visualPos.getX(), visualPos.getY(), visualPos.getZ())
                        .mul(plotPose);
                shells[i].setTransform(instancePose).light(0).setChanged();
                writePlot(i, plotPose, graph);
            } else {
                hide(plotLines[i]);
            }
            if (lastValues[i] == null || lastValues[i].length != graph.values.length)
                lastValues[i] = new long[graph.values.length];
            System.arraycopy(graph.values, 0, lastValues[i], 0, graph.values.length);
            lastMin[i] = graph.min;
            lastMax[i] = graph.max;
            lastActive[i] = graph.active;
            lastMinBound[i] = graph.minBound;
            lastMaxBound[i] = graph.maxBound;
            graphWritten[i] = true;
        }
        lastYaw = yaw;
    }

    private void writePlot(int graphIndex, Matrix4f plotPose, GraphUnit graph) {
        long lowest = lowest(graph);
        long highest = highest(graph);
        long range = highest - lowest;
        int needed = Math.max(graph.values.length - 1, 0);
        while (plotLines[graphIndex].size() < needed)
            plotLines[graphIndex].add(
                    instancerProvider()
                            .instancer(InstanceTypes.TRANSFORMED, LINE_MODEL)
                            .createInstance());
        for (int v = 0; v < needed; v++) {
            float y0 = plotY(graph.values[v], lowest, highest, range);
            float y1 = plotY(graph.values[v + 1], lowest, highest, range);
            float z0 = plotZ(v, graph.values.length);
            float z1 = plotZ(v + 1, graph.values.length);
            writeLine(plotLines[graphIndex].get(v), plotPose, .03225F, y0, z0, .03225F, y1, z1);
        }
        for (int i = needed; i < plotLines[graphIndex].size(); i++)
            plotLines[graphIndex].get(i).setVisible(false);
    }

    private void writeLine(
            TransformedInstance instance,
            Matrix4f base,
            float x0,
            float y0,
            float z0,
            float x1,
            float y1,
            float z1) {
        Vector3f a = base.transformPosition(from.set(x0, y0, z0));
        Vector3f b = base.transformPosition(to.set(x1, y1, z1));
        float dx = b.x - a.x, dy = b.y - a.y, dz = b.z - a.z;
        float length = Mth.sqrt(dx * dx + dy * dy + dz * dz);
        instance.setVisible(length > 0F);
        if (length <= 0F) return;
        rotation.rotationTo(0F, 1F, 0F, dx / length, dy / length, dz / length);
        linePose.translation(visualPos.getX() + a.x, visualPos.getY() + a.y, visualPos.getZ() + a.z)
                .rotate(rotation)
                .scale(length);
        instance.setTransform(linePose).colorArgb(PLOT_COLOR).light(0).setChanged();
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return new AABB(pos).inflate(1D);
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        for (var shell : shells) consumer.accept(shell);
    }

    @Override
    protected void _delete() {
        for (var shell : shells) shell.delete();
        for (var lines : plotLines) for (var line : lines) line.delete();
        for (int i = 0; i < GRAPHS; i++) {
            lowers[i].delete();
            uppers[i].delete();
            labels[i].delete();
        }
    }
}
