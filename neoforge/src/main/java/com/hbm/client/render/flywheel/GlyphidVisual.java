// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.client.VanishedEntities;
import com.hbm.entity.mob.glyphid.EntityGlyphid;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import dev.engine_room.flywheel.api.instance.Instancer;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.material.Transparency;
import dev.engine_room.flywheel.api.material.WriteMask;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.material.CutoutShaders;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.model.SimpleModel;
import java.util.Arrays;
import java.util.List;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class GlyphidVisual extends HbmDynamicEntityVisual<EntityGlyphid> {
    static final HFRWavefrontObject MESH = ResourceManager.glyphid;
    private static final int LEG_PAIRS = 3;
    private static final double STEPPY = 15D;
    private static final double BEND = 60D;
    static final int[] PART_IDS = partIds();
    private static final int[] ARMOR_BIT = armorBits();
    private static final Material OVERLAY_MATERIAL =
            SimpleMaterial.builder()
                    .transparency(Transparency.ORDER_INDEPENDENT)
                    .writeMask(WriteMask.COLOR)
                    .texture(ResourceManager.glyphid_infestation_tex)
                    .mipmap(false)
                    .backfaceCulling(false)
                    .build();
    static final PackedQuadMesh[] PART_MESHES = buildMeshes();

    static final Model[] GLYPHID = buildSkin(ResourceManager.glyphid_tex);
    static final Model[] GLYPHID_SCOUT = buildSkin(ResourceManager.glyphid_scout_tex);
    static final Model[] GLYPHID_BLASTER = buildSkin(ResourceManager.glyphid_blaster_tex);
    static final Model[] GLYPHID_BOMBARDIER = buildSkin(ResourceManager.glyphid_bombardier_tex);
    static final Model[] GLYPHID_BRAWLER = buildSkin(ResourceManager.glyphid_brawler_tex);
    static final Model[] GLYPHID_BRENDA = buildSkin(ResourceManager.glyphid_brenda_tex);
    static final Model[] GLYPHID_DIGGER = buildSkin(ResourceManager.glyphid_digger_tex);
    static final Model[] GLYPHID_BEHEMOTH = buildSkin(ResourceManager.glyphid_behemoth_tex);
    private static final Model[] OVERLAY_MODELS = buildSkin(OVERLAY_MATERIAL);
    private final Vector3f interpolatedPosition = new Vector3f();
    private final Matrix4f base = new Matrix4f();
    private final Matrix4f scratch = new Matrix4f();
    private final Matrix4f headPose = new Matrix4f();
    private final Matrix4f[] poses = new Matrix4f[PART_IDS.length];
    private final Part[] parts = new Part[PART_IDS.length];
    private int damageTint = OverlayTexture.NO_OVERLAY;
    private boolean shown;
    private float lastX = Float.NaN, lastY, lastZ, lastYaw, lastScale;
    private double lastWalk, lastSwing;
    private int lastLight, lastTint;
    private byte lastArmor;
    private boolean lastShown, lastInfected;

    public GlyphidVisual(
            VisualizationContext ctx, EntityGlyphid entity, float partialTick, Model[] models) {
        super(ctx, entity, partialTick);
        for (int i = 0; i < parts.length; i++) {
            poses[i] = new Matrix4f();
            parts[i] =
                    new Part(
                            instancerProvider()
                                    .instancer(InstanceTypes.TRANSFORMED, models[PART_IDS[i]]),
                            instancerProvider()
                                    .instancer(
                                            InstanceTypes.TRANSFORMED,
                                            OVERLAY_MODELS[PART_IDS[i]]));
        }
        writeFrame(partialTick);
    }

    public static void initModels() {}

    private static int[] partIds() {
        int[] single =
                MESH.partIds(
                        "Body",
                        "ArmorFront",
                        "ArmorLeft",
                        "ArmorRight",
                        "ArmLeftUpper",
                        "ArmLeftMid",
                        "ArmLeftLower",
                        "ArmLeftArmor",
                        "ArmRightUpper",
                        "ArmRightMid",
                        "ArmRightLower",
                        "ArmRightArmor",
                        "JawTop",
                        "JawLeft",
                        "JawRight");
        int[] legs = MESH.partIds("LegLeftUpper", "LegLeftLower", "LegRightUpper", "LegRightLower");
        int[] ids = new int[single.length + LEG_PAIRS * legs.length];
        System.arraycopy(single, 0, ids, 0, single.length);
        for (int pair = 0; pair < LEG_PAIRS; pair++)
            System.arraycopy(legs, 0, ids, single.length + pair * legs.length, legs.length);
        return ids;
    }

    private static int[] armorBits() {
        int[] bits = new int[PART_IDS.length];
        Arrays.fill(bits, -1);
        bits[1] = 0;
        bits[2] = 1;
        bits[3] = 2;
        bits[7] = 3;
        bits[11] = 4;
        return bits;
    }

    static boolean drawn(int slot, byte armor) {
        return ARMOR_BIT[slot] < 0 || (armor & (1 << ARMOR_BIT[slot])) > 0;
    }

    private static PackedQuadMesh[] buildMeshes() {
        var meshes = new PackedQuadMesh[MESH.groups.length];
        for (int i = 0; i < meshes.length; i++) meshes[i] = PackedQuadMesh.of(MESH, i);
        return meshes;
    }

    private static Model[] buildSkin(Identifier texture) {
        return buildSkin(
                SimpleMaterial.builder()
                        .texture(texture)
                        .mipmap(false)
                        .cutout(CutoutShaders.ONE_TENTH)
                        .backfaceCulling(false)
                        .build());
    }

    static Model[] buildSkin(Material material) {
        var models = new Model[PART_MESHES.length];
        for (int i = 0; i < models.length; i++)
            models[i] =
                    new SimpleModel(List.of(new Model.ConfiguredMesh(material, PART_MESHES[i])));
        return models;
    }

    static void skeleton(
            Matrix4f base,
            double walkCycle,
            double swing,
            Matrix4f scratch,
            Matrix4f headPose,
            Matrix4f[] out) {
        double cy0 = Math.sin(walkCycle % (Math.PI * 2));
        double cy1 = Math.sin(walkCycle % (Math.PI * 2) - Math.PI * 0.5);
        double cy2 = Math.sin(walkCycle % (Math.PI * 2) - Math.PI);
        double cy3 = Math.sin(walkCycle % (Math.PI * 2) - Math.PI * 0.75);
        double bite =
                Math.min(1D, Math.max(0D, Math.sin(swing * Math.PI * 2 - Math.PI * 0.5))) * 20D;
        double headTilt = Math.sin(swing * Math.PI) * 30D;

        out[0].set(base);
        out[1].set(base);
        out[2].set(base);
        out[3].set(base);

        scratch.set(base)
                .translate(0.25F, 0.625F, 0.0625F)
                .rotateY((float) Math.toRadians(10D))
                .rotateX((float) Math.toRadians(35D + cy1 * 20D))
                .translate(-0.25F, -0.625F, -0.0625F);
        out[4].set(scratch);
        scratch.translate(0.25F, 0.625F, 0.4375F)
                .rotateX((float) Math.toRadians(-75D - cy1 * 20D + cy0 * 20D))
                .translate(-0.25F, -0.625F, -0.4375F);
        out[5].set(scratch);
        scratch.translate(0.25F, 0.625F, 0.9375F)
                .rotateX((float) Math.toRadians(90D - cy0 * 45D))
                .translate(-0.25F, -0.625F, -0.9375F);
        out[6].set(scratch);
        out[7].set(scratch);

        scratch.set(base)
                .translate(-0.25F, 0.625F, 0.0625F)
                .rotateY((float) Math.toRadians(-10D))
                .rotateX((float) Math.toRadians(35D + cy2 * 20D))
                .translate(0.25F, -0.625F, -0.0625F);
        out[8].set(scratch);
        scratch.translate(-0.25F, 0.625F, 0.4375F)
                .rotateX((float) Math.toRadians(-75D - cy2 * 20D + cy3 * 20D))
                .translate(0.25F, -0.625F, -0.4375F);
        out[9].set(scratch);
        scratch.translate(-0.25F, 0.625F, 0.9375F)
                .rotateX((float) Math.toRadians(90D - cy3 * 45D))
                .translate(0.25F, -0.625F, -0.9375F);
        out[10].set(scratch);
        out[11].set(scratch);

        headPose.set(base)
                .translate(0F, 0.5F, 0.25F)
                .rotateZ((float) Math.toRadians(headTilt))
                .translate(0F, -0.5F, -0.25F);

        out[12].set(headPose)
                .translate(0F, 0.5F, 0.25F)
                .rotateX((float) Math.toRadians(-bite))
                .translate(0F, -0.5F, -0.25F);
        out[13].set(headPose)
                .translate(0F, 0.5F, 0.25F)
                .rotateY((float) Math.toRadians(bite))
                .rotateX((float) Math.toRadians(bite))
                .translate(0F, -0.5F, -0.25F);
        out[14].set(headPose)
                .translate(0F, 0.5F, 0.25F)
                .rotateY((float) Math.toRadians(-bite))
                .rotateX((float) Math.toRadians(bite))
                .translate(0F, -0.5F, -0.25F);

        for (int i = 0; i < LEG_PAIRS; i++) {
            int at = 15 + i * 4;
            double c0 = cy0 * (i == 1 ? -1D : 1D);
            double c1 = cy1 * (i == 1 ? -1D : 1D);

            scratch.set(base)
                    .translate(0F, 0.25F, 0F)
                    .rotateY((float) Math.toRadians(i * 30D - 15D + c0 * 7.5D))
                    .rotateZ((float) Math.toRadians(STEPPY + c1 * STEPPY))
                    .translate(0F, -0.25F, 0F);
            out[at].set(scratch);
            scratch.translate(0.5625F, 0.25F, 0F)
                    .rotateZ((float) Math.toRadians(-BEND - c1 * STEPPY))
                    .translate(-0.5625F, -0.25F, 0F);
            out[at + 1].set(scratch);

            scratch.set(base)
                    .translate(0F, 0.25F, 0F)
                    .rotateY((float) Math.toRadians(i * 30D - 45D + c0 * 7.5D))
                    .rotateZ((float) Math.toRadians(-STEPPY + c1 * STEPPY))
                    .translate(0F, -0.25F, 0F);
            out[at + 2].set(scratch);
            scratch.translate(-0.5625F, 0.25F, 0F)
                    .rotateZ((float) Math.toRadians(BEND - c1 * STEPPY))
                    .translate(0.5625F, -0.25F, 0F);
            out[at + 3].set(scratch);
        }
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
        boolean infected = entity.subtype() == EntityGlyphid.TYPE_INFECTED;
        int light = computePackedLight(partialTick);
        damageTint = ObjEntityVisual.damageOverlay(entity);
        if (visualPos.x == lastX
                && visualPos.y == lastY
                && visualPos.z == lastZ
                && yaw == lastYaw
                && walkCycle == lastWalk
                && swing == lastSwing
                && scale == lastScale
                && armorByte == lastArmor
                && infected == lastInfected
                && light == lastLight
                && damageTint == lastTint
                && shown == lastShown) return;
        lastX = visualPos.x;
        lastY = visualPos.y;
        lastZ = visualPos.z;
        lastYaw = yaw;
        lastWalk = walkCycle;
        lastSwing = swing;
        lastScale = scale;
        lastArmor = armorByte;
        lastInfected = infected;
        lastLight = light;
        lastTint = damageTint;
        lastShown = shown;

        base.translation(visualPos.x, visualPos.y, visualPos.z)
                .rotateY((float) Math.toRadians(-yaw))
                .scale(scale);
        skeleton(base, walkCycle, swing, scratch, headPose, poses);
        for (int i = 0; i < parts.length; i++) {
            boolean drawn = drawn(i, armorByte);
            parts[i].set(poses[i], light, drawn, infected && drawn);
        }
    }

    @Override
    protected void _delete() {
        for (Part part : parts) part.delete();
    }

    private final class Part {
        private final TransformedInstance base;
        private final TransformedInstance overlay;
        private boolean baseShown = true, overlayShown = true;

        private Part(
                Instancer<TransformedInstance> baseInstancer,
                Instancer<TransformedInstance> overlayInstancer) {
            base = baseInstancer.createInstance();
            overlay = overlayInstancer.createInstance();
        }

        private void set(Matrix4f pose, int light, boolean visible, boolean overlayVisible) {
            baseShown = write(base, baseShown, shown && visible, pose, light);
            overlayShown = write(overlay, overlayShown, shown && overlayVisible, pose, light);
        }

        private boolean write(
                TransformedInstance instance,
                boolean wasShown,
                boolean show,
                Matrix4f pose,
                int light) {
            if (show) {
                if (!wasShown) instance.setVisible(true);
                instance.setTransform(pose).overlay(damageTint).light(light).setChanged();
            } else if (wasShown) {
                instance.setVisible(false);
            }
            return show;
        }

        private void delete() {
            base.delete();
            overlay.delete();
        }
    }
}
