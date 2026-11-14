/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.animloader;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public record ColladaScene(ArmatureScene model, SampledClip animation) {

    private static final String[] NO_TOKENS = new String[0];

    public static ColladaScene load(Identifier id, boolean flipV, int lengthMillis) {
        try (var in = Minecraft.getInstance().getResourceManager().open(id)) {
            return read(in, flipV, lengthMillis);
        } catch (IOException e) {
            throw new UncheckedIOException("COLLADA " + id, e);
        }
    }

    public static ColladaScene read(InputStream in, boolean flipV, int lengthMillis)
            throws IOException {
        Document document;
        try {
            var factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            var builder = factory.newDocumentBuilder();
            builder.setErrorHandler(new DefaultHandler());
            document = builder.parse(in);
        } catch (ParserConfigurationException | SAXException e) {
            throw new IOException(e);
        }

        var tracks = new ArrayList<LocalPose[]>();
        var trackByNode = new HashMap<String, Integer>();
        int frameCount = 1;
        var animations = (Element) document.getElementsByTagName("library_animations").item(0);
        if (animations != null) {
            for (var animation : children(animations)) {
                if (!animation.getTagName().equals("animation")) continue;
                var frames = frames(animation);
                if (frames == null) continue;
                Integer previous =
                        trackByNode.putIfAbsent(animation.getAttribute("name"), tracks.size());
                if (previous == null) tracks.add(frames);
                else tracks.set(previous, frames);
                frameCount = frames.length;
            }
        }

        var meshes = meshes(document, flipV);
        var scenes = (Element) document.getElementsByTagName("library_visual_scenes").item(0);
        if (scenes == null)
            throw new IllegalArgumentException("COLLADA document without library_visual_scenes");
        var clip = new SampledClip(lengthMillis, frameCount, tracks.toArray(new LocalPose[0][]));
        var root =
                new ArmatureScene.Node(
                        null, -1, null, nodes(children(scenes).getFirst(), meshes, trackByNode));
        return new ColladaScene(ArmatureScene.freeze(root, clip), clip);
    }

    private static LocalPose @Nullable [] frames(Element animation) {
        var channels = children(animation);
        if (channels.isEmpty()) return null;
        LocalPose[] frames = null;
        for (var channel : channels) {
            String id = channel.getAttribute("id");
            if (id.endsWith("transform")) {
                float[] values = floats(output(channel));
                if (values.length % 16 != 0)
                    throw new IllegalArgumentException("Incomplete local pose in " + id);
                frames = new LocalPose[values.length / 16];
                for (int i = 0; i < frames.length; i++)
                    frames[i] = new LocalPose(columnMajor(values, i * 16), false);
            } else if (id.endsWith("hide_viewport")) {
                if (frames == null)
                    throw new IllegalArgumentException("visibility before transform in " + id);
                String[] flags = tokens(output(channel));
                if (flags.length != frames.length)
                    throw new IllegalArgumentException("Inconsistent visibility samples in " + id);
                for (int i = 0; i < flags.length; i++)
                    frames[i] = frames[i].withHidden(Integer.parseInt(flags[i]) > 0);
            }
        }
        if (frames == null) {
            throw new IllegalArgumentException(
                    "animation " + animation.getAttribute("name") + " has no transform");
        }
        return frames;
    }

    private static Element output(Element channel) {
        var sampler = (Element) channel.getElementsByTagName("sampler").item(0);
        String source = null;
        if (sampler != null) {
            for (var input : children(sampler)) {
                if (input.getTagName().equals("input")
                        && input.getAttribute("semantic").equals("OUTPUT")) {
                    source = local(input.getAttribute("source"));
                    break;
                }
            }
        }
        for (var child : children(channel)) {
            if (child.getAttribute("id").equals(source)) return firstArray(child);
        }
        throw new IllegalArgumentException("no OUTPUT source in " + channel.getAttribute("id"));
    }

    private static Map<String, TriangleSource> meshes(Document document, boolean flipV) {
        var meshes = new HashMap<String, TriangleSource>();
        var geometries = (Element) document.getElementsByTagName("library_geometries").item(0);
        if (geometries == null) return meshes;
        for (var geometry : children(geometries)) {
            if (!geometry.getTagName().equals("geometry")) continue;
            String id = geometry.getAttribute("id");
            Element mesh = null;
            for (var child : children(geometry)) {
                if (child.getTagName().equals("mesh")) {
                    mesh = child;
                    break;
                }
            }
            if (mesh == null)
                throw new IllegalArgumentException("geometry " + id + " without mesh");
            float[] positions = null, normals = null, uvs = null;
            var indices = new IntArrayList();
            for (var part : children(mesh)) {
                String partId = part.getAttribute("id");
                if (partId.endsWith("mesh-positions")) positions = floats(firstArray(part));
                else if (partId.endsWith("mesh-normals")) normals = floats(firstArray(part));
                else if (partId.endsWith("mesh-map-0")) uvs = floats(firstArray(part));
                else if (part.getTagName().equals("triangles")) {
                    for (var index : tokens((Element) part.getElementsByTagName("p").item(0))) {
                        indices.add(Integer.parseInt(index));
                    }
                }
            }
            if (indices.isEmpty()) {
                meshes.put(id, null);
                continue;
            }
            if (positions == null) continue;
            meshes.put(
                    id, new TriangleSource(vertices(id, positions, normals, uvs, indices, flipV)));
        }
        return meshes;
    }

    private static float[] vertices(
            String id,
            float[] positions,
            float[] normals,
            float[] uvs,
            IntArrayList indices,
            boolean flipV) {
        if (indices.size() % 9 != 0)
            throw new IllegalArgumentException("geometry " + id + " ends mid-triangle");
        int corners = indices.size() / 3;
        var vertices = new float[corners * 8];
        for (int corner = 0; corner < corners; corner++) {
            int position = indices.getInt(corner * 3) * 3;
            int normal = indices.getInt(corner * 3 + 1) * 3;
            int uv = indices.getInt(corner * 3 + 2) * 2;
            int at = corner * 8;
            vertices[at] = positions[position];
            vertices[at + 1] = positions[position + 1];
            vertices[at + 2] = positions[position + 2];
            vertices[at + 3] = uvs[uv];
            vertices[at + 4] = flipV ? 1 - uvs[uv + 1] : uvs[uv + 1];
            vertices[at + 5] = normals[normal];
            vertices[at + 6] = normals[normal + 1];
            vertices[at + 7] = normals[normal + 2];
        }
        return vertices;
    }

    private static List<ArmatureScene.Node> nodes(
            Element parent, Map<String, TriangleSource> meshes, Map<String, Integer> tracks) {
        var nodes = new ArrayList<ArmatureScene.Node>();
        for (var child : children(parent)) {
            if (child.getTagName().equals("node")) nodes.add(node(child, meshes, tracks));
        }
        return nodes;
    }

    private static ArmatureScene.Node node(
            Element element, Map<String, TriangleSource> meshes, Map<String, Integer> tracks) {
        int track = tracks.getOrDefault(element.getAttribute("name"), -1);
        Matrix4f localPose = null;
        TriangleSource geometry = null;
        var children = new ArrayList<ArmatureScene.Node>();
        for (var child : children(element)) {
            if (child.getAttribute("sid").equals("transform")) {
                float[] values = floats(child);
                if (values.length != 16)
                    throw new IllegalArgumentException("Incomplete static local matrix");
                localPose = new Matrix4f().set(columnMajor(values, 0));
            } else if (child.getTagName().equals("instance_geometry")) {
                String geometryId = local(child.getAttribute("url"));
                geometry = meshes.get(geometryId);
                if (geometry == null && !meshes.containsKey(geometryId)) {
                    throw new IllegalArgumentException("Missing geometry " + geometryId);
                }
            } else if (child.getTagName().equals("node")) {
                children.add(node(child, meshes, tracks));
            }
        }
        return new ArmatureScene.Node(localPose, track, geometry, children);
    }

    private static Element firstArray(Element element) {
        return (Element) element.getElementsByTagName("float_array").item(0);
    }

    private static List<Element> children(Element parent) {
        var children = new ArrayList<Element>();
        for (var child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child.getNodeType() == Node.ELEMENT_NODE) children.add((Element) child);
        }
        return children;
    }

    private static String local(String reference) {
        return reference.startsWith("#") ? reference.substring(1) : reference;
    }

    private static String[] tokens(Element element) {
        String text = element.getTextContent().strip();
        return text.isEmpty() ? NO_TOKENS : text.split("\\s+");
    }

    private static float[] floats(Element element) {
        String[] tokens = tokens(element);
        var values = new float[tokens.length];
        for (int i = 0; i < tokens.length; i++) values[i] = Float.parseFloat(tokens[i]);
        return values;
    }

    private static float[] columnMajor(float[] rowMajor, int at) {
        var matrix = new float[16];
        for (int row = 0; row < 4; row++) {
            for (int column = 0; column < 4; column++)
                matrix[column * 4 + row] = rowMajor[at + row * 4 + column];
        }
        return matrix;
    }
}
