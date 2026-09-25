// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.command;

import com.hbm.entity.mob.glyphid.GlyphidPathDebug;
import com.hbm.extprop.HbmLivingProps;
import com.hbm.handler.radiation.RadiationSystemNT;
import com.hbm.items.ICustomizable;
import com.hbm.items.ISatChip;
import com.hbm.items.ModItems;
import com.hbm.saveddata.SatelliteSavedData;
import com.hbm.saveddata.satellites.Satellite;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

public final class ModCommands {

    private ModCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        ConfigCommand.register(dispatcher);
        dispatcher.register(
                Commands.literal("ntmcustomize")
                        .executes(ModCommands::customize)
                        .then(
                                Commands.argument("args", StringArgumentType.greedyString())
                                        .executes(ModCommands::customize)));
        dispatcher.register(
                Commands.literal("hbmglyphidpath")
                        .requires(Commands.hasPermission(Commands.LEVEL_ADMINS))
                        .executes(ModCommands::toggleGlyphidPath));
        dispatcher.register(
                Commands.literal("hbmrad")
                        .requires(Commands.hasPermission(Commands.LEVEL_ADMINS))
                        .then(
                                Commands.literal("set")
                                        .then(
                                                Commands.argument(
                                                                "pos", BlockPosArgument.blockPos())
                                                        .then(
                                                                Commands.argument(
                                                                                "value",
                                                                                DoubleArgumentType
                                                                                        .doubleArg(
                                                                                                0D))
                                                                        .executes(
                                                                                ModCommands
                                                                                        ::setChunk))))
                        .then(Commands.literal("clearall").executes(ModCommands::clearAll))
                        .then(Commands.literal("reset").executes(ModCommands::clearAll))
                        .then(Commands.literal("resetplayers").executes(ModCommands::resetPlayers))
                        .then(
                                Commands.literal("player")
                                        .then(
                                                Commands.argument(
                                                                "targets", EntityArgument.players())
                                                        .executes(ModCommands::queryPlayers)
                                                        .then(
                                                                Commands.argument(
                                                                                "value",
                                                                                DoubleArgumentType
                                                                                        .doubleArg(
                                                                                                0D))
                                                                        .executes(
                                                                                ModCommands
                                                                                        ::setPlayers)))));
        dispatcher.register(
                Commands.literal("ntmsatellites")
                        .requires(Commands.hasPermission(Commands.LEVEL_OWNERS))
                        .then(Commands.literal("orbit").executes(ModCommands::orbitSatellite))
                        .then(
                                Commands.literal("descend")
                                        .then(
                                                Commands.argument(
                                                                "frequency",
                                                                IntegerArgumentType.integer())
                                                        .suggests(ModCommands::suggestFrequencies)
                                                        .executes(ModCommands::descendSatellite)))
                        .then(Commands.literal("list").executes(ModCommands::listSatellites)));
    }

    private static CompletableFuture<Suggestions> suggestFrequencies(
            CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(
                SatelliteSavedData.get(ctx.getSource().getLevel()).sats.keySet().stream()
                        .map(String::valueOf),
                builder);
    }

    private static int orbitSatellite(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        if (!(source.getEntity() instanceof ServerPlayer player)) return notAPlayer(source);

        ItemStack held = player.getMainHandItem();
        if (!(held.getItem() instanceof ISatChip)
                || held.is(ModItems.SAT_CHIP.get())
                || !Satellite.orbit(
                        source.getLevel(),
                        held,
                        ISatChip.getFreqS(held),
                        player.getX(),
                        player.getY(),
                        player.getZ())) {
            source.sendFailure(
                    Component.translatable("commands.satellite.not_a_satellite")
                            .withStyle(ChatFormatting.RED));
            return 0;
        }

        held.shrink(1);
        source.sendSuccess(
                () ->
                        Component.translatable("commands.satellite.satellite_orbited")
                                .withStyle(ChatFormatting.GREEN),
                false);
        return 1;
    }

    private static int descendSatellite(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        if (!(source.getEntity() instanceof ServerPlayer)) return notAPlayer(source);

        if (!SatelliteSavedData.get(source.getLevel())
                .descend(IntegerArgumentType.getInteger(ctx, "frequency"))) {
            source.sendFailure(
                    Component.translatable("commands.satellite.no_satellite")
                            .withStyle(ChatFormatting.RED));
            return 0;
        }
        source.sendSuccess(
                () ->
                        Component.translatable("commands.satellite.satellite_descended")
                                .withStyle(ChatFormatting.GREEN),
                false);
        return 1;
    }

    private static int listSatellites(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        if (!(source.getEntity() instanceof ServerPlayer)) return notAPlayer(source);

        Map<Integer, Satellite> sats = SatelliteSavedData.get(source.getLevel()).sats;
        if (sats.isEmpty()) {
            source.sendFailure(
                    Component.translatable("commands.satellite.no_active_satellites")
                            .withStyle(ChatFormatting.RED));
            return 0;
        }
        sats.forEach(
                (freq, sat) ->
                        source.sendSuccess(
                                () ->
                                        Component.literal(
                                                        freq
                                                                + " - "
                                                                + sat.getClass().getSimpleName())
                                                .withStyle(ChatFormatting.GREEN),
                                false));
        return sats.size();
    }

    private static int notAPlayer(CommandSourceStack source) {
        source.sendFailure(
                Component.translatable("commands.satellite.should_be_run_as_player")
                        .withStyle(ChatFormatting.RED));
        return 0;
    }

    private static int toggleGlyphidPath(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(
                    Component.translatable("commands.hbm.glyphidPathDebugIs")
                            .withStyle(ChatFormatting.RED));
            return 0;
        }
        boolean enabled = GlyphidPathDebug.toggle(player);
        source.sendSuccess(
                () ->
                        Component.translatable(
                                        enabled
                                                ? "commands.hbm.glyphidPathOn"
                                                : "commands.hbm.glyphidPathOff")
                                .withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.YELLOW),
                false);
        return 1;
    }

    private static int customize(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(
                    Component.translatable("commands.hbm.customizationIsOnly")
                            .withStyle(ChatFormatting.RED));
            return 0;
        }
        var stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof ICustomizable customizable)) {
            source.sendFailure(
                    Component.translatable("commands.hbm.youHaveToHold")
                            .withStyle(ChatFormatting.RED));
            return 0;
        }
        String[] args;
        try {
            String raw = StringArgumentType.getString(ctx, "args").trim();
            args = raw.isEmpty() ? new String[0] : raw.split("\\s+");
        } catch (IllegalArgumentException ignored) {
            args = new String[0];
        }
        customizable.customize(player, stack, args);
        return 1;
    }

    private static int setChunk(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        ServerLevel level = src.getLevel();
        BlockPos pos = BlockPosArgument.getBlockPos(ctx, "pos");
        double value = DoubleArgumentType.getDouble(ctx, "value");
        RadiationSystemNT.setRadForCoord(level, pos, value);
        src.sendSuccess(
                () ->
                        Component.translatable(
                                "commands.hbm.setRadiationAt",
                                pos.getX(),
                                pos.getY(),
                                pos.getZ(),
                                value),
                true);
        return 1;
    }

    private static int clearAll(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        ServerLevel level = src.getLevel();
        if (!RadiationSystemNT.jettisonData(level)) {
            src.sendFailure(
                    Component.translatable(
                            "commands.hbmrad.removeall.failed",
                            level.dimension().identifier().toString()));
            return 0;
        }
        src.sendSuccess(
                () ->
                        Component.translatable(
                                "commands.hbmrad.removeall",
                                level.dimension().identifier().toString()),
                true);
        return 1;
    }

    private static int resetPlayers(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        for (ServerPlayer p : src.getServer().getPlayerList().getPlayers()) {
            HbmLivingProps.getData(p).radiation = 0D;
        }
        src.sendSuccess(() -> Component.translatable("commands.hbmrad.player_success"), true);
        return 1;
    }

    private static int queryPlayers(CommandContext<CommandSourceStack> ctx)
            throws CommandSyntaxException {
        CommandSourceStack src = ctx.getSource();
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
        for (ServerPlayer p : targets) {
            src.sendSuccess(
                    () ->
                            Component.literal(
                                    p.getName().getString()
                                            + ": "
                                            + HbmLivingProps.getData(p).radiation
                                            + " RAD"),
                    false);
        }
        return targets.size();
    }

    private static int setPlayers(CommandContext<CommandSourceStack> ctx)
            throws CommandSyntaxException {
        CommandSourceStack src = ctx.getSource();
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
        double value =
                Mth.clamp(DoubleArgumentType.getDouble(ctx, "value"), 0D, HbmLivingProps.RAD_CAP);
        for (ServerPlayer p : targets) HbmLivingProps.getData(p).radiation = value;
        src.sendSuccess(
                () -> Component.translatable("commands.hbm.setRadiationFor", targets.size(), value),
                true);
        return targets.size();
    }
}
