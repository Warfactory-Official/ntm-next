// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.blocks.machine.rbmk.RBMKMiniPanelBase;
import com.hbm.client.render.RenderRBMKGauge;
import com.hbm.main.ResourceManager;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKGauge.GaugeUnit;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKGauge;
import com.hbm.util.Facing;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.material.CardinalLightingMode;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.material.CutoutShaders;
import dev.engine_room.flywheel.lib.material.Materials;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.model.SingleMeshModel;
import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

public final class RBMKGaugeVisual extends HbmDynamicBlockEntityVisual<BlockEntityRBMKGauge>
        implements ShaderLightVisual {
    private static final int GAUGE = ResourceManager.rbmk_gauge.partId("Gauge");
    private static final int NEEDLE = ResourceManager.rbmk_gauge.partId("Needle");
    private static final Material NEEDLE_MATERIAL =
            SimpleMaterial.builderOf(Materials.CUTOUT_NO_CULL)
                    .texture(ResourceManager.white_tex)
                    .mipmap(false)
                    .cutout(CutoutShaders.ONE_TENTH)
                    .useLight(false)
                    .useOverlay(false)
                    .cardinalLightingMode(CardinalLightingMode.CHUNK)
                    .ambientOcclusion(false)
                    .backfaceCulling(false)
                    .build();
    private static final MeshPart GAUGE_PART =
            MeshPart.obj(
                    ResourceManager.rbmk_gauge.groups[GAUGE],
                    ResourceManager.rbmk_gauge.smoothing(),
                    MeshPart.litCutout(ResourceManager.rbmk_gauge_tex));
    private static final Model NEEDLE_MODEL =
            new SingleMeshModel(
                    PackedQuadMesh.of(
                            ResourceManager.rbmk_gauge.groups[NEEDLE],
                            ResourceManager.rbmk_gauge.smoothing()),
                    NEEDLE_MATERIAL);
    private final TransformedInstance[] gauges =
            new TransformedInstance[BlockEntityRBMKGauge.GAUGES];
    private final TransformedInstance[] needles =
            new TransformedInstance[BlockEntityRBMKGauge.GAUGES];
    private final Matrix4f[] gaugePoses = new Matrix4f[BlockEntityRBMKGauge.GAUGES];
    private final Matrix4f[] needlePoses = new Matrix4f[BlockEntityRBMKGauge.GAUGES];
    private final Matrix4f world = new Matrix4f();
    private final double[] lastAngles = new double[BlockEntityRBMKGauge.GAUGES];
    private final int[] lastColors = new int[BlockEntityRBMKGauge.GAUGES];
    private final boolean[] lastActive = new boolean[BlockEntityRBMKGauge.GAUGES];
    private final WorldText[] lowers = new WorldText[BlockEntityRBMKGauge.GAUGES];
    private final WorldText[] uppers = new WorldText[BlockEntityRBMKGauge.GAUGES];
    private final WorldText[] labels = new WorldText[BlockEntityRBMKGauge.GAUGES];
    private final WorldText.Posing[] labelPosings =
            new WorldText.Posing[BlockEntityRBMKGauge.GAUGES];
    private final Matrix4f[] lowerPoses = new Matrix4f[BlockEntityRBMKGauge.GAUGES];
    private final Matrix4f[] upperPoses = new Matrix4f[BlockEntityRBMKGauge.GAUGES];
    private final long[] lastMins = new long[BlockEntityRBMKGauge.GAUGES];
    private final long[] lastMaxes = new long[BlockEntityRBMKGauge.GAUGES];
    private final @Nullable String[] lastLabels = new String[BlockEntityRBMKGauge.GAUGES];
    private final boolean[] textActive = new boolean[BlockEntityRBMKGauge.GAUGES];
    private boolean initialized;
    private boolean textInitialized;

    public RBMKGaugeVisual(
            VisualizationContext context, BlockEntityRBMKGauge blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        Direction facing = blockEntity.getBlockState().getValue(RBMKMiniPanelBase.FACING);
        for (int i = 0; i < gauges.length; i++) {
            gauges[i] =
                    instancerProvider()
                            .instancer(InstanceTypes.TRANSFORMED, GAUGE_PART.model())
                            .createInstance();
            needles[i] =
                    instancerProvider()
                            .instancer(InstanceTypes.TRANSFORMED, NEEDLE_MODEL)
                            .createInstance();
            gaugePoses[i] =
                    new Matrix4f()
                            .translate(.5F, 0F, .5F)
                            .rotateY(Facing.yaw(facing, 90) * Mth.DEG_TO_RAD)
                            .translate(.25F, (i / 2) * -.5F + .25F, (i % 2) * -.5F + .25F);
            needlePoses[i] = new Matrix4f();
            lowers[i] = new WorldText(instancerProvider(), WorldText.Style.NORMAL, false);
            uppers[i] = new WorldText(instancerProvider(), WorldText.Style.NORMAL, false);
            labels[i] = new WorldText(instancerProvider(), WorldText.Style.NORMAL, true);
            lowerPoses[i] = linePose(gaugePoses[i], 0);
            upperPoses[i] = linePose(gaugePoses[i], 1);
            Matrix4f gauge = gaugePoses[i];
            labelPosings[i] =
                    (width, out) -> {
                        float scale = Math.min(.0125F, .4F / Math.max(width, 1));
                        out.translation(visualPos.getX(), visualPos.getY(), visualPos.getZ())
                                .mul(gauge)
                                .translate(.01F, .3125F, 0F)
                                .scale(scale, -scale, scale)
                                .rotateY(90F * Mth.DEG_TO_RAD)
                                .translate(-width / 2, 0F, 0F);
                    };
        }
        updateMovingParts(partialTick);
        updateText();
    }

    public static void initModels() {}

    private Matrix4f linePose(Matrix4f gauge, int j) {
        return new Matrix4f()
                .translation(visualPos.getX(), visualPos.getY(), visualPos.getZ())
                .mul(gauge)
                .translate(0F, .4375F, -.125F)
                .rotateX(-(10F + j * 50F) * Mth.DEG_TO_RAD)
                .translate(0F, -.4375F, .125F)
                .translate(.032F, .4375F, .125F)
                .scale(
                        RenderRBMKGauge.LINE_SCALE,
                        -RenderRBMKGauge.LINE_SCALE,
                        RenderRBMKGauge.LINE_SCALE)
                .rotateY(90F * Mth.DEG_TO_RAD);
    }

    @Override
    protected void frame(Context context) {
        updateMovingParts(context.partialTick());
        updateText();
    }

    private void updateText() {
        float y = -Minecraft.getInstance().font.lineHeight / 2;
        for (int i = 0; i < gauges.length; i++) {
            GaugeUnit unit = blockEntity.gauges[i];
            if (!textInitialized
                    || unit.active != textActive[i]
                    || unit.min != lastMins[i]
                    || unit.max != lastMaxes[i]
                    || !Objects.equals(unit.label, lastLabels[i])) {
                textActive[i] = unit.active;
                lastMins[i] = unit.min;
                lastMaxes[i] = unit.max;
                lastLabels[i] = unit.label;
                boolean labelled = unit.active && unit.label != null && !unit.label.isEmpty();
                lowers[i].set(
                        unit.active ? Component.literal(RenderRBMKGauge.bound(unit.min)) : null,
                        0F,
                        y,
                        ARGB.opaque(0));
                uppers[i].set(
                        unit.active ? Component.literal(RenderRBMKGauge.bound(unit.max)) : null,
                        0F,
                        y,
                        ARGB.opaque(0));
                labels[i].set(
                        labelled ? Component.literal(unit.label) : null,
                        0F,
                        y,
                        ARGB.opaque(0x00FF00));
            }
            lowers[i].write(lowerPoses[i]);
            uppers[i].write(upperPoses[i]);
            labels[i].write(labelPosings[i]);
        }
        textInitialized = true;
    }

    public void updateMovingParts(float partialTick) {
        for (int i = 0; i < gauges.length; i++) {
            GaugeUnit unit = blockEntity.gauges[i];
            boolean active = unit.active;
            double lower = Math.min(unit.min, unit.max);
            double upper = Math.max(unit.min, unit.max);
            if (lower == upper) upper += 1D;
            double value =
                    unit.lastRenderValue + (unit.renderValue - unit.lastRenderValue) * partialTick;
            double angle = (value - lower) / (upper - lower) * 50D;
            if (unit.min > unit.max) angle = 50D - angle;
            angle = Mth.clamp(angle, 0D, 80D);
            int color = 0xFF000000 | unit.color;
            boolean activeChanged = !initialized || active != lastActive[i];
            boolean angleChanged =
                    !initialized
                            || Double.doubleToLongBits(angle)
                                    != Double.doubleToLongBits(lastAngles[i]);
            boolean colorChanged = !initialized || color != lastColors[i];
            if (activeChanged) writeGauge(i, gaugePoses[i], active);
            if (active && (activeChanged || angleChanged || colorChanged)) {
                needlePoses[i]
                        .set(gaugePoses[i])
                        .translate(0F, .4375F, -.125F)
                        .rotateX((float) (-(angle - 85D)) * Mth.DEG_TO_RAD)
                        .translate(0F, -.4375F, .125F);
                needles[i].setVisible(true);
                world.translation(visualPos.getX(), visualPos.getY(), visualPos.getZ())
                        .mul(needlePoses[i]);
                needles[i]
                        .setTransform(world)
                        .light(LightCoordsUtil.FULL_BRIGHT)
                        .colorArgb(color)
                        .setChanged();
            } else if (!active && activeChanged) {
                needles[i].setVisible(false);
            }
            lastActive[i] = active;
            lastAngles[i] = angle;
            lastColors[i] = color;
        }
        initialized = true;
    }

    private void writeGauge(int index, Matrix4f local, boolean visible) {
        TransformedInstance gauge = gauges[index];
        gauge.setVisible(visible);
        if (!visible) return;
        world.translation(visualPos.getX(), visualPos.getY(), visualPos.getZ()).mul(local);
        gauge.setTransform(world).light(0).setChanged();
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return new AABB(pos).inflate(1);
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        for (TransformedInstance gauge : gauges) consumer.accept(gauge);
    }

    @Override
    protected void _delete() {
        for (TransformedInstance gauge : gauges) gauge.delete();
        for (TransformedInstance needle : needles) needle.delete();
        for (int i = 0; i < gauges.length; i++) {
            lowers[i].delete();
            uppers[i].delete();
            labels[i].delete();
        }
    }
}
