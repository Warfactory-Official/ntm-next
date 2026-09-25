// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.interfaces.injected.GunTickState;
import com.hbm.items.weapon.ItemCrucible;
import com.hbm.items.weapon.sedna.GunTimers;
import com.hbm.packet.SyncSource;
import com.hbm.util.TooltipStyle;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.serialization.MapCodec;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import org.apache.commons.lang3.function.TriConsumer;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class MixinItemStack implements GunTickState, SyncSource {

    @Shadow private int count;

    @WrapOperation(
            method =
                    "forEachModifier(Lnet/minecraft/world/entity/EquipmentSlot;Ljava/util/function/BiConsumer;)V",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/item/component/ItemAttributeModifiers;forEach(Lnet/minecraft/world/entity/EquipmentSlot;Ljava/util/function/BiConsumer;)V"))
    private void hbm$crucibleEquipmentAttributes(
            ItemAttributeModifiers modifiers,
            EquipmentSlot slot,
            BiConsumer<Holder<Attribute>, AttributeModifier> consumer,
            Operation<Void> original) {
        ItemStack stack = (ItemStack) (Object) this;
        if (!(stack.getItem() instanceof ItemCrucible sword) || sword.canOperate(stack)) {
            original.call(modifiers, slot, consumer);
        }
    }

    @WrapOperation(
            method =
                    "forEachModifier(Lnet/minecraft/world/entity/EquipmentSlotGroup;Lorg/apache/commons/lang3/function/TriConsumer;)V",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/item/component/ItemAttributeModifiers;forEach(Lnet/minecraft/world/entity/EquipmentSlotGroup;Lorg/apache/commons/lang3/function/TriConsumer;)V"))
    private void hbm$crucibleTooltipAttributes(
            ItemAttributeModifiers modifiers,
            EquipmentSlotGroup slot,
            TriConsumer<Holder<Attribute>, AttributeModifier, ItemAttributeModifiers.Display>
                    consumer,
            Operation<Void> original) {
        ItemStack stack = (ItemStack) (Object) this;
        if (!(stack.getItem() instanceof ItemCrucible sword) || sword.canOperate(stack)) {
            original.call(modifiers, slot, consumer);
        }
    }

    @Inject(method = "setCount", at = @At("HEAD"))
    private void hbm$syncCount(int next, CallbackInfo ci) {
        if (count != next) syncChanged(3);
    }

    @Unique private @Nullable GunTimers hbm$gunTimers;

    @ModifyExpressionValue(
            method = "<clinit>",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lcom/mojang/serialization/MapCodec;recursive(Ljava/lang/String;Ljava/util/function/Function;)Lcom/mojang/serialization/MapCodec;"))
    private static MapCodec<ItemStack> hbm$saveGunCounters(MapCodec<ItemStack> codec) {
        return codec.xmap(stack -> stack, GunTimers::forSave);
    }

    @ModifyArg(
            method = "addDetailsToTooltip",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/item/Item;appendHoverText(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/Item$TooltipContext;Lnet/minecraft/world/item/component/TooltipDisplay;Ljava/util/function/Consumer;Lnet/minecraft/world/item/TooltipFlag;)V"),
            index = 3)
    private Consumer<Component> hbm$grayHoverText(Consumer<Component> builder) {
        return TooltipStyle.isNtm(((ItemStack) (Object) this).getItem())
                ? TooltipStyle.defaultGray(builder)
                : builder;
    }

    @Inject(method = "copy", at = @At("RETURN"))
    private void hbm$copyGunTimers(CallbackInfoReturnable<ItemStack> cir) {
        if (hbm$gunTimers != null && !cir.getReturnValue().isEmpty()) {
            ((GunTickState) (Object) cir.getReturnValue()).hbm$setGunTimers(hbm$gunTimers.copy());
        }
    }

    @Override
    public @Nullable GunTimers hbm$gunTimers() {
        return hbm$gunTimers;
    }

    @Override
    public void hbm$setGunTimers(@Nullable GunTimers timers) {
        hbm$gunTimers = timers;
    }
}
