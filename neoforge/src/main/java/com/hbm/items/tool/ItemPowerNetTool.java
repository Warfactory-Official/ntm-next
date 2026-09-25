// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.tool;

import com.hbm.api.energymk2.CableData;
import com.hbm.api.energymk2.PowerGraph;
import com.hbm.api.energymk2.PowerNetwork;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.particle.helper.ParticleCreators;
import com.hbm.uninos.graph.GraphNode;
import com.hbm.uninos.graph.LevelNodeGraph;
import com.hbm.uninos.graph.NetCensus;
import com.hbm.uninos.graph.NodeNetwork;
import com.hbm.util.I18nUtil;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
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

public class ItemPowerNetTool extends Item {

    private static final int RADIUS = 20;
    private static final int LABEL_COLOR = 0xFFFF00;
    private static final float LABEL_SCALE = 0.5F;

    public ItemPowerNetTool(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos clicked = ctx.getClickedPos();
        BlockPos target = MultiblockSurface.coreOfAny(level, clicked);
        if (target == null) target = clicked;

        if (!(level instanceof ServerLevel sl)) {
            return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }
        LevelNodeGraph<CableData> graph = PowerGraph.get(sl);
        if (graph.getNode(target.asLong()) == null) return InteractionResult.PASS;

        Player player = ctx.getPlayer();
        NodeNetwork<CableData> net = graph.networkAt(target.asLong());
        if (net == null) {
            if (player != null) {
                player.sendSystemMessage(
                        Component.translatable("desc.powerNetTool.noNetwork")
                                .withStyle(ChatFormatting.RED));
            }
            return InteractionResult.SUCCESS;
        }

        String id = Integer.toHexString(System.identityHashCode(net));
        NetCensus census = PowerNetwork.census(sl, graph, net);
        if (player != null) {
            player.sendSystemMessage(
                    Component.translatable("desc.powerNetTool.start", id)
                            .withStyle(ChatFormatting.GOLD));
            report(player, "desc.powerNetTool.links", census.links());
            report(player, "desc.powerNetTool.providers", census.providers());
            report(player, "desc.powerNetTool.receivers", census.receivers());
            player.sendSystemMessage(
                    Component.translatable("desc.powerNetTool.end", id)
                            .withStyle(ChatFormatting.GOLD));
        }

        for (GraphNode<CableData> node : net.nodes) {
            BlockPos at = BlockPos.of(node.posKey);
            ParticleCreators.debugText(
                    sl,
                    at.getX() + 0.5,
                    at.getY() + 1.5,
                    at.getZ() + 0.5,
                    LABEL_COLOR,
                    LABEL_SCALE,
                    id,
                    RADIUS);
        }
        return InteractionResult.SUCCESS;
    }

    private static void report(Player player, String key, int count) {
        player.sendSystemMessage(
                Component.translatable(key, count).withStyle(ChatFormatting.YELLOW));
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        for (String line : I18nUtil.loreLines("desc.powerNetTool.info", RADIUS)) {
            adder.accept(Component.literal(line).withStyle(ChatFormatting.RED));
        }
    }
}
