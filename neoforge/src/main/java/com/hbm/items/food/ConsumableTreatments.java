// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.food;

import com.hbm.config.VersatileConfig;
import com.hbm.sound.EntitySounds;
import com.hbm.util.InventoryUtil;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class ConsumableTreatments {
    public static final net.minecraft.world.food.FoodProperties MEDICINE_FOOD =
            new net.minecraft.world.food.FoodProperties.Builder()
                    .nutrition(0)
                    .saturationModifier(0F)
                    .alwaysEdible()
                    .build();
    public static final net.minecraft.world.item.component.Consumable MEDICINE_CONSUMABLE =
            net.minecraft.world.item.component.Consumable.builder().consumeSeconds(0.5F).build();
    private static Reference2ObjectOpenHashMap<Item, ConsumableTreatment> treatments =
            new Reference2ObjectOpenHashMap<>(0);
    private static Reference2ObjectOpenHashMap<Item, net.minecraft.util.RandomSource> randoms =
            new Reference2ObjectOpenHashMap<>(0);

    private ConsumableTreatments() {}

    public static void applyDataPack(RegistryAccess registries) {
        var next = new Reference2ObjectOpenHashMap<Item, ConsumableTreatment>();
        var nextRandoms = new Reference2ObjectOpenHashMap<Item, net.minecraft.util.RandomSource>();
        Map<Item, Identifier> claims = new HashMap<>();
        for (var holder :
                registries.lookupOrThrow(ConsumableTreatment.REGISTRY).listElements().toList()) {
            for (var target : holder.value().targets()) {
                Identifier prior = claims.putIfAbsent(target.value(), holder.key().identifier());
                if (prior != null)
                    throw new IllegalStateException(
                            "Duplicate treatment target "
                                    + target.unwrapKey().orElseThrow().identifier()
                                    + ": "
                                    + prior
                                    + " and "
                                    + holder.key().identifier());
                next.put(target.value(), holder.value());
                nextRandoms.put(target.value(), net.minecraft.util.RandomSource.create());
            }
        }
        treatments = next;
        randoms = nextRandoms;
    }

    public static ConsumableTreatment forStack(ItemStack stack) {
        return treatments.get(stack.getItem());
    }

    public static InteractionResult use(
            ItemStack stack, Level level, Player player, InteractionHand hand) {
        var rule = forStack(stack);
        if (rule == null) return null;
        if (rule.blockWhenPotionSick() && VersatileConfig.hasPotionSickness(player))
            return InteractionResult.PASS;
        if (rule.trigger() != ConsumableTreatment.Trigger.USE) return null;
        if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
        Item item = stack.getItem();
        ItemStack held =
                InventoryUtil.exchangeHeld(
                        player,
                        stack,
                        rule.remainder()
                                .map(remainder -> new ItemStack(remainder.value()))
                                .orElse(ItemStack.EMPTY));
        execute(rule, item, player);
        return InteractionResult.SUCCESS.heldItemTransformedTo(held);
    }

    public static void consumed(
            ConsumableTreatment rule,
            Item item,
            Level level,
            net.minecraft.world.entity.LivingEntity entity) {
        if (rule != null
                && rule.trigger() == ConsumableTreatment.Trigger.CONSUME
                && level instanceof ServerLevel
                && entity instanceof Player player) execute(rule, item, player);
    }

    private static void execute(ConsumableTreatment rule, Item item, Player player) {
        rule.sound()
                .ifPresent(
                        sound ->
                                EntitySounds.play(
                                        player,
                                        player.getX(),
                                        player.getY(),
                                        player.getZ(),
                                        sound.value(),
                                        SoundSource.PLAYERS,
                                        1F,
                                        1F));
        for (TreatmentAction action : rule.actions()) action.apply(player, randoms.get(item));
    }

    public static void clear() {
        treatments = new Reference2ObjectOpenHashMap<>(0);
        randoms = new Reference2ObjectOpenHashMap<>(0);
    }
}
