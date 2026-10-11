// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.api.entity.LivingHooks;
import com.hbm.blocks.ClimbBoxes;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.config.FabricHbmConfig;
import com.hbm.extprop.HbmLivingProps;
import com.hbm.handler.ArmorModHandler;
import com.hbm.handler.ArmorUtil;
import com.hbm.handler.EntityEffectHandler;
import com.hbm.hazard.HazardSystem;
import com.hbm.interfaces.NtmDamageContext;
import com.hbm.interfaces.injected.ArmorModWearer;
import com.hbm.inventory.InventoryChanges;
import com.hbm.items.IAnimatedItem;
import com.hbm.items.ISwingReceiver;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModItems;
import com.hbm.items.armor.ArmorMods;
import com.hbm.items.armor.ArmorSuitEffects;
import com.hbm.items.armor.ItemArmorMod;
import com.hbm.items.armor.ItemModDefuser;
import com.hbm.items.armor.ModArmorItem;
import com.hbm.items.armor.ServoEffects;
import com.hbm.items.food.FoodAdditives;
import com.hbm.items.weapon.sedna.AkimboGhost;
import com.hbm.util.EntityDamageUtil;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class MixinLivingEntity implements NtmDamageContext, ArmorModWearer {

    @Unique private ArmorMods hbm$feetMods = ArmorMods.EMPTY;
    @Unique private ArmorMods hbm$legsMods = ArmorMods.EMPTY;
    @Unique private ArmorMods hbm$chestMods = ArmorMods.EMPTY;
    @Unique private ArmorMods hbm$headMods = ArmorMods.EMPTY;
    @Unique private int hbm$armorSlots;
    @Unique private @Nullable ItemStack[] hbm$socketStacks;
    @Unique private long hbm$installedMods;
    @Unique private long hbm$serverMods;
    @Unique private long hbm$clientMods;
    @Unique private long hbm$hazardMods;
    @Unique private int hbm$hazardGeneration;
    @Unique private int hbm$servoSources;
    @Unique private int hbm$servoPieces;
    @Unique private long hbm$servoMods;

    @Unique
    private ArmorMods hbm$mods(int piece) {
        return switch (piece) {
            case 0 -> hbm$feetMods;
            case 1 -> hbm$legsMods;
            case 2 -> hbm$chestMods;
            case 3 -> hbm$headMods;
            default -> throw new AssertionError();
        };
    }

    @Override
    public void hbm$reconcileArmorMods() {
        LivingEntity self = (LivingEntity) (Object) this;
        boolean healthChanged = false;
        boolean changed = false;
        for (int piece = 0; piece < ArmorUtil.ARMOR_SLOTS.length; piece++) {
            ItemStack armor = self.getItemBySlot(ArmorUtil.ARMOR_SLOTS[piece]);
            ArmorMods next = ArmorMods.EMPTY;
            EquipmentSlot slot = EquipmentSlot.MAINHAND;
            int shape = 0;
            if (ArmorModHandler.isArmor(armor)) {
                next = ArmorModHandler.mods(armor);
                slot = armor.get(net.minecraft.core.component.DataComponents.EQUIPPABLE).slot();
                shape = slot.ordinal() + 1;
            }
            ArmorMods previous = hbm$mods(piece);
            int shift = piece * 4;
            int previousShape = (hbm$armorSlots >>> shift) & 15;
            if (previous == next && shape == previousShape) continue;
            changed = true;
            if (!self.level().isClientSide()
                    && (shape != previousShape || !next.sameAttributes(previous)))
                healthChanged |=
                        ArmorModHandler.reconcileAttributes(self, previous, next, slot, piece);
            hbm$armorSlots = (hbm$armorSlots & ~(15 << shift)) | shape << shift;
            switch (piece) {
                case 0 -> hbm$feetMods = next;
                case 1 -> hbm$legsMods = next;
                case 2 -> hbm$chestMods = next;
                case 3 -> hbm$headMods = next;
            }
            int offset = piece * ArmorModHandler.MOD_SLOTS;
            int source = ServoEffects.source(next, slot);
            int sourceShift = piece * 8;
            hbm$servoPieces = (hbm$servoPieces & ~(255 << sourceShift)) | source << sourceShift;
            long mask = 511L << offset;
            hbm$installedMods &= ~mask;
            hbm$serverMods &= ~mask;
            hbm$clientMods &= ~mask;
            hbm$hazardMods &= ~mask;
            hbm$servoMods &= ~mask;
            if (source != 0) hbm$servoMods |= 1L << (offset + ArmorModHandler.SERVOS);
            for (int socket = 0; socket < ArmorModHandler.MOD_SLOTS; socket++) {
                int index = offset + socket;
                if (!(next.item(socket) instanceof ItemArmorMod mod)) {
                    if (hbm$socketStacks != null) hbm$socketStacks[index] = null;
                    continue;
                }
                if (hbm$socketStacks == null)
                    hbm$socketStacks = new ItemStack[4 * ArmorModHandler.MOD_SLOTS];
                if (hbm$socketStacks[index] == null
                        || !java.util.Objects.equals(
                                previous.template(socket), next.template(socket)))
                    hbm$socketStacks[index] = next.copy(socket);
                long bit = 1L << index;
                hbm$installedMods |= bit;
                if (mod.ticksServer()) hbm$serverMods |= bit;
                if (mod.ticksClient()) hbm$clientMods |= bit;
                ItemStack payload = hbm$socketStacks[index];
                if (!self.level().isClientSide()
                        && (HazardSystem.readsLiveStorage(payload)
                                || HazardSystem.isStackHazardous(payload))) hbm$hazardMods |= bit;
            }
        }
        if (healthChanged && self.getHealth() > self.getMaxHealth())
            self.setHealth(self.getMaxHealth());
        if (hbm$installedMods == 0) hbm$socketStacks = null;
        if (changed) {
            int sources = 0;
            for (int piece = 0; piece < ArmorUtil.ARMOR_SLOTS.length; piece++) {
                int source = (hbm$servoPieces >>> (piece * 8)) & 255;
                sources =
                        Math.max(sources & 15, source & 15)
                                | Math.max(sources >>> 4, source >>> 4) << 4;
            }
            if (!self.level().isClientSide() && sources != hbm$servoSources)
                ServoEffects.sourcesChanged(self, hbm$servoSources, sources);
            hbm$servoSources = sources;
        }
    }

    @Unique
    private void hbm$refreshSocketHazards() {
        int generation = HazardSystem.generation();
        if (generation == hbm$hazardGeneration) return;
        hbm$hazardGeneration = generation;
        hbm$hazardMods = 0;
        long mask = hbm$installedMods;
        while (mask != 0) {
            int index = Long.numberOfTrailingZeros(mask);
            long bit = 1L << index;
            mask &= ~bit;
            ItemStack payload = hbm$socketStacks[index];
            if (HazardSystem.readsLiveStorage(payload) || HazardSystem.isStackHazardous(payload))
                hbm$hazardMods |= bit;
        }
    }

    @Override
    public void hbm$tickArmorMods() {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self instanceof Creeper creeper && HbmLivingProps.isDefused(creeper))
            ItemModDefuser.castrateCreeper(creeper, null, false);
        hbm$refreshSocketHazards();
        long mask = hbm$serverMods | hbm$hazardMods | hbm$servoMods;
        while (mask != 0 && !self.isRemoved()) {
            int index = Long.numberOfTrailingZeros(mask);
            long bit = 1L << index;
            mask &= ~bit;
            if (((hbm$serverMods | hbm$hazardMods | hbm$servoMods) & bit) == 0) continue;
            int piece = index / ArmorModHandler.MOD_SLOTS;
            ItemStack armor = self.getItemBySlot(ArmorUtil.ARMOR_SLOTS[piece]);
            if ((hbm$servoMods & bit) != 0)
                ServoEffects.serverTick(self, (hbm$servoPieces >>> (piece * 8)) & 255);
            if ((hbm$serverMods & bit) != 0)
                ((ItemArmorMod) hbm$socketStacks[index].getItem()).modUpdate(self, armor);
            if (ArmorModHandler.mods(self.getItemBySlot(ArmorUtil.ARMOR_SLOTS[piece]))
                    != hbm$mods(piece)) hbm$reconcileArmorMods();
            if ((hbm$hazardMods & bit) != 0) {
                ItemStack payload = hbm$socketStacks[index];
                HazardSystem.applyHazards(payload, self);
                hbm$reconcileArmorMods();
                if ((hbm$hazardMods & bit) != 0 && hbm$socketStacks[index] == payload) {
                    int socket = index % ArmorModHandler.MOD_SLOTS;
                    var template = hbm$mods(piece).template(socket);
                    if (payload.getCount() != template.count()
                            || !payload.getComponentsPatch().equals(template.components()))
                        hbm$socketStacks[index] = hbm$mods(piece).copy(socket);
                }
            }
        }
    }

    @Override
    public void hbm$tickClientArmorMods() {
        hbm$reconcileArmorMods();
        LivingEntity self = (LivingEntity) (Object) this;
        if (hbm$servoSources != 0) ServoEffects.clientTick(self, hbm$servoSources);
        long mask = hbm$clientMods;
        while (mask != 0) {
            int index = Long.numberOfTrailingZeros(mask);
            mask &= mask - 1;
            ItemStack payload = hbm$socketStacks[index];
            ((ItemArmorMod) payload.getItem())
                    .modUpdateClient(
                            self,
                            self.getItemBySlot(
                                    ArmorUtil.ARMOR_SLOTS[index / ArmorModHandler.MOD_SLOTS]));
        }
    }

    @Inject(method = "collectEquipmentChanges", at = @At("RETURN"))
    private void hbm$reconcileChangedArmor(
            Map<EquipmentSlot, ItemStack> previous,
            CallbackInfoReturnable<Map<EquipmentSlot, ItemStack>> cir) {
        if (cir.getReturnValue() != null) hbm$reconcileArmorMods();
    }

    @Inject(
            method = "tick",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/entity/LivingEntity;detectEquipmentUpdates()V",
                            shift = At.Shift.AFTER))
    private void hbm$tickEquippedArmor(CallbackInfo ci) {
        hbm$tickArmorMods();
    }

    @Inject(method = "tickEffects", at = @At("RETURN"))
    private void hbm$holdClientServoDuration(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level().isClientSide() && hbm$servoSources != 0)
            ServoEffects.clientTick(self, hbm$servoSources);
    }

    @Unique private float hbm$pierceDT;
    @Unique private float hbm$pierceDR;
    @Unique private double hbm$knockbackMultiplier = Double.NaN;
    @Unique private boolean hbm$scaledKnockback;
    @Unique private boolean hbm$ignoreEarlyCancellation;
    @Unique private @Nullable AABB hbm$climbProbed;
    @Unique private int hbm$climbProbeTick;
    @Unique private @Nullable BlockPos hbm$climbBoxAt;

    @Shadow private Optional<BlockPos> lastClimbablePos;

    @Inject(method = "collectEquipmentChanges", at = @At("HEAD"))
    private void hbm$equipAnimatedTool(
            Map<EquipmentSlot, ItemStack> previous,
            CallbackInfoReturnable<Map<EquipmentSlot, ItemStack>> cir) {
        if (!((Object) this instanceof ServerPlayer player)) return;
        ItemStack current = player.getMainHandItem();
        if (previous.get(EquipmentSlot.MAINHAND).getItem() != current.getItem()
                && current.getItem() instanceof IAnimatedItem item) item.onEquip(player, current);
    }

    @Inject(method = "collectEquipmentChanges", at = @At("RETURN"))
    private void hbm$equipmentFusesChanged(
            Map<EquipmentSlot, ItemStack> previous,
            CallbackInfoReturnable<Map<EquipmentSlot, ItemStack>> cir) {
        if (cir.getReturnValue() != null) {
            LivingEntity owner = (LivingEntity) (Object) this;
            cir.getReturnValue()
                    .forEach(
                            (slot, stack) -> InventoryChanges.equipmentChanged(owner, slot, stack));
        }
    }

    @Override
    public float hbm$pierceDT() {
        return hbm$pierceDT;
    }

    @Override
    public float hbm$pierceDR() {
        return hbm$pierceDR;
    }

    @Override
    public void hbm$setPiercing(float dt, float dr) {
        hbm$pierceDT = dt;
        hbm$pierceDR = dr;
    }

    @Override
    public double hbm$knockbackMultiplier() {
        return hbm$knockbackMultiplier;
    }

    @Override
    public void hbm$setKnockbackMultiplier(double multiplier) {
        hbm$knockbackMultiplier = multiplier;
    }

    @Override
    public boolean hbm$ignoreEarlyCancellation() {
        return hbm$ignoreEarlyCancellation;
    }

    @Override
    public void hbm$setIgnoreEarlyCancellation(boolean ignore) {
        hbm$ignoreEarlyCancellation = ignore;
    }

    @WrapOperation(
            method = "dealDefaultKnockback",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/entity/LivingEntity;knockback(DDDLnet/minecraft/world/damagesource/DamageSource;F)V"))
    private void hbm$scaledHitKnockback(
            LivingEntity self,
            double strength,
            double x,
            double z,
            DamageSource source,
            float damage,
            Operation<Void> original) {
        if (Double.isNaN(hbm$knockbackMultiplier)) {
            original.call(self, strength, x, z, source, damage);
        } else if (hbm$knockbackMultiplier > 0D) {
            boolean previous = hbm$scaledKnockback;
            hbm$scaledKnockback = true;
            try {
                original.call(self, 0.4D * hbm$knockbackMultiplier, x, z, source, damage);
            } finally {
                hbm$scaledKnockback = previous;
            }
        }
    }

    @WrapOperation(
            method = "knockback(DDDLnet/minecraft/world/damagesource/DamageSource;FZ)V",
            at = @At(value = "INVOKE", target = "Ljava/lang/Math;min(DD)D"))
    private double hbm$scaledHitLift(double limit, double lift, Operation<Double> original) {
        return hbm$scaledKnockback
                ? (lift > 0.2D ? 0.2D * hbm$knockbackMultiplier : lift)
                : original.call(limit, lift);
    }

    @ModifyArg(
            method = "getDamageAfterArmorAbsorb",
            index = 3,
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/damagesource/CombatRules;getDamageAfterAbsorb(Lnet/minecraft/world/entity/LivingEntity;FLnet/minecraft/world/damagesource/DamageSource;FF)F"))
    private float hbm$piercedVanillaArmor(float armor) {
        return FabricHbmConfig.runtime().damageCompatibilityMode()
                ? armor
                : armor * (1F - hbm$pierceDR);
    }

    @WrapOperation(
            method = "getDamageAfterArmorAbsorb",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/entity/LivingEntity;hurtArmor(Lnet/minecraft/world/damagesource/DamageSource;F)V"))
    private void hbm$armorWearOutsideSedna(
            LivingEntity self, DamageSource source, float damage, Operation<Void> original) {
        if (Double.isNaN(hbm$knockbackMultiplier)
                || FabricHbmConfig.runtime().damageCompatibilityMode()) {
            original.call(self, source, damage);
        }
    }

    @WrapOperation(
            method = "completeUsingItem",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/item/ItemStack;finishUsingItem(Lnet/minecraft/world/level/Level;"
                                            + "Lnet/minecraft/world/entity/LivingEntity;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack hbm$applyFoodAdditives(
            ItemStack stack, Level level, LivingEntity entity, Operation<ItemStack> original) {
        int additives =
                FoodAdditives.isFood(stack)
                        ? stack.getOrDefault(ModDataComponents.FOOD_ADDITIVES.get(), 0)
                        : 0;
        ItemStack result = original.call(stack, level, entity);
        if (additives != 0) FoodAdditives.apply(entity, additives);
        return result;
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void hbm$livingUpdate(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        EntityEffectHandler.onLivingTick(self);
        LivingHooks.tick(self);
    }

    @ModifyReturnValue(method = "onClimbable", at = @At("RETURN"))
    private boolean hbm$climbBox(boolean climbing) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (climbing
                || self.isSpectator()
                || self.isFallFlying() && self.getInBlockState().is(BlockTags.CAN_GLIDE_THROUGH)) {
            return climbing;
        }
        AABB bb = self.getBoundingBox();
        if (bb != hbm$climbProbed || self.tickCount != hbm$climbProbeTick) {
            hbm$climbProbed = bb;
            hbm$climbProbeTick = self.tickCount;
            hbm$climbBoxAt = ClimbBoxes.find(self.level(), bb);
        }
        if (hbm$climbBoxAt == null) return false;
        lastClimbablePos = Optional.of(hbm$climbBoxAt);
        return true;
    }

    @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
    private void hbm$armorCancel(
            ServerLevel level,
            DamageSource source,
            float damage,
            CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof Player)) return;
        if (source.is(DamageTypeTags.IS_FIRE)
                && ArmorSuitEffects.wearsAny(self, ModArmorItem.Suit.ASBESTOS)) {
            cir.setReturnValue(false);
        } else if (!source.is(DamageTypeTags.BYPASSES_ARMOR)
                && self.getItemBySlot(EquipmentSlot.HEAD).getItem() == ModItems.NO9.get()
                && damage <= 0.5F) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "doHurtEquipment", at = @At("HEAD"))
    private void hbm$drainSuitsOnArmorDamage(
            DamageSource source, float damage, EquipmentSlot[] slots, CallbackInfo ci) {
        if (damage <= 0F) return;
        int wear = (int) Math.max(1.0F, damage / 4.0F);
        for (EquipmentSlot slot : slots)
            ArmorUtil.drainSupplyForWear((LivingEntity) (Object) this, slot, wear);
    }

    @Inject(method = "swing(Lnet/minecraft/world/InteractionHand;Z)V", at = @At("HEAD"))
    private void hbm$entitySwing(
            InteractionHand hand, boolean sendToSwingingEntity, CallbackInfo ci) {
        if (hand != InteractionHand.MAIN_HAND || !((Object) this instanceof ServerPlayer player))
            return;
        ItemStack stack = player.getMainHandItem();
        if (stack.getItem() instanceof ISwingReceiver receiver)
            receiver.onEntitySwing(player, stack);
    }

    @Inject(
            method =
                    "drop(Lnet/minecraft/world/item/ItemStack;ZZ)Lnet/minecraft/world/entity/item/ItemEntity;",
            at = @At("HEAD"),
            cancellable = true)
    private void hbm$discardAkimboGhost(
            ItemStack itemStack,
            boolean randomly,
            boolean thrownFromHand,
            CallbackInfoReturnable<ItemEntity> cir) {
        if (AkimboGhost.isGhost(itemStack)) cir.setReturnValue(null);
    }

    @WrapOperation(
            method = "checkFallDamage",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/server/level/ServerLevel;sendParticles"
                                            + "(Lnet/minecraft/core/particles/ParticleOptions;DDDIDDDD)I"))
    private int hbm$landingParticlesFollowTheMachine(
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

    @ModifyVariable(method = "actuallyHurt", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float hbm$acceptedDamage(float amount, ServerLevel level, DamageSource source) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.isInvulnerableTo(level, source)) return amount;
        float result = EntityDamageUtil.modifyAcceptedDamage(self, source, amount);
        return result;
    }
}
