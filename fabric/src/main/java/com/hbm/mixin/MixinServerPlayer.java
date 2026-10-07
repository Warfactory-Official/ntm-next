// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.advancement.HbmCriteria;
import com.hbm.api.entity.PlayerRespawns;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.inventory.InventoryChanges;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.TeleportTransition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
public abstract class MixinServerPlayer {

    @Inject(method = "initMenu", at = @At("TAIL"))
    private void hbm$watchMenuForHazards(AbstractContainerMenu container, CallbackInfo ci) {
        container.hbm$fuseOwner((ServerPlayer) (Object) this);
        InventoryChanges.bindMenu(container, (ServerPlayer) (Object) this);
        container.addSlotListener(new InventoryChanges.MenuWatcher((ServerPlayer) (Object) this));
    }

    @WrapOperation(
            method = "doCloseContainer",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/inventory/AbstractContainerMenu;removed(Lnet/minecraft/world/entity/player/Player;)V"))
    private void hbm$retireDeclaredMenu(
            AbstractContainerMenu menu, Player player, Operation<Void> original) {
        try {
            original.call(menu, player);
        } finally {
            InventoryChanges.unbindMenu(menu);
        }
    }

    @Inject(method = "openHorseInventory", at = @At("HEAD"))
    private void hbm$bindHorseInventory(AbstractHorse horse, Container inventory, CallbackInfo ci) {
        if (inventory instanceof SimpleContainer simple) simple.hbm$fuseOwner(horse);
    }

    @Inject(method = "onItemPickup", at = @At("HEAD"))
    private void hbm$itemPickedUp(ItemEntity entity, CallbackInfo ci) {
        HbmCriteria.itemPickedUp((ServerPlayer) (Object) this, entity.getItem());
    }

    @WrapOperation(
            method = "checkFallDamage",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/server/level/ServerLevel;sendParticles"
                                            + "(Lnet/minecraft/core/particles/ParticleOptions;DDDIDDDD)I"))
    private int hbm$fallBurstFollowsTheMachine(
            ServerLevel level,
            ParticleOptions particle,
            double x,
            double y,
            double z,
            int count,
            double xDist,
            double yDist,
            double zDist,
            double speed,
            Operation<Integer> original,
            @Local(argsOnly = true, name = "pos") BlockPos pos,
            @Local(argsOnly = true, name = "onState") BlockState onState) {
        ParticleOptions owned = MultiblockSurface.particleOptions(level, pos, onState, particle);
        return owned == null
                ? 0
                : original.call(level, owned, x, y, z, count, xDist, yDist, zDist, speed);
    }

    @Inject(method = "findRespawnPositionAndUseSpawnBlock", at = @At("HEAD"), cancellable = true)
    private void hbm$respawnRules(
            boolean consumeSpawnBlock,
            TeleportTransition.PostTeleportTransition then,
            CallbackInfoReturnable<TeleportTransition> cir) {
        TeleportTransition transition = PlayerRespawns.respawn((ServerPlayer) (Object) this, then);
        if (transition != null) cir.setReturnValue(transition);
    }
}
