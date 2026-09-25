// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.render.loader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import net.minecraft.resources.Identifier;

public final class ObjMesh {

    static final int MAGIC = 0x48424D4F;
    static final int VERSION = 1;

    private ObjMesh() {}

    public static Identifier resource(Identifier obj) {
        String path = obj.getPath();
        if (!path.endsWith(".obj"))
            throw new IllegalArgumentException("Expected OBJ identifier: " + obj);
        return obj.withPath(path.substring(0, path.length() - 4) + ".hbm_mesh");
    }

    public static GroupObject[] read(InputStream input, String source) throws IOException {
        try {
            ByteBuffer data = ByteBuffer.wrap(input.readAllBytes());
            if (data.getInt() != MAGIC || data.getInt() != VERSION) {
                throw new IOException("Unsupported mesh header: " + source);
            }
            float[] positions = table(data, 3);
            float[] uvs = table(data, 2);
            float[] normals = table(data, 3);
            GroupObject[] groups = new GroupObject[count(data, 32)];
            for (int g = 0; g < groups.length; g++) {
                int nameLength = count(data, 1);
                String name =
                        new String(
                                data.array(), data.position(), nameLength, StandardCharsets.UTF_8);
                data.position(data.position() + nameLength);
                float[] bounds = floats(data, 6);
                int faces = count(data, 11);
                float[] quads = new float[Math.multiplyExact(faces, GroupObject.QUAD)];
                float[] faceNormals = new float[Math.multiplyExact(faces, 3)];
                int position = 0, uv = 0, normal = 0, faceNormal = 0;
                for (int f = 0; f < faces; f++) {
                    int corners = data.get() & 0xFF;
                    if (corners != 3 && corners != 4)
                        throw new IOException("Invalid face size: " + source);
                    faceNormal += delta(data);
                    System.arraycopy(normals, faceNormal * 3, faceNormals, f * 3, 3);
                    int base = f * GroupObject.QUAD;
                    for (int c = 0; c < corners; c++) {
                        int vertex = base + c * GroupObject.STRIDE;
                        position += delta(data);
                        uv += delta(data);
                        normal += delta(data);
                        System.arraycopy(positions, position * 3, quads, vertex, 3);
                        System.arraycopy(uvs, uv * 2, quads, vertex + 3, 2);
                        System.arraycopy(normals, normal * 3, quads, vertex + 5, 3);
                    }
                    if (corners == 3)
                        System.arraycopy(
                                quads,
                                base + 2 * GroupObject.STRIDE,
                                quads,
                                base + 3 * GroupObject.STRIDE,
                                GroupObject.STRIDE);
                }
                groups[g] = new GroupObject(name, quads, faceNormals, bounds);
            }
            if (data.hasRemaining()) throw new IOException("Trailing mesh data: " + source);
            return groups;
        } catch (IOException | RuntimeException e) {
            throw new IOException("Invalid mesh: " + source, e);
        }
    }

    private static int delta(ByteBuffer data) throws IOException {
        int value = 0;
        for (int shift = 0; shift < Integer.SIZE; shift += 7) {
            int next = data.get() & 0xFF;
            if (shift == 28 && (next & 0xF0) != 0) throw new IOException("Mesh index overflow");
            value |= (next & 0x7F) << shift;
            if ((next & 0x80) == 0) return (value >>> 1) ^ -(value & 1);
        }
        throw new IOException("Mesh index overflow");
    }

    private static int count(ByteBuffer data, int width) throws IOException {
        int count = data.getInt();
        if (count < 0 || count > data.remaining() / width)
            throw new IOException("Invalid mesh count: " + count);
        return count;
    }

    private static float[] table(ByteBuffer data, int width) throws IOException {
        return floats(data, count(data, width * Float.BYTES) * width);
    }

    private static float[] floats(ByteBuffer data, int count) {
        float[] values = new float[count];
        data.asFloatBuffer().get(values);
        data.position(data.position() + count * Float.BYTES);
        return values;
    }
}
