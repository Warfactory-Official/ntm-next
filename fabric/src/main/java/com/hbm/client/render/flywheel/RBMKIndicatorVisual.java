// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.blocks.machine.rbmk.RBMKMiniPanelBase;
import com.hbm.main.ResourceManager;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKIndicator.IndicatorUnit;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKIndicator;
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
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

public final class RBMKIndicatorVisual extends HbmDynamicBlockEntityVisual<BlockEntityRBMKIndicator>
        implements ShaderLightVisual {
    private static final int INDICATORS = BlockEntityRBMKIndicator.INDICATORS;
    private static final int BASE = ResourceManager.rbmk_indicator.partId("Base");
    private static final int LIGHT = ResourceManager.rbmk_indicator.partId("Light");
    private static final Material BRIGHT_MATERIAL =
            SimpleMaterial.builderOf(Materials.CUTOUT_NO_CULL)
                    .texture(ResourceManager.rbmk_indicator_tex)
                    .mipmap(false)
                    .cutout(CutoutShaders.ONE_TENTH)
                    .useLight(false)
                    .useOverlay(false)
                    .cardinalLightingMode(CardinalLightingMode.OFF)
                    .ambientOcclusion(false)
                    .backfaceCulling(false)
                    .build();
    private static final MeshPart BASE_PART =
            MeshPart.obj(
                    ResourceManager.rbmk_indicator.groups[BASE],
                    ResourceManager.rbmk_indicator.smoothing(),
                    MeshPart.litCutout(ResourceManager.rbmk_indicator_tex));
    private static final PackedQuadMesh LIGHT_MESH =
            PackedQuadMesh.of(
                    ResourceManager.rbmk_indicator.groups[LIGHT],
                    ResourceManager.rbmk_indicator.smoothing());
    private static final MeshPart LIGHT_PART =
            MeshPart.create(LIGHT_MESH, MeshPart.litCutout(ResourceManager.rbmk_indicator_tex));
    private static final Model BRIGHT_MODEL = new SingleMeshModel(LIGHT_MESH, BRIGHT_MATERIAL);
    private final TransformedInstance[] bases = new TransformedInstance[INDICATORS];
    private final TransformedInstance[] lights = new TransformedInstance[INDICATORS];
    private final TransformedInstance[] brights = new TransformedInstance[INDICATORS];
    private final Matrix4f[] localPoses = new Matrix4f[INDICATORS];
    private final Matrix4f world = new Matrix4f();
    private final boolean[] lastBaseVisible = new boolean[INDICATORS];
    private final boolean[] lastLightVisible = new boolean[INDICATORS];
    private final boolean[] lastBrightVisible = new boolean[INDICATORS];
    private final int[] lastLightColor = new int[INDICATORS];
    private final int[] lastBrightColor = new int[INDICATORS];
    private final WorldText[] labels = new WorldText[INDICATORS];
    private final WorldText.Posing[] labelPosings = new WorldText.Posing[INDICATORS];
    private final String[] lastLabels = new String[INDICATORS];
    private final boolean[] textActive = new boolean[INDICATORS];
    private boolean initialized;
    private boolean textInitialized;

    public RBMKIndicatorVisual(
            VisualizationContext context, BlockEntityRBMKIndicator blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        Direction facing = blockState.getValue(RBMKMiniPanelBase.FACING);
        Matrix4f root =
                new Matrix4f()
                        .translate(.5F, 0F, .5F)
                        .rotateY(Facing.yaw(facing, 90) * Mth.DEG_TO_RAD);
        for (int i = 0; i < INDICATORS; i++) {
            bases[i] =
                    instancerProvider()
                            .instancer(InstanceTypes.TRANSFORMED, BASE_PART.model())
                            .createInstance();
            lights[i] =
                    instancerProvider()
                            .instancer(InstanceTypes.TRANSFORMED, LIGHT_PART.model())
                            .createInstance();
            brights[i] =
                    instancerProvider()
                            .instancer(InstanceTypes.TRANSFORMED, BRIGHT_MODEL)
                            .createInstance();
            localPoses[i] =
                    new Matrix4f(root)
                            .translate(.25F, (i / 2) * -.3125F + .3125F, (i % 2) * -.5F + .25F);
            labels[i] = new WorldText(instancerProvider(), WorldText.Style.NORMAL, false);
            Matrix4f local = localPoses[i];
            labelPosings[i] =
                    (width, out) -> {
                        float scale = Math.min(.0125F, .3F / Math.max(width, 1));
                        out.translation(visualPos.getX(), visualPos.getY(), visualPos.getZ())
                                .mul(local)
                                .translate(.0725F, .5F, 0F)
                                .scale(scale, -scale, scale)
                                .rotateY(90F * Mth.DEG_TO_RAD)
                                .translate(-width / 2, 0F, 0F);
                    };
        }
        updateMovingParts(partialTick);
        updateText();
    }

    public static void initModels() {}

    private static int dim(int color, float mult) {
        return ARGB.colorFromFloat(
                1F,
                ((color >> 16) & 0xFF) / 255F * mult,
                ((color >> 8) & 0xFF) / 255F * mult,
                (color & 0xFF) / 255F * mult);
    }

    @Override
    protected void frame(Context context) {
        updateMovingParts(context.partialTick());
        updateText();
    }

    private void updateText() {
        float y = -Minecraft.getInstance().font.lineHeight / 2;
        for (int i = 0; i < INDICATORS; i++) {
            IndicatorUnit unit = blockEntity.indicators[i];
            if (!textInitialized
                    || unit.active != textActive[i]
                    || !unit.label.equals(lastLabels[i])) {
                textActive[i] = unit.active;
                lastLabels[i] = unit.label;
                labels[i].set(
                        unit.active && !unit.label.isEmpty() ? Component.literal(unit.label) : null,
                        0F,
                        y,
                        ARGB.opaque(0));
            }
            labels[i].write(labelPosings[i]);
        }
        textInitialized = true;
    }

    public void updateMovingParts(float partialTick) {
        for (int i = 0; i < INDICATORS; i++) {
            IndicatorUnit unit = blockEntity.indicators[i];
            boolean baseVisible = unit.active;
            boolean lightVisible = unit.active && !unit.light;
            boolean brightVisible = unit.active && unit.light;
            int lightColor = dim(unit.color, .35F);
            int brightColor = unit.color;
            if (!initialized || baseVisible != lastBaseVisible[i])
                writePhysical(bases[i], localPoses[i], baseVisible, -1);
            if (!initialized
                    || lightVisible != lastLightVisible[i]
                    || lightColor != lastLightColor[i])
                writePhysical(lights[i], localPoses[i], lightVisible, lightColor);
            if (!initialized
                    || brightVisible != lastBrightVisible[i]
                    || brightColor != lastBrightColor[i])
                writeBright(brights[i], localPoses[i], brightVisible, brightColor);
            lastBaseVisible[i] = baseVisible;
            lastLightVisible[i] = lightVisible;
            lastBrightVisible[i] = brightVisible;
            lastLightColor[i] = lightColor;
            lastBrightColor[i] = brightColor;
        }
        initialized = true;
    }

    private void writePhysical(
            TransformedInstance instance, Matrix4f local, boolean visible, int color) {
        instance.setVisible(visible);
        if (!visible) return;
        world.translation(visualPos.getX(), visualPos.getY(), visualPos.getZ()).mul(local);
        instance.setTransform(world).light(0).colorArgb(color).setChanged();
    }

    private void writeBright(
            TransformedInstance instance, Matrix4f local, boolean visible, int color) {
        instance.setVisible(visible);
        if (!visible) return;
        world.translation(visualPos.getX(), visualPos.getY(), visualPos.getZ()).mul(local);
        instance.setTransform(world)
                .light(LightCoordsUtil.FULL_BRIGHT)
                .colorArgb(0xFF000000 | color)
                .setChanged();
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return new AABB(pos).inflate(1);
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        for (int i = 0; i < INDICATORS; i++) {
            consumer.accept(bases[i]);
            consumer.accept(lights[i]);
        }
    }

    @Override
    protected void _delete() {
        for (int i = 0; i < INDICATORS; i++) {
            bases[i].delete();
            lights[i].delete();
            brights[i].delete();
            labels[i].delete();
        }
    }
}
