// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.tool;

import com.hbm.api.energymk2.PowerGraph;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.items.ModDataComponents;
import com.hbm.tileentity.network.BlockEntityPylonBase;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public class ItemWiring extends Item {

    public ItemWiring(Properties props) {
        super(props);
    }

    private static @Nullable BlockEntityPylonBase pylonAt(Level level, BlockPos pos) {

        BlockPos core = MultiblockSurface.coreOfAny(level, pos);
        BlockPos target = core == null ? pos : core;
        return level.getBlockEntity(target) instanceof BlockEntityPylonBase pylon ? pylon : null;
    }

    private static String connect(
            ServerLevel level, BlockEntityPylonBase first, BlockEntityPylonBase second) {
        return switch (BlockEntityPylonBase.canConnect(first, second)) {
            case 0 -> {
                PowerGraph.get(level)
                        .addRemoteLink(first.getBlockPos().asLong(), second.getBlockPos().asLong());
                first.refreshConnections();
                second.refreshConnections();
                yield "Wire end";
            }
            case 1 -> "Wire error - Pylons are not the same type";
            case 2 -> "Wire error - Cannot connect to the same pylon";
            default -> "Wire error - Pylon is too far away";
        };
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || player.isShiftKeyDown()) return InteractionResult.PASS;
        Level level = context.getLevel();
        BlockEntityPylonBase clicked = pylonAt(level, context.getClickedPos());
        if (clicked == null) return InteractionResult.PASS;
        ItemStack stack = context.getItemInHand();

        Long stored = stack.get(ModDataComponents.WIRE_TARGET.get());
        if (stored == null) {
            stack.set(ModDataComponents.WIRE_TARGET.get(), clicked.getBlockPos().asLong());
            if (!level.isClientSide())
                player.sendSystemMessage(Component.translatable("desc.item.wiring.wireStart"));
            return InteractionResult.SUCCESS;
        }

        if (level.isClientSide()) return InteractionResult.SUCCESS;
        ServerLevel server = (ServerLevel) level;
        stack.remove(ModDataComponents.WIRE_TARGET.get());
        BlockEntityPylonBase first = pylonAt(server, BlockPos.of(stored));
        player.sendSystemMessage(
                Component.literal(first == null ? "Wire error" : connect(server, first, clicked)));
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        Long stored = stack.get(ModDataComponents.WIRE_TARGET.get());
        if (stored == null) {
            adder.accept(Component.translatable("desc.item.wiring.rightClickPolesTo"));
            return;
        }
        BlockPos pos = BlockPos.of(stored);
        adder.accept(Component.translatable("desc.item.wiring.wireStartX", pos.getX()));
        adder.accept(Component.translatable("desc.item.wiring.wireStartY", pos.getY()));
        adder.accept(Component.translatable("desc.item.wiring.wireStartZ", pos.getZ()));
    }
}
