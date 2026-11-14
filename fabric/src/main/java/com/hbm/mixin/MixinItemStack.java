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

import com.hbm.handler.ArmorUtil;
import com.hbm.handler.EquipmentProtections;
import com.hbm.interfaces.injected.GunTickState;
import com.hbm.inventory.InventorySlotBinding;
import com.hbm.items.food.ConsumableTreatment.Trigger;
import com.hbm.items.food.ConsumableTreatments;
import com.hbm.items.weapon.CrucibleItem;
import com.hbm.items.weapon.sedna.GunTimers;
import com.hbm.packet.SyncBindings;
import com.hbm.packet.SyncSource;
import com.hbm.util.TooltipStyle;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.serialization.MapCodec;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import org.apache.commons.lang3.function.TriConsumer;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
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
        if (!(stack.getItem() instanceof CrucibleItem sword) || sword.canOperate(stack)) {
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
        if (!(stack.getItem() instanceof CrucibleItem sword) || sword.canOperate(stack)) {
            original.call(modifiers, slot, consumer);
        }
    }

    @Redirect(
            method = "setCount",
            at =
                    @At(
                            value = "FIELD",
                            target = "Lnet/minecraft/world/item/ItemStack;count:I",
                            opcode = Opcodes.PUTFIELD))
    private void hbm$syncCount(ItemStack stack, int next) {
        if (count != next) {
            count = next;
            syncChanged(3);
            InventorySlotBinding.changed(stack);
        }
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

    @ModifyReturnValue(method = "getUseDuration", at = @At("RETURN"))
    private int hbm$declaredUseDuration(int duration) {
        var rule = ConsumableTreatments.forStack((ItemStack) (Object) this);
        return rule != null && rule.trigger() == Trigger.USE ? 0 : duration;
    }

    @WrapOperation(
            method = "use",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/item/Item;use(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;"))
    private InteractionResult hbm$declaredUse(
            Item item,
            Level level,
            Player player,
            InteractionHand hand,
            Operation<InteractionResult> original) {
        ItemStack stack = (ItemStack) (Object) this;
        InteractionResult treatment = ConsumableTreatments.use(stack, level, player, hand);
        if (treatment != null) return treatment;
        if (EquipmentProtections.isMask(stack) && ArmorUtil.ejectGasMaskFilter(player, stack))
            return InteractionResult.SUCCESS;
        if (EquipmentProtections.isFilter(stack))
            return ArmorUtil.installFilterFromHand(level, player, hand);
        return original.call(item, level, player, hand);
    }

    @WrapOperation(
            method = "finishUsingItem",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/item/Item;finishUsingItem(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack hbm$declaredTreatment(
            Item item,
            ItemStack stack,
            Level level,
            LivingEntity entity,
            Operation<ItemStack> original) {
        var rule = ConsumableTreatments.forStack(stack);
        ItemStack result = original.call(item, stack, level, entity);
        ConsumableTreatments.consumed(rule, item, level, entity);
        return result;
    }

    @WrapOperation(
            method = "addDetailsToTooltip",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/item/Item;appendHoverText(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/Item$TooltipContext;Lnet/minecraft/world/item/component/TooltipDisplay;Ljava/util/function/Consumer;Lnet/minecraft/world/item/TooltipFlag;)V"))
    private void hbm$declaredEquipmentTooltip(
            Item item,
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> builder,
            TooltipFlag flag,
            Operation<Void> original) {
        Consumer<Component> adder =
                TooltipStyle.isNtm(item) ? TooltipStyle.defaultGray(builder) : builder;
        boolean mask = EquipmentProtections.isMask(stack);
        boolean late = EquipmentProtections.lateFilterTooltip(stack);
        if (mask && !late) {
            ArmorUtil.addGasMaskTooltip(stack, context, flag, adder);
            ArmorUtil.addGasMaskBlacklist(stack, adder);
        }
        EquipmentProtections.appendCladdingTooltip(stack, adder);
        original.call(item, stack, context, display, adder, flag);
        if (mask && late) {
            ArmorUtil.addGasMaskTooltip(stack, context, flag, adder);
            ArmorUtil.addGasMaskBlacklist(stack, adder);
        }
    }

    @Inject(method = "copy", at = @At("RETURN"))
    private void hbm$copyGunTimers(CallbackInfoReturnable<ItemStack> cir) {
        if (hbm$gunTimers != null && !cir.getReturnValue().isEmpty()) {
            ((com.hbm.interfaces.injected.GunTickState) (java.lang.Object) (cir.getReturnValue()))
                    .hbm$setGunTimers(hbm$gunTimers.copy());
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

    private SyncSource hbm$syncOwner;

    private int hbm$syncMask;

    private int hbm$syncBindings;

    private long hbm$syncUnits;

    public final boolean syncBound() {
        return hbm$syncOwner != null;
    }

    public final void syncChanged(int mask) {
        if (hbm$syncOwner == null) return;
        if (hbm$syncUnits == 0) hbm$syncOwner.syncChanged(hbm$syncMask);
        else hbm$syncOwner.syncUnitsChanged(hbm$syncMask, hbm$syncUnits);
    }

    public final void syncUnitsChanged(int mask, long units) {
        syncChanged(mask);
    }

    public final void bindSync(SyncSource owner, int mask) {
        bindSyncUnits(owner, mask, 0);
    }

    public final void bindSyncUnits(SyncSource owner, int mask, long units) {
        if (hbm$syncOwner != null && hbm$syncOwner != owner) {
            throw new IllegalStateException("Mutable sync state has two owners");
        }
        hbm$syncOwner = owner;
        hbm$syncMask |= mask;
        hbm$syncUnits |= units;
        if (hbm$syncBindings++ == 0) SyncBindings.bindFields(this, owner);
    }

    public final void unbindSync(SyncSource owner) {
        assert hbm$syncOwner == owner;
        if (--hbm$syncBindings != 0) return;
        SyncBindings.unbindFields(this, owner);
        hbm$syncOwner = null;
        hbm$syncMask = 0;
        hbm$syncUnits = 0;
    }
}
