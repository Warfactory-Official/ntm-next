// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.command;

import com.hbm.config.ConfigEntry.Domain;
import com.hbm.config.ConfigEntry;
import com.hbm.config.ConfigSchema;
import com.hbm.config.ConfigStore;
import com.hbm.platform.Services;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public final class ConfigCommand {

    private static final String VALUE = "value";

    @FunctionalInterface
    public interface Feedback<S> {
        void send(S source, Supplier<Component> message, boolean broadcast);
    }

    private ConfigCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        dispatcher.register(
                tree(
                        "ntmserver",
                        Domain.RUNTIME,
                        "commands.hbm.config.serverVariables",
                        () -> Services.CONFIG.runtime().store(),
                        Commands.hasPermission(Commands.LEVEL_ADMINS),
                        CommandSourceStack::sendSuccess));
    }

    public static <S> void registerClient(CommandDispatcher<S> dispatcher, Feedback<S> feedback) {
        dispatcher.register(
                tree(
                        "ntmclient",
                        Domain.CLIENT,
                        "commands.hbm.config.clientVariables",
                        Services.CONFIG::clientStore,
                        source -> true,
                        feedback));
    }

    private static <S> LiteralArgumentBuilder<S> tree(
            String name,
            Domain domain,
            String title,
            Supplier<ConfigStore> store,
            Predicate<S> requirement,
            Feedback<S> feedback) {
        LiteralArgumentBuilder<S> get = LiteralArgumentBuilder.literal("get");
        LiteralArgumentBuilder<S> set = LiteralArgumentBuilder.literal("set");

        Map<String, LiteralArgumentBuilder<S>> gets = new LinkedHashMap<>();
        Map<String, LiteralArgumentBuilder<S>> sets = new LinkedHashMap<>();
        for (ConfigEntry<?> entry : ConfigSchema.of(domain)) {
            gets.computeIfAbsent(entry.section(), LiteralArgumentBuilder::literal)
                    .then(
                            LiteralArgumentBuilder.<S>literal(entry.key())
                                    .executes(
                                            ctx ->
                                                    report(
                                                            ctx.getSource(),
                                                            entry,
                                                            store.get(),
                                                            feedback)));
            sets.computeIfAbsent(entry.section(), LiteralArgumentBuilder::literal)
                    .then(
                            LiteralArgumentBuilder.<S>literal(entry.key())
                                    .then(valueNode(entry, feedback)));
        }
        gets.values().forEach(get::then);
        sets.values().forEach(set::then);
        return LiteralArgumentBuilder.<S>literal(name)
                .requires(requirement)
                .then(
                        LiteralArgumentBuilder.<S>literal("list")
                                .executes(
                                        ctx ->
                                                list(
                                                        ctx.getSource(),
                                                        domain,
                                                        title,
                                                        store.get(),
                                                        feedback)))
                .then(
                        LiteralArgumentBuilder.<S>literal("reload")
                                .executes(ctx -> reload(ctx.getSource(), feedback)))
                .then(get)
                .then(set);
    }

    private static <S> ArgumentBuilder<S, ?> valueNode(ConfigEntry<?> entry, Feedback<S> feedback) {
        return switch (entry.kind()) {
            case BOOL ->
                    RequiredArgumentBuilder.<S, Boolean>argument(VALUE, BoolArgumentType.bool())
                            .executes(
                                    ctx ->
                                            apply(
                                                    ctx,
                                                    entry,
                                                    BoolArgumentType.getBool(ctx, VALUE),
                                                    feedback));
            case INT ->
                    RequiredArgumentBuilder.<S, Integer>argument(
                                    VALUE,
                                    IntegerArgumentType.integer(entry.intMin(), entry.intMax()))
                            .executes(
                                    ctx ->
                                            apply(
                                                    ctx,
                                                    entry,
                                                    IntegerArgumentType.getInteger(ctx, VALUE),
                                                    feedback));
            case DOUBLE ->
                    RequiredArgumentBuilder.<S, Double>argument(
                                    VALUE, DoubleArgumentType.doubleArg(entry.min(), entry.max()))
                            .executes(
                                    ctx ->
                                            apply(
                                                    ctx,
                                                    entry,
                                                    DoubleArgumentType.getDouble(ctx, VALUE),
                                                    feedback));
            case STRING ->
                    RequiredArgumentBuilder.<S, String>argument(VALUE, StringArgumentType.string())
                            .executes(
                                    ctx ->
                                            apply(
                                                    ctx,
                                                    entry,
                                                    StringArgumentType.getString(ctx, VALUE),
                                                    feedback));
        };
    }

    @SuppressWarnings("unchecked")
    private static <S> int apply(
            CommandContext<S> ctx, ConfigEntry<?> entry, Object value, Feedback<S> feedback) {
        Services.CONFIG.set((ConfigEntry<Object>) entry, value);
        feedback.send(
                ctx.getSource(),
                () ->
                        Component.translatable("commands.hbm.config.valueUpdated")
                                .withStyle(ChatFormatting.YELLOW),
                true);
        return 1;
    }

    private static <S> int report(
            S source, ConfigEntry<?> entry, ConfigStore store, Feedback<S> feedback) {
        feedback.send(source, () -> line(entry, store), false);
        return 1;
    }

    private static <S> int list(
            S source, Domain domain, String title, ConfigStore store, Feedback<S> feedback) {
        List<ConfigEntry<?>> entries = ConfigSchema.of(domain);
        feedback.send(
                source, () -> Component.translatable(title).withStyle(ChatFormatting.RED), false);
        for (ConfigEntry<?> entry : entries) {
            feedback.send(source, () -> line(entry, store), false);
        }
        return entries.size();
    }

    private static <S> int reload(S source, Feedback<S> feedback) {
        Services.CONFIG.reload();
        feedback.send(
                source,
                () ->
                        Component.translatable("commands.hbm.config.variablesLoaded")
                                .withStyle(ChatFormatting.YELLOW),
                true);
        return 1;
    }

    private static Component line(ConfigEntry<?> entry, ConfigStore store) {
        return Component.translatable(
                "commands.hbm.config.variable",
                Component.literal(entry.section() + "." + entry.key())
                        .withStyle(ChatFormatting.GOLD),
                Component.literal(String.valueOf(store.get(entry)))
                        .withStyle(ChatFormatting.YELLOW));
    }
}
