// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.client.model.SectionGeometry;
import com.hbm.client.model.SectionedModel;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import java.util.Map;
import net.fabricmc.fabric.api.client.renderer.v1.sprite.FabricPreparations;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ModelManager.class)
public class MixinModelManager {
    @Inject(method = "loadModels", at = @At("HEAD"))
    private static void hbm$blockSprites(
            CallbackInfoReturnable<?> cir,
            @Local(argsOnly = true, ordinal = 0) SpriteLoader.Preparations blockAtlas) {
        SectionGeometry.blockSprites(((FabricPreparations) blockAtlas).spriteFinder());
    }

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
