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

import com.hbm.api.client.CloudLayers;
import java.util.Optional;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.renderer.CloudRenderer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CloudRenderer.class)
public class MixinCloudRenderer {
    @Unique
    private boolean hbm$vanilla() {
        return (Object) this.getClass() == CloudRenderer.class;
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void hbm$addonLayers(
            int color,
            CloudStatus status,
            float bottomY,
            int range,
            Vec3 camera,
            long gameTime,
            float partialTicks,
            CallbackInfo ci) {
        if (hbm$vanilla())
            CloudLayers.render(color, status, bottomY, range, camera, gameTime, partialTicks);
    }

    @Inject(
            method =
                    "apply(Ljava/util/Optional;Lnet/minecraft/server/packs/resources/ResourceManager;"
                            + "Lnet/minecraft/util/profiling/ProfilerFiller;)V",
            at = @At("TAIL"))
    private void hbm$addonReload(
            Optional<?> texture,
            ResourceManager manager,
            ProfilerFiller profiler,
            CallbackInfo ci) {
        if (hbm$vanilla()) CloudLayers.reload();
    }

    @Inject(method = "markForRebuild", at = @At("TAIL"))
    private void hbm$addonRebuild(CallbackInfo ci) {
        if (hbm$vanilla()) CloudLayers.markForRebuild();
    }

    @Inject(method = "endFrame", at = @At("TAIL"))
    private void hbm$addonEndFrame(CallbackInfo ci) {
        if (hbm$vanilla()) CloudLayers.endFrame();
    }

    @Inject(method = "close", at = @At("TAIL"))
    private void hbm$addonClose(CallbackInfo ci) {
        if (hbm$vanilla()) CloudLayers.close();
    }
}
