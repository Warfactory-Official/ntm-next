// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.client.model.SectionGeometry;
import com.hbm.client.model.SectionedModel;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import java.util.Map;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModelManager.class)
public class MixinModelManager {
    @ModifyReturnValue(method = "createBlockStateToModelDispatch", at = @At("RETURN"))
    private static Map<BlockState, BlockStateModel> hbm$sectionModels(
            Map<BlockState, BlockStateModel> models) {
        return SectionedModel.prepare(models);
    }

    @Inject(method = "apply", at = @At("HEAD"))
    private void hbm$publish(CallbackInfo ci) {
        SectionedModel.publish();
    }

    @Inject(method = "apply", at = @At("RETURN"))
    private void hbm$reloadSections(CallbackInfo ci) {
        SectionGeometry.reload();
    }
}
