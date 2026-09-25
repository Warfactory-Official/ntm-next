// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.tool;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.machine.BlockCMAnchor;
import com.hbm.interfaces.IItemLookOverlay;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.items.ModDataComponents;
import com.hbm.util.I18nUtil;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class ItemCMStructure extends Item implements IItemLookOverlay {

    private static final String OUTPUT = "hbm/CMstructureOutput.txt";

    public ItemCMStructure(Properties properties) {
        super(properties);
    }

    private static BlockPos local(Direction facing, BlockPos anchor, BlockPos pos) {
        int dx = pos.getX() - anchor.getX();
        int dz = pos.getZ() - anchor.getZ();
        int y = pos.getY() - anchor.getY();
        return switch (facing) {
            case SOUTH -> new BlockPos(-dx, y, -dz);
            case WEST -> new BlockPos(-dz, y, dx);
            case EAST -> new BlockPos(dz, y, -dx);
            default -> new BlockPos(dx, y, dz);
        };
    }

    private static Selection selection(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.CM_STRUCTURE.get(), Selection.EMPTY);
    }

    private static void write(MinecraftServer server, Level level, Selection selection) {
        BlockPos anchor = selection.anchor().orElseThrow();
        BlockPos first = selection.first().orElseThrow();
        BlockPos second = selection.second().orElseThrow();
        Direction facing = level.getBlockState(anchor).getValue(BlockCMAnchor.FACING);

        StringBuilder json = new StringBuilder("{\n  \"cells\": [\n");
        boolean any = false;
        for (BlockPos pos : BlockPos.betweenClosed(first, second)) {
            if (pos.equals(anchor)) continue;
            BlockState state = level.getBlockState(pos);
            if (state.isAir()) continue;
            BlockPos cell = local(facing, anchor, pos);
            if (any) json.append(",\n");
            json.append("    { \"blocks\": \"")
                    .append(BuiltInRegistries.BLOCK.getKey(state.getBlock()))
                    .append("\", \"x\": ")
                    .append(cell.getX())
                    .append(", \"y\": ")
                    .append(cell.getY())
                    .append(", \"z\": ")
                    .append(cell.getZ())
                    .append(" }");
            any = true;
        }
        json.append("\n  ]\n}\n");

        Path file = server.getServerDirectory().resolve(OUTPUT);
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, json.toString());
        } catch (IOException e) {
            throw new UncheckedIOException("writing " + file, e);
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        ItemStack stack = context.getItemInHand();

        if (level.getBlockState(pos).is(ModBlocks.CUSTOM_MACHINE_ANCHOR.get())) {
            stack.set(ModDataComponents.CM_STRUCTURE.get(), selection(stack).withAnchor(pos));
            return InteractionResult.SUCCESS;
        }

        Selection selection = selection(stack);
        if (selection.anchor().isEmpty()) return InteractionResult.PASS;

        if (selection.first().isEmpty()) {
            stack.set(ModDataComponents.CM_STRUCTURE.get(), selection.withFirst(pos));
        } else if (selection.second().isEmpty()) {
            stack.set(ModDataComponents.CM_STRUCTURE.get(), selection.withSecond(pos));
        } else {
            if (level.getServer() != null) write(level.getServer(), level, selection);
            stack.set(
                    ModDataComponents.CM_STRUCTURE.get(),
                    Selection.anchoredAt(selection.anchor().get()));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        super.appendHoverText(stack, context, display, adder, flag);
        for (String line : I18nUtil.resolveKeyArray("item.hbm.structure_custommachine.desc")) {
            adder.accept(Component.literal(line).withStyle(ChatFormatting.YELLOW));
        }
    }

    @Override
    public void buildLookOverlay(
            Level level, BlockPos pos, ItemStack stack, Player player, ILookOverlay.LookInfo info) {
        Selection selection = selection(stack);
        info.title(stack.getHoverName().getString(), 0xffff00, 0x404000);
        if (selection.anchor().isEmpty()) {
            info.line(I18nUtil.resolveKey("item.hbm.structure_custommachine.noAnchor"), 0xFF5555);
            return;
        }
        info.line(
                I18nUtil.resolveKey(
                        "item.hbm.structure_custommachine.anchor",
                        coords(selection.anchor().get())),
                0xFFAA00);
        selection
                .first()
                .ifPresent(
                        first ->
                                info.line(
                                        I18nUtil.resolveKey(
                                                "item.hbm.structure_custommachine.first",
                                                coords(first)),
                                        0xFFFF55));
        selection
                .second()
                .ifPresent(
                        second ->
                                info.line(
                                        I18nUtil.resolveKey(
                                                "item.hbm.structure_custommachine.second",
                                                coords(second)),
                                        0xFFFF55));
    }

    private static String coords(BlockPos pos) {
        return pos.getX() + " / " + pos.getY() + " / " + pos.getZ();
    }

    public record Selection(
            Optional<BlockPos> anchor, Optional<BlockPos> first, Optional<BlockPos> second) {

        public static final Selection EMPTY =
                new Selection(Optional.empty(), Optional.empty(), Optional.empty());

        public static final Codec<Selection> CODEC =
                RecordCodecBuilder.create(
                        i ->
                                i.group(
                                                BlockPos.CODEC
                                                        .optionalFieldOf("anchor")
                                                        .forGetter(Selection::anchor),
                                                BlockPos.CODEC
                                                        .optionalFieldOf("first")
                                                        .forGetter(Selection::first),
                                                BlockPos.CODEC
                                                        .optionalFieldOf("second")
                                                        .forGetter(Selection::second))
                                        .apply(i, Selection::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, Selection> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.optional(BlockPos.STREAM_CODEC),
                        Selection::anchor,
                        ByteBufCodecs.optional(BlockPos.STREAM_CODEC),
                        Selection::first,
                        ByteBufCodecs.optional(BlockPos.STREAM_CODEC),
                        Selection::second,
                        Selection::new);

        public static Selection anchoredAt(BlockPos anchor) {
            return new Selection(Optional.of(anchor), Optional.empty(), Optional.empty());
        }

        public Selection withAnchor(BlockPos pos) {
            return new Selection(Optional.of(pos.immutable()), first, second);
        }

        public Selection withFirst(BlockPos pos) {
            return new Selection(anchor, Optional.of(pos.immutable()), second);
        }

        public Selection withSecond(BlockPos pos) {
            return new Selection(anchor, first, Optional.of(pos.immutable()));
        }
    }
}
