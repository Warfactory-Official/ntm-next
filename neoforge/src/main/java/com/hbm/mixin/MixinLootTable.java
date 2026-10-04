// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.world.loot.RandomSlotLoot;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.serialization.Codec;
import java.util.Optional;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LootTable.class)
public abstract class MixinLootTable implements RandomSlotLoot {
    @Shadow @Final private Optional<Identifier> randomSequence;
    @Unique private boolean hbm$randomSlots;

    @ModifyExpressionValue(
            method = "<clinit>",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lcom/mojang/serialization/Codec;lazyInitialized(Ljava/util/function/Supplier;)Lcom/mojang/serialization/Codec;"))
    private static Codec<LootTable> hbm$containerCodec(Codec<LootTable> original) {
        return RandomSlotLoot.codec(original);
    }

    @Override
    public boolean hbm$randomSlots() {
        return hbm$randomSlots;
    }

    @Override
    public void hbm$randomSlots(boolean enabled) {
        hbm$randomSlots = enabled;
    }

    @Inject(method = "fill", at = @At("HEAD"), cancellable = true)
    private void hbm$fillRandomSlots(
            Container container, LootParams params, long seed, CallbackInfo ci) {
        if (!hbm$randomSlots) return;
        LootContext context =
                new LootContext.Builder(params).withOptionalRandomSeed(seed).create(randomSequence);
        RandomSlotLoot.fill((LootTable) (Object) this, container, context);
        ci.cancel();
    }
}
