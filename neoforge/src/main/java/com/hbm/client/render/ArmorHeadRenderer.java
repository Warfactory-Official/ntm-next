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

import com.hbm.handler.ArmorUtil;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModItems;
import com.hbm.items.armor.GasMaskItem;
import com.hbm.items.armor.HazmatMaskItem;
import com.hbm.items.armor.ModArmorItem;
import com.hbm.items.armor.ModGasmaskItem;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.util.RegistryUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public final class ArmorHeadRenderer {
    private static final int NO9_HELMET = ResourceManager.armor_no9.partId("Helmet");
    private static final int NO9_INSIGNIA = ResourceManager.armor_no9.partId("Insignia");
    private static final int NO9_FLAME = ResourceManager.armor_no9.partId("Flame");
    private static final int LAMP_COLOR = ARGB.colorFromFloat(1F, 1F, 1F, .8F);

    private static final Identifier M65_TEXTURE = RegistryUtil.id("textures/models/model_m65.png");
    private static final Identifier M65_MONO_TEXTURE =
            RegistryUtil.id("textures/models/model_m65_mono.png");
    private static final Identifier OLDE_TEXTURE = RegistryUtil.id("textures/armor/mask_olde.png");
    private static final Identifier GOGGLES_TEXTURE =
            RegistryUtil.id("textures/models/goggles.png");
    private static final Identifier LIQUIDATOR_TEXTURE =
            RegistryUtil.id("textures/armor/liquidator_helmet.png");

    private static final Identifier GAS_MASK_TEXTURE =
            RegistryUtil.id("textures/models/gas_mask.png");

    private static final ModelPart M65_MASK = createM65Mask();
    private static final ModelPart GAS_MASK = createGasMask();
    private static final float GAS_MASK_SCALE = 1.15F;
    private static final ModelPart M65_FILTER = createM65Filter();
    private static final ModelPart GOGGLES = createGoggles();
    private static final float M65_SCALE = 18F / 16F * 1.01F;

    private static final Mask GOGGLES_MASK = new PartMask(GOGGLES, GOGGLES_TEXTURE);
    private static final Mask ASHGLASSES_MASK =
            new WholeMeshMask(ResourceManager.armor_goggles, ResourceManager.goggles_tex);
    private static final Mask HAT_MASK =
            new WholeMeshMask(ResourceManager.armor_hat, ResourceManager.hat_tex);
    private static final Mask GAS_MASK_MASK =
            new ScaledPartMask(GAS_MASK, GAS_MASK_TEXTURE, GAS_MASK_SCALE);
    private static final Mask NO9_MASK = new No9Mask();

    private ArmorHeadRenderer() {}

    public static boolean draws(ItemStack stack) {
        return resolve(stack) != null;
    }

    public static boolean submit(
            PoseStack pose,
            SubmitNodeCollector collector,
            int light,
            HumanoidModel<HumanoidRenderState> model,
            HumanoidRenderState state,
            EquipmentSlot slot,
            ItemStack stack,
            int outlineColor) {
        if (slot != EquipmentSlot.HEAD) return false;
        Mask mask = resolve(stack);
        if (mask == null) return false;
        if (!ArmorWorldRenderer.visible(model.head, state)) return true;
        pose.pushPose();
        model.root().translateAndRotate(pose);
        mask.submit(pose, collector, light, model.head, state, stack, outlineColor);
        pose.popPose();
        return true;
    }

    public static void submitAttachment(
            PoseStack pose,
            SubmitNodeCollector collector,
            int light,
            ModelPart head,
            HumanoidRenderState state,
            ModGasmaskItem attachment,
            ItemStack mod,
            int outlineColor) {
        if (!ArmorWorldRenderer.visible(head, state)) return;
        Identifier texture =
                attachment.variant() == ModGasmaskItem.Variant.MONO
                        ? M65_MONO_TEXTURE
                        : M65_TEXTURE;
        new M65Mask(texture, true).submit(pose, collector, light, head, state, mod, outlineColor);
    }

    private static @Nullable Mask resolve(ItemStack stack) {
        if (!(stack.getItem() instanceof ModArmorItem armor)) return null;
        if (armor instanceof HazmatMaskItem hazmatMask) {
            Identifier texture = hazmatMask.variant().modelTexture();
            return texture == null ? null : new M65Mask(texture, true);
        }
        if (armor instanceof GasMaskItem gasMask) {
            if (gasMask.type() == GasMaskItem.Type.GAS_MASK) return GAS_MASK_MASK;
            return new M65Mask(
                    switch (gasMask.type()) {
                        case M65 -> M65_TEXTURE;
                        case MONO -> M65_MONO_TEXTURE;
                        case OLDE -> OLDE_TEXTURE;
                        case GAS_MASK -> throw new IllegalStateException();
                    },
                    true);
        }
        if (armor == ModItems.GOGGLES.get()) return GOGGLES_MASK;
        if (armor == ModItems.ASHGLASSES.get()) return ASHGLASSES_MASK;
        if (armor.suit() == ModArmorItem.Suit.LIQUIDATOR)
            return new M65Mask(LIQUIDATOR_TEXTURE, true);
        if (armor == ModItems.NO9.get()) return NO9_MASK;
        if (armor == ModItems.NOSSY_HAT.get()) return HAT_MASK;
        return null;
    }

    public sealed interface Mask {
        void submit(
                PoseStack pose,
                SubmitNodeCollector collector,
                int light,
                ModelPart head,
                HumanoidRenderState state,
                ItemStack stack,
                int outlineColor);
    }

    private record M65Mask(Identifier texture, boolean filterable) implements Mask {
        @Override
        public void submit(
                PoseStack pose,
                SubmitNodeCollector collector,
                int light,
                ModelPart head,
                HumanoidRenderState state,
                ItemStack stack,
                int outlineColor) {
            submitPart(
                    pose,
                    collector,
                    light,
                    head,
                    state,
                    M65_MASK,
                    texture,
                    M65_SCALE,
                    stack,
                    outlineColor);
            if (filterable && !ArmorUtil.getGasMaskFilter(stack).isEmpty()) {
                submitPart(
                        pose,
                        collector,
                        light,
                        head,
                        state,
                        M65_FILTER,
                        texture,
                        M65_SCALE,
                        stack,
                        outlineColor);
            }
        }
    }

    private record ScaledPartMask(ModelPart part, Identifier texture, float adultScale)
            implements Mask {
        @Override
        public void submit(
                PoseStack pose,
                SubmitNodeCollector collector,
                int light,
                ModelPart head,
                HumanoidRenderState state,
                ItemStack stack,
                int outlineColor) {
            submitPart(
                    pose,
                    collector,
                    light,
                    head,
                    state,
                    part,
                    texture,
                    state.isBaby ? 1F : adultScale,
                    stack,
                    outlineColor);
        }
    }

    private record PartMask(ModelPart part, Identifier texture) implements Mask {
        @Override
        public void submit(
                PoseStack pose,
                SubmitNodeCollector collector,
                int light,
                ModelPart head,
                HumanoidRenderState state,
                ItemStack stack,
                int outlineColor) {
            submitPart(pose, collector, light, head, state, part, texture, 1F, stack, outlineColor);
        }
    }

    public record WholeMeshMask(HFRWavefrontObject mesh, Identifier texture) implements Mask {
        @Override
        public void submit(
                PoseStack pose,
                SubmitNodeCollector collector,
                int light,
                ModelPart head,
                HumanoidRenderState state,
                ItemStack stack,
                int outlineColor) {
            RenderType base = WorldRenderPipeline.oneSidedCutout(texture);
            pose.pushPose();
            ArmorWorldRenderer.enterFrame(pose, head, ArmorWorldRenderer.Bone.HEAD, state);
            pose.scale(1F / 16F, 1F / 16F, 1F / 16F);
            collector.submitCustomGeometry(
                    pose, base, (p, buffer) -> mesh.render(p, buffer, light, -1));
            if (outlineColor != 0) {
                base.outline()
                        .ifPresent(
                                outline ->
                                        collector.submitCustomGeometry(
                                                pose,
                                                outline,
                                                (p, buffer) ->
                                                        mesh.render(
                                                                p,
                                                                buffer,
                                                                LightCoordsUtil.FULL_BRIGHT,
                                                                outlineColor)));
            }
            if (stack.hasFoil()) {
                collector.submitCustomGeometry(
                        pose, ArmorGlint.TYPE, (p, buffer) -> mesh.render(p, buffer, light, -1));
            }
            pose.popPose();
        }
    }

    private record No9Mask() implements Mask {
        @Override
        public void submit(
                PoseStack pose,
                SubmitNodeCollector collector,
                int light,
                ModelPart head,
                HumanoidRenderState state,
                ItemStack stack,
                int outlineColor) {
            submitNo9(pose, collector, light, head, state, stack, outlineColor);
        }
    }

    private static void submitNo9(
            PoseStack pose,
            SubmitNodeCollector collector,
            int light,
            ModelPart head,
            HumanoidRenderState state,
            ItemStack stack,
            int outlineColor) {
        HFRWavefrontObject mesh = ResourceManager.armor_no9;
        pose.pushPose();
        ArmorWorldRenderer.enterFrame(pose, head, ArmorWorldRenderer.Bone.HEAD, state);
        pose.scale(1F / 16F, 1F / 16F, 1F / 16F);
        submitGroup(
                pose, collector, light, mesh, NO9_HELMET, ResourceManager.no9_tex, outlineColor);
        submitGroup(
                pose,
                collector,
                light,
                mesh,
                NO9_INSIGNIA,
                ResourceManager.no9_insignia_tex,
                outlineColor);
        if (stack.getOrDefault(ModDataComponents.NO9_LAMP.get(), false)) {
            collector.submitCustomGeometry(
                    pose,
                    ArmorRenderTypes.LAMP,
                    (p, buffer) ->
                            mesh.renderPart(
                                    p, buffer, LightCoordsUtil.FULL_BRIGHT, LAMP_COLOR, NO9_FLAME));
        }
        if (stack.hasFoil()) {
            collector.submitCustomGeometry(
                    pose,
                    ArmorGlint.TYPE,
                    (p, buffer) -> {
                        mesh.renderPart(p, buffer, light, -1, NO9_HELMET);
                        mesh.renderPart(p, buffer, light, -1, NO9_INSIGNIA);
                    });
        }
        pose.popPose();
    }

    private static void submitGroup(
            PoseStack pose,
            SubmitNodeCollector collector,
            int light,
            HFRWavefrontObject mesh,
            int group,
            Identifier texture,
            int outlineColor) {
        RenderType base = WorldRenderPipeline.oneSidedCutout(texture);
        collector.submitCustomGeometry(
                pose, base, (p, buffer) -> mesh.renderPart(p, buffer, light, -1, group));
        if (outlineColor != 0) {
            base.outline()
                    .ifPresent(
                            outline ->
                                    collector.submitCustomGeometry(
                                            pose,
                                            outline,
                                            (p, buffer) ->
                                                    mesh.renderPart(
                                                            p,
                                                            buffer,
                                                            LightCoordsUtil.FULL_BRIGHT,
                                                            outlineColor,
                                                            group)));
        }
    }

    private static void submitPart(
            PoseStack pose,
            SubmitNodeCollector collector,
            int light,
            ModelPart head,
            HumanoidRenderState state,
            ModelPart part,
            Identifier texture,
            float scale,
            ItemStack stack,
            int outlineColor) {
        pose.pushPose();
        ArmorWorldRenderer.enterFrame(pose, head, ArmorWorldRenderer.Bone.HEAD, state);
        pose.scale(scale, scale, scale);
        collector.submitModelPart(
                part,
                pose,
                WorldRenderPipeline.oneSidedCutout(texture),
                light,
                OverlayTexture.NO_OVERLAY,
                null,
                -1,
                outlineColor);
        if (stack.hasFoil()) {
            collector.submitModelPart(
                    part,
                    pose,
                    ArmorGlint.TYPE,
                    light,
                    OverlayTexture.NO_OVERLAY,
                    null,
                    -1,
                    outlineColor);
        }
        pose.popPose();
    }

    private static ModelPart createM65Mask() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild(
                "mask_head",
                CubeListBuilder.create().texOffs(0, 0).addBox(0F, 0F, 0F, 8F, 8F, 8F),
                PartPose.offset(-4F, -7.5F, -4F));
        root.addOrReplaceChild(
                "nose",
                CubeListBuilder.create().texOffs(0, 16).addBox(0F, 0F, 0F, 3F, 3F, 1F),
                PartPose.offset(-1.5F, -3F, -5F));
        root.addOrReplaceChild(
                "outlet",
                CubeListBuilder.create().texOffs(0, 20).addBox(0F, -2F, 0F, 2F, 2F, 1F),
                PartPose.offsetAndRotation(-1F, -3F, -5F, -0.4799655F, 0F, 0F));
        root.addOrReplaceChild(
                "nose_slope",
                CubeListBuilder.create().texOffs(8, 16).addBox(0F, 0F, -2F, 3F, 2F, 2F),
                PartPose.offsetAndRotation(-1.5F, -1.5F, -4F, 0.6108652F, 0F, 0F));
        root.addOrReplaceChild(
                "eye_left",
                CubeListBuilder.create().texOffs(0, 23).addBox(0F, 0F, 0F, 3F, 3F, 0F),
                PartPose.offset(-3.5F, -5.5F, -4.2F));
        root.addOrReplaceChild(
                "eye_right",
                CubeListBuilder.create().texOffs(0, 26).addBox(0F, 0F, 0F, 3F, 3F, 0F),
                PartPose.offset(.5F, -5.5F, -4.2F));
        root.addOrReplaceChild(
                "outlet_cap",
                CubeListBuilder.create().texOffs(6, 20).addBox(0F, 0F, 0F, 2F, 2F, 1F),
                PartPose.offset(-1F, -2.7F, -6F));
        return LayerDefinition.create(mesh, 32, 32).bakeRoot();
    }

    private static ModelPart createM65Filter() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild(
                "connector",
                CubeListBuilder.create().texOffs(6, 23).addBox(0F, 0F, -3F, 2F, 2F, 1F),
                PartPose.offsetAndRotation(-1F, -1.5F, -4F, 0.6108652F, 0F, 0F));
        root.addOrReplaceChild(
                "filter_one",
                CubeListBuilder.create().texOffs(18, 21).addBox(0F, -1F, -5F, 3F, 4F, 2F),
                PartPose.offsetAndRotation(-1.5F, -1.5F, -4F, 0.6108652F, 0F, 0F));
        root.addOrReplaceChild(
                "filter_two",
                CubeListBuilder.create().texOffs(18, 16).addBox(0F, -.5F, -5F, 4F, 3F, 2F),
                PartPose.offsetAndRotation(-2F, -1.5F, -4F, 0.6108652F, 0F, 0F));
        return LayerDefinition.create(mesh, 32, 32).bakeRoot();
    }

    private static ModelPart createGasMask() {
        float lift = 0.075F / 2F;
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild(
                "shell",
                CubeListBuilder.create().texOffs(0, 0).addBox(0F, 0F, 0F, 8F, 8F, 3F),
                PartPose.offset(-4F, -8F + lift, -4F));
        root.addOrReplaceChild(
                "eye_left",
                CubeListBuilder.create().texOffs(22, 0).addBox(0F, 0F, 0F, 2F, 2F, 1F),
                PartPose.offset(-3F, -5F + lift, -4.5333334F));
        root.addOrReplaceChild(
                "eye_right",
                CubeListBuilder.create().texOffs(22, 0).addBox(0F, 0F, 0F, 2F, 2F, 1F),
                PartPose.offset(1F, -5F + lift, -4.5F));
        root.addOrReplaceChild(
                "connector",
                CubeListBuilder.create().texOffs(0, 11).addBox(0F, 0F, 0F, 2F, 2F, 2F),
                PartPose.offsetAndRotation(-1F, -3F + lift, -4F, -0.7853982F, 0F, 0F));
        root.addOrReplaceChild(
                "filter",
                CubeListBuilder.create().texOffs(0, 15).addBox(0F, 2F, -0.5F, 3F, 4F, 3F),
                PartPose.offsetAndRotation(-1.5F, -3F + lift, -4F, -0.7853982F, 0F, 0F));
        root.addOrReplaceChild(
                "strap",
                CubeListBuilder.create().texOffs(0, 22).addBox(0F, 0F, 0F, 8F, 1F, 5F),
                PartPose.offset(-4F, -5F + lift, -1F));
        return LayerDefinition.create(mesh, 64, 32).bakeRoot();
    }

    private static ModelPart createGoggles() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild(
                "band",
                CubeListBuilder.create().texOffs(0, 0).addBox(0F, 0F, 0F, 9F, 3F, 1F),
                PartPose.offset(-4.5F, -5F, -4.5F));
        root.addOrReplaceChild(
                "case",
                CubeListBuilder.create().texOffs(0, 4).addBox(0F, 0F, 0F, 9F, 2F, 5F),
                PartPose.offset(-4.5F, -5F, -3.5F));
        root.addOrReplaceChild(
                "lens_right",
                CubeListBuilder.create().texOffs(26, 0).addBox(0F, 0F, 0F, 2F, 2F, 1F),
                PartPose.offset(1F, -4.5F, -5F));
        root.addOrReplaceChild(
                "lens_left",
                CubeListBuilder.create().texOffs(20, 0).addBox(0F, 0F, 0F, 2F, 2F, 1F),
                PartPose.offset(-3F, -4.5F, -5F));
        root.addOrReplaceChild(
                "back",
                CubeListBuilder.create().texOffs(0, 11).addBox(0F, 0F, 0F, 9F, 1F, 4F),
                PartPose.offset(-4.5F, -5F, .5F));
        return LayerDefinition.create(mesh, 64, 32).bakeRoot();
    }
}
