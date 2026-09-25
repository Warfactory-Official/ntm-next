// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.tool;

import com.hbm.blocks.ModBlocks;
import com.hbm.items.ModItems;
import com.hbm.tileentity.BlockEntityBedrockOre;
import java.util.EnumSet;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.EndPlatformFeature;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class ItemSurveyScanner extends Item {

    public enum Deposit {
        OIL("chat.surveyScanner.foundOil", ChatFormatting.BLACK),
        BEDROCK_OIL("chat.surveyScanner.foundBedrockOil", ChatFormatting.BLACK),
        COLTAN("chat.surveyScanner.foundColtan", ChatFormatting.GOLD),
        DEPTH("chat.surveyScanner.foundDepthRock", ChatFormatting.GRAY),
        SCHIST("chat.surveyScanner.foundSchist", ChatFormatting.DARK_AQUA),
        AUSTRALIUM("chat.surveyScanner.foundAustralium", ChatFormatting.YELLOW);

        private final String key;
        private final ChatFormatting color;

        Deposit(String key, ChatFormatting color) {
            this.key = key;
            this.color = color;
        }

        public Component message() {
            return Component.translatable(key).withStyle(color);
        }
    }

    public record Survey(Set<Deposit> deposits, ItemStack bedrockOre) {}

    public ItemSurveyScanner(Properties props) {
        super(props);
    }

    private static @Nullable Deposit classify(BlockState state) {
        Block block = state.getBlock();
        if (block == ModBlocks.ORE_OIL.get()) return Deposit.OIL;
        if (block == ModBlocks.ORE_COLTAN.get()) return Deposit.COLTAN;
        if (block == ModBlocks.ORE_BEDROCK_OIL.get()) return Deposit.BEDROCK_OIL;
        if (block == ModBlocks.STONE_DEPTH.get() || block == ModBlocks.STONE_DEPTH_NETHER.get()) {
            return Deposit.DEPTH;
        }
        if (block == ModBlocks.STONE_GNEISS.get()) return Deposit.SCHIST;
        if (block == ModBlocks.ORE_AUSTRALIUM.get()) return Deposit.AUSTRALIUM;
        return null;
    }

    public static Survey scan(Level level, BlockPos origin) {
        EnumSet<Deposit> deposits = EnumSet.noneOf(Deposit.class);
        ItemStack bedrockOre = ItemStack.EMPTY;

        int floor = level.getMinY();

        for (int a = -5; a <= 5; a++) {
            for (int b = -5; b <= 5; b++) {
                for (int y = origin.getY() + 15; y > floor + 1; y -= 2) {
                    Deposit hit =
                            classify(
                                    level.getBlockState(
                                            new BlockPos(
                                                    origin.getX() + a * 5,
                                                    y,
                                                    origin.getZ() + b * 5)));
                    if (hit != null) deposits.add(hit);
                }

                BlockPos node = new BlockPos(origin.getX() + a * 2, floor, origin.getZ() + b * 2);
                if (level.getBlockState(node).is(ModBlocks.ORE_BEDROCK.get())) {

                    bedrockOre =
                            level.getBlockEntity(node) instanceof BlockEntityBedrockOre ore
                                    ? ore.resource
                                    : ItemStack.EMPTY;
                }
            }
        }
        return new Survey(deposits, bedrockOre);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer serverPlayer) {
            Survey survey = scan(level, player.blockPosition());
            for (Deposit deposit : survey.deposits())
                serverPlayer.sendSystemMessage(deposit.message());
            if (!survey.bedrockOre().isEmpty()) {
                serverPlayer.sendSystemMessage(
                        Component.translatable(
                                        "chat.surveyScanner.foundBedrockOreFor",
                                        survey.bedrockOre().getHoverName())
                                .withStyle(ChatFormatting.RED));
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Player player = ctx.getPlayer();
        if (player == null) return InteractionResult.PASS;
        if (!ctx.getLevel()
                .getBlockState(ctx.getClickedPos())
                .is(ModBlocks.BLOCK_BERYLLIUM.get())) {
            return InteractionResult.PASS;
        }
        if (!hasEntanglementKit(player)) return InteractionResult.PASS;
        if (player instanceof ServerPlayer serverPlayer) travelToTheEnd(serverPlayer);
        return InteractionResult.SUCCESS;
    }

    private static boolean hasEntanglementKit(Player player) {
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (stack.is(ModItems.ENTANGLEMENT_KIT.get())) return true;
        }
        return false;
    }

    private static void travelToTheEnd(ServerPlayer player) {
        ServerLevel end = player.level().getServer().getLevel(Level.END);
        if (end == null) return;

        Vec3 spawn = Vec3.atBottomCenterOf(ServerLevel.END_SPAWN_POINT);
        EndPlatformFeature.createEndPlatform(end, BlockPos.containing(spawn).below(), true);
        player.teleport(
                new TeleportTransition(
                        end,
                        spawn.subtract(0.0D, 1.0D, 0.0D),
                        Vec3.ZERO,
                        Direction.WEST.toYRot(),
                        0.0F,
                        Relative.union(Relative.DELTA, Set.of(Relative.X_ROT)),
                        TeleportTransition.PLAY_PORTAL_SOUND.then(
                                TeleportTransition.PLACE_PORTAL_TICKET)));
    }
}
