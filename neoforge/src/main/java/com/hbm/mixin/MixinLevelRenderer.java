/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.mixin;

import com.hbm.client.render.ArmorOverheadRenderer;
import com.hbm.client.render.CargoElevatorOutline;
import com.hbm.client.render.ConveyorPreviewRenderer;
import com.hbm.client.render.GlyphidPathRenderer;
import com.hbm.client.render.NTMSkybox;
import com.hbm.client.render.flywheel.VisualTextures;
import com.hbm.items.weapon.sedna.factory.XFactoryDrill;
import com.hbm.items.weapon.sedna.impl.GunDrillItem;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.framegraph.FramePass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.renderpearl.api.commands.RenderPass;
import java.util.Optional;
import java.util.OptionalDouble;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class MixinLevelRenderer {
    @WrapOperation(
            method = "lambda$addMainPass$0",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/renderer/LevelRenderer;executeOit(Lnet/minecraft/client/renderer/chunk/ChunkSectionsToRender;Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher$PreparedFrame;)V"),
            require = 1)
    private void hbm$orderedNativeBlends(
            LevelRenderer renderer,
            ChunkSectionsToRender chunks,
            FeatureRenderDispatcher.PreparedFrame frame,
            Operation<Void> original) {
        original.call(renderer, chunks, frame);
        if (!((com.hbm.client.render.OrderedBlendFrame) (frame)).hbm$hasOrderedBlends()) return;
        var target = Minecraft.getInstance().gameRenderer.mainRenderTarget();
        try (RenderPass pass =
                RenderSystem.getDevice()
                        .createCommandEncoder()
                        .createRenderPass(
                                () -> "NTM ordered blends",
                                target.getColorTextureView(),
                                Optional.empty(),
                                target.getDepthTextureView(),
                                OptionalDouble.empty())) {
            RenderSystem.bindDefaultUniforms(pass);
            ((com.hbm.client.render.OrderedBlendFrame) (frame)).hbm$drawOrderedBlends(pass);
        }
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void hbm$animateVisualBodies(CallbackInfo ci) {
        VisualTextures.activateSprites();
    }

    private static final String SKY_PASS =
            "addSkyPass(Lcom/mojang/blaze3d/framegraph/FrameGraphBuilder;Lnet/minecraft/client/renderer/state/level/CameraRenderState;Lcom/mojang/renderpearl/api/buffers/GpuBufferSlice;)V";

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
        if (!(held.getItem() instanceof GunDrillItem)) {
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
