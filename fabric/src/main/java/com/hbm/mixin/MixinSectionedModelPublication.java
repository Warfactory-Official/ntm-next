// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.client.model.OccluderMasks;
import com.hbm.client.model.SectionedModel;
import java.util.Map;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModelManager.class)
public class MixinSectionedModelPublication {

    @ModifyArg(
            method = "apply",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/renderer/block/BlockStateModelSet;<init>(Ljava/util/Map;Lnet/minecraft/client/renderer/block/dispatch/BlockStateModel;)V"),
            index = 0)
    private Map<BlockState, BlockStateModel> hbm$sectionModels(
            Map<BlockState, BlockStateModel> models) {
        OccluderMasks.derive(models);
        return SectionedModel.wrap(models);
    }

    @Inject(method = "apply", at = @At("RETURN"))
    private void hbm$publish(CallbackInfo ci) {
        SectionedModel.publish();
    }
}
