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

import com.mojang.serialization.Codec;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public interface StatementParser {

    StatementResult eval(ParseContext ctx, String line);

    void generateJumpPoints(ParseContext ctx, String line, int index);

    enum StatementResult {
        OK,
        UNRECOGNIZED_COMMAND,
        PARAMETER_ERROR,
        END_TICK,
        SHUTDOWN,
        SKIP,
        UNDEFINED,
        STACK_EXCEEDED
    }

    final class ParseContext {
        public static final int MAX_BUFFER_LENGTH = 256;
        public static final int MAX_STACK_SIZE = 256;

        public Level world;
        public CompoundTag variables = new CompoundTag();
        public final HashMap<String, Integer> jmp = new HashMap<>();
        public String splitString = ";";
        public int clockSpeed = 1;
        public int current = 0;
        private String buffer = "";
        private final String[] stack = new String[MAX_STACK_SIZE];
        private int stackSize = 0;

        public ParseContext() {
            Arrays.fill(stack, "");
        }

        public String readBuffer() {
            return buffer;
        }

        public boolean writeBuffer(String buffer) {
            if (buffer.length() > MAX_BUFFER_LENGTH) {
                this.buffer = buffer.substring(0, MAX_BUFFER_LENGTH);
                return false;
            }

            this.buffer = buffer;
            return true;
        }

        public boolean push(String line) {
            if (stackSize >= MAX_STACK_SIZE) return false;
            if (line.length() > MAX_BUFFER_LENGTH) line = line.substring(0, MAX_BUFFER_LENGTH);
            stack[stackSize] = line;
            stackSize++;
            return true;
        }

        public String pop() {
            if (stackSize <= 0) return null;
            String ret = stack[stackSize - 1];
            stack[stackSize - 1] = "";
            stackSize--;
            return ret;
        }

        public String peek() {
            if (stackSize <= 0) return null;
            return stack[stackSize - 1];
        }

        public void turnOff() {
            clockSpeed = 1;
            current = 0;
            buffer = "";
            if (!variables.isEmpty()) variables = new CompoundTag();
        }

        public void load(ValueInput input, String[] script, StatementParser parser) {
            current = input.getIntOr("current", current);
            clockSpeed = input.getIntOr("clockSpeed", clockSpeed);
            buffer = input.getStringOr("buffer", buffer);
            splitString = input.getStringOr("splitString", splitString);
            variables = input.read("variables", CompoundTag.CODEC).orElseGet(CompoundTag::new);

            List<String> saved = input.read("stack", Codec.STRING.listOf()).orElse(List.of());
            stackSize = Math.min(saved.size(), MAX_STACK_SIZE);
            Arrays.fill(stack, "");
            for (int i = 0; i < stackSize; i++) stack[i] = saved.get(i);
            for (int i = 0; i < script.length; i++) parser.generateJumpPoints(this, script[i], i);
        }

        public void save(ValueOutput output) {
            output.putInt("current", current);
            output.putInt("clockSpeed", clockSpeed);
            output.putString("buffer", buffer);
            output.putString("splitString", splitString);
            output.store("variables", CompoundTag.CODEC, variables);
            output.store(
                    "stack", Codec.STRING.listOf(), Arrays.asList(stack).subList(0, stackSize));
        }
    }
}
