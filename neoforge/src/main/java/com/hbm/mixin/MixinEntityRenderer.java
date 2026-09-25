// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.client.VanishedEntities;
import com.hbm.client.render.CullableRenderer;
import com.hbm.client.render.MotionRenderState;
import com.hbm.client.render.VanishedRenderState;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public abstract class MixinEntityRenderer implements CullableRenderer {

    @Shadow
    protected abstract boolean affectedByCulling(Entity entity);

    @Override
    public boolean hbm$affectedByCulling(Entity entity) {
        return affectedByCulling(entity);
    }

    @Inject(
            method =
                    "createRenderState(Lnet/minecraft/world/entity/Entity;F)"
                            + "Lnet/minecraft/client/renderer/entity/state/EntityRenderState;",
            at = @At("RETURN"))
    private void hbm$extract(
            Entity entity, float partialTicks, CallbackInfoReturnable<EntityRenderState> cir) {
        EntityRenderState state = cir.getReturnValue();
        ((VanishedRenderState) state).hbm$setVanished(VanishedEntities.isVanished(entity));
        if (state instanceof MotionRenderState motion) {

            double motionY =
                    entity.isClientAuthoritative() && !entity.isLocalInstanceAuthoritative()
                            ? entity.getY() - entity.yOld
                            : entity.getDeltaMovement().y;
            motion.hbm$setMotion(entity.onGround(), motionY);
        }
    }
}
