// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.module;

import com.hbm.blockentity.network.RTTYSystem.RTTYChannel;
import com.hbm.blockentity.network.RTTYSystem;
import com.hbm.data.MachineData;
import com.hbm.util.Calculator;
import java.util.Locale;

public class ParseMSES1 implements IParse {

    @Override
    public EnumStatementReturn eval(ParseContext ctx, String line) {
        String lower = line.toLowerCase(Locale.US);

        if (line.isEmpty() || lower.startsWith("dest ") || lower.startsWith("# ")) {
            return EnumStatementReturn.SKIP;
        }

        if (lower.equals("nop")) {
            return EnumStatementReturn.OK;
        }

        if (lower.startsWith("clockspeed ")) {
            if (line.length() <= 11) return EnumStatementReturn.PARAMETER_ERROR;
            try {
                int speed = Integer.parseInt(line.substring(11));
                if (speed < 1 || speed > MachineData.AUTOCAL_MAX_CLOCK.get()) {
                    return EnumStatementReturn.PARAMETER_ERROR;
                }
                ctx.clockSpeed = speed;
            } catch (Throwable ex) {
                return EnumStatementReturn.PARAMETER_ERROR;
            }
            return EnumStatementReturn.SKIP;
        }

        if (lower.startsWith("jmp ")) {
            if (line.length() <= 4) return EnumStatementReturn.PARAMETER_ERROR;
            return jump(ctx, substitute(ctx, line.substring(4), false));
        }

        if (lower.startsWith("jmpif ")) {
            if (line.length() <= 6) return EnumStatementReturn.PARAMETER_ERROR;
            if (!ctx.readBuffer().equals("true")) return EnumStatementReturn.OK;
            return jump(ctx, substitute(ctx, line.substring(6), false));
        }

        if (lower.startsWith("jmpnot ")) {
            if (line.length() <= 7) return EnumStatementReturn.PARAMETER_ERROR;
            if (ctx.readBuffer().equals("true")) return EnumStatementReturn.OK;
            return jump(ctx, substitute(ctx, line.substring(7), false));
        }

        if (lower.equals("endtick")) {
            return EnumStatementReturn.END_TICK;
        }

        if (lower.equals("shutdown")) {
            return EnumStatementReturn.SHUTDOWN;
        }

        if (lower.startsWith("load ")) {
            if (line.length() <= 5) return EnumStatementReturn.PARAMETER_ERROR;
            ctx.writeBuffer(ctx.variables.getStringOr(line.substring(5), ""));
            return EnumStatementReturn.OK;
        }

        if (lower.startsWith("save ")) {
            if (line.length() <= 5 || ctx.readBuffer().isEmpty())
                return EnumStatementReturn.PARAMETER_ERROR;
            ctx.variables.putString(line.substring(5), ctx.readBuffer());
            return EnumStatementReturn.OK;
        }

        if (lower.startsWith("buffer ")) {
            if (line.length() <= 7) return EnumStatementReturn.PARAMETER_ERROR;
            ctx.writeBuffer(line.substring(7));
            return EnumStatementReturn.OK;
        }

        if (lower.startsWith("eval ")) {
            if (line.length() <= 5) return EnumStatementReturn.PARAMETER_ERROR;
            return evaluate(ctx, substitute(ctx, line.substring(5), true), false);
        }

        if (lower.equals("eval")) {
            if (ctx.readBuffer().isEmpty()) return EnumStatementReturn.PARAMETER_ERROR;
            return evaluate(ctx, substitute(ctx, ctx.readBuffer(), true), false);
        }

        if (lower.startsWith("evalr ")) {
            if (line.length() <= 6) return EnumStatementReturn.PARAMETER_ERROR;
            return evaluate(ctx, substitute(ctx, line.substring(6), true), true);
        }

        if (lower.equals("evalr")) {
            if (ctx.readBuffer().isEmpty()) return EnumStatementReturn.PARAMETER_ERROR;
            return evaluate(ctx, substitute(ctx, ctx.readBuffer(), true), true);
        }

        if (lower.equals("rounddown") || lower.equals("floor")) {
            if (ctx.readBuffer().isEmpty()) return EnumStatementReturn.PARAMETER_ERROR;
            try {
                double d = Double.parseDouble(ctx.readBuffer());
                ctx.writeBuffer("" + (int) Math.floor(d));
            } catch (Exception ex) {
                return EnumStatementReturn.PARAMETER_ERROR;
            }
            return EnumStatementReturn.OK;
        }

        if (lower.equals("roundup") || lower.equals("ceil")) {
            if (ctx.readBuffer().isEmpty()) return EnumStatementReturn.PARAMETER_ERROR;
            try {
                double d = Double.parseDouble(ctx.readBuffer());
                ctx.writeBuffer("" + (int) Math.ceil(d));
            } catch (Exception ex) {
                return EnumStatementReturn.PARAMETER_ERROR;
            }
            return EnumStatementReturn.OK;
        }

        if (lower.equals("round") || lower.equals("nearest")) {
            if (ctx.readBuffer().isEmpty()) return EnumStatementReturn.PARAMETER_ERROR;
            try {
                double d = Double.parseDouble(ctx.readBuffer());
                ctx.writeBuffer("" + (int) Math.round(d));
            } catch (Exception ex) {
                return EnumStatementReturn.PARAMETER_ERROR;
            }
            return EnumStatementReturn.OK;
        }

        if (lower.startsWith("concat ")) {
            if (line.length() <= 7 || ctx.readBuffer().isEmpty())
                return EnumStatementReturn.PARAMETER_ERROR;
            ctx.writeBuffer(substitute(ctx, line.substring(7), false));
            return EnumStatementReturn.OK;
        }

        if (lower.startsWith("eq ")) {
            if (line.length() <= 3 || ctx.readBuffer().isEmpty())
                return EnumStatementReturn.PARAMETER_ERROR;
            ctx.writeBuffer(
                    ctx.readBuffer().equals(substitute(ctx, line.substring(3), false))
                            ? "true"
                            : "false");
            return EnumStatementReturn.OK;
        }

        if (lower.startsWith("gtb ")) {
            if (line.length() <= 4 || ctx.readBuffer().isEmpty())
                return EnumStatementReturn.PARAMETER_ERROR;
            try {
                double buffer = Double.parseDouble(ctx.readBuffer());
                double val = Double.parseDouble(substitute(ctx, line.substring(4), false));
                ctx.writeBuffer(val > buffer ? "true" : "false");
            } catch (Exception ex) {
                return EnumStatementReturn.PARAMETER_ERROR;
            }
            return EnumStatementReturn.OK;
        }

        if (lower.startsWith("ltb ")) {
            if (line.length() <= 4 || ctx.readBuffer().isEmpty())
                return EnumStatementReturn.PARAMETER_ERROR;
            try {
                double buffer = Double.parseDouble(ctx.readBuffer());
                double val = Double.parseDouble(substitute(ctx, line.substring(4), false));
                ctx.writeBuffer(val < buffer ? "true" : "false");
            } catch (Exception ex) {
                return EnumStatementReturn.PARAMETER_ERROR;
            }
            return EnumStatementReturn.OK;
        }

        if (lower.startsWith("geb ")) {
            if (line.length() <= 4 || ctx.readBuffer().isEmpty())
                return EnumStatementReturn.PARAMETER_ERROR;
            try {
                double buffer = Double.parseDouble(ctx.readBuffer());
                double val = Double.parseDouble(substitute(ctx, line.substring(4), false));
                ctx.writeBuffer(val >= buffer ? "true" : "false");
            } catch (Exception ex) {
                return EnumStatementReturn.PARAMETER_ERROR;
            }
            return EnumStatementReturn.OK;
        }

        if (lower.startsWith("leb ")) {
            if (line.length() <= 4 || ctx.readBuffer().isEmpty())
                return EnumStatementReturn.PARAMETER_ERROR;
            try {
                double buffer = Double.parseDouble(ctx.readBuffer());
                double val = Double.parseDouble(substitute(ctx, line.substring(4), false));
                ctx.writeBuffer(val <= buffer ? "true" : "false");
            } catch (Exception ex) {
                return EnumStatementReturn.PARAMETER_ERROR;
            }
            return EnumStatementReturn.OK;
        }

        if (lower.startsWith("send ")) {
            if (line.length() <= 5 || ctx.readBuffer().isEmpty())
                return EnumStatementReturn.PARAMETER_ERROR;
            RTTYSystem.broadcast(
                    ctx.world, substitute(ctx, line.substring(5), false), ctx.readBuffer());
            return EnumStatementReturn.OK;
        }

        if (lower.startsWith("listen ")) {
            if (line.length() <= 7) return EnumStatementReturn.PARAMETER_ERROR;
            RTTYChannel chan =
                    RTTYSystem.listen(ctx.world, substitute(ctx, line.substring(7), false));
            if (chan != null) ctx.writeBuffer(chan.signal + "");
            return EnumStatementReturn.OK;
        }

        return EnumStatementReturn.UNRECOGNIZED_COMMAND;
    }

    private static EnumStatementReturn jump(ParseContext ctx, String jmpKey) {
        Integer target = ctx.jmp.get(jmpKey);
        if (target == null) return EnumStatementReturn.PARAMETER_ERROR;
        ctx.current = target;
        return EnumStatementReturn.OK;
    }

    private static EnumStatementReturn evaluate(
            ParseContext ctx, String statement, boolean rounded) {
        try {
            double result = Calculator.evaluateExpression(statement);
            ctx.writeBuffer(rounded ? "" + (int) Math.round(result) : "" + result);
        } catch (Throwable ex) {
            return EnumStatementReturn.PARAMETER_ERROR;
        }
        return EnumStatementReturn.OK;
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
