// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.module.mses;

import com.hbm.data.MachineData;
import com.hbm.tileentity.network.RTTYSystem.RTTYChannel;
import com.hbm.tileentity.network.RTTYSystem;
import com.hbm.util.Calculator;
import java.util.regex.Pattern;

import static com.hbm.module.mses.CompiledMses.*;

public final class MsesRuntime {
    private MsesRuntime() {}

    public static String substitute(MsesState c, String text, boolean numeric) {
        if (!text.contains("$")) return text;
        StringBuilder out = new StringBuilder();
        int p = 0;
        while (p < text.length()) {
            int a = text.indexOf('$', p);
            if (a < 0) {
                out.append(text, p, text.length());
                break;
            }
            out.append(text, p, a);
            int b = text.indexOf('$', a + 1);
            if (b < 0) break;
            String name = text.substring(a + 1, b);
            String v = "buffer".equals(name) ? c.readBuffer() : c.variable(name);
            out.append(numeric && v.isEmpty() ? "0" : v);
            p = b + 1;
        }
        return out.toString();
    }

    public static boolean plainNumber(String s, boolean allowNegative) {
        int p = 0, digits = 0;
        boolean dot = false;
        if (s.isEmpty()) return false;
        if (s.charAt(0) == '-') {
            if (!allowNegative) return false;
            p++;
        }
        for (; p < s.length(); p++) {
            char ch = s.charAt(p);
            if (ch >= '0' && ch <= '9') digits++;
            else if (ch == '.' && !dot) dot = true;
            else return false;
        }
        return digits != 0;
    }

    public static int clockspeed(MsesState c, int speed) {
        try {
            if (speed < 1 || speed > MachineData.AUTOCAL_MAX_CLOCK.get()) return PARAMETER_ERROR;
            c.clockSpeed = speed;
            return SKIP;
        } catch (Throwable ex) {
            return PARAMETER_ERROR;
        }
    }

    public static int jump(MsesState c, String key) {
        Integer target = c.jmp.get(key);
        if (target == null) return PARAMETER_ERROR;
        c.current = target;
        return OK;
    }

    public static int evaluate(MsesState c, String expression, boolean rounded) {
        try {
            double d = Calculator.evaluateExpression(expression);
            if (rounded) c.writeInt((int) Math.round(d));
            else c.writeDouble(d);
            return OK;
        } catch (Throwable ex) {
            return PARAMETER_ERROR;
        }
    }

    public static int evaluateBuffer(MsesState c, boolean rounded) {
        if (c.bufferEmpty()) return PARAMETER_ERROR;
        return evaluate(c, substitute(c, c.readBuffer(), true), rounded);
    }

    public static int round(MsesState c, int mode) {
        if (c.bufferEmpty()) return PARAMETER_ERROR;
        try {
            double d = c.bufferJava();

            c.writeInt(
                    switch (mode) {
                        case 0 -> (int) Math.floor(d);
                        case 1 -> (int) Math.ceil(d);
                        default -> (int) Math.round(d);
                    });
            return OK;
        } catch (Exception ex) {
            return PARAMETER_ERROR;
        }
    }

    public static int eq(MsesState c, String operand) {
        c.writeBuffer(c.readBuffer().equals(operand) ? "true" : "false");
        return OK;
    }

    public static int compare(MsesState c, double buffer, double val, int mode) {
        boolean result =
                switch (mode) {
                    case 0 -> val > buffer;
                    case 1 -> val < buffer;
                    case 2 -> val >= buffer;
                    default -> val <= buffer;
                };
        c.writeBuffer(result ? "true" : "false");
        return OK;
    }

    public static int splitter(MsesState c, String delimiter) {
        c.splitString = delimiter;
        return OK;
    }

    public static int split(MsesState c, int index) {
        try {
            if (index < 1) return PARAMETER_ERROR;
            String[] fragments = c.readBuffer().split(Pattern.quote(c.splitString));
            if (index > fragments.length) return PARAMETER_ERROR;
            c.writeBuffer(fragments[index - 1]);
            return OK;
        } catch (Throwable ex) {
            return PARAMETER_ERROR;
        }
    }

    public static int split(MsesState c, String index) {
        try {
            return split(c, Integer.parseInt(index));
        } catch (Throwable ex) {
            return PARAMETER_ERROR;
        }
    }

    public static int splitcount(MsesState c) {
        if (c.bufferEmpty()) return PARAMETER_ERROR;
        c.writeInt(c.readBuffer().split(Pattern.quote(c.splitString)).length);
        return OK;
    }

    public static int length(MsesState c) {
        c.writeInt(c.readBuffer().length());
        return OK;
    }

    public static int pushBuffer(MsesState c) {
        if (c.bufferEmpty()) return PARAMETER_ERROR;
        return c.push(c.readBuffer()) ? OK : STACK_EXCEEDED;
    }

    public static int push(MsesState c, String value) {
        return c.push(value) ? OK : STACK_EXCEEDED;
    }

    public static int pop(MsesState c) {
        String value = c.pop();
        if (value == null) return UNDEFINED;
        c.writeBuffer(value);
        return OK;
    }

    public static int peek(MsesState c) {
        String value = c.peek();
        if (value == null) return UNDEFINED;
        c.writeBuffer(value);
        return OK;
    }

    public static int substring(MsesState c, int length, boolean last) {
        try {
            String s = c.readBuffer();
            if (length > s.length()) length = s.length();
            c.writeBuffer(last ? s.substring(s.length() - length) : s.substring(0, length));
            return OK;
        } catch (Exception ex) {
            return PARAMETER_ERROR;
        }
    }

    public static int substring(MsesState c, String length, boolean last) {
        try {
            return substring(c, Integer.parseInt(length), last);
        } catch (Exception ex) {
            return PARAMETER_ERROR;
        }
    }

    public static int send(MsesState c, String channel) {
        RTTYSystem.broadcast(c.world, channel, c.readBuffer());
        return OK;
    }

    public static int listen(MsesState c, String channel) {
        RTTYChannel chan = RTTYSystem.listen(c.world, channel);
        if (chan != null) c.writeBuffer(chan.signal + "");
        return OK;
    }

    public static int poll(MsesState c, String channel) {
        RTTYChannel chan = RTTYSystem.listen(c.world, channel);
        if (chan != null && chan.timeStamp >= c.world.getGameTime() - 1)
            c.writeBuffer(chan.signal + "");
        return OK;
    }

    public static int worldtime(MsesState c) {
        c.writeBuffer("" + c.world.getGameTime());
        return OK;
    }

    public static int nullLine() {
        throw new NullPointerException();
    }
}
