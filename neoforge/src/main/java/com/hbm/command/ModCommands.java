/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.command;

import com.hbm.entity.mob.glyphid.GlyphidPathDebug;
import com.hbm.extprop.LivingHazards;
import com.hbm.handler.radiation.RadiationSystemNT;
import com.hbm.items.ItemCustomizationHandler;
import com.hbm.items.ModItems;
import com.hbm.items.SatelliteFrequencyProvider;
import com.hbm.saveddata.SatelliteSavedData;
import com.hbm.saveddata.satellites.SatelliteDeployment;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Collection;
import java.util.List;
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
                satelliteLevels(ctx.getSource()).stream()
                        .flatMap(level -> SatelliteSavedData.get(level).sats.keySet().stream())
                        .map(String::valueOf)
                        .distinct(),
                builder);
    }

    private static int orbitSatellite(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        if (!(source.getEntity() instanceof ServerPlayer player)) return notAPlayer(source);

        ItemStack held = player.getMainHandItem();
        if (!(held.getItem() instanceof SatelliteFrequencyProvider)
                || held.is(ModItems.SAT_CHIP.get())
                || !SatelliteDeployment.launch(
                        source.getLevel(),
                        held,
                        SatelliteFrequencyProvider.frequency(held),
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

        var frequency = IntegerArgumentType.getInteger(ctx, "frequency");
        if (!satellites(source, frequency).descend(frequency)) {
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

        var count = 0;
        for (var level : satelliteLevels(source)) {
            var sats = SatelliteSavedData.get(level).sats;
            sats.forEach(
                    (freq, sat) ->
                            source.sendSuccess(
                                    () ->
                                            Component.literal(
                                                            "["
                                                                    + level.dimension().identifier()
                                                                    + "] "
                                                                    + freq
                                                                    + " - "
                                                                    + sat.kind().label())
                                                    .withStyle(ChatFormatting.GREEN),
                                    false));
            count += sats.size();
        }
        if (count == 0) {
            source.sendFailure(
                    Component.translatable("commands.satellite.no_active_satellites")
                            .withStyle(ChatFormatting.RED));
            return 0;
        }
        return count;
    }

    private static SatelliteSavedData satellites(CommandSourceStack source, int frequency) {
        BlockPos pos = BlockPos.containing(source.getPosition());
        return SatelliteSavedData.at(source.getLevel(), pos.getX(), pos.getZ(), frequency);
    }

    private static List<ServerLevel> satelliteLevels(CommandSourceStack source) {
        var pos = BlockPos.containing(source.getPosition());
        return SatelliteSavedData.levels(source.getLevel(), pos.getX(), pos.getZ());
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
        if (!(stack.getItem() instanceof ItemCustomizationHandler customizable)) {
            source.sendFailure(
                    Component.translatable("commands.hbm.youHaveToHold")
                            .withStyle(ChatFormatting.RED));
            return 0;
        }
        List<String> words;
        try {
            String raw = StringArgumentType.getString(ctx, "args").trim();
            words = raw.isEmpty() ? List.of() : List.of(raw.split("\\s+"));
        } catch (IllegalArgumentException ignored) {
            words = List.of();
        }
        customizable.customizeByCommand(player, stack, words);
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
            LivingHazards.getOrCreate(p).radiation = 0D;
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
                                            + LivingHazards.storedRadiation(p)
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
                Mth.clamp(
                        DoubleArgumentType.getDouble(ctx, "value"),
                        0D,
                        LivingHazards.RADIATION_CAP);
        for (ServerPlayer p : targets) LivingHazards.getOrCreate(p).radiation = value;
        src.sendSuccess(
                () -> Component.translatable("commands.hbm.setRadiationFor", targets.size(), value),
                true);
        return targets.size();
    }
}
