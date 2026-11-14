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

import com.hbm.handler.radio.RadioChannel;
import com.hbm.handler.radio.RadioSystem;
import java.util.Locale;
import java.util.regex.Pattern;

public class ParseMSES1Ext1 extends ParseMSES1 {

    @Override
    public StatementResult eval(ParseContext ctx, String line) {
        String lower = line.toLowerCase(Locale.ROOT);

        if (lower.startsWith("splitter ")) {
            if (line.length() <= 9) return StatementResult.PARAMETER_ERROR;
            ctx.splitString = substitute(ctx, line.substring(9), false);
            return StatementResult.OK;
        }

        if (lower.startsWith("split ")) {
            if (line.length() <= 6) return StatementResult.PARAMETER_ERROR;
            try {
                int index = Integer.parseInt(substitute(ctx, line.substring(6), true));
                if (index < 1) return StatementResult.PARAMETER_ERROR;
                String[] frags = ctx.readBuffer().split(Pattern.quote(ctx.splitString));
                if (index > frags.length) return StatementResult.PARAMETER_ERROR;
                ctx.writeBuffer(frags[index - 1]);
                return StatementResult.OK;
            } catch (Throwable ex) {
                return StatementResult.PARAMETER_ERROR;
            }
        }

        if (lower.equals("splitcount")) {
            if (ctx.readBuffer().isEmpty()) return StatementResult.PARAMETER_ERROR;
            String[] frags = ctx.readBuffer().split(Pattern.quote(ctx.splitString));
            ctx.writeBuffer(frags.length + "");
            return StatementResult.OK;
        }

        if (lower.equals("push")) {
            if (ctx.readBuffer().isEmpty()) return StatementResult.PARAMETER_ERROR;
            return ctx.push(ctx.readBuffer()) ? StatementResult.OK : StatementResult.STACK_EXCEEDED;
        }

        if (lower.startsWith("push ")) {
            if (line.length() <= 5) return StatementResult.PARAMETER_ERROR;
            return ctx.push(substitute(ctx, line.substring(5), false))
                    ? StatementResult.OK
                    : StatementResult.STACK_EXCEEDED;
        }

        if (lower.equals("pop")) {
            String val = ctx.pop();
            if (val == null) return StatementResult.UNDEFINED;
            ctx.writeBuffer(val);
            return StatementResult.OK;
        }

        if (lower.equals("peek")) {
            String val = ctx.peek();
            if (val == null) return StatementResult.UNDEFINED;
            ctx.writeBuffer(val);
            return StatementResult.OK;
        }

        if (lower.equals("length")) {
            ctx.writeBuffer("" + ctx.readBuffer().length());
            return StatementResult.OK;
        }

        if (lower.startsWith("first ")) {
            if (line.length() <= 6) return StatementResult.PARAMETER_ERROR;
            try {
                int length = Integer.parseInt(substitute(ctx, line.substring(6), true));
                int max = ctx.readBuffer().length();
                if (length > max) length = max;
                ctx.writeBuffer(ctx.readBuffer().substring(0, length));
                return StatementResult.OK;
            } catch (Exception x) {
                return StatementResult.PARAMETER_ERROR;
            }
        }

        if (lower.startsWith("last ")) {
            if (line.length() <= 5) return StatementResult.PARAMETER_ERROR;
            try {
                int length = Integer.parseInt(substitute(ctx, line.substring(5), true));
                int max = ctx.readBuffer().length();
                if (length > max) length = max;
                ctx.writeBuffer(ctx.readBuffer().substring(max - length, max));
                return StatementResult.OK;
            } catch (Exception x) {
                return StatementResult.PARAMETER_ERROR;
            }
        }

        if (lower.startsWith("poll ")) {
            if (line.length() <= 5) return StatementResult.PARAMETER_ERROR;
            RadioChannel chan =
                    RadioSystem.read(ctx.world, substitute(ctx, line.substring(5), false));
            if (chan != null && chan.stamp() >= ctx.world.getGameTime() - 1)
                ctx.writeBuffer(chan.text());
            return StatementResult.OK;
        }

        if (lower.equals("worldtime")) {
            ctx.writeBuffer("" + ctx.world.getGameTime());
            return StatementResult.OK;
        }

        return super.eval(ctx, line);
    }
}
