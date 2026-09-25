// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.handler.ArmorModHandler;
import com.hbm.items.armor.ItemJetpack;
import com.hbm.items.armor.ItemModGasmask;
import com.hbm.items.armor.ItemModTesla;
import com.hbm.items.armor.ItemWings;
import com.hbm.lib.Library;
import com.hbm.main.ResourceManager;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

public final class ArmorSocketRenderer {

    private static final int TESLA = ResourceManager.armor_mod_tesla.partId("Cube");
    private static final int WING_LEFT_BASE = ResourceManager.armor_wings.partId("LeftBase");
    private static final int WING_LEFT_TIP = ResourceManager.armor_wings.partId("LeftTip");
    private static final int WING_RIGHT_BASE = ResourceManager.armor_wings.partId("RightBase");
    private static final int WING_RIGHT_TIP = ResourceManager.armor_wings.partId("RightTip");

    private static final double PX = 0.0625D;
    private static final int PIVOT_SIDE = 1;
    private static final int PIVOT_FRONT = 5;
    private static final int PIVOT_Z = 3;
    private static final int TIP_SIDE = 16;
    private static final int TIP_Z = 2;
    private static final double WING_INWARD = 10D;

    private static final ModelPart JETPACK = createJetpack();

    private ArmorSocketRenderer() {}

    private static ModelPart createJetpack() {
        MeshDefinition mesh = new MeshDefinition();

        PartDefinition root =
                mesh.getRoot()
                        .addOrReplaceChild(
                                "jetpack", CubeListBuilder.create(), PartPose.offset(0F, 0F, 2F));
        box(root, "pack", 12, 10, 4F, 6F, 1F, -2F, 3F, 0F);
        box(root, "tank_right", 0, 0, 3F, 8F, 3F, 0.5F, 2F, 0.5F);
        box(root, "tank_left", 0, 11, 3F, 8F, 3F, -3.5F, 2F, 0.5F);
        box(root, "tip_right", 0, 22, 2F, 1F, 2F, 1F, 1F, 1F);
        box(root, "tip_left", 0, 25, 2F, 1F, 2F, -3F, 1F, 1F);
        box(root, "duct_right", 8, 22, 2F, 1F, 2F, 1F, 9.5F, 1F);
        box(root, "duct_left", 8, 25, 2F, 1F, 2F, -3F, 9.5F, 1F);
        box(root, "thruster_right", 12, 0, 3F, 2F, 3F, 0.5F, 10.5F, 0.5F);
        box(root, "thruster_left", 12, 5, 3F, 2F, 3F, -3.5F, 10.5F, 0.5F);
        return LayerDefinition.create(mesh, 32, 32).bakeRoot();
    }

    private static void box(
            PartDefinition parent,
            String name,
            int u,
            int v,
            float w,
            float h,
            float d,
            float x,
            float y,
            float z) {
        parent.addOrReplaceChild(
                name,
                CubeListBuilder.create().texOffs(u, v).addBox(0F, 0F, 0F, w, h, d),
                PartPose.offset(x, y, z));
    }

    private static void submitJetpack(
            PoseStack pose,
            SubmitNodeCollector collector,
            int light,
            ModelPart body,
            HumanoidRenderState state,
            ItemJetpack jetpack,
            ItemStack mod,
            int outlineColor) {
        pose.pushPose();
        ArmorWorldRenderer.enterFrame(pose, body, ArmorWorldRenderer.Bone.BODY, state);
        collector.submitModelPart(
                JETPACK,
                pose,
                WorldRenderPipeline.oneSidedCutout(sheet(jetpack)),
                light,
                OverlayTexture.NO_OVERLAY,
                null,
                -1,
                null,
                outlineColor);
        if (mod.hasFoil()) {
            collector.submitModelPart(
                    JETPACK,
                    pose,
                    RenderTypes.armorEntityGlint(),
                    light,
                    OverlayTexture.NO_OVERLAY,
                    null,
                    -1,
                    null,
                    outlineColor);
        }
        pose.popPose();
    }

    private static Identifier sheet(ItemJetpack jetpack) {
        return switch (jetpack.kind) {
            case BRAKE -> Library.id("textures/models/model_jetpack_break.png");
            case VECTOR -> Library.id("textures/models/model_jetpack_vector.png");
            case BOOST -> Library.id("textures/models/model_jetpack_boost.png");
            default -> Library.id("textures/models/model_jetpack_fly.png");
        };
    }

    private static void submitWings(
            PoseStack pose,
            SubmitNodeCollector collector,
            int light,
            ModelPart body,
            HumanoidRenderState state,
            ItemWings.Kind kind) {
        MotionRenderState motion = (MotionRenderState) state;

        double rot = Math.sin(state.ageInTicks * 0.2D) * 20;
        double rot2 = Math.sin(state.ageInTicks * 0.2D - Math.PI * 0.5) * 50 + 30;
        if (kind == ItemWings.Kind.MURK && motion.hbm$onGround()) {
            rot = 20;
            rot2 = 160;
        }
        if (kind == ItemWings.Kind.LIMP) {
            if (motion.hbm$onGround()) {
                rot = 30;
                rot2 = -30;
            } else if (motion.hbm$motionY() < -0.1) {
                rot = 0;
                rot2 = 10;
            } else {
                rot = 30;
                rot2 = 20;
            }
        }

        pose.pushPose();
        ArmorWorldRenderer.enterBone(pose, body, ArmorWorldRenderer.Bone.BODY, state);
        pose.translate(0D, -2 * PX, 0D);

        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees((float) -WING_INWARD));
        pose.translate(PIVOT_SIDE * PX, PIVOT_FRONT * PX, PIVOT_Z * PX);
        pose.mulPose(Axis.YP.rotationDegrees((float) (rot * 0.5)));
        pose.mulPose(Axis.ZP.rotationDegrees((float) (rot + 5)));
        pose.mulPose(Axis.XP.rotationDegrees(45F));
        pose.translate(-PIVOT_SIDE * PX, -PIVOT_FRONT * PX, -PIVOT_Z * PX);
        pose.translate(PIVOT_SIDE * PX, PIVOT_FRONT * PX, PIVOT_Z * PX);
        pose.mulPose(Axis.ZP.rotationDegrees((float) rot));
        pose.translate(-PIVOT_SIDE * PX, -PIVOT_FRONT * PX, -PIVOT_Z * PX);
        wingGroup(pose, collector, light, WING_LEFT_BASE);
        pose.translate(TIP_SIDE * PX, PIVOT_FRONT * PX, TIP_Z * PX);
        pose.mulPose(Axis.YP.rotationDegrees((float) rot2));
        pose.mulPose(Axis.ZP.rotationDegrees((float) (rot2 * 0.25 + 5)));
        pose.translate(-TIP_SIDE * PX, -PIVOT_FRONT * PX, -TIP_Z * PX);
        wingGroup(pose, collector, light, WING_LEFT_TIP);
        pose.popPose();

        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees((float) WING_INWARD));
        pose.translate(-PIVOT_SIDE * PX, PIVOT_FRONT * PX, PIVOT_Z * PX);
        pose.mulPose(Axis.YP.rotationDegrees((float) (-rot * 0.5)));
        pose.mulPose(Axis.ZP.rotationDegrees((float) (-rot - 5)));
        pose.mulPose(Axis.XP.rotationDegrees(45F));
        pose.translate(PIVOT_SIDE * PX, -PIVOT_FRONT * PX, -PIVOT_Z * PX);
        pose.translate(-PIVOT_SIDE * PX, PIVOT_FRONT * PX, PIVOT_Z * PX);
        pose.mulPose(Axis.ZP.rotationDegrees((float) -rot));
        pose.translate(PIVOT_SIDE * PX, -PIVOT_FRONT * PX, -PIVOT_Z * PX);
        wingGroup(pose, collector, light, WING_RIGHT_BASE);
        pose.translate(-TIP_SIDE * PX, PIVOT_FRONT * PX, TIP_Z * PX);
        pose.mulPose(Axis.YP.rotationDegrees((float) -rot2));
        pose.mulPose(Axis.ZP.rotationDegrees((float) (-rot2 * 0.25 - 5)));
        pose.translate(TIP_SIDE * PX, -PIVOT_FRONT * PX, -TIP_Z * PX);
        wingGroup(pose, collector, light, WING_RIGHT_TIP);
        pose.popPose();

        pose.popPose();
    }

    private static void wingGroup(
            PoseStack pose, SubmitNodeCollector collector, int light, int group) {
        pose.pushPose();
        pose.scale(0.0625F, 0.0625F, 0.0625F);
        collector.submitCustomGeometry(
                pose,
                RenderTypes.entityCutoutCull(ResourceManager.wings_murk),
                (p, buffer) -> ResourceManager.armor_wings.renderPart(p, buffer, light, -1, group));
        pose.popPose();
    }

    public static boolean drawsBare(ItemStack chest) {
        return chest.getItem() instanceof ItemJetpack || chest.getItem() instanceof ItemWings;
    }

    public static void submit(
            PoseStack pose,
            SubmitNodeCollector collector,
            int light,
            HumanoidModel<HumanoidRenderState> model,
            HumanoidRenderState state,
            EquipmentSlot slot,
            ItemStack armor,
            int outlineColor) {

        if (slot == EquipmentSlot.CHEST && armor.getItem() instanceof ItemJetpack bare) {
            submitJetpack(pose, collector, light, model.body, state, bare, armor, outlineColor);
            return;
        }
        if (slot == EquipmentSlot.CHEST && armor.getItem() instanceof ItemWings bare) {
            submitWings(pose, collector, light, model.body, state, bare.kind);
            return;
        }
        if (!ArmorModHandler.isArmor(armor) || !ArmorModHandler.hasMods(armor)) return;
        switch (slot) {
            case HEAD -> {
                ItemStack mod = ArmorModHandler.pryMod(armor, ArmorModHandler.HELMET_ONLY);
                if (mod.getItem() instanceof ItemModGasmask attachment) {
                    ArmorHeadRenderer.submitAttachment(
                            pose,
                            collector,
                            light,
                            model.head,
                            state,
                            attachment,
                            mod,
                            outlineColor);
                }
            }
            case CHEST -> {
                ItemStack mod = ArmorModHandler.pryMod(armor, ArmorModHandler.PLATE_ONLY);
                if (mod.getItem() instanceof ItemModTesla) {
                    ArmorWorldRenderer.submitGroup(
                            pose,
                            collector,
                            light,
                            ResourceManager.armor_mod_tesla,
                            model.body,
                            ArmorWorldRenderer.Bone.BODY,
                            state,
                            ResourceManager.mod_tesla_tex,
                            TESLA);
                }
                if (mod.getItem() instanceof ItemJetpack jetpack) {
                    submitJetpack(
                            pose, collector, light, model.body, state, jetpack, mod, outlineColor);
                }
                if (mod.getItem() instanceof ItemWings wings) {
                    submitWings(pose, collector, light, model.body, state, wings.kind);
                }
            }
            default -> {}
        }
    }
}
