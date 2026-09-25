// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.tool;

import com.google.common.base.Suppliers;
import com.hbm.blocks.ModBlocks;
import com.hbm.inventory.ItemStackContainer;
import com.hbm.inventory.container.MenuRebarPlacer;
import com.hbm.items.ModDataComponents;
import com.hbm.registration.RegistryHandle;
import com.hbm.tileentity.BlockEntityRebar;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.Nullable;

public class ItemRebarPlacer extends Item {

    public static final ItemStackContainer.Store PATTERN =
            new ItemStackContainer.Store() {
                @Override
                public ItemContainerContents read(ItemStack stack) {
                    return stack.getOrDefault(
                            ModDataComponents.REBAR_CONCRETE.get(), ItemContainerContents.EMPTY);
                }

                @Override
                public void write(ItemStack stack, ItemContainerContents contents) {
                    stack.set(ModDataComponents.REBAR_CONCRETE.get(), contents);
                }
            };

    private static final Supplier<List<Block>> CONCRETE =
            Suppliers.memoize(
                    () -> {
                        List<RegistryHandle<? extends Block>> handles =
                                new ArrayList<>(
                                        List.of(
                                                ModBlocks.CONCRETE,
                                                ModBlocks.CONCRETE_REBAR,
                                                ModBlocks.CONCRETE_SMOOTH,
                                                ModBlocks.CONCRETE_PILLAR));
                        handles.addAll(ModBlocks.CONCRETE_COLORED);
                        handles.addAll(
                                List.of(
                                        ModBlocks.CONCRETE_EXT_MACHINE,
                                        ModBlocks.CONCRETE_EXT_MACHINE_STRIPE,
                                        ModBlocks.CONCRETE_EXT_INDIGO,
                                        ModBlocks.CONCRETE_EXT_PURPLE,
                                        ModBlocks.CONCRETE_EXT_PINK,
                                        ModBlocks.CONCRETE_EXT_HAZARD,
                                        ModBlocks.CONCRETE_EXT_SAND,
                                        ModBlocks.CONCRETE_EXT_BRONZE));
                        return handles.stream().<Block>map(RegistryHandle::get).toList();
                    });

    public ItemRebarPlacer(Properties properties) {
        super(properties);
    }

    public static List<Block> acceptableConcrete() {
        return CONCRETE.get();
    }

    public static boolean isValidConcrete(Block block) {
        return CONCRETE.get().contains(block);
    }

    public static boolean isValidConcrete(ItemStack stack) {
        return stack.getItem() instanceof BlockItem item && isValidConcrete(item.getBlock());
    }

    public static ItemStack pattern(ItemStack placer) {
        return PATTERN.read(placer).copyOne();
    }

    @Override
    public void inventoryTick(
            ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
        if (!stack.has(ModDataComponents.REBAR_CORNER.get())) return;
        boolean held = slot == EquipmentSlot.MAINHAND || slot == EquipmentSlot.OFFHAND;
        if (!held || !isValidConcrete(pattern(stack)))
            stack.remove(ModDataComponents.REBAR_CORNER.get());
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!player.isShiftKeyDown()) return InteractionResult.PASS;
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide()) {
            Component title =
                    stack.has(DataComponents.CUSTOM_NAME)
                            ? stack.getHoverName()
                            : Component.translatable("container.rebar");
            player.openMenu(
                    new SimpleMenuProvider(
                            (id, inv, opener) ->
                                    new MenuRebarPlacer(
                                            id,
                                            inv,
                                            new ItemStackContainer(opener, stack, 1, PATTERN)),
                            title));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (player == null) return InteractionResult.PASS;

        ItemStack stack = context.getItemInHand();
        if (!stack.has(ModDataComponents.REBAR_CONCRETE.get())) {
            PATTERN.write(
                    stack,
                    ItemContainerContents.fromItems(
                            List.of(new ItemStack(ModBlocks.CONCRETE_REBAR.get()))));
        }
        ItemStack concrete = pattern(stack);
        if (!isValidConcrete(concrete)) {
            say(
                    player,
                    Component.translatable("desc.item.rebarPlacer.noValidConcreteType")
                            .withStyle(ChatFormatting.RED));
            return InteractionResult.SUCCESS;
        }

        BlockPos target = context.getClickedPos().relative(context.getClickedFace());
        Long corner = stack.get(ModDataComponents.REBAR_CORNER.get());
        if (corner == null) {
            stack.set(ModDataComponents.REBAR_CORNER.get(), target.asLong());
            return InteractionResult.SUCCESS;
        }

        int left = countRebar(player);
        if (left <= 0) {
            say(
                    player,
                    Component.translatable("desc.item.rebarPlacer.outOfRebar")
                            .withStyle(ChatFormatting.RED));
            stack.remove(ModDataComponents.REBAR_CORNER.get());
            return InteractionResult.SUCCESS;
        }

        BlockPos first = BlockPos.of(corner);
        Block solid = ((BlockItem) concrete.getItem()).getBlock();
        int used = 0;
        outer:
        for (int y = Math.min(first.getY(), target.getY());
                y <= Math.max(first.getY(), target.getY());
                y++) {
            for (int z = Math.min(first.getZ(), target.getZ());
                    z <= Math.max(first.getZ(), target.getZ());
                    z++) {
                for (int x = Math.min(first.getX(), target.getX());
                        x <= Math.max(first.getX(), target.getX());
                        x++) {
                    if (left <= 0) break outer;
                    BlockPos pos = new BlockPos(x, y, z);

                    if (level.getBlockState(pos).canBeReplaced()
                            && player.mayUseItemAt(pos, context.getClickedFace(), stack)
                            && level.setBlock(
                                    pos,
                                    ModBlocks.REBAR.get().defaultBlockState(),
                                    Block.UPDATE_ALL)) {
                        if (level.getBlockEntity(pos) instanceof BlockEntityRebar rebar)
                            rebar.setConcrete(solid);
                        used++;
                        left--;
                    }
                }
            }
        }

        consumeRebar(player, used);
        say(
                player,
                Component.translatable("desc.item.rebarPlacer.placedRebar", used)
                        .withStyle(ChatFormatting.GREEN));
        stack.remove(ModDataComponents.REBAR_CORNER.get());
        return InteractionResult.SUCCESS;
    }

    public static int countRebar(Player player) {
        int count = 0;
        for (ItemStack carried : player.getInventory().getNonEquipmentItems()) {
            if (carried.is(ModBlocks.REBAR.get().asItem())) count += carried.getCount();
        }
        return count;
    }

    private static void consumeRebar(Player player, int amount) {
        for (ItemStack carried : player.getInventory().getNonEquipmentItems()) {
            if (amount <= 0) break;
            if (!carried.is(ModBlocks.REBAR.get().asItem())) continue;
            int removing = Math.min(amount, carried.getCount());
            carried.shrink(removing);
            amount -= removing;
        }
        player.inventoryMenu.broadcastChanges();
    }

    private void say(Player player, MutableComponent message) {
        player.sendSystemMessage(
                Component.literal("[")
                        .withStyle(ChatFormatting.DARK_AQUA)
                        .append(
                                getName(new ItemStack(this))
                                        .copy()
                                        .withStyle(ChatFormatting.DARK_AQUA))
                        .append(Component.literal("] ").withStyle(ChatFormatting.DARK_AQUA))
                        .append(message));
    }
}
