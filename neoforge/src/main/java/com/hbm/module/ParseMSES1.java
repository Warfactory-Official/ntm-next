/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) Contributors to Hbm's Nuclear Tech Mod. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only,
 * as published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this code. If not, see <https://www.gnu.org/licenses/>.
 *
 * Under GPLv3 §7(c) and §7(e), these additional terms apply to LGPL-covered
 * material authored by movblock:
 *
 * - the license grants no rights to the name "NTM NEXT";
 * - a modified version must not be named NTM NEXT or presented as the
 *   official release, and must be marked as different from it.
 *
 * Accurately identifying a work as derived from NTM NEXT does not violate
 * these additional terms, provided it is clearly distinguished from the
 * official release and does not imply endorsement. Ports are encouraged to
 * acknowledge NTM NEXT as their upstream project and provide a link to it.
 */

package com.hbm.module;

import com.hbm.data.MachineData;
import com.hbm.handler.radio.RadioChannel;
import com.hbm.handler.radio.RadioSystem;
import com.hbm.util.ArithmeticExpression;
import java.util.Locale;

public class ParseMSES1 implements StatementParser {

    @Override
    public StatementResult eval(ParseContext ctx, String line) {
        String lower = line.toLowerCase(Locale.ROOT);

        if (line.isEmpty() || lower.startsWith("dest ") || lower.startsWith("# ")) {
            return StatementResult.SKIP;
        }

        if (lower.equals("nop")) {
            return StatementResult.OK;
        }

        if (lower.startsWith("clockspeed ")) {
            if (line.length() <= 11) return StatementResult.PARAMETER_ERROR;
            try {
                int speed = Integer.parseInt(line.substring(11));
                if (speed < 1 || speed > MachineData.AUTOCAL_MAX_CLOCK.get()) {
                    return StatementResult.PARAMETER_ERROR;
                }
                ctx.clockSpeed = speed;
            } catch (Throwable ex) {
                return StatementResult.PARAMETER_ERROR;
            }
            return StatementResult.SKIP;
        }

        if (lower.startsWith("jmp ")) {
            if (line.length() <= 4) return StatementResult.PARAMETER_ERROR;
            return jump(ctx, substitute(ctx, line.substring(4), false));
        }

        if (lower.startsWith("jmpif ")) {
            if (line.length() <= 6) return StatementResult.PARAMETER_ERROR;
            if (!ctx.readBuffer().equals("true")) return StatementResult.OK;
            return jump(ctx, substitute(ctx, line.substring(6), false));
        }

        if (lower.startsWith("jmpnot ")) {
            if (line.length() <= 7) return StatementResult.PARAMETER_ERROR;
            if (ctx.readBuffer().equals("true")) return StatementResult.OK;
            return jump(ctx, substitute(ctx, line.substring(7), false));
        }

        if (lower.equals("endtick")) {
            return StatementResult.END_TICK;
        }

        if (lower.equals("shutdown")) {
            return StatementResult.SHUTDOWN;
        }

        if (lower.startsWith("load ")) {
            if (line.length() <= 5) return StatementResult.PARAMETER_ERROR;
            ctx.writeBuffer(ctx.variables.getStringOr(line.substring(5), ""));
            return StatementResult.OK;
        }

        if (lower.startsWith("save ")) {
            if (line.length() <= 5 || ctx.readBuffer().isEmpty())
                return StatementResult.PARAMETER_ERROR;
            ctx.variables.putString(line.substring(5), ctx.readBuffer());
            return StatementResult.OK;
        }

        if (lower.startsWith("buffer ")) {
            if (line.length() <= 7) return StatementResult.PARAMETER_ERROR;
            ctx.writeBuffer(line.substring(7));
            return StatementResult.OK;
        }

        if (lower.startsWith("eval ")) {
            if (line.length() <= 5) return StatementResult.PARAMETER_ERROR;
            return evaluate(ctx, substitute(ctx, line.substring(5), true), false);
        }

        if (lower.equals("eval")) {
            if (ctx.readBuffer().isEmpty()) return StatementResult.PARAMETER_ERROR;
            return evaluate(ctx, substitute(ctx, ctx.readBuffer(), true), false);
        }

        if (lower.startsWith("evalr ")) {
            if (line.length() <= 6) return StatementResult.PARAMETER_ERROR;
            return evaluate(ctx, substitute(ctx, line.substring(6), true), true);
        }

        if (lower.equals("evalr")) {
            if (ctx.readBuffer().isEmpty()) return StatementResult.PARAMETER_ERROR;
            return evaluate(ctx, substitute(ctx, ctx.readBuffer(), true), true);
        }

        if (lower.equals("rounddown") || lower.equals("floor")) {
            if (ctx.readBuffer().isEmpty()) return StatementResult.PARAMETER_ERROR;
            try {
                double d = Double.parseDouble(ctx.readBuffer());
                ctx.writeBuffer("" + (int) Math.floor(d));
            } catch (Exception ex) {
                return StatementResult.PARAMETER_ERROR;
            }
            return StatementResult.OK;
        }

        if (lower.equals("roundup") || lower.equals("ceil")) {
            if (ctx.readBuffer().isEmpty()) return StatementResult.PARAMETER_ERROR;
            try {
                double d = Double.parseDouble(ctx.readBuffer());
                ctx.writeBuffer("" + (int) Math.ceil(d));
            } catch (Exception ex) {
                return StatementResult.PARAMETER_ERROR;
            }
            return StatementResult.OK;
        }

        if (lower.equals("round") || lower.equals("nearest")) {
            if (ctx.readBuffer().isEmpty()) return StatementResult.PARAMETER_ERROR;
            try {
                double d = Double.parseDouble(ctx.readBuffer());
                ctx.writeBuffer("" + (int) Math.round(d));
            } catch (Exception ex) {
                return StatementResult.PARAMETER_ERROR;
            }
            return StatementResult.OK;
        }

        if (lower.startsWith("concat ")) {
            if (line.length() <= 7 || ctx.readBuffer().isEmpty())
                return StatementResult.PARAMETER_ERROR;
            ctx.writeBuffer(substitute(ctx, line.substring(7), false));
            return StatementResult.OK;
        }

        if (lower.startsWith("eq ")) {
            if (line.length() <= 3 || ctx.readBuffer().isEmpty())
                return StatementResult.PARAMETER_ERROR;
            ctx.writeBuffer(
                    ctx.readBuffer().equals(substitute(ctx, line.substring(3), false))
                            ? "true"
                            : "false");
            return StatementResult.OK;
        }

        if (lower.startsWith("gtb ")) {
            if (line.length() <= 4 || ctx.readBuffer().isEmpty())
                return StatementResult.PARAMETER_ERROR;
            try {
                double buffer = Double.parseDouble(ctx.readBuffer());
                double val = Double.parseDouble(substitute(ctx, line.substring(4), false));
                ctx.writeBuffer(val > buffer ? "true" : "false");
            } catch (Exception ex) {
                return StatementResult.PARAMETER_ERROR;
            }
            return StatementResult.OK;
        }

        if (lower.startsWith("ltb ")) {
            if (line.length() <= 4 || ctx.readBuffer().isEmpty())
                return StatementResult.PARAMETER_ERROR;
            try {
                double buffer = Double.parseDouble(ctx.readBuffer());
                double val = Double.parseDouble(substitute(ctx, line.substring(4), false));
                ctx.writeBuffer(val < buffer ? "true" : "false");
            } catch (Exception ex) {
                return StatementResult.PARAMETER_ERROR;
            }
            return StatementResult.OK;
        }

        if (lower.startsWith("geb ")) {
            if (line.length() <= 4 || ctx.readBuffer().isEmpty())
                return StatementResult.PARAMETER_ERROR;
            try {
                double buffer = Double.parseDouble(ctx.readBuffer());
                double val = Double.parseDouble(substitute(ctx, line.substring(4), false));
                ctx.writeBuffer(val >= buffer ? "true" : "false");
            } catch (Exception ex) {
                return StatementResult.PARAMETER_ERROR;
            }
            return StatementResult.OK;
        }

        if (lower.startsWith("leb ")) {
            if (line.length() <= 4 || ctx.readBuffer().isEmpty())
                return StatementResult.PARAMETER_ERROR;
            try {
                double buffer = Double.parseDouble(ctx.readBuffer());
                double val = Double.parseDouble(substitute(ctx, line.substring(4), false));
                ctx.writeBuffer(val <= buffer ? "true" : "false");
            } catch (Exception ex) {
                return StatementResult.PARAMETER_ERROR;
            }
            return StatementResult.OK;
        }

        if (lower.startsWith("send ")) {
            if (line.length() <= 5 || ctx.readBuffer().isEmpty())
                return StatementResult.PARAMETER_ERROR;
            RadioSystem.send(
                    ctx.world, substitute(ctx, line.substring(5), false), ctx.readBuffer());
            return StatementResult.OK;
        }

        if (lower.startsWith("listen ")) {
            if (line.length() <= 7) return StatementResult.PARAMETER_ERROR;
            RadioChannel chan =
                    RadioSystem.read(ctx.world, substitute(ctx, line.substring(7), false));
            if (chan != null) ctx.writeBuffer(chan.text());
            return StatementResult.OK;
        }

        return StatementResult.UNRECOGNIZED_COMMAND;
    }

    private static StatementResult jump(ParseContext ctx, String jmpKey) {
        Integer target = ctx.jmp.get(jmpKey);
        if (target == null) return StatementResult.PARAMETER_ERROR;
        ctx.current = target;
        return StatementResult.OK;
    }

    private static StatementResult evaluate(ParseContext ctx, String statement, boolean rounded) {
        try {
            double result = ArithmeticExpression.evaluate(statement);
            ctx.writeBuffer(rounded ? "" + (int) Math.round(result) : "" + result);
        } catch (Throwable ex) {
            return StatementResult.PARAMETER_ERROR;
        }
        return StatementResult.OK;
    }

    public String substitute(ParseContext ctx, String statement, boolean forceNumber) {
        if (!statement.contains("$")) return statement;

        StringBuilder joined = new StringBuilder();
        StringBuilder var = new StringBuilder();
        boolean readingVar = false;

        for (char c : statement.toCharArray()) {
            if (c == '$') {
                if (!readingVar) {
                    readingVar = true;
                } else {
                    String varName = var.toString();
                    String variable =
                            "buffer".equals(varName)
                                    ? ctx.readBuffer()
                                    : ctx.variables.getStringOr(varName, "");
                    if (forceNumber && variable.isEmpty()) variable = "0";
                    joined.append(variable);
                    var.setLength(0);
                    readingVar = false;
                }
            } else if (readingVar) {
                var.append(c);
            } else {
                joined.append(c);
            }
        }

        return joined.toString();
    }

    @Override
    public void generateJumpPoints(ParseContext ctx, String line, int index) {
        if (line.startsWith("dest ") && line.length() > 5) {
            ctx.jmp.put(line.substring(5), index);
        }
    }
}
