// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.module;

import com.mojang.serialization.Codec;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public interface IParse {

    EnumStatementReturn eval(ParseContext ctx, String line);

    void generateJumpPoints(ParseContext ctx, String line, int index);

    enum EnumStatementReturn {
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

        public void load(ValueInput input, String[] script, IParse parser) {
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
