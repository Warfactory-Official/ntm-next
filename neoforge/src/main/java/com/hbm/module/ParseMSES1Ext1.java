// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.module;

import com.hbm.blockentity.network.RTTYSystem.RTTYChannel;
import com.hbm.blockentity.network.RTTYSystem;
import java.util.Locale;
import java.util.regex.Pattern;

public class ParseMSES1Ext1 extends ParseMSES1 {

    @Override
    public EnumStatementReturn eval(ParseContext ctx, String line) {
        String lower = line.toLowerCase(Locale.US);

        if (lower.startsWith("splitter ")) {
            if (line.length() <= 9) return EnumStatementReturn.PARAMETER_ERROR;
            ctx.splitString = substitute(ctx, line.substring(9), false);
            return EnumStatementReturn.OK;
        }

        if (lower.startsWith("split ")) {
            if (line.length() <= 6) return EnumStatementReturn.PARAMETER_ERROR;
            try {
                int index = Integer.parseInt(substitute(ctx, line.substring(6), true));
                if (index < 1) return EnumStatementReturn.PARAMETER_ERROR;
                String[] frags = ctx.readBuffer().split(Pattern.quote(ctx.splitString));
                if (index > frags.length) return EnumStatementReturn.PARAMETER_ERROR;
                ctx.writeBuffer(frags[index - 1]);
                return EnumStatementReturn.OK;
            } catch (Throwable ex) {
                return EnumStatementReturn.PARAMETER_ERROR;
            }
        }

        if (lower.equals("splitcount")) {
            if (ctx.readBuffer().isEmpty()) return EnumStatementReturn.PARAMETER_ERROR;
            String[] frags = ctx.readBuffer().split(Pattern.quote(ctx.splitString));
            ctx.writeBuffer(frags.length + "");
            return EnumStatementReturn.OK;
        }

        if (lower.equals("push")) {
            if (ctx.readBuffer().isEmpty()) return EnumStatementReturn.PARAMETER_ERROR;
            return ctx.push(ctx.readBuffer())
                    ? EnumStatementReturn.OK
                    : EnumStatementReturn.STACK_EXCEEDED;
        }

        if (lower.startsWith("push ")) {
            if (line.length() <= 5) return EnumStatementReturn.PARAMETER_ERROR;
            return ctx.push(substitute(ctx, line.substring(5), false))
                    ? EnumStatementReturn.OK
                    : EnumStatementReturn.STACK_EXCEEDED;
        }

        if (lower.equals("pop")) {
            String val = ctx.pop();
            if (val == null) return EnumStatementReturn.UNDEFINED;
            ctx.writeBuffer(val);
            return EnumStatementReturn.OK;
        }

        if (lower.equals("peek")) {
            String val = ctx.peek();
            if (val == null) return EnumStatementReturn.UNDEFINED;
            ctx.writeBuffer(val);
            return EnumStatementReturn.OK;
        }

        if (lower.equals("length")) {
            ctx.writeBuffer("" + ctx.readBuffer().length());
            return EnumStatementReturn.OK;
        }

        if (lower.startsWith("first ")) {
            if (line.length() <= 6) return EnumStatementReturn.PARAMETER_ERROR;
            try {
                int length = Integer.parseInt(substitute(ctx, line.substring(6), true));
                int max = ctx.readBuffer().length();
                if (length > max) length = max;
                ctx.writeBuffer(ctx.readBuffer().substring(0, length));
                return EnumStatementReturn.OK;
            } catch (Exception x) {
                return EnumStatementReturn.PARAMETER_ERROR;
            }
        }

        if (lower.startsWith("last ")) {
            if (line.length() <= 5) return EnumStatementReturn.PARAMETER_ERROR;
            try {
                int length = Integer.parseInt(substitute(ctx, line.substring(5), true));
                int max = ctx.readBuffer().length();
                if (length > max) length = max;
                ctx.writeBuffer(ctx.readBuffer().substring(max - length, max));
                return EnumStatementReturn.OK;
            } catch (Exception x) {
                return EnumStatementReturn.PARAMETER_ERROR;
            }
        }

        if (lower.startsWith("poll ")) {
            if (line.length() <= 5) return EnumStatementReturn.PARAMETER_ERROR;
            RTTYChannel chan =
                    RTTYSystem.listen(ctx.world, substitute(ctx, line.substring(5), false));
            if (chan != null && chan.timeStamp >= ctx.world.getGameTime() - 1)
                ctx.writeBuffer(chan.signal + "");
            return EnumStatementReturn.OK;
        }

        if (lower.equals("worldtime")) {
            ctx.writeBuffer("" + ctx.world.getGameTime());
            return EnumStatementReturn.OK;
        }

        return super.eval(ctx, line);
    }
}
