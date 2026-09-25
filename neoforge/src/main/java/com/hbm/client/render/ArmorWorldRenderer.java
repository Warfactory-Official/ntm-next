// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.items.ModItems;
import com.hbm.items.armor.ModArmorItem.Suit;
import com.hbm.items.armor.ModArmorItem;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.util.GameTime;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

public final class ArmorWorldRenderer {

    private static final int HELMET = 0,
            CHEST = 1,
            LEFT_ARM = 2,
            RIGHT_ARM = 3,
            LEFT_LEG = 4,
            RIGHT_LEG = 5,
            LEFT_BOOT = 6,
            RIGHT_BOOT = 7;

    private static final Map<Suit, MeshSet> MESH_SETS = new EnumMap<>(Suit.class);

    static {
        register(
                Suit.T51,
                ResourceManager.armor_t51,
                ResourceManager.t51_helmet_tex,
                ResourceManager.t51_chest_tex,
                ResourceManager.t51_arm_tex,
                ResourceManager.t51_leg_tex,
                "Helmet",
                "Chest",
                "LeftArm",
                "RightArm",
                "LeftLeg",
                "RightLeg",
                "LeftBoot",
                "RightBoot",
                0F);
        register(
                Suit.TAURUN,
                ResourceManager.armor_taurun,
                ResourceManager.taurun_helmet_tex,
                ResourceManager.taurun_chest_tex,
                ResourceManager.taurun_arm_tex,
                ResourceManager.taurun_leg_tex,
                "Helmet",
                "Chest",
                "LeftArm",
                "RightArm",
                "LeftLeg",
                "RightLeg",
                "LeftBoot",
                "RightBoot",
                .01F);
        register(
                Suit.AJR,
                ResourceManager.armor_ajr,
                ResourceManager.ajr_helmet_tex,
                ResourceManager.ajr_chest_tex,
                ResourceManager.ajr_arm_tex,
                ResourceManager.ajr_leg_tex,
                "Head",
                "Body",
                "LeftArm",
                "RightArm",
                "LeftLeg",
                "RightLeg",
                "LeftBoot",
                "RightBoot",
                0F);

        register(
                Suit.AJRO,
                ResourceManager.armor_ajr,
                ResourceManager.ajro_helmet_tex,
                ResourceManager.ajro_chest_tex,
                ResourceManager.ajro_arm_tex,
                ResourceManager.ajro_leg_tex,
                "Head",
                "Body",
                "LeftArm",
                "RightArm",
                "LeftLeg",
                "RightLeg",
                "LeftBoot",
                "RightBoot",
                0F);

        register(
                Suit.RPA,
                ResourceManager.armor_remnant,
                ResourceManager.rpa_helmet_tex,
                ResourceManager.rpa_chest_tex,
                ResourceManager.rpa_arm_tex,
                ResourceManager.rpa_leg_tex,
                "Head",
                "Body",
                "LeftArm",
                "RightArm",
                "LeftLeg",
                "RightLeg",
                "LeftBoot",
                "RightBoot",
                0F,
                extra(CHEST, "Glow", ResourceManager.rpa_chest_tex, true, null),
                spinning(CHEST, "Fan", ResourceManager.rpa_chest_tex));

        register(
                Suit.NCRPA,
                ResourceManager.armor_ncr,
                ResourceManager.ncrpa_helmet_tex,
                ResourceManager.ncrpa_chest_tex,
                ResourceManager.ncrpa_arm_tex,
                ResourceManager.ncrpa_leg_tex,
                "Helmet",
                "Chest",
                "LeftArm",
                "RightArm",
                "LeftLeg",
                "RightLeg",
                "LeftBoot",
                "RightBoot",
                0F,
                extra(HELMET, "Eyes", ResourceManager.ncrpa_helmet_tex, true, null));

        register(
                Suit.BISMUTH,
                ResourceManager.armor_bismuth,
                ResourceManager.bismuth_tex,
                ResourceManager.bismuth_tex,
                ResourceManager.bismuth_tex,
                ResourceManager.bismuth_tex,
                "Head",
                "Body",
                "LeftArm",
                "RightArm",
                "LeftLeg",
                "RightLeg",
                "LeftFoot",
                "RightFoot",
                0F);

        register(
                Suit.BJ,
                ResourceManager.armor_bj,
                ResourceManager.bj_eyepatch_tex,
                ResourceManager.bj_chest_tex,
                ResourceManager.bj_arm_tex,
                ResourceManager.bj_leg_tex,
                "Head",
                "Body",
                "LeftArm",
                "RightArm",
                "LeftLeg",
                "RightLeg",
                "LeftFoot",
                "RightFoot",
                0F,
                extra(
                        CHEST,
                        "Jetpack",
                        ResourceManager.bj_jetpack_tex,
                        false,
                        ModItems.BJ_PLATE_JETPACK));

        register(
                Suit.ENVSUIT,
                ResourceManager.armor_envsuit,
                ResourceManager.envsuit_helmet_tex,
                ResourceManager.envsuit_chest_tex,
                ResourceManager.envsuit_arm_tex,
                ResourceManager.envsuit_leg_tex,
                "Helmet",
                "Chest",
                "LeftArm",
                "RightArm",
                "LeftLeg",
                "RightLeg",
                "LeftFoot",
                "RightFoot",
                0F,
                tinted(HELMET, "Lamps", ResourceManager.white_tex, 0xFFFFFFCC));
        register(
                Suit.HEV,
                ResourceManager.armor_hev,
                ResourceManager.hev_helmet_tex,
                ResourceManager.hev_chest_tex,
                ResourceManager.hev_arm_tex,
                ResourceManager.hev_leg_tex,
                "Head",
                "Body",
                "LeftArm",
                "RightArm",
                "LeftLeg",
                "RightLeg",
                "LeftFoot",
                "RightFoot",
                0F);

        register(
                Suit.DIGAMMA,
                ResourceManager.armor_fau,
                ResourceManager.fau_helmet_tex,
                ResourceManager.fau_chest_tex,
                ResourceManager.fau_arm_tex,
                ResourceManager.fau_leg_tex,
                "Head",
                "Body",
                "LeftArm",
                "RightArm",
                "LeftLeg",
                "RightLeg",
                "LeftBoot",
                "RightBoot",
                0F,
                extra(CHEST, "Cassette", ResourceManager.fau_cassette_tex, false, null));
        register(
                Suit.DNS,
                ResourceManager.armor_dnt,
                ResourceManager.dns_helmet_tex,
                ResourceManager.dns_chest_tex,
                ResourceManager.dns_arm_tex,
                ResourceManager.dns_leg_tex,
                "Head",
                "Body",
                "LeftArm",
                "RightArm",
                "LeftLeg",
                "RightLeg",
                "LeftBoot",
                "RightBoot",
                0F);

        register(
                Suit.TRENCHMASTER,
                ResourceManager.armor_trenchmaster,
                ResourceManager.trenchmaster_helmet_tex,
                ResourceManager.trenchmaster_chest_tex,
                ResourceManager.trenchmaster_arm_tex,
                ResourceManager.trenchmaster_leg_tex,
                "Helmet",
                "Chest",
                "LeftArm",
                "RightArm",
                "LeftLeg",
                "RightLeg",
                "LeftBoot",
                "RightBoot",
                0F,
                extra(HELMET, "Light", ResourceManager.trenchmaster_helmet_tex, true, null));

        register(
                Suit.DESH,
                ResourceManager.armor_steamsuit,
                ResourceManager.steamsuit_helmet_tex,
                ResourceManager.steamsuit_chest_tex,
                ResourceManager.steamsuit_arm_tex,
                ResourceManager.steamsuit_leg_tex,
                "Head",
                "Body",
                "LeftArm",
                "RightArm",
                "LeftLeg",
                "RightLeg",
                "LeftBoot",
                "RightBoot",
                0F);
        register(
                Suit.DIESEL,
                ResourceManager.armor_bnuuy,
                ResourceManager.bnuuy_helmet_tex,
                ResourceManager.bnuuy_chest_tex,
                ResourceManager.bnuuy_arm_tex,
                ResourceManager.bnuuy_leg_tex,
                "Head",
                "Body",
                "LeftArm",
                "RightArm",
                "LeftLeg",
                "RightLeg",
                "LeftBoot",
                "RightBoot",
                0F);
    }

    private static ExtraSpec extra(
            int anchor,
            String group,
            Identifier texture,
            boolean fullbright,
            @Nullable ItemLike onlyIfItem) {
        return new ExtraSpec(anchor, group, texture, fullbright, -1, false, onlyIfItem);
    }

    private static ExtraSpec tinted(int anchor, String group, Identifier texture, int argb) {
        return new ExtraSpec(anchor, group, texture, true, argb, false, null);
    }

    private static ExtraSpec spinning(int anchor, String group, Identifier texture) {
        return new ExtraSpec(anchor, group, texture, false, -1, true, null);
    }

    private static void register(
            Suit suit,
            HFRWavefrontObject mesh,
            Identifier helmetTex,
            Identifier chestTex,
            Identifier armTex,
            Identifier legTex,
            String helmet,
            String chest,
            String leftArm,
            String rightArm,
            String leftLeg,
            String rightLeg,
            String leftBoot,
            String rightBoot,
            float legXOffset,
            ExtraSpec... extraSpecs) {
        int[] parts =
                mesh.partIds(
                        new String[] {
                            helmet, chest, leftArm, rightArm, leftLeg, rightLeg, leftBoot, rightBoot
                        });
        List<ExtraPart> extras = new ArrayList<>(extraSpecs.length);
        for (ExtraSpec spec : extraSpecs) {
            int group = mesh.partIds(new String[] {spec.group})[0];
            extras.add(
                    new ExtraPart(
                            spec.anchor,
                            group,
                            spec.texture,
                            spec.fullbright,
                            spec.tint,
                            spec.spin,
                            spec.onlyIfItem == null ? null : spec.onlyIfItem.asItem()));
        }
        MESH_SETS.put(
                suit,
                new MeshSet(mesh, helmetTex, chestTex, armTex, legTex, parts, legXOffset, extras));
    }

    private ArmorWorldRenderer() {}

    public static boolean hidesSkin(ItemStack stack) {
        return stack.getItem() instanceof ModArmorItem armor && MESH_SETS.containsKey(armor.suit());
    }

    public static void submit(
            PoseStack pose,
            SubmitNodeCollector collector,
            int light,
            HumanoidModel<HumanoidRenderState> model,
            HumanoidRenderState state,
            EquipmentSlot slot,
            Suit suit,
            ItemStack stack) {
        MeshSet set = MESH_SETS.get(suit);
        if (set == null) return;

        pose.pushPose();
        switch (slot) {
            case HEAD -> {
                limb(
                        pose,
                        collector,
                        light,
                        set.mesh,
                        model.head,
                        Bone.HEAD,
                        state,
                        set.helmetTex,
                        set.parts[HELMET],
                        0F);
                extras(pose, collector, light, set, HELMET, model.head, Bone.HEAD, state, stack);
            }
            case CHEST -> {
                limb(
                        pose,
                        collector,
                        light,
                        set.mesh,
                        model.body,
                        Bone.BODY,
                        state,
                        set.chestTex,
                        set.parts[CHEST],
                        0F);
                limb(
                        pose,
                        collector,
                        light,
                        set.mesh,
                        model.leftArm,
                        Bone.LEFT_ARM,
                        state,
                        set.armTex,
                        set.parts[LEFT_ARM],
                        0F);
                limb(
                        pose,
                        collector,
                        light,
                        set.mesh,
                        model.rightArm,
                        Bone.RIGHT_ARM,
                        state,
                        set.armTex,
                        set.parts[RIGHT_ARM],
                        0F);
                extras(pose, collector, light, set, CHEST, model.body, Bone.BODY, state, stack);
            }
            case LEGS -> {
                limb(
                        pose,
                        collector,
                        light,
                        set.mesh,
                        model.leftLeg,
                        Bone.LEFT_LEG,
                        state,
                        set.legTex,
                        set.parts[LEFT_LEG],
                        -set.legXOffset);
                limb(
                        pose,
                        collector,
                        light,
                        set.mesh,
                        model.rightLeg,
                        Bone.RIGHT_LEG,
                        state,
                        set.legTex,
                        set.parts[RIGHT_LEG],
                        set.legXOffset);
            }
            case FEET -> {
                limb(
                        pose,
                        collector,
                        light,
                        set.mesh,
                        model.leftLeg,
                        Bone.LEFT_LEG,
                        state,
                        set.legTex,
                        set.parts[LEFT_BOOT],
                        -set.legXOffset);
                limb(
                        pose,
                        collector,
                        light,
                        set.mesh,
                        model.rightLeg,
                        Bone.RIGHT_LEG,
                        state,
                        set.legTex,
                        set.parts[RIGHT_BOOT],
                        set.legXOffset);
            }
            default -> {}
        }
        pose.popPose();
    }

    public static void submitGroup(
            PoseStack pose,
            SubmitNodeCollector collector,
            int light,
            HFRWavefrontObject mesh,
            ModelPart limb,
            Bone bone,
            HumanoidRenderState state,
            Identifier texture,
            int group) {
        limb(pose, collector, light, mesh, limb, bone, state, texture, group, 0F);
    }

    public static void enterBone(
            PoseStack pose, ModelPart limb, Bone bone, HumanoidRenderState state) {
        enterFrame(pose, limb, bone, state);
        restOffset(pose, limb, bone, state, 0.0625F);
    }

    public static void enterFrame(
            PoseStack pose, ModelPart limb, Bone bone, HumanoidRenderState state) {
        limb.translateAndRotate(pose);
        if (!fitted(state)) return;
        Fit fit = fit(limb, bone);
        pose.translate(fit.anchor.x() / 16F, fit.anchor.y() / 16F, fit.anchor.z() / 16F);
        pose.scale(fit.scale, fit.scale, fit.scale);
        pose.translate(-bone.anchor.x() / 16F, -bone.anchor.y() / 16F, -bone.anchor.z() / 16F);
    }

    private static boolean fitted(HumanoidRenderState state) {
        return state.entityType != EntityTypes.ARMOR_STAND;
    }

    private static float scale(ModelPart limb, Bone bone, HumanoidRenderState state) {
        return fitted(state) ? fit(limb, bone).scale : limb.getInitialPose().xScale();
    }

    private static void extras(
            PoseStack pose,
            SubmitNodeCollector collector,
            int light,
            MeshSet set,
            int anchor,
            ModelPart anchorLimb,
            Bone bone,
            HumanoidRenderState state,
            ItemStack stack) {
        for (ExtraPart extra : set.extras) {
            if (extra.anchor != anchor) continue;
            if (extra.onlyIfItem != null && !stack.is(extra.onlyIfItem)) continue;
            int ownLight = extra.fullbright ? LightCoordsUtil.FULL_BRIGHT : light;
            if (extra.spin) {
                spinningLimb(
                        pose,
                        collector,
                        ownLight,
                        set.mesh,
                        anchorLimb,
                        bone,
                        state,
                        extra.texture,
                        extra.group);
            } else {
                limb(
                        pose,
                        collector,
                        ownLight,
                        set.mesh,
                        anchorLimb,
                        bone,
                        state,
                        extra.texture,
                        extra.group,
                        0F,
                        ownLight,
                        extra.tint);
            }
        }
    }

    private static void limb(
            PoseStack pose,
            SubmitNodeCollector collector,
            int light,
            HFRWavefrontObject mesh,
            ModelPart limb,
            Bone bone,
            HumanoidRenderState state,
            Identifier texture,
            int group,
            float xOffset) {
        limb(pose, collector, light, mesh, limb, bone, state, texture, group, xOffset, light, -1);
    }

    private static void limb(
            PoseStack pose,
            SubmitNodeCollector collector,
            int light,
            HFRWavefrontObject mesh,
            ModelPart limb,
            Bone bone,
            HumanoidRenderState state,
            Identifier texture,
            int group,
            float xOffset,
            int ownLight,
            int tint) {
        pose.pushPose();
        if (xOffset != 0F) pose.translate(xOffset * scale(limb, bone, state), 0F, 0F);
        enterFrame(pose, limb, bone, state);
        toModelPixels(pose, limb, bone, state);
        collector.submitCustomGeometry(
                pose,
                WorldRenderPipeline.oneSidedCutout(texture),
                (p, buffer) -> mesh.renderPart(p, buffer, ownLight, tint, group));
        pose.popPose();
    }

    private static void spinningLimb(
            PoseStack pose,
            SubmitNodeCollector collector,
            int light,
            HFRWavefrontObject mesh,
            ModelPart limb,
            Bone bone,
            HumanoidRenderState state,
            Identifier texture,
            int group) {
        pose.pushPose();
        enterFrame(pose, limb, bone, state);
        float pivot = 4.875F * 0.0625F;
        pose.translate(0F, pivot, 0F);
        pose.mulPose(Axis.ZP.rotationDegrees((float) (-(GameTime.now() / 2D) % 360D)));
        pose.translate(0F, -pivot, 0F);
        toModelPixels(pose, limb, bone, state);
        collector.submitCustomGeometry(
                pose,
                WorldRenderPipeline.oneSidedCutout(texture),
                (p, buffer) -> mesh.renderPart(p, buffer, light, -1, group));
        pose.popPose();
    }

    public enum Bone {
        HEAD(new Vector3f(), 8F, 8F, new Vector3f(), false, 16F),
        BODY(new Vector3f(), 8F, 12F, new Vector3f(), true, 24F),
        RIGHT_ARM(new Vector3f(-5F, 2F, 0F), 4F, 12F, new Vector3f(-1F, -2F, 0F), true, 24F),
        LEFT_ARM(new Vector3f(5F, 2F, 0F), 4F, 12F, new Vector3f(1F, -2F, 0F), true, 24F),

        RIGHT_LEG(new Vector3f(-1.9F, 12F, 0F), 4F, 12F, new Vector3f(0F, 12F, 0F), false, 24F),
        LEFT_LEG(new Vector3f(1.9F, 12F, 0F), 4F, 12F, new Vector3f(0F, 12F, 0F), false, 24F);

        private final Vector3fc pivot;
        private final float width;
        private final float height;
        private final Vector3fc anchor;
        private final boolean fromTop;
        private final float lift;

        Bone(
                Vector3fc pivot,
                float width,
                float height,
                Vector3fc anchor,
                boolean fromTop,
                float lift) {
            this.pivot = pivot;
            this.width = width;
            this.height = height;
            this.anchor = anchor;
            this.fromTop = fromTop;
            this.lift = lift;
        }
    }

    private record Fit(float scale, Vector3fc anchor) {}

    private static final Map<ModelPart, Fit> FITS = new WeakHashMap<>();

    private static Fit fit(ModelPart limb, Bone bone) {
        return FITS.computeIfAbsent(
                limb,
                part -> {
                    ModelPart.Cube[] flesh = new ModelPart.Cube[1];
                    part.visit(
                            new PoseStack(),
                            (pose, path, index, cube) -> {
                                if (path.isEmpty()
                                        && (flesh[0] == null
                                                || footprint(cube) > footprint(flesh[0])))
                                    flesh[0] = cube;
                            });
                    ModelPart.Cube cube = flesh[0];
                    float[] box =
                            cube != null
                                    ? new float[] {
                                        cube.minX, cube.minY, cube.minZ, cube.maxX, cube.maxY,
                                        cube.maxZ
                                    }
                                    : descendantFlesh(part);
                    if (box == null) return new Fit(1F, bone.anchor);
                    float scale =
                            Math.max(
                                    (box[3] - box[0]) / bone.width,
                                    (box[4] - box[1]) / bone.height);
                    return new Fit(
                            scale,
                            new Vector3f(
                                    (box[0] + box[3]) / 2F,
                                    bone.fromTop ? box[1] : box[4],
                                    (box[2] + box[5]) / 2F));
                });
    }

    private static float @Nullable [] descendantFlesh(ModelPart part) {
        PoseStack own = new PoseStack();
        part.translateAndRotate(own);
        PoseStack local = new PoseStack();
        local.last().pose().set(own.last().pose()).invert();
        float[][] flesh = new float[1][];
        part.visit(
                local,
                (pose, path, index, cube) -> {
                    float[] box = {
                        Float.MAX_VALUE,
                        Float.MAX_VALUE,
                        Float.MAX_VALUE,
                        -Float.MAX_VALUE,
                        -Float.MAX_VALUE,
                        -Float.MAX_VALUE
                    };
                    Vector3f corner = new Vector3f();
                    for (int i = 0; i < 8; i++) {
                        pose.pose()
                                .transformPosition(
                                        ((i & 1) == 0 ? cube.minX : cube.maxX) / 16F,
                                        ((i & 2) == 0 ? cube.minY : cube.maxY) / 16F,
                                        ((i & 4) == 0 ? cube.minZ : cube.maxZ) / 16F,
                                        corner)
                                .mul(16F);
                        box[0] = Math.min(box[0], corner.x);
                        box[1] = Math.min(box[1], corner.y);
                        box[2] = Math.min(box[2], corner.z);
                        box[3] = Math.max(box[3], corner.x);
                        box[4] = Math.max(box[4], corner.y);
                        box[5] = Math.max(box[5], corner.z);
                    }
                    if (flesh[0] == null || footprint(box) > footprint(flesh[0])) flesh[0] = box;
                });
        return flesh[0];
    }

    private static float footprint(ModelPart.Cube cube) {
        return (cube.maxX - cube.minX) * (cube.maxZ - cube.minZ);
    }

    private static float footprint(float[] box) {
        return (box[3] - box[0]) * (box[5] - box[2]);
    }

    private static void toModelPixels(
            PoseStack pose, ModelPart limb, Bone bone, HumanoidRenderState state) {
        pose.scale(0.0625F, 0.0625F, 0.0625F);
        restOffset(pose, limb, bone, state, 1F);
    }

    private static void restOffset(
            PoseStack pose, ModelPart limb, Bone bone, HumanoidRenderState state, float unit) {
        PartPose initial = limb.getInitialPose();
        if (fitted(state)) {
            pose.translate(-bone.pivot.x() * unit, -bone.pivot.y() * unit, -bone.pivot.z() * unit);
        } else if (initial.xScale() != 1F) {
            pose.translate(
                    -initial.x() / initial.xScale() * unit,
                    (bone.lift - initial.y() / initial.yScale()) * unit,
                    -initial.z() / initial.zScale() * unit);
        } else {
            pose.translate(-initial.x() * unit, -initial.y() * unit, -initial.z() * unit);
        }
    }

    private record ExtraSpec(
            int anchor,
            String group,
            Identifier texture,
            boolean fullbright,
            int tint,
            boolean spin,
            @Nullable ItemLike onlyIfItem) {}

    private record MeshSet(
            HFRWavefrontObject mesh,
            Identifier helmetTex,
            Identifier chestTex,
            Identifier armTex,
            Identifier legTex,
            int[] parts,
            float legXOffset,
            List<ExtraPart> extras) {}

    private record ExtraPart(
            int anchor,
            int group,
            Identifier texture,
            boolean fullbright,
            int tint,
            boolean spin,
            @Nullable Item onlyIfItem) {}
}
