// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.blocks.machine.rbmk.RBMKMiniPanelBase;
import com.hbm.main.ResourceManager;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKNumitron.DisplayUnit;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKNumitron;
import com.hbm.util.BobMathUtil;
import com.hbm.util.Facing;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.material.CardinalLightingMode;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.instance.UvTransformedInstance;
import dev.engine_room.flywheel.lib.material.CutoutShaders;
import dev.engine_room.flywheel.lib.material.LightShaders;
import dev.engine_room.flywheel.lib.material.Materials;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.model.SingleMeshModel;
import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

public final class RBMKNumitronVisual extends HbmDynamicBlockEntityVisual<BlockEntityRBMKNumitron>
        implements ShaderLightVisual {
    public static final int DIGITS = 7;
    private static final float SCALE = 200F;
    private static final float W = 8F / SCALE;
    private static final float H = 13F / SCALE;
    private static final float Y_OFFSET = .5625F;
    private static final float PLANE = .03135F;
    private static final MeshPart[] BODY_PARTS =
            MeshPart.objParts(
                    ResourceManager.rbmk_numitron,
                    MeshPart.litCutout(ResourceManager.rbmk_numitron_tex));
    private static final Model GLYPH_MODEL =
            new SingleMeshModel(
                    glyphMesh(),
                    SimpleMaterial.builderOf(Materials.CUTOUT_NO_CULL)
                            .texture(ResourceManager.rbmk_numitron_lights_tex)
                            .mipmap(false)
                            .cutout(CutoutShaders.ONE_TENTH)
                            .light(LightShaders.FLAT)
                            .useLight(false)
                            .useOverlay(false)
                            .cardinalLightingMode(CardinalLightingMode.CHUNK)
                            .ambientOcclusion(false)
                            .backfaceCulling(false)
                            .build());
    private final TransformedInstance[][] bodies;
    private final UvTransformedInstance[][] glyphs;
    private final Matrix4f pose = new Matrix4f();
    private final Matrix4f glyphPose = new Matrix4f();
    private final Matrix4f displayPose = new Matrix4f();
    private final Matrix4f world = new Matrix4f();
    private final long[] lastValues = new long[BlockEntityRBMKNumitron.DISPLAYS];
    private final long[] lastActiveDigits = new long[BlockEntityRBMKNumitron.DISPLAYS];
    private final boolean[] lastActive = new boolean[BlockEntityRBMKNumitron.DISPLAYS];
    private final boolean[] lastLeadingZeroes = new boolean[BlockEntityRBMKNumitron.DISPLAYS];
    private final boolean[] lastShortenNumber = new boolean[BlockEntityRBMKNumitron.DISPLAYS];
    private final boolean[] displayWritten = new boolean[BlockEntityRBMKNumitron.DISPLAYS];
    private final WorldText[] labels = new WorldText[BlockEntityRBMKNumitron.DISPLAYS];
    private final WorldText.Posing[] labelPosings =
            new WorldText.Posing[BlockEntityRBMKNumitron.DISPLAYS];
    private final @Nullable String[] shownLabels = new String[BlockEntityRBMKNumitron.DISPLAYS];
    private float lastYaw;

    public RBMKNumitronVisual(
            VisualizationContext context, BlockEntityRBMKNumitron blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        for (int i = 0; i < labels.length; i++) {
            int display = i;
            labels[i] = new WorldText(instancerProvider(), WorldText.Style.NORMAL, true);
            labelPosings[i] =
                    (width, out) -> {
                        float scale = Math.min(.0125F, .75F / Math.max(width, 1));
                        out.translation(visualPos.getX(), visualPos.getY(), visualPos.getZ())
                                .mul(pose)
                                .translate(.25F, display * -.5F + .25F, 0F)
                                .translate(.01F, .3125F, 0F)
                                .scale(scale, -scale, scale)
                                .rotateY(90F * Mth.DEG_TO_RAD)
                                .translate(-width / 2, 0F, 0F);
                    };
        }
        bodies = new TransformedInstance[BlockEntityRBMKNumitron.DISPLAYS][BODY_PARTS.length];
        for (int i = 0; i < bodies.length; i++)
            for (int j = 0; j < BODY_PARTS.length; j++)
                bodies[i][j] =
                        instancerProvider()
                                .instancer(InstanceTypes.TRANSFORMED, BODY_PARTS[j].model())
                                .createInstance();
        glyphs = new UvTransformedInstance[BlockEntityRBMKNumitron.DISPLAYS][DIGITS];
        for (int i = 0; i < glyphs.length; i++)
            for (int j = 0; j < DIGITS; j++)
                glyphs[i][j] =
                        instancerProvider()
                                .instancer(InstanceTypes.UV_TRANSFORMED, GLYPH_MODEL)
                                .createInstance();
        updateMovingParts(partialTick);
        updateLabels();
    }

    public static void initModels() {}

    private static PackedQuadMesh glyphMesh() {
        return PackedQuadMesh.builder(1)
                .vertex(PLANE, -H + Y_OFFSET, W, 0F, 1F, -1)
                .vertex(PLANE, H + Y_OFFSET, W, 0F, 0F, -1)
                .vertex(PLANE, H + Y_OFFSET, -W, 1F, 0F, -1)
                .vertex(PLANE, -H + Y_OFFSET, -W, 1F, 1F, -1)
                .build();
    }

    private static float glyphU(char c) {
        int digit = c - '0';
        if (digit >= 0 && digit <= 9) return .1F * digit;
        return switch (c) {
            case '.' -> .9F;
            case 'k' -> 0F;
            case 'M' -> .1F;
            case 'G' -> .2F;
            case 'T' -> .3F;
            case 'P' -> .4F;
            case 'E' -> .5F;
            default -> .8F;
        };
    }

    private static float glyphV(char c) {
        int digit = c - '0';
        return digit >= 0 && digit <= 9 ? 0F : .5F;
    }

    private static String format(DisplayUnit unit) {
        String value;
        if (unit.shortenNumber) value = BobMathUtil.getShortNumber(unit.value);
        else if (unit.value > 9999999L) value = "9999999";
        else if (unit.value < -999999L) value = "-999999";
        else value = Long.toString(unit.value);
        if (value.length() < DIGITS && value.charAt(0) == '-' && unit.leadingZeroes) {
            value = value.substring(1);
            while (value.length() < DIGITS - 1) value = "0" + value;
            return "-" + value;
        }
        String fill = unit.leadingZeroes ? "0" : " ";
        while (value.length() < DIGITS) value = fill + value;
        return value;
    }

    @Override
    protected void frame(Context context) {
        updateMovingParts(context.partialTick());
        updateLabels();
    }

    private void updateLabels() {
        float y = -Minecraft.getInstance().font.lineHeight / 2;
        for (int i = 0; i < labels.length; i++) {
            DisplayUnit unit = blockEntity.displays[i];
            String label =
                    unit.active && unit.label != null && !unit.label.isEmpty() ? unit.label : null;
            if (!Objects.equals(label, shownLabels[i])) {
                shownLabels[i] = label;
                labels[i].set(
                        label == null ? null : Component.literal(label),
                        0F,
                        y,
                        ARGB.opaque(0x00FF00));
            }
            labels[i].write(labelPosings[i]);
        }
    }

    public void updateMovingParts(float partialTick) {
        float yaw = Facing.yaw(blockEntity.getBlockState().getValue(RBMKMiniPanelBase.FACING), 90);
        Matrix4f base = pose.identity().translate(.5F, 0F, .5F).rotateY(yaw * Mth.DEG_TO_RAD);
        for (int i = 0; i < bodies.length; i++) {
            DisplayUnit unit = blockEntity.displays[i];
            boolean active = unit.active;
            if (displayWritten[i]
                    && lastYaw == yaw
                    && lastActive[i] == active
                    && (!active
                            || lastValues[i] == unit.value
                                    && lastActiveDigits[i] == unit.activeDigits
                                    && lastLeadingZeroes[i] == unit.leadingZeroes
                                    && lastShortenNumber[i] == unit.shortenNumber)) continue;
            displayPose.set(base).translate(.25F, i * -.5F + .25F, 0F);
            for (int j = 0; j < BODY_PARTS.length; j++) writeBody(i, j, displayPose, active);
            String value = active ? format(unit) : "       ";
            for (int j = 0; j < DIGITS; j++) {
                char c = value.charAt(j);
                boolean present = active && c != ' ' && (unit.activeDigits & (0x40L >> j)) != 0;
                UvTransformedInstance glyph = glyphs[i][j];
                glyph.setVisible(present);
                if (!present) continue;
                glyphPose.set(displayPose).translate(0F, 0F, -(j - 3) * .1F);
                world.translation(visualPos.getX(), visualPos.getY(), visualPos.getZ())
                        .mul(glyphPose);
                glyph.setTransform(world).light(LightCoordsUtil.FULL_BRIGHT);
                glyph.uvRegion(glyphU(c), glyphV(c), .1F, .5F).setChanged();
            }
            lastValues[i] = unit.value;
            lastActiveDigits[i] = unit.activeDigits;
            lastActive[i] = active;
            lastLeadingZeroes[i] = unit.leadingZeroes;
            lastShortenNumber[i] = unit.shortenNumber;
            displayWritten[i] = true;
        }
        lastYaw = yaw;
    }

    private void writeBody(int display, int index, Matrix4f local, boolean visible) {
        TransformedInstance body = bodies[display][index];
        body.setVisible(visible);
        if (!visible) return;
        world.translation(visualPos.getX(), visualPos.getY(), visualPos.getZ()).mul(local);
        body.setTransform(world).light(0).setChanged();
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return new AABB(pos).inflate(1);
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        for (TransformedInstance[] display : bodies)
            for (TransformedInstance body : display) consumer.accept(body);
    }

    @Override
    protected void _delete() {
        for (TransformedInstance[] display : bodies)
            for (TransformedInstance body : display) body.delete();
        for (UvTransformedInstance[] display : glyphs)
            for (UvTransformedInstance glyph : display) glyph.delete();
        for (WorldText label : labels) label.delete();
    }
}
