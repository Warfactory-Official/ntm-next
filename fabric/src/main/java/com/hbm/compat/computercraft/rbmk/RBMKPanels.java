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

package com.hbm.compat.computercraft.rbmk;

import com.hbm.blockentity.machine.rbmk.RBMKGaugeBlockEntity.GaugeUnit;
import com.hbm.blockentity.machine.rbmk.RBMKGaugeBlockEntity;
import com.hbm.blockentity.machine.rbmk.RBMKGraphBlockEntity.GraphUnit;
import com.hbm.blockentity.machine.rbmk.RBMKGraphBlockEntity;
import com.hbm.blockentity.machine.rbmk.RBMKIndicatorBlockEntity.IndicatorUnit;
import com.hbm.blockentity.machine.rbmk.RBMKIndicatorBlockEntity;
import com.hbm.blockentity.machine.rbmk.RBMKKeyPadBlockEntity.KeyUnit;
import com.hbm.blockentity.machine.rbmk.RBMKKeyPadBlockEntity;
import com.hbm.blockentity.machine.rbmk.RBMKLeverBlockEntity.LeverUnit;
import com.hbm.blockentity.machine.rbmk.RBMKLeverBlockEntity;
import com.hbm.blockentity.machine.rbmk.RBMKNumitronBlockEntity.DisplayUnit;
import com.hbm.blockentity.machine.rbmk.RBMKNumitronBlockEntity;
import com.hbm.compat.computercraft.rbmk.RBMKPanelPeripheral.Panel;

public final class RBMKPanels {

    private static final Panel<RBMKGaugeBlockEntity, GaugeUnit> GAUGE =
            Panel.<RBMKGaugeBlockEntity, GaugeUnit>builder(
                            "rbmk_gauge",
                            "getGaugeInfo",
                            RBMKGaugeBlockEntity.GAUGES,
                            gauge -> gauge.gauges)
                    .flag(
                            "active",
                            unit -> unit.active,
                            "setGaugeActive",
                            (unit, on) -> unit.hbm$syncSet$GaugeUnit$active(on))
                    .flag(
                            "polling",
                            unit -> unit.polling,
                            "setGaugePolling",
                            (unit, on) -> unit.hbm$syncSet$GaugeUnit$polling(on))
                    .color(
                            "color",
                            unit -> unit.color,
                            "setGaugeColor",
                            (unit, color) -> unit.hbm$syncSet$GaugeUnit$color((int) color))
                    .text(
                            "label",
                            unit -> unit.label,
                            "setGaugeLabel",
                            (unit, label) -> unit.hbm$syncSet$GaugeUnit$label(label))
                    .text(
                            "channel",
                            unit -> unit.rtty,
                            "setGaugeChannel",
                            (unit, channel) -> unit.hbm$syncSet$GaugeUnit$rtty(channel))
                    .number(
                            "min",
                            unit -> unit.min,
                            "setGaugeMin",
                            (unit, min) -> unit.hbm$syncSet$GaugeUnit$min(min))
                    .number(
                            "max",
                            unit -> unit.max,
                            "setGaugeMax",
                            (unit, max) -> unit.hbm$syncSet$GaugeUnit$max(max))
                    .number(
                            "value",
                            unit -> unit.value,
                            "setGaugeValue",
                            (unit, value) -> unit.hbm$syncSet$GaugeUnit$value(value))
                    .build();

    private static final Panel<RBMKGraphBlockEntity, GraphUnit> GRAPH = graphPanel();

    private static final Panel<RBMKIndicatorBlockEntity, IndicatorUnit> INDICATOR =
            Panel.<RBMKIndicatorBlockEntity, IndicatorUnit>builder(
                            "rbmk_indicator",
                            "getIndicatorInfo",
                            RBMKIndicatorBlockEntity.INDICATORS,
                            indicator -> indicator.indicators)
                    .flag(
                            "active",
                            unit -> unit.active,
                            "setIndicatorActive",
                            (unit, on) -> unit.hbm$syncSet$IndicatorUnit$active(on))
                    .flag("polling", unit -> unit.polling)
                    .flag(
                            "light",
                            unit -> unit.light,
                            "setIndicatorLight",
                            (unit, on) -> unit.hbm$syncSet$IndicatorUnit$light(on))
                    .color(
                            "color",
                            unit -> unit.color,
                            "setIndicatorColor",
                            (unit, color) -> unit.hbm$syncSet$IndicatorUnit$color((int) color))
                    .text(
                            "label",
                            unit -> unit.label,
                            "setIndicatorLabel",
                            (unit, label) -> unit.hbm$syncSet$IndicatorUnit$label(label))
                    .text("channel", unit -> unit.rtty)
                    .number("min", unit -> unit.min, null, null)
                    .number("max", unit -> unit.max, null, null)
                    .write(
                            "setIndicatorBounds",
                            (indicator, unit, args) -> {
                                long min = args.getInt(1), max = args.getInt(2);
                                if (unit.min != min || unit.max != max) {
                                    unit.hbm$syncSet$IndicatorUnit$min(min);
                                    unit.hbm$syncSet$IndicatorUnit$max(max);
                                    indicator.markChanged();
                                }
                                return new Object[] {true};
                            })
                    .build();

    private static final Panel<RBMKKeyPadBlockEntity, KeyUnit> KEY_PAD = keyPadPanel();

    private static final Panel<RBMKLeverBlockEntity, LeverUnit> LEVER =
            Panel.<RBMKLeverBlockEntity, LeverUnit>builder(
                            "rbmk_lever",
                            "getLeverInfo",
                            RBMKLeverBlockEntity.LEVERS,
                            lever -> lever.levers)
                    .flag(
                            "active",
                            unit -> unit.active,
                            "setLeverActive",
                            (unit, on) -> unit.hbm$syncSet$LeverUnit$active(on))
                    .flag("polling", unit -> unit.polling)
                    .flag("state", unit -> unit.flipProgress >= 1F)
                    .fraction("progress", unit -> unit.flipProgress)
                    .text(
                            "label",
                            unit -> unit.label,
                            "setLeverLabel",
                            (unit, label) -> unit.hbm$syncSet$LeverUnit$label(label))
                    .text("channel", unit -> unit.rtty)
                    .text("commandOn", unit -> unit.commandOn)
                    .text("commandOff", unit -> unit.commandOff)
                    .build();

    private static final Panel<RBMKNumitronBlockEntity, DisplayUnit> NUMITRON =
            Panel.<RBMKNumitronBlockEntity, DisplayUnit>builder(
                            "rbmk_numitron",
                            "getDisplayInfo",
                            RBMKNumitronBlockEntity.DISPLAYS,
                            numitron -> numitron.displays)
                    .flag(
                            "shorten_number",
                            unit -> unit.shortenNumber,
                            "setDisplayShortenNumber",
                            (unit, on) -> unit.hbm$syncSet$DisplayUnit$shortenNumber(on))
                    .bounded(
                            "active_digits",
                            unit -> unit.activeDigits,
                            "setDisplayActiveDigits",
                            (unit, digits) -> unit.hbm$syncSet$DisplayUnit$activeDigits(digits),
                            0,
                            127)
                    .flag(
                            "leading_zeroes",
                            unit -> unit.leadingZeroes,
                            "setDisplayLeadingZeroes",
                            (unit, on) -> unit.hbm$syncSet$DisplayUnit$leadingZeroes(on))
                    .flag(
                            "active",
                            unit -> unit.active,
                            "setDisplayActive",
                            (unit, on) -> unit.hbm$syncSet$DisplayUnit$active(on))
                    .flag(
                            "polling",
                            unit -> unit.polling,
                            "setDisplayPolling",
                            (unit, on) -> unit.hbm$syncSet$DisplayUnit$polling(on))
                    .text(
                            "label",
                            unit -> unit.label,
                            "setDisplayLabel",
                            (unit, label) -> unit.hbm$syncSet$DisplayUnit$label(label))
                    .text(
                            "channel",
                            unit -> unit.rtty,
                            "setDisplayChannel",
                            (unit, channel) -> unit.hbm$syncSet$DisplayUnit$rtty(channel))
                    .number(
                            "value",
                            unit -> unit.value,
                            "setDisplayValue",
                            (unit, value) -> unit.hbm$syncSet$DisplayUnit$value(value))
                    .build();

    private RBMKPanels() {}

    private static Panel<RBMKGraphBlockEntity, GraphUnit> graphPanel() {
        var builder =
                Panel.<RBMKGraphBlockEntity, GraphUnit>builder(
                                "rbmk_graph",
                                "getGraphInfo",
                                RBMKGraphBlockEntity.GRAPHS,
                                graph -> graph.graphs)
                        .flag(
                                "active",
                                unit -> unit.active,
                                "setGraphActive",
                                (unit, on) -> unit.hbm$syncSet$GraphUnit$active(on))
                        .flag(
                                "polling",
                                unit -> unit.polling,
                                "setGraphPolling",
                                (unit, on) -> unit.hbm$syncSet$GraphUnit$polling(on))
                        .text(
                                "label",
                                unit -> unit.label,
                                "setGraphLabel",
                                (unit, label) -> unit.hbm$syncSet$GraphUnit$label(label))
                        .text(
                                "channel",
                                unit -> unit.rtty,
                                "setGraphChannel",
                                (unit, channel) -> unit.hbm$syncSet$GraphUnit$rtty(channel))
                        .series("values", unit -> unit.values, RBMKGraphBlockEntity.SAMPLES);
        var samples = builder.numberOf("values");
        return builder.read(
                        "getGraphMin",
                        0,
                        (published, first) -> {
                            var min = Long.MAX_VALUE;
                            for (var i = 0; i < RBMKGraphBlockEntity.SAMPLES; i++) {
                                min = Math.min(min, published.longAt(first + samples + i));
                            }
                            return new Object[] {min == Long.MAX_VALUE ? 0L : min};
                        })
                .read(
                        "getGraphMax",
                        0,
                        (published, first) -> {
                            var max = Long.MIN_VALUE;
                            for (var i = 0; i < RBMKGraphBlockEntity.SAMPLES; i++) {
                                max = Math.max(max, published.longAt(first + samples + i));
                            }
                            return new Object[] {max == Long.MIN_VALUE ? 0L : max};
                        })
                .read(
                        "getGraphAvg",
                        0D,
                        (published, first) -> {
                            var sum = 0L;
                            for (var i = 0; i < RBMKGraphBlockEntity.SAMPLES; i++)
                                sum += published.longAt(first + samples + i);
                            return new Object[] {(double) sum / RBMKGraphBlockEntity.SAMPLES};
                        })
                .write(
                        "pushGraphValue",
                        (graph, unit, args) -> {
                            unit.pushValue(args.getInt(1));
                            graph.markChanged();
                            return new Object[] {true};
                        })
                .build();
    }

    private static Panel<RBMKKeyPadBlockEntity, KeyUnit> keyPadPanel() {
        var builder =
                Panel.<RBMKKeyPadBlockEntity, KeyUnit>builder(
                                "rbmk_keypad",
                                "getKeyInfo",
                                RBMKKeyPadBlockEntity.KEYS,
                                pad -> pad.keys)
                        .flag(
                                "active",
                                unit -> unit.active,
                                "setKeyActive",
                                (unit, on) -> unit.hbm$syncSet$KeyUnit$active(on))
                        .flag(
                                "polling",
                                unit -> unit.polling,
                                "setKeyPolling",
                                (unit, on) -> unit.hbm$syncSet$KeyUnit$polling(on))
                        .flag("pressed", unit -> unit.isPressed)
                        .color(
                                "color",
                                unit -> unit.color,
                                "setKeyColor",
                                (unit, color) -> unit.hbm$syncSet$KeyUnit$color((int) color))
                        .text(
                                "label",
                                unit -> unit.label,
                                "setKeyLabel",
                                (unit, label) -> unit.hbm$syncSet$KeyUnit$label(label))
                        .text(
                                "channel",
                                unit -> unit.rtty,
                                "setKeyChannel",
                                (unit, channel) -> unit.hbm$syncSet$KeyUnit$rtty(channel))
                        .text(
                                "command",
                                unit -> unit.command,
                                "setKeyCommand",
                                (unit, command) -> unit.hbm$syncSet$KeyUnit$command(command));
        var pressed = builder.numberOf("pressed");
        return builder.read(
                        "getKeyPressed",
                        false,
                        (published, first) -> new Object[] {published.booleanAt(first + pressed)})
                .write(
                        "pressKey",
                        (pad, unit, args) -> {
                            if (!unit.active) return new Object[] {false, "Key is not active"};
                            unit.click(pad.getLevel());
                            return new Object[] {true};
                        })
                .build();
    }

    public static RBMKPanelPeripheral<RBMKGaugeBlockEntity, GaugeUnit> gauge(
            RBMKGaugeBlockEntity machine) {
        return new RBMKPanelPeripheral<>(machine, GAUGE);
    }

    public static RBMKPanelPeripheral<RBMKGraphBlockEntity, GraphUnit> graph(
            RBMKGraphBlockEntity machine) {
        return new RBMKPanelPeripheral<>(machine, GRAPH);
    }

    public static RBMKPanelPeripheral<RBMKIndicatorBlockEntity, IndicatorUnit> indicator(
            RBMKIndicatorBlockEntity machine) {
        return new RBMKPanelPeripheral<>(machine, INDICATOR);
    }

    public static RBMKPanelPeripheral<RBMKKeyPadBlockEntity, KeyUnit> keyPad(
            RBMKKeyPadBlockEntity machine) {
        return new RBMKPanelPeripheral<>(machine, KEY_PAD);
    }

    public static RBMKPanelPeripheral<RBMKLeverBlockEntity, LeverUnit> lever(
            RBMKLeverBlockEntity machine) {
        return new RBMKPanelPeripheral<>(machine, LEVER);
    }

    public static RBMKPanelPeripheral<RBMKNumitronBlockEntity, DisplayUnit> numitron(
            RBMKNumitronBlockEntity machine) {
        return new RBMKPanelPeripheral<>(machine, NUMITRON);
    }
}
