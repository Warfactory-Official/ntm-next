// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.render.loader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.resources.Identifier;

public final class ObjLoader {

    private ObjLoader() {}

    public static GroupObject[] parse(InputStream in, Identifier id) throws IOException {
        return parse(in, id.toString());
    }

    public static GroupObject[] parse(InputStream in, String source) throws IOException {
        Floats positions = new Floats(3);
        Floats uvs = new Floats(2);
        Floats normals = new Floats(3);
        List<Group> groups = new ArrayList<>();
        Group current = null;
        Line tokens = new Line(source);

        try (BufferedReader reader =
                new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                tokens.reset(line);
                if (!tokens.hasNext() || line.charAt(tokens.at) == '#') continue;
                if (tokens.command("v")) {
                    positions.add(tokens.number(), tokens.number(), tokens.number());
                    tokens.extraNumbers(4);
                } else if (tokens.command("vn")) {
                    normals.add(tokens.number(), tokens.number(), tokens.number());
                    tokens.extraNumbers(1);
                } else if (tokens.command("vt")) {
                    uvs.add(tokens.number(), 1F - tokens.number());
                    tokens.extraNumbers(1);
                } else if (tokens.command("f")) {
                    if (current == null) current = new Group("Default");
                    current.face(tokens, positions, uvs, normals);
                } else if (tokens.command("g") || tokens.command("o")) {
                    String name = tokens.name();
                    if (current != null) groups.add(current);
                    current = new Group(name);
                }
            }
            if (current != null) groups.add(current);
        }

        GroupObject[] out = new GroupObject[groups.size()];
        for (int i = 0; i < out.length; i++) out[i] = groups.get(i).build();
        return out;
    }

    private static final class Line {
        private final String source;
        private String text;
        private int line;
        private int at;
        private int end;

        Line(String source) {
            this.source = source;
        }

        void reset(String text) {
            this.text = text;
            line++;
            at = 0;
            end = text.length();
            while (at < end && text.charAt(at) <= ' ') at++;
            while (end > at && text.charAt(end - 1) <= ' ') end--;
        }

        boolean command(String command) {
            int next = at + command.length();
            if (next >= end || !text.startsWith(command, at) || !space(text.charAt(next)))
                return false;
            at = next;
            skipSpace();
            return true;
        }

        boolean hasNext() {
            return at < end;
        }

        boolean consume(char value) {
            if (at >= end || text.charAt(at) != value) return false;
            at++;
            return true;
        }

        private void digits() {
            int start = at;
            while (at < end && text.charAt(at) >= '0' && text.charAt(at) <= '9') at++;
            if (at == start) throw fail(null);
        }

        float number() {
            int start = at;
            consume('-');
            digits();
            if (consume('.')) digits();
            int stop = at;
            endToken();
            try {
                return Float.parseFloat(text.substring(start, stop));
            } catch (NumberFormatException e) {
                throw fail(e);
            }
        }

        void extraNumbers(int maximum) {
            for (int count = 0; hasNext(); count++) {
                if (count == maximum) throw fail(null);
                number();
            }
        }

        int index() {
            int start = at;
            digits();
            try {
                return Integer.parseInt(text, start, at, 10) - 1;
            } catch (NumberFormatException e) {
                throw fail(e);
            }
        }

        String name() {
            int start = at;
            while (at < end) {
                char c = text.charAt(at);
                if (!(c >= 'a' && c <= 'z'
                        || c >= 'A' && c <= 'Z'
                        || c >= '0' && c <= '9'
                        || c == '_'
                        || c == '.')) break;
                at++;
            }
            if (at == start) throw fail(null);
            int stop = at;
            endToken();
            if (hasNext()) throw fail(null);
            return text.substring(start, stop);
        }

        void endToken() {
            if (at < end && !space(text.charAt(at))) throw fail(null);
            skipSpace();
        }

        private void skipSpace() {
            while (at < end && space(text.charAt(at))) at++;
        }

        private static boolean space(char value) {
            return value == ' ' || value >= '\t' && value <= '\r';
        }

        RuntimeException fail(Throwable cause) {
            return new RuntimeException("Error parsing OBJ " + source + " at line " + line, cause);
        }
    }

    private static final class Group {
        private final String name;
        private final Floats quads = new Floats(GroupObject.QUAD);
        private final Floats faceNormals = new Floats(3);
        private final float[] faceNormal = new float[3];
        private final int[] normalIndex = new int[4];

        Group(String name) {
            this.name = name;
        }

        void face(Line tokens, Floats positions, Floats uvs, Floats normals) {
            int base = quads.size();
            quads.grow(GroupObject.QUAD);
            int corners = 0;
            int format = -1;
            while (tokens.hasNext()) {
                if (corners == 4) throw tokens.fail(null);
                int position = tokens.index();
                int uv = -1;
                int normal = -1;
                int currentFormat = 0;
                if (tokens.consume('/')) {
                    if (tokens.consume('/')) {
                        normal = tokens.index();
                        currentFormat = 2;
                    } else {
                        uv = tokens.index();
                        currentFormat = 1;
                        if (tokens.consume('/')) {
                            normal = tokens.index();
                            currentFormat = 3;
                        }
                    }
                }
                tokens.endToken();
                if (corners == 0) format = currentFormat;
                else if (format != currentFormat) throw tokens.fail(null);
                int at = base + corners * GroupObject.STRIDE;
                quads.set(at, positions.get(position, 0));
                quads.set(at + 1, positions.get(position, 1));
                quads.set(at + 2, positions.get(position, 2));
                if ((format & 1) != 0) {
                    quads.set(at + 3, uvs.get(uv, 0));
                    quads.set(at + 4, uvs.get(uv, 1));
                }
                normalIndex[corners++] = normal;
            }
            if (corners < 3) throw tokens.fail(null);
            if (corners == 3) {

                System.arraycopy(
                        quads.array(),
                        base + 2 * GroupObject.STRIDE,
                        quads.array(),
                        base + 3 * GroupObject.STRIDE,
                        GroupObject.STRIDE);
                normalIndex[3] = normalIndex[2];
            }

            GroupObject.normal(quads.array(), base, GroupObject.STRIDE, faceNormal, 0);
            faceNormals.add(faceNormal[0], faceNormal[1], faceNormal[2]);
            for (int corner = 0; corner < 4; corner++) {
                int at = base + corner * GroupObject.STRIDE;
                int vn = normalIndex[corner];
                quads.set(at + 5, vn >= 0 ? normals.get(vn, 0) : faceNormal[0]);
                quads.set(at + 6, vn >= 0 ? normals.get(vn, 1) : faceNormal[1]);
                quads.set(at + 7, vn >= 0 ? normals.get(vn, 2) : faceNormal[2]);
            }
        }

        GroupObject build() {
            return new GroupObject(name, quads.trimmed(), faceNormals.trimmed());
        }
    }

    private static final class Floats {
        private final int width;
        private float[] data = new float[64];
        private int size;

        Floats(int width) {
            this.width = width;
        }

        void add(float a, float b) {
            grow(2);
            data[size - 2] = a;
            data[size - 1] = b;
        }

        void add(float a, float b, float c) {
            grow(3);
            data[size - 3] = a;
            data[size - 2] = b;
            data[size - 1] = c;
        }

        void grow(int by) {
            if (size + by > data.length) {
                int capacity = data.length;
                while (capacity < size + by) capacity <<= 1;
                data = Arrays.copyOf(data, capacity);
            }
            size += by;
        }

        float get(int record, int component) {
            return data[record * width + component];
        }

        void set(int at, float value) {
            data[at] = value;
        }

        float[] array() {
            return data;
        }

        int size() {
            return size;
        }

        float[] trimmed() {
            return size == data.length ? data : Arrays.copyOf(data, size);
        }
    }
}
