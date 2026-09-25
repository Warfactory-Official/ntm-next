// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.handler.radiation;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

public final class RadVisCommand {

    private RadVisCommand() {}

    public static <S> LiteralArgumentBuilder<S> build() {
        RadVisOverlay.Config cfg = RadVisOverlay.CONFIG;
        LiteralArgumentBuilder<S> root =
                LiteralArgumentBuilder.<S>literal("radvis").executes(ctx -> say("usage"));
        root.then(
                LiteralArgumentBuilder.<S>literal("on")
                        .executes(
                                ctx -> {
                                    cfg.enabled = true;
                                    return say("enabled");
                                }));
        root.then(
                LiteralArgumentBuilder.<S>literal("off")
                        .executes(
                                ctx -> {
                                    cfg.enabled = false;
                                    return say("disabled");
                                }));
        root.then(
                LiteralArgumentBuilder.<S>literal("radius")
                        .then(
                                RequiredArgumentBuilder.<S, Integer>argument(
                                                "chunks", IntegerArgumentType.integer())
                                        .executes(
                                                ctx -> {
                                                    cfg.radiusChunks =
                                                            Math.clamp(
                                                                    IntegerArgumentType.getInteger(
                                                                            ctx, "chunks"),
                                                                    0,
                                                                    RadVisServer.MAX_RADIUS);
                                                    return say("radius", cfg.radiusChunks);
                                                })));
        LiteralArgumentBuilder<S> mode = LiteralArgumentBuilder.literal("mode");
        for (RadVisGeometry.Mode m : RadVisGeometry.Mode.values()) {
            String name = m.name().toLowerCase(Locale.ROOT);
            mode.then(
                    LiteralArgumentBuilder.<S>literal(name)
                            .executes(
                                    ctx -> {
                                        cfg.mode = m;
                                        return say("mode", name);
                                    }));
        }
        root.then(mode);
        root.then(
                LiteralArgumentBuilder.<S>literal("y")
                        .then(
                                LiteralArgumentBuilder.<S>literal("auto")
                                        .executes(
                                                ctx -> {
                                                    cfg.sliceAutoY = true;
                                                    return say("y_auto");
                                                }))
                        .then(
                                RequiredArgumentBuilder.<S, Integer>argument(
                                                "y", IntegerArgumentType.integer())
                                        .executes(
                                                ctx -> {
                                                    cfg.sliceAutoY = false;
                                                    cfg.sliceY =
                                                            IntegerArgumentType.getInteger(
                                                                    ctx, "y");
                                                    return say("y", cfg.sliceY);
                                                })));
        root.then(toggle("xray", on -> cfg.xray = on));
        root.then(
                LiteralArgumentBuilder.<S>literal("color")
                        .then(
                                LiteralArgumentBuilder.<S>literal("rad")
                                        .executes(
                                                ctx -> {
                                                    cfg.colorByRad = true;
                                                    return say("color_rad");
                                                }))
                        .then(
                                LiteralArgumentBuilder.<S>literal("pocket")
                                        .executes(
                                                ctx -> {
                                                    cfg.colorByRad = false;
                                                    return say("color_pocket");
                                                })));
        root.then(
                LiteralArgumentBuilder.<S>literal("alpha")
                        .then(
                                RequiredArgumentBuilder.<S, Float>argument(
                                                "alpha", FloatArgumentType.floatArg())
                                        .executes(
                                                ctx -> {
                                                    cfg.alpha =
                                                            Math.clamp(
                                                                    FloatArgumentType.getFloat(
                                                                            ctx, "alpha"),
                                                                    0.0f,
                                                                    1.0f);
                                                    return say("alpha", cfg.alpha);
                                                })));
        root.then(toggle("verify", on -> cfg.verify = on));
        root.then(
                LiteralArgumentBuilder.<S>literal("verifyInterval")
                        .then(
                                RequiredArgumentBuilder.<S, Integer>argument(
                                                "ticks", IntegerArgumentType.integer(1))
                                        .executes(
                                                ctx -> {
                                                    cfg.verifyInterval =
                                                            IntegerArgumentType.getInteger(
                                                                    ctx, "ticks");
                                                    return say(
                                                            "verify_interval", cfg.verifyInterval);
                                                })));
        root.then(
                LiteralArgumentBuilder.<S>literal("anchor")
                        .then(
                                LiteralArgumentBuilder.<S>literal("auto")
                                        .executes(
                                                ctx -> {
                                                    cfg.focusAnchor = null;
                                                    return say("anchor_auto");
                                                }))
                        .then(
                                LiteralArgumentBuilder.<S>literal("here")
                                        .executes(
                                                ctx -> {
                                                    cfg.focusAnchor =
                                                            Minecraft.getInstance()
                                                                    .player
                                                                    .blockPosition();
                                                    cfg.focusEnabled = true;
                                                    return say("anchor_here");
                                                }))
                        .then(
                                RequiredArgumentBuilder.<S, Integer>argument(
                                                "x", IntegerArgumentType.integer())
                                        .then(
                                                RequiredArgumentBuilder.<S, Integer>argument(
                                                                "y", IntegerArgumentType.integer())
                                                        .then(
                                                                RequiredArgumentBuilder
                                                                        .<S, Integer>argument(
                                                                                "z",
                                                                                IntegerArgumentType
                                                                                        .integer())
                                                                        .executes(
                                                                                ctx -> {
                                                                                    cfg.focusAnchor =
                                                                                            new BlockPos(
                                                                                                    arg(
                                                                                                            ctx,
                                                                                                            "x"),
                                                                                                    arg(
                                                                                                            ctx,
                                                                                                            "y"),
                                                                                                    arg(
                                                                                                            ctx,
                                                                                                            "z"));
                                                                                    cfg.focusEnabled =
                                                                                            true;
                                                                                    return say(
                                                                                            "anchor",
                                                                                            cfg
                                                                                                    .focusAnchor
                                                                                                    .getX(),
                                                                                            cfg
                                                                                                    .focusAnchor
                                                                                                    .getY(),
                                                                                            cfg
                                                                                                    .focusAnchor
                                                                                                    .getZ());
                                                                                })))));
        root.then(
                LiteralArgumentBuilder.<S>literal("filterBox")
                        .then(
                                RequiredArgumentBuilder.<S, Integer>argument(
                                                "dx", IntegerArgumentType.integer())
                                        .then(
                                                RequiredArgumentBuilder.<S, Integer>argument(
                                                                "dy", IntegerArgumentType.integer())
                                                        .then(
                                                                RequiredArgumentBuilder
                                                                        .<S, Integer>argument(
                                                                                "dz",
                                                                                IntegerArgumentType
                                                                                        .integer())
                                                                        .executes(
                                                                                ctx -> {
                                                                                    cfg.focusDx =
                                                                                            Math
                                                                                                    .max(
                                                                                                            0,
                                                                                                            arg(
                                                                                                                    ctx,
                                                                                                                    "dx"));
                                                                                    cfg.focusDy =
                                                                                            Math
                                                                                                    .max(
                                                                                                            0,
                                                                                                            arg(
                                                                                                                    ctx,
                                                                                                                    "dy"));
                                                                                    cfg.focusDz =
                                                                                            Math
                                                                                                    .max(
                                                                                                            0,
                                                                                                            arg(
                                                                                                                    ctx,
                                                                                                                    "dz"));
                                                                                    if (cfg.focusAnchor
                                                                                            == null) {
                                                                                        cfg.focusAnchor =
                                                                                                Minecraft
                                                                                                        .getInstance()
                                                                                                        .player
                                                                                                        .blockPosition();
                                                                                    }
                                                                                    cfg.focusEnabled =
                                                                                            cfg.focusDx
                                                                                                            != 0
                                                                                                    || cfg.focusDy
                                                                                                            != 0
                                                                                                    || cfg.focusDz
                                                                                                            != 0;
                                                                                    return say(
                                                                                            "filter_box",
                                                                                            cfg.focusDx,
                                                                                            cfg.focusDy,
                                                                                            cfg.focusDz);
                                                                                })))));
        root.then(
                LiteralArgumentBuilder.<S>literal("reset")
                        .executes(
                                ctx -> {
                                    cfg.reset();
                                    return say("reset");
                                }));
        root.then(
                LiteralArgumentBuilder.<S>literal("clearcaches")
                        .executes(
                                ctx -> {
                                    RadVisOverlay.clear();
                                    return say("clear_caches");
                                }));
        return root;
    }

    private static <S> LiteralArgumentBuilder<S> toggle(
            String name, java.util.function.Consumer<Boolean> set) {
        return LiteralArgumentBuilder.<S>literal(name)
                .then(
                        LiteralArgumentBuilder.<S>literal("on")
                                .executes(
                                        ctx -> {
                                            set.accept(true);
                                            return say(name + "_on");
                                        }))
                .then(
                        LiteralArgumentBuilder.<S>literal("off")
                                .executes(
                                        ctx -> {
                                            set.accept(false);
                                            return say(name + "_off");
                                        }));
    }

    private static <S> int arg(CommandContext<S> ctx, String name) {
        return IntegerArgumentType.getInteger(ctx, name);
    }

    private static int say(String key, Object... args) {
        Minecraft.getInstance()
                .player
                .sendSystemMessage(Component.translatable("commands.hbm.radvis." + key, args));
        return 1;
    }
}
