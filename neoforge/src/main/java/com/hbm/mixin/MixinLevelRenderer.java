// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.client.render.ArmorOverheadRenderer;
import com.hbm.client.render.CargoElevatorOutline;
import com.hbm.client.render.ConveyorPreviewRenderer;
import com.hbm.client.render.GlyphidPathRenderer;
import com.hbm.client.render.NTMSkybox;
import com.hbm.client.render.flywheel.VisualTextures;
import com.hbm.items.weapon.sedna.factory.XFactoryDrill;
import com.hbm.items.weapon.sedna.impl.ItemGunDrill;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.framegraph.FramePass;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class MixinLevelRenderer {

    @Inject(method = "render", at = @At("HEAD"))
    private void hbm$animateVisualBodies(CallbackInfo ci) {
        VisualTextures.activateSprites();
    }

    private static final String SKY_PASS =
            "addSkyPass(Lcom/mojang/blaze3d/framegraph/FrameGraphBuilder;Lnet/minecraft/client/renderer/state/level/CameraRenderState;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lorg/joml/Matrix4fc;)V";

    @WrapOperation(
            method = SKY_PASS,
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lcom/mojang/blaze3d/framegraph/FramePass;executes(Ljava/lang/Runnable;)V"))
    private void hbm$ntmSkyExtras(FramePass pass, Runnable task, Operation<Void> original) {
        original.call(
                pass,
                (Runnable)
                        () -> {
                            task.run();
                            NTMSkybox.renderExtras();
                        });
    }

    @Inject(method = "submitBlockOutline", at = @At("HEAD"))
    private void hbm$armorOverhead(
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            LevelRenderState levelRenderState,
            CallbackInfo ci) {
        ArmorOverheadRenderer.submit(poseStack, submitNodeCollector, levelRenderState);
    }

    @Inject(method = "submitBlockOutline", at = @At("HEAD"))
    private void hbm$conveyorPreview(
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            LevelRenderState levelRenderState,
            CallbackInfo ci) {
        ConveyorPreviewRenderer.submit(poseStack, submitNodeCollector, levelRenderState);
    }

    @Inject(method = "submitBlockOutline", at = @At("HEAD"))
    private void hbm$glyphidPaths(
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            LevelRenderState levelRenderState,
            CallbackInfo ci) {
        GlyphidPathRenderer.submit(poseStack, submitNodeCollector, levelRenderState);
    }

    @Inject(method = "submitBlockOutline", at = @At("HEAD"), cancellable = true)
    private void hbm$drillHighlight(
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            LevelRenderState levelRenderState,
            CallbackInfo ci) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        ItemStack held = player.getMainHandItem();
        if (!(held.getItem() instanceof ItemGunDrill)) {
            if (CargoElevatorOutline.submit(poseStack, submitNodeCollector, levelRenderState))
                ci.cancel();
            return;
        }

        if (levelRenderState.blockOutlineRenderState != null) {
            XFactoryDrill.submitBlockHighlight(
                    poseStack,
                    submitNodeCollector,
                    levelRenderState.cameraRenderState.pos,
                    player,
                    held);
        }
        ci.cancel();
    }
}
