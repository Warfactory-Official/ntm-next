// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.client.VanishedEntities;
import com.hbm.entity.mob.glyphid.EntityGlyphidNuclear;
import com.hbm.lib.Library;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import dev.engine_room.flywheel.api.instance.Instancer;
import dev.engine_room.flywheel.api.material.CardinalLightingMode;
import dev.engine_room.flywheel.api.material.DepthTest;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.material.Transparency;
import dev.engine_room.flywheel.api.material.WriteMask;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.material.CutoutShaders;
import dev.engine_room.flywheel.lib.material.Materials;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.model.SimpleModel;
import java.util.List;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class GlyphidNuclearVisual extends HbmDynamicEntityVisual<EntityGlyphidNuclear> {
    private static final HFRWavefrontObject PROJECTILES = ResourceManager.projectiles;
    private static final Identifier MINI_NUKE_TEX =
            Library.id("textures/models/projectiles/mini_nuke.png");
    private static final Material FLASH_MATERIAL =
            SimpleMaterial.builder()
                    .transparency(Transparency.ORDER_INDEPENDENT)
                    .writeMask(WriteMask.COLOR)
                    .depthTest(DepthTest.EQUAL)
                    .useLight(false)
                    .texture(ResourceManager.white_tex)
                    .mipmap(false)
                    .backfaceCulling(false)
                    .build();
    private static final Material MAIN_MATERIAL =
            SimpleMaterial.builder()
                    .texture(ResourceManager.glyphid_nuclear_tex)
                    .mipmap(false)
                    .cutout(CutoutShaders.ONE_TENTH)
                    .backfaceCulling(false)
                    .build();
    private static final Model[] MAIN_MODELS = GlyphidVisual.buildSkin(MAIN_MATERIAL);
    private static final Model[] FLASH_MODELS = GlyphidVisual.buildSkin(FLASH_MATERIAL);
    private static final PackedQuadMesh MINI_NUKE_MESH =
            PackedQuadMesh.of(PROJECTILES, PROJECTILES.partId("MiniNuke"));
    private static final Model MINI_NUKE_MODEL =
            new SimpleModel(
                    List.of(
                            new Model.ConfiguredMesh(
                                    SimpleMaterial.builderOf(Materials.CUTOUT)
                                            .texture(MINI_NUKE_TEX)
                                            .mipmap(false)
                                            .cutout(CutoutShaders.ONE_TENTH)
                                            .cardinalLightingMode(CardinalLightingMode.ENTITY)
                                            .backfaceCulling(false)
                                            .build(),
                                    MINI_NUKE_MESH)));
    private static final Model MINI_NUKE_FLASH_MODEL =
            new SimpleModel(List.of(new Model.ConfiguredMesh(FLASH_MATERIAL, MINI_NUKE_MESH)));
    private final Vector3f interpolatedPosition = new Vector3f();
    private final Matrix4f base = new Matrix4f();
    private final Matrix4f scratch = new Matrix4f();
    private final Matrix4f headPose = new Matrix4f();
    private final Matrix4f[] poses = new Matrix4f[GlyphidVisual.PART_IDS.length];
    private final Part[] parts = new Part[GlyphidVisual.PART_IDS.length];
    private final Part miniNuke;
    private int damageTint = OverlayTexture.NO_OVERLAY;
    private boolean shown;

    public GlyphidNuclearVisual(
            VisualizationContext ctx, EntityGlyphidNuclear entity, float partialTick) {
        super(ctx, entity, partialTick);
        for (int i = 0; i < parts.length; i++) {
            int id = GlyphidVisual.PART_IDS[i];
            poses[i] = new Matrix4f();
            parts[i] =
                    new Part(
                            instancerProvider()
                                    .instancer(InstanceTypes.TRANSFORMED, MAIN_MODELS[id]),
                            instancerProvider()
                                    .instancer(InstanceTypes.TRANSFORMED, FLASH_MODELS[id]));
        }
        miniNuke =
                new Part(
                        instancerProvider().instancer(InstanceTypes.TRANSFORMED, MINI_NUKE_MODEL),
                        instancerProvider()
                                .instancer(InstanceTypes.TRANSFORMED, MINI_NUKE_FLASH_MODEL));
        writeFrame(partialTick);
    }

    public static void initModels() {}

    private static int flashPackedAlpha(float swell) {
        int a = (int) (swell * 0.2F * 255.0F);

        if ((int) (swell * 10.0F) % 4 < 2) return 0;

        if (a < 0) a = 0;
        if (a > 255) a = 255;
        return a;
    }

    @Override
    protected void frame(DynamicVisual.Context ctx) {
        writeFrame(ctx.partialTick());
    }

    private void writeFrame(float partialTick) {
        shown = !VanishedEntities.isVanished(entity);
        Vector3f visualPos = getVisualPosition(partialTick, interpolatedPosition);
        float yaw = Mth.rotLerp(partialTick, entity.yBodyRotO, entity.yBodyRot);
        double walkCycle = entity.walkAnimation.position(partialTick);
        double swing = entity.getAttackAnim(partialTick);
        float scale = (float) entity.getGlyphidScale();
        byte armorByte = entity.armor();

        float deathAge = entity.deathTicks + partialTick;

        float swell = deathAge / 95F;
        float flash = 1.0F + Mth.sin(swell * 100.0F) * swell * 0.01F;
        if (swell < 0.0F) swell = 0.0F;
        if (swell > 1.0F) swell = 1.0F;
        swell *= swell;
        swell *= swell;
        float scaleHorizontal = (1.0F + swell * 0.4F) * flash;
        float scaleVertical = (1.0F + swell * 0.1F) / flash;

        int packedAlpha = flashPackedAlpha(deathAge / 20F);
        int flashColor = ARGB.color(packedAlpha, 255, 255, 255);
        boolean flashOn = packedAlpha > 0;

        int light = computePackedLight(partialTick);
        damageTint = ObjEntityVisual.damageOverlay(entity);

        base.translation(visualPos.x, visualPos.y, visualPos.z)
                .rotateY((float) Math.toRadians(-yaw))
                .scale(scaleHorizontal, scaleVertical, scaleHorizontal)
                .scale(scale);
        GlyphidVisual.skeleton(base, walkCycle, swing, scratch, headPose, poses);
        for (int i = 0; i < parts.length; i++) {
            boolean drawn = GlyphidVisual.drawn(i, armorByte);
            parts[i].set(poses[i], light, drawn, flashColor, flashOn && drawn);
        }

        scratch.set(base).translate(0F, 1F, 0F).rotateX((float) Math.toRadians(90D));
        miniNuke.set(scratch, light, true, flashColor, flashOn);
    }

    @Override
    protected void _delete() {
        for (Part part : parts) part.delete();
        miniNuke.delete();
    }

    private final class Part {
        private final TransformedInstance base;
        private final TransformedInstance flash;
        private boolean baseShown = true, flashShown = true;

        private Part(
                Instancer<TransformedInstance> baseInstancer,
                Instancer<TransformedInstance> flashInstancer) {
            base = baseInstancer.createInstance();
            flash = flashInstancer.createInstance();
        }

        private void set(
                Matrix4f pose, int light, boolean visible, int flashColor, boolean flashVisible) {
            boolean showBase = shown && visible;
            if (showBase) {
                if (!baseShown) base.setVisible(true);
                base.setTransform(pose).overlay(damageTint).light(light).setChanged();
            } else if (baseShown) {
                base.setVisible(false);
            }
            baseShown = showBase;
            boolean showFlash = shown && flashVisible;
            if (showFlash) {
                if (!flashShown) flash.setVisible(true);
                flash.setTransform(pose);
                flash.colorArgb(flashColor);
                flash.setChanged();
            } else if (flashShown) {
                flash.setVisible(false);
            }
            flashShown = showFlash;
        }

        private void delete() {
            base.delete();
            flash.delete();
        }
    }
}
