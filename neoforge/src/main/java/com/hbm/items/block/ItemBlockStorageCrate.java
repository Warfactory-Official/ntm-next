// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.block;

import com.hbm.blocks.machine.storage.BlockCrate;
import com.hbm.config.InteractionConfig;
import com.hbm.extprop.HbmPlayerProps;
import com.hbm.inventory.ItemStackContainer;
import com.hbm.inventory.container.MenuCrate;
import com.hbm.items.ModDataComponents;
import com.hbm.items.tool.ItemKey;
import com.hbm.items.tool.ItemKeyPin;
import com.hbm.platform.Services;
import com.hbm.tileentity.machine.LockStateData;
import com.hbm.tileentity.machine.storage.BlockEntityCrate;
import com.hbm.tileentity.machine.storage.CrateType;
import com.hbm.tileentity.machine.storage.LockedContents;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public class ItemBlockStorageCrate extends BlockItem {

    private final CrateType crateType;

    public ItemBlockStorageCrate(BlockCrate block, Properties properties) {
        super(block, properties);
        this.crateType = block.crateType();
    }

    private static boolean opensHeld(Player player) {
        return player.level().isClientSide()
                ? InteractionConfig.crateOpensHeld
                : HbmPlayerProps.getData(player).crateOpenHeld;
    }

    private static boolean isLocked(ItemStack stack) {
        LockStateData lock = stack.get(ModDataComponents.LOCK_STATE.get());
        return lock != null && lock.isLocked();
    }

    private static boolean sealed(ItemStack stack) {
        return isLocked(stack) || stack.has(ModDataComponents.SPIDERS.get());
    }

    private static boolean canOpen(Player player, ItemStack stack) {
        if (stack.getCount() != 1) return false;
        if (!isLocked(stack)) return true;
        int pins = stack.get(ModDataComponents.LOCK_STATE.get()).lock();
        for (ItemStack key : player.getInventory().getNonEquipmentItems()) {
            if (key.getItem() instanceof ItemKey && ItemKeyPin.getPins(key) == pins) return true;
        }
        return false;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player != null && opensHeld(player) && !player.isSecondaryUseActive())
            return InteractionResult.PASS;
        return super.useOn(context);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!opensHeld(player) || !canOpen(player, stack)) return InteractionResult.PASS;
        if (level instanceof ServerLevel server) {
            ItemStackContainer.Store store = store(server, stack);
            if (store == null) return InteractionResult.FAIL;

            if (stack.has(ModDataComponents.SPIDERS.get())) {
                BlockEntityCrate.spawnSpiders(
                        server, player.getX(), player.getY(), player.getZ(), player);
                stack.remove(ModDataComponents.SPIDERS.get());
            }

            stamp(stack, server);
            Component name = stack.get(DataComponents.CUSTOM_NAME);
            player.openMenu(
                    new SimpleMenuProvider(
                            (id, inv, opener) ->
                                    new MenuCrate(
                                            id,
                                            inv,
                                            crateType,
                                            new ItemStackContainer(
                                                    opener, stack, crateType.slots, store) {
                                                @Override
                                                public void stopOpen(ContainerUser user) {
                                                    super.stopOpen(user);
                                                    stack.remove(ModDataComponents.STACKLOCK.get());
                                                    if (!stack.getComponentsPatch().isEmpty())
                                                        stamp(stack, server);
                                                }
                                            }),
                            name != null ? name : Component.translatable(crateType.containerKey)));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult place(BlockPlaceContext context) {
        if (context.getLevel() instanceof ServerLevel server
                && store(server, context.getItemInHand()) == null) {
            return InteractionResult.FAIL;
        }
        return super.place(context);
    }

    private static ItemStackContainer.@Nullable Store store(ServerLevel level, ItemStack stack) {
        LockedContents locked = stack.get(ModDataComponents.LOCKED_CONTENTS.get());
        ItemContainerContents opened =
                locked == null
                        ? stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
                        : locked.open(level);
        if (opened == null) return null;
        return new ItemStackContainer.Store() {
            @Override
            public ItemContainerContents read(ItemStack target) {
                return opened;
            }

            @Override
            public void write(ItemStack target, ItemContainerContents contents) {
                target.remove(ModDataComponents.LOCKED_CONTENTS.get());
                target.remove(DataComponents.CONTAINER);
                stamp(target, level);
                if (contents == ItemContainerContents.EMPTY) return;
                if (sealed(target)) {
                    target.set(
                            ModDataComponents.LOCKED_CONTENTS.get(),
                            LockedContents.seal(level, contents));
                } else {
                    target.set(DataComponents.CONTAINER, contents);
                }
            }
        };
    }

    private static void stamp(ItemStack stack, ServerLevel level) {
        stack.set(ModDataComponents.STACKLOCK.get(), level.getRandom().nextLong());
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        super.appendHoverText(stack, context, display, adder, flag);
        if (stack.has(ModDataComponents.SPIDERS.get())) {
            if (isLocked(stack)) {
                adder.accept(
                        Component.translatable("desc.item.storageCrate.thisContainerIsLocked")
                                .withStyle(ChatFormatting.RED));
            }
            adder.accept(
                    Component.translatable("desc.item.storageCrate.skitteringEmanatesFrom")
                            .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            return;
        }
        if (!isLocked(stack)) return;
        LockedContents locked = stack.get(ModDataComponents.LOCKED_CONTENTS.get());
        adder.accept(
                Component.translatable("desc.item.storageCrate.thisContainerIsLocked")
                        .withStyle(ChatFormatting.RED));
        adder.accept(
                Component.translatable(
                                locked != null && locked.heavy()
                                        ? "desc.item.storageCrate.itFeelsHeavy"
                                        : "desc.item.storageCrate.itFeelsEmpty")
                        .withStyle(ChatFormatting.YELLOW));
    }

    @Override
    public boolean canFitInsideContainerItems() {
        return false;
    }
}
