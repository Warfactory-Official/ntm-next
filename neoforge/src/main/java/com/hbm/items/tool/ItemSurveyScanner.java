// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.tool;

import com.hbm.blockentity.BlockEntityBedrockOre;
import com.hbm.blocks.ModBlocks;
import com.hbm.items.ModItems;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
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

    public record Survey(Set<Deposit> deposits, ItemStack bedrockOre, List<Component> lines) {}

    private record Line(Component message, List<Supplier<? extends Block>> blocks) {}

    private static final List<Line> LINES = new ArrayList<>();
    private static final Map<Supplier<? extends Block>, Deposit> ALIASES = new LinkedHashMap<>();

    public ItemSurveyScanner(Properties props) {
        super(props);
    }

    public static void alias(Supplier<? extends Block> block, Deposit deposit) {
        ALIASES.put(block, deposit);
    }

    public static void addLine(
            String key, ChatFormatting color, List<Supplier<? extends Block>> blocks) {
        LINES.add(new Line(Component.translatable(key).withStyle(color), List.copyOf(blocks)));
    }

    private static @Nullable Deposit classify(Block block) {
        if (block == ModBlocks.ORE_OIL.get()) return Deposit.OIL;
        if (block == ModBlocks.ORE_COLTAN.get()) return Deposit.COLTAN;
        if (block == ModBlocks.ORE_BEDROCK_OIL.get()) return Deposit.BEDROCK_OIL;
        if (block == ModBlocks.STONE_DEPTH.get() || block == ModBlocks.STONE_DEPTH_NETHER.get()) {
            return Deposit.DEPTH;
        }
        if (block == ModBlocks.STONE_GNEISS.get()) return Deposit.SCHIST;
        if (block == ModBlocks.ORE_AUSTRALIUM.get()) return Deposit.AUSTRALIUM;
        return Resolved.ALIASES.get(block);
    }

    public static Survey scan(Level level, BlockPos origin) {
        EnumSet<Deposit> deposits = EnumSet.noneOf(Deposit.class);
        BitSet lines = new BitSet();
        ItemStack bedrockOre = ItemStack.EMPTY;
        int floor = level.getMinY();

        for (int a = -5; a <= 5; a++) {
            for (int b = -5; b <= 5; b++) {
                for (int y = origin.getY() + 15; y > floor + 1; y -= 2) {
                    Block block =
                            level.getBlockState(
                                            new BlockPos(
                                                    origin.getX() + a * 5,
                                                    y,
                                                    origin.getZ() + b * 5))
                                    .getBlock();
                    Deposit hit = classify(block);
                    if (hit != null) deposits.add(hit);
                    Integer line = Resolved.LINES.get(block);
                    if (line != null) lines.set(line);
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
        List<Component> found = new ArrayList<>();
        for (int line = lines.nextSetBit(0); line >= 0; line = lines.nextSetBit(line + 1)) {
            found.add(LINES.get(line).message());
        }
        return new Survey(deposits, bedrockOre, found);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer serverPlayer) {
            Survey survey = scan(level, player.blockPosition());
            List<Component> messages = new ArrayList<>();
            for (Deposit deposit : survey.deposits()) messages.add(deposit.message());
            messages.addAll(survey.deposits().contains(Deposit.OIL) ? 1 : 0, survey.lines());
            for (Component message : messages) serverPlayer.sendSystemMessage(message);
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

    private static final class Resolved {
        static final Map<Block, Deposit> ALIASES = new HashMap<>();
        static final Map<Block, Integer> LINES = new HashMap<>();

        static {
            ItemSurveyScanner.ALIASES.forEach(
                    (block, deposit) -> ALIASES.put(block.get(), deposit));
            for (int i = 0; i < ItemSurveyScanner.LINES.size(); i++) {
                for (Supplier<? extends Block> block : ItemSurveyScanner.LINES.get(i).blocks()) {
                    LINES.put(block.get(), i);
                }
            }
        }
    }
}
