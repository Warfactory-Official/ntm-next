// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.module.mses;

import com.mojang.serialization.Codec;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class MsesState {
    public static final int MAX_BUFFER_LENGTH = 256;
    public static final int MAX_STACK_SIZE = 256;

    static final byte UNSET = 0, TEXT = 1, PLAIN = 2, DOUBLE = 3, INT = 4;
    private static final String[] NO_SLOTS = {};

    public Level world;
    public final HashMap<String, Integer> jmp = new HashMap<>();
    public String splitString = ";";
    public int clockSpeed = 1;
    public int current = 0;

    byte bKind = TEXT;
    String bText = "";
    double bNum;

    private String[] slots = NO_SLOTS;
    byte[] vKind = {};
    String[] vText = NO_SLOTS;
    double[] vNum = {};
    private CompoundTag rest = new CompoundTag();

    private final String[] stack = new String[MAX_STACK_SIZE];
    private int stackSize = 0;

    public MsesState() {
        Arrays.fill(stack, "");
    }

    public String readBuffer() {
        String text = bText;
        if (text == null)
            bText = text = bKind == DOUBLE ? Double.toString(bNum) : Integer.toString((int) bNum);
        return text;
    }

    public boolean writeBuffer(String buffer) {
        bKind = TEXT;
        if (buffer.length() > MAX_BUFFER_LENGTH) {
            bText = buffer.substring(0, MAX_BUFFER_LENGTH);
            return false;
        }
        bText = buffer;
        return true;
    }

    void writePlain(String text, double value) {
        bKind = PLAIN;
        bText = text;
        bNum = value;
    }

    void writeDouble(double value) {
        bKind = DOUBLE;
        bText = null;
        bNum = value;
    }

    void writeDouble(double value, String text) {
        bKind = DOUBLE;
        bText = text;
        bNum = value;
    }

    void writeInt(int value) {
        bKind = INT;
        bText = null;
        bNum = value;
    }

    boolean bufferEmpty() {
        return (bKind == TEXT || bKind == PLAIN) && bText.isEmpty();
    }

    boolean bufferIsTrue() {
        return bKind == TEXT && "true".equals(bText);
    }

    double bufferPlain(boolean allowNegative) {
        switch (bKind) {
            case DOUBLE -> {
                return plainDouble(bNum, allowNegative);
            }
            case INT -> {
                return allowNegative || bNum >= 0 ? bNum : Double.NaN;
            }
            case PLAIN -> {
                return allowNegative || bText.charAt(0) != '-' ? bNum : Double.NaN;
            }
            default -> {
                String text = bText;
                if (text.isEmpty()) return 0.0D;
                if (!MsesRuntime.plainNumber(text, true)) return Double.NaN;
                bKind = PLAIN;
                bNum = Double.parseDouble(text);
                return allowNegative || text.charAt(0) != '-' ? bNum : Double.NaN;
            }
        }
    }

    double bufferJava() {
        return bKind == TEXT ? Double.parseDouble(bText) : bNum;
    }

    String bufferText(boolean numeric) {
        String text = readBuffer();
        return numeric && text.isEmpty() ? "0" : text;
    }

    private static double plainDouble(double value, boolean allowNegative) {
        double abs = Math.abs(value);
        if (!(abs >= 1.0E-3D && abs < 1.0E7D) && value != 0.0D) return Double.NaN;
        return allowNegative || Double.doubleToRawLongBits(value) >= 0 ? value : Double.NaN;
    }

    int load(int slot) {
        switch (vKind[slot]) {
            case UNSET -> writeBuffer("");
            case TEXT -> writeBuffer(vText[slot]);
            case PLAIN -> {
                if (vText[slot].length() > MAX_BUFFER_LENGTH) writeBuffer(vText[slot]);
                else writePlain(vText[slot], vNum[slot]);
            }
            default -> {
                bKind = vKind[slot];
                bText = vText[slot];
                bNum = vNum[slot];
            }
        }
        return CompiledMses.OK;
    }

    int save(int slot) {
        if (bufferEmpty()) return CompiledMses.PARAMETER_ERROR;
        vKind[slot] = bKind;
        vText[slot] = bText;
        vNum[slot] = bNum;
        return CompiledMses.OK;
    }

    String slotText(int slot, boolean numeric) {
        String text =
                switch (vKind[slot]) {
                    case UNSET -> "";
                    case DOUBLE ->
                            vText[slot] != null
                                    ? vText[slot]
                                    : (vText[slot] = Double.toString(vNum[slot]));
                    case INT ->
                            vText[slot] != null
                                    ? vText[slot]
                                    : (vText[slot] = Integer.toString((int) vNum[slot]));
                    default -> vText[slot];
                };
        return numeric && text.isEmpty() ? "0" : text;
    }

    double slotPlain(int slot, boolean allowNegative) {
        switch (vKind[slot]) {
            case UNSET -> {
                return 0.0D;
            }
            case DOUBLE -> {
                return plainDouble(vNum[slot], allowNegative);
            }
            case INT -> {
                return allowNegative || vNum[slot] >= 0 ? vNum[slot] : Double.NaN;
            }
            case PLAIN -> {
                return allowNegative || vText[slot].charAt(0) != '-' ? vNum[slot] : Double.NaN;
            }
            default -> {
                String text = vText[slot];
                if (text.isEmpty()) return 0.0D;
                if (!MsesRuntime.plainNumber(text, true)) return Double.NaN;
                vKind[slot] = PLAIN;
                vNum[slot] = Double.parseDouble(text);
                return allowNegative || text.charAt(0) != '-' ? vNum[slot] : Double.NaN;
            }
        }
    }

    double slotJava(int slot) {
        return switch (vKind[slot]) {
            case UNSET -> Double.parseDouble("");
            case TEXT -> Double.parseDouble(vText[slot]);
            default -> vNum[slot];
        };
    }

    public void bind(String[] names) {
        if (names == slots) return;

        assert slotsUnset();
        slots = names;
        vKind = new byte[names.length];
        vText = new String[names.length];
        vNum = new double[names.length];
        for (int i = 0; i < names.length; i++) {
            Optional<String> saved = rest.getString(names[i]);
            if (saved.isPresent()) {
                vKind[i] = TEXT;
                vText[i] = saved.get();
                rest.remove(names[i]);
            }
        }
    }

    private void exportSlots(CompoundTag into) {
        for (int i = 0; i < slots.length; i++) {
            if (vKind[i] != UNSET) into.putString(slots[i], slotText(i, false));
        }
    }

    public CompoundTag variables() {
        CompoundTag out = rest.copy();
        exportSlots(out);
        return out;
    }

    public String variable(String name) {
        for (int i = 0; i < slots.length; i++) {
            if (slots[i].equals(name))
                return vKind[i] == UNSET ? rest.getStringOr(name, "") : slotText(i, false);
        }
        return rest.getStringOr(name, "");
    }

    public void putVariable(String name, String value) {
        for (int i = 0; i < slots.length; i++) {
            if (slots[i].equals(name)) {
                vKind[i] = TEXT;
                vText[i] = value;
                return;
            }
        }
        rest.putString(name, value);
    }

    public boolean hasVariables() {
        return !rest.isEmpty() || !slotsUnset();
    }

    private boolean slotsUnset() {
        for (byte kind : vKind) if (kind != UNSET) return false;
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

    public List<String> stack() {
        return List.of(Arrays.copyOf(stack, stackSize));
    }

    public void turnOff() {
        clockSpeed = 1;
        current = 0;
        writeBuffer("");
        if (hasVariables()) {
            rest = new CompoundTag();
            Arrays.fill(vKind, UNSET);
            Arrays.fill(vText, null);
        }
    }

    public void load(ValueInput input, String[] script) {
        current = input.getIntOr("current", current);
        clockSpeed = input.getIntOr("clockSpeed", clockSpeed);
        bText = input.getStringOr("buffer", readBuffer());
        bKind = TEXT;
        splitString = input.getStringOr("splitString", splitString);
        rest = input.read("variables", CompoundTag.CODEC).orElseGet(CompoundTag::new);
        slots = NO_SLOTS;
        vKind = new byte[0];
        vText = NO_SLOTS;
        vNum = new double[0];

        List<String> saved = input.read("stack", Codec.STRING.listOf()).orElse(List.of());
        stackSize = Math.min(saved.size(), MAX_STACK_SIZE);
        Arrays.fill(stack, "");
        for (int i = 0; i < stackSize; i++) stack[i] = saved.get(i);
        for (int i = 0; i < script.length; i++) generateJumpPoint(script[i], i);
    }

    public void generateJumpPoint(String line, int index) {
        if (line.startsWith("dest ") && line.length() > 5) jmp.put(line.substring(5), index);
    }

    public void save(ValueOutput output) {
        output.putInt("current", current);
        output.putInt("clockSpeed", clockSpeed);
        output.putString("buffer", readBuffer());
        output.putString("splitString", splitString);
        output.store("variables", CompoundTag.CODEC, variables());
        output.store("stack", Codec.STRING.listOf(), Arrays.asList(stack).subList(0, stackSize));
    }
}
