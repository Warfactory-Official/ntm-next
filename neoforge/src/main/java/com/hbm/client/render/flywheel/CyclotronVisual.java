// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.client.render.RenderCyclotron;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.machine.BlockEntityMachineCyclotron;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

public final class CyclotronVisual extends HbmDynamicBlockEntityVisual<BlockEntityMachineCyclotron>
        implements ShaderLightVisual {
    private static final int BAYS = BlockEntityMachineCyclotron.PLUGS;
    private static final HFRWavefrontObject MODEL = ResourceManager.cyclotron;
    private static final MeshPart[][] PARTS = buildParts();
    private static final int MOTTO = RenderCyclotron.MOTTO.length();
    private final AABB bodyBounds;
    private final TransformedInstance[][] sheets = new TransformedInstance[BAYS][2];
    private final Matrix4f localPose = new Matrix4f();
    private final Matrix4f instancePose = new Matrix4f();
    private final double[] lightBounds = new double[6];
    private @Nullable AABB lastLightBounds;
    private int lastMask;
    private boolean initialized;
    private final WorldText[] motto = new WorldText[MOTTO];
    private final Component[] glyphs = new Component[MOTTO];
    private final Component[] advances = new Component[MOTTO];
    private final WorldText.Posing[] mottoPosings = new WorldText.Posing[MOTTO];
    private final Matrix4f ring = new Matrix4f();
    private float glyphYaw;
    private boolean mottoShown;

    public CyclotronVisual(
            VisualizationContext context,
            BlockEntityMachineCyclotron blockEntity,
            float partialTick) {
        super(context, blockEntity, partialTick);
        localPose.translation(.5F, 0F, .5F);
        bodyBounds = LightBounds.of(MODEL, "Body", localPose, pos);
        for (int i = 0; i < BAYS; i++) {
            for (int j = 0; j < 2; j++) {
                sheets[i][j] =
                        instancerProvider()
                                .instancer(InstanceTypes.TRANSFORMED, PARTS[i][j].model())
                                .createInstance();
                sheets[i][j].setVisible(false);
            }
        }
        for (int i = 0; i < MOTTO; i++) {
            String glyph = String.valueOf(RenderCyclotron.MOTTO.charAt(i));
            motto[i] = new WorldText(instancerProvider(), WorldText.Style.ADDITIVE, false);
            glyphs[i] =
                    Component.literal(glyph)
                            .withStyle(Style.EMPTY.withFont(RenderCyclotron.ALT_FONT));
            advances[i] = Component.literal(glyph);
            mottoPosings[i] =
                    (width, out) -> {
                        out.set(ring)
                                .rotateY(glyphYaw * Mth.DEG_TO_RAD)
                                .translate((float) RenderCyclotron.RING_RADIUS, 0F, 0F)
                                .rotateY(-90F * Mth.DEG_TO_RAD)
                                .scale(
                                        RenderCyclotron.RING_SCALE,
                                        RenderCyclotron.RING_SCALE,
                                        RenderCyclotron.RING_SCALE);
                        glyphYaw -= width * 2F;
                    };
        }
        updateMovingParts(partialTick);
        updateMotto();
    }

    public static void initModels() {}

    private static MeshPart[][] buildParts() {
        Identifier[] empty = {
            ResourceManager.cyclotron_ashes_tex, ResourceManager.cyclotron_book_tex,
            ResourceManager.cyclotron_gavel_tex, ResourceManager.cyclotron_coin_tex
        };
        Identifier[] filled = {
            ResourceManager.cyclotron_ashes_filled_tex, ResourceManager.cyclotron_book_filled_tex,
            ResourceManager.cyclotron_gavel_filled_tex, ResourceManager.cyclotron_coin_filled_tex
        };
        MeshPart[][] parts = new MeshPart[BAYS][2];
        for (int i = 0; i < BAYS; i++) {
            var group = MODEL.groups[MODEL.partId("B" + (i + 1))];
            parts[i][0] =
                    MeshPart.obj(
                            group,
                            false,
                            SimpleMaterial.builderOf(MeshPart.litCutout(empty[i]))
                                    .backfaceCulling(false)
                                    .build());
            parts[i][1] =
                    MeshPart.obj(
                            group,
                            false,
                            SimpleMaterial.builderOf(MeshPart.litCutout(filled[i]))
                                    .backfaceCulling(false)
                                    .build());
        }
        return parts;
    }

    @Override
    protected void frame(Context context) {
        updateMovingParts(context.partialTick());
        updateMotto();
    }

    private void updateMotto() {
        boolean shown = lastMask == (1 << BAYS) - 1;
        if (shown != mottoShown) {
            mottoShown = shown;
            for (int i = 0; i < MOTTO; i++) {
                motto[i].set(
                        shown ? glyphs[i] : null,
                        advances[i],
                        0F,
                        0F,
                        ARGB.opaque(RenderCyclotron.MOTTO_COLOR));
            }
        }
        ring.translation(visualPos.getX(), visualPos.getY(), visualPos.getZ())
                .translate(.5F, 0F, .5F)
                .rotateY(RenderCyclotron.ringYaw() * Mth.DEG_TO_RAD)
                .translate(0F, (float) RenderCyclotron.RING_LIFT, 0F)
                .rotateX(180F * Mth.DEG_TO_RAD);
        glyphYaw = 0F;
        for (int i = 0; i < MOTTO; i++) motto[i].write(mottoPosings[i]);
    }

    public void updateMovingParts(float partialTick) {
        int mask = 0;
        for (int i = 0; i < BAYS; i++) if (blockEntity.getPlug(i)) mask |= 1 << i;
        if (initialized && mask == lastMask) return;
        instancePose
                .translation(visualPos.getX(), visualPos.getY(), visualPos.getZ())
                .mul(localPose);
        LightBounds.resetBounds(lightBounds, bodyBounds);
        for (int i = 0; i < BAYS; i++) {
            int selected = (mask & (1 << i)) != 0 ? 1 : 0;
            for (int j = 0; j < 2; j++) {
                boolean visible = j == selected;
                TransformedInstance sheet = sheets[i][j];
                sheet.setVisible(visible);
                if (visible) {
                    sheet.setTransform(instancePose).light(0).setChanged();
                    LightBounds.includeLightBounds(
                            lightBounds, PARTS[i][j].model(), localPose, pos);
                }
            }
        }
        lastLightBounds = LightBounds.sections(lightSections, lightBounds, lastLightBounds);
        lastMask = mask;
        initialized = true;
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return bodyBounds.minmax(
                new AABB(
                                pos.getX() - 2,
                                pos.getY(),
                                pos.getZ() - 2,
                                pos.getX() + 3,
                                pos.getY() + 4,
                                pos.getZ() + 3)
                        .inflate(1));
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        for (var bay : sheets) for (var sheet : bay) consumer.accept(sheet);
    }

    @Override
    protected void _delete() {
        for (int i = 0; i < BAYS; i++) for (int j = 0; j < 2; j++) sheets[i][j].delete();
        for (WorldText glyph : motto) glyph.delete();
    }
}
