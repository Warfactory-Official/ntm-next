// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.food;

import com.hbm.config.VersatileConfig;
import com.hbm.items.special.ItemCustomLore;
import com.hbm.lib.Library;
import com.hbm.platform.Services;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public class ItemDrinkEnergy extends ItemCustomLore {

    private static final Identifier BOTTLE_OPENER = Library.id("bottle_opener");

    private final @Nullable Identifier cap;
    private final boolean requiresOpener;
    private final Effect effect;

    public ItemDrinkEnergy(
            Properties properties,
            @Nullable Identifier cap,
            boolean requiresOpener,
            Effect effect) {
        super(properties);
        this.cap = cap;
        this.requiresOpener = requiresOpener;
        this.effect = effect;
    }

    private static Item resolve(Identifier id) {
        return BuiltInRegistries.ITEM
                .get(id)
                .orElseThrow(
                        () ->
                                new IllegalStateException(
                                        "ItemDrinkEnergy cap/opener not registered: " + id))
                .value();
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (VersatileConfig.hasPotionSickness(player)) return InteractionResult.PASS;
        if (requiresOpener) {
            Item opener = resolve(BOTTLE_OPENER);
            if (!player.getInventory().contains(s -> s.getItem() == opener))
                return InteractionResult.PASS;
        }
        return super.use(level, player, hand);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!(entity instanceof ServerPlayer player))
            return super.finishUsingItem(stack, level, entity);

        if (Services.PLATFORM.isFakePlayer(player)) {
            ItemStack result = super.finishUsingItem(stack, level, entity);
            level.explode(
                    player,
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    5.0F,
                    true,
                    Level.ExplosionInteraction.BLOCK);
            return result;
        }

        boolean returns = !player.getAbilities().instabuild;
        if (returns && cap != null)
            player.getInventory().placeItemBackInInventory(new ItemStack(resolve(cap)));
        ItemStack result = super.finishUsingItem(stack, level, entity);

        VersatileConfig.applyPotionSickness(player, 5);
        effect.apply(player);

        if (returns) {

            ItemStackTemplate container = getCraftingRemainder();
            if (container != null) {
                if (result.isEmpty()) return container.create();
                player.getInventory().placeItemBackInInventory(container.create());
            }
        }
        return result;
    }

    public interface Effect {
        void apply(ServerPlayer player);
    }
}
