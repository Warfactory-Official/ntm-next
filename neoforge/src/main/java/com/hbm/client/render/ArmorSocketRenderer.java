/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) Contributors to Hbm's Nuclear Tech Mod. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only,
 * as published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this code. If not, see <https://www.gnu.org/licenses/>.
 *
 * Under GPLv3 §7(c) and §7(e), these additional terms apply to LGPL-covered
 * material authored by movblock:
 *
 * - the license grants no rights to the name "NTM NEXT";
 * - a modified version must not be named NTM NEXT or presented as the
 *   official release, and must be marked as different from it.
 *
 * Accurately identifying a work as derived from NTM NEXT does not violate
 * these additional terms, provided it is clearly distinguished from the
 * official release and does not imply endorsement. Ports are encouraged to
 * acknowledge NTM NEXT as their upstream project and provide a link to it.
 */

package com.hbm.client.render;

import com.hbm.handler.ArmorModHandler;
import com.hbm.items.armor.JetpackItem;
import com.hbm.items.armor.ModGasmaskItem;
import com.hbm.items.armor.ModTeslaItem;
import com.hbm.items.armor.WingsItem;
import com.hbm.main.ResourceManager;
import com.hbm.util.RegistryUtil;
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
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;

public final class ArmorSocketRenderer {

    private static final int TESLA = ResourceManager.armor_mod_tesla.partId("Cube");
    private static final int WING_LEFT_BASE = ResourceManager.armor_wings.partId("LeftBase");
    private static final int WING_LEFT_TIP = ResourceManager.armor_wings.partId("LeftTip");
    private static final int WING_RIGHT_BASE = ResourceManager.armor_wings.partId("RightBase");
    private static final int WING_RIGHT_TIP = ResourceManager.armor_wings.partId("RightTip");
    private static final float PX = 0.0625F;
    private static final float ROOT_X = PX, ROOT_Y = 5 * PX, ROOT_Z = 3 * PX;
    private static final float JOINT_X = 16 * PX, JOINT_Z = 2 * PX;
    private static final float INWARD = 10F;

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
            JetpackItem jetpack,
            ItemStack mod,
            int outlineColor) {
        if (!ArmorWorldRenderer.visible(body, state)) return;
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

    private static Identifier sheet(JetpackItem jetpack) {
        return switch (jetpack.kind) {
            case BRAKE -> RegistryUtil.id("textures/models/model_jetpack_break.png");
            case VECTOR -> RegistryUtil.id("textures/models/model_jetpack_vector.png");
            case BOOST -> RegistryUtil.id("textures/models/model_jetpack_boost.png");
            default -> RegistryUtil.id("textures/models/model_jetpack_fly.png");
        };
    }

    public static boolean wingsGrounded(HumanoidRenderState state) {
        return ((com.hbm.client.render.MotionRenderState) (state)).hbm$onGround();
    }

    private static void submitWings(
            PoseStack pose,
            SubmitNodeCollector collector,
            int light,
            ModelPart body,
            HumanoidRenderState state,
            WingsItem.Kind kind) {
        if (!ArmorWorldRenderer.visible(body, state)) return;
        boolean grounded = wingsGrounded(state);
        double beat = Mth.floor(state.ageInTicks) * 0.2D;
        double spread =
                switch (kind) {
                    case MURK -> grounded ? 20D : Math.sin(beat) * 20D;
                    case LIMP ->
                            !grounded
                                            && ((com.hbm.client.render.MotionRenderState) (state))
                                                            .hbm$motionY()
                                                    < -0.1D
                                    ? 0D
                                    : 30D;
                };
        double fold =
                switch (kind) {
                    case MURK -> grounded ? 160D : Math.sin(beat - Math.PI * 0.5D) * 50D + 30D;
                    case LIMP ->
                            grounded
                                    ? -30D
                                    : ((com.hbm.client.render.MotionRenderState) (state))
                                                            .hbm$motionY()
                                                    < -0.1D
                                            ? 10D
                                            : 20D;
                };
        pose.pushPose();
        ArmorWorldRenderer.enterBone(pose, body, ArmorWorldRenderer.Bone.BODY, state);
        pose.translate(0F, -2F * PX, 0F);
        wing(pose, collector, light, 1, spread, fold, WING_LEFT_BASE, WING_LEFT_TIP);
        wing(pose, collector, light, -1, spread, fold, WING_RIGHT_BASE, WING_RIGHT_TIP);
        pose.popPose();
    }

    private static void wing(
            PoseStack pose,
            SubmitNodeCollector collector,
            int light,
            int side,
            double spread,
            double fold,
            int base,
            int tip) {
        pose.pushPose();
        pose.mulPose((Quaternionfc) Axis.YP.rotationDegrees(-side * INWARD));
        hinge(
                pose,
                side * ROOT_X,
                ROOT_Y,
                ROOT_Z,
                new Quaternionf()
                        .rotateY(turn(side * spread * 0.5D))
                        .rotateZ(turn(side * (spread + 5D)))
                        .rotateX(turn(45D))
                        .rotateZ(turn(side * spread)));
        wingGroup(pose, collector, light, base);
        hinge(
                pose,
                side * JOINT_X,
                ROOT_Y,
                JOINT_Z,
                new Quaternionf()
                        .rotateY(turn(side * fold))
                        .rotateZ(turn(side * (fold * 0.25D + 5D))));
        wingGroup(pose, collector, light, tip);
        pose.popPose();
    }

    private static void hinge(PoseStack pose, float x, float y, float z, Quaternionf turn) {
        pose.translate(x, y, z);
        pose.mulPose((Quaternionfc) turn);
        pose.translate(-x, -y, -z);
    }

    private static float turn(double degrees) {
        return (float) degrees * Mth.DEG_TO_RAD;
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
        return chest.getItem() instanceof JetpackItem || chest.getItem() instanceof WingsItem;
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
        if (slot == EquipmentSlot.CHEST && armor.getItem() instanceof JetpackItem bare) {
            pose.pushPose();
            model.root().translateAndRotate(pose);
            submitJetpack(pose, collector, light, model.body, state, bare, armor, outlineColor);
            pose.popPose();
            return;
        }
        if (slot == EquipmentSlot.CHEST && armor.getItem() instanceof WingsItem bare) {
            pose.pushPose();
            model.root().translateAndRotate(pose);
            submitWings(pose, collector, light, model.body, state, bare.kind);
            pose.popPose();
            return;
        }
        if (!ArmorModHandler.isArmor(armor) || !ArmorModHandler.hasMods(armor)) return;
        switch (slot) {
            case HEAD -> {
                ItemStack mod = ArmorModHandler.pryMod(armor, ArmorModHandler.HELMET_ONLY);
                if (mod.getItem() instanceof ModGasmaskItem attachment) {
                    pose.pushPose();
                    model.root().translateAndRotate(pose);
                    ArmorHeadRenderer.submitAttachment(
                            pose,
                            collector,
                            light,
                            model.head,
                            state,
                            attachment,
                            mod,
                            outlineColor);
                    pose.popPose();
                }
            }
            case CHEST -> {
                ItemStack mod = ArmorModHandler.pryMod(armor, ArmorModHandler.PLATE_ONLY);
                if (mod.getItem() instanceof ModTeslaItem) {
                    pose.pushPose();
                    model.root().translateAndRotate(pose);
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
                    pose.popPose();
                }
                if (mod.getItem() instanceof JetpackItem jetpack) {
                    pose.pushPose();
                    model.root().translateAndRotate(pose);
                    submitJetpack(
                            pose, collector, light, model.body, state, jetpack, mod, outlineColor);
                    pose.popPose();
                }
                if (mod.getItem() instanceof WingsItem wings) {
                    pose.pushPose();
                    model.root().translateAndRotate(pose);
                    submitWings(pose, collector, light, model.body, state, wings.kind);
                    pose.popPose();
                }
            }
            default -> {}
        }
    }
}
