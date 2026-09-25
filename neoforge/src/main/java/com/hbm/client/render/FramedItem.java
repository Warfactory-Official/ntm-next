// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.tags.HbmItemTags;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.function.Consumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

public final class FramedItem {

    public enum Arm {
        BLOCK,
        MESH,
        SPRITE,

        STILL_MESH;

        public boolean mesh() {
            return this == MESH || this == STILL_MESH;
        }

        float bob() {
            return this == STILL_MESH ? 0F : FRAMED_BOB;
        }

        public float groundLift() {
            return bob() + GROUND_AUTOLIFT_CANCEL;
        }
    }

    static final float FRAMED_SPRITE_SCALE = 0.5128205F;

    static final float FRAMED_BOB = 0.1F;

    static final float GROUND_AUTOLIFT_CANCEL = 0.25F;
    static final float DECO_GROUND_LIFT = GROUND_AUTOLIFT_CANCEL;

    private static final float F9 = 0.25F;

    private static final float SPRITE_DEPTH = 0.0625F;
    private static final float SPRITE_STEP = SPRITE_DEPTH + 0.021875F;
    private static final float SPRITE_ANCHOR_Y = 0.25F;
    private static final float SPRITE_ANCHOR_Z = -SPRITE_DEPTH / 2.0F;

    private static final double FLAT_DEPTH = 1.0D / 16.0D;
    private static final double FLAT_SPAN = 1.0D;

    private FramedItem() {}

    public static Arm resolve(
            ItemModelResolver resolver,
            ItemStackRenderState out,
            ItemStack stack,
            @Nullable Level level,
            @Nullable ItemOwner owner,
            int seed) {

        if (!stack.is(HbmItemTags.FRAMED_MESH)) {
            resolver.updateForTopItem(out, stack, ItemDisplayContext.NONE, level, owner, seed);

            if (stack.getItem() instanceof BlockItem && out.usesBlockLight()) return Arm.BLOCK;
            if (!bespoke(out) && flatIcon(out.getModelBoundingBox())) return Arm.SPRITE;
        }

        resolver.updateForTopItem(out, stack, ItemDisplayContext.GROUND, level, owner, seed);
        return stack.is(HbmItemTags.FRAMED_UNBOBBED) ? Arm.STILL_MESH : Arm.MESH;
    }

    private static boolean flatIcon(AABB bounds) {
        return bounds.getZsize() <= FLAT_DEPTH
                && bounds.getXsize() <= FLAT_SPAN
                && bounds.getYsize() <= FLAT_SPAN;
    }

    private static boolean bespoke(ItemStackRenderState state) {
        for (int layer = 0; layer < state.activeLayerCount; layer++) {
            if (state.layers[layer].specialRenderer != null) return true;
        }
        return false;
    }

    public static void framedBlockPrefix(PoseStack poseStack) {
        poseStack.translate(0.0, FRAMED_BOB, 0.0);
        decoBlockPrefix(poseStack);
    }

    public static void decoArm(PoseStack poseStack, Arm arm) {
        switch (arm) {
            case BLOCK -> decoBlockPrefix(poseStack);
            case MESH, STILL_MESH -> poseStack.translate(0.0, DECO_GROUND_LIFT, 0.0);
            case SPRITE ->
                    poseStack.scale(FRAMED_SPRITE_SCALE, FRAMED_SPRITE_SCALE, FRAMED_SPRITE_SCALE);
        }
    }

    public static void decoBlockPrefix(PoseStack poseStack) {
        poseStack.scale(1.25F, 1.25F, 1.25F);
        poseStack.translate(0.0, 0.05F, 0.0);
        poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));

        poseStack.scale(F9, F9, F9);
    }

    public static void framedMeshPrefix(PoseStack poseStack, Arm arm) {
        poseStack.translate(0.0, arm.bob(), 0.0);
    }

    public static void framedSpritePrefix(PoseStack poseStack) {
        poseStack.translate(0.0, FRAMED_BOB, 0.0);
        poseStack.scale(FRAMED_SPRITE_SCALE, FRAMED_SPRITE_SCALE, FRAMED_SPRITE_SCALE);
        poseStack.translate(0.0, -0.05, 0.0);
    }

    public static void framedSpriteAnchor(PoseStack poseStack) {
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        poseStack.translate(0.0F, SPRITE_ANCHOR_Y, SPRITE_ANCHOR_Z);
    }

    public static void framedSpriteSinglePose(PoseStack poseStack) {
        framedSpriteAnchor(poseStack);
        poseStack.translate(0.0F, 0.0F, SPRITE_STEP / 2.0F);
    }

    public static int miniBlockCount(int stackSize) {
        if (stackSize > 40) return 5;
        if (stackSize > 20) return 4;
        if (stackSize > 5) return 3;
        if (stackSize > 1) return 2;
        return 1;
    }

    public static int miniItemCount(int stackSize) {
        if (stackSize < 2) return 1;
        if (stackSize < 16) return 2;
        if (stackSize < 32) return 3;
        return 4;
    }

    private static float jitter(RandomSource random) {
        return (random.nextFloat() * 2.0F - 1.0F) * 0.2F / F9;
    }

    private static float spread(RandomSource random) {
        return (random.nextFloat() * 2.0F - 1.0F) * 0.3F / 0.5F;
    }

    static void framedBlockCopies(
            PoseStack poseStack,
            ItemStackRenderState item,
            SubmitNodeCollector collector,
            int lightCoords,
            int outlineColor,
            int copies) {
        framedBlockCopies(
                poseStack,
                copies,
                copy ->
                        item.submit(
                                copy,
                                collector,
                                lightCoords,
                                OverlayTexture.NO_OVERLAY,
                                outlineColor));
    }

    public static void framedBlockCopies(
            PoseStack poseStack, int copies, Consumer<PoseStack> each) {
        RandomSource random = RandomSource.create(187L);
        for (int copy = 0; copy < copies; copy++) {
            poseStack.pushPose();
            if (copy > 0) poseStack.translate(jitter(random), jitter(random), jitter(random));
            each.accept(poseStack);
            poseStack.popPose();
        }
    }

    static void framedSpriteCopies(
            PoseStack poseStack,
            ItemStackRenderState item,
            SubmitNodeCollector collector,
            int lightCoords,
            int outlineColor,
            int copies) {
        framedSpriteCopies(
                poseStack,
                copies,
                copy ->
                        item.submit(
                                copy,
                                collector,
                                lightCoords,
                                OverlayTexture.NO_OVERLAY,
                                outlineColor));
    }

    public static void framedSpriteCopies(
            PoseStack poseStack, int copies, Consumer<PoseStack> each) {
        poseStack.translate(0.0, 0.0, -SPRITE_STEP * copies / 2.0F);
        RandomSource random = RandomSource.create(187L);
        for (int copy = 0; copy < copies; copy++) {
            if (copy > 0) {
                float x = spread(random);
                float y = spread(random);
                spread(random);
                poseStack.translate(x, y, SPRITE_STEP);
            } else {
                poseStack.translate(0.0, 0.0, SPRITE_STEP);
            }
            each.accept(poseStack);
        }
    }
}
