// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.animloader;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import org.slf4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public final class ColladaLoader {

    private static final Logger LOGGER = LogUtils.getLogger();

    private ColladaLoader() {}

    public static AnimatedModel load(Identifier file) {
        return load(file, false);
    }

    public static AnimatedModel load(Identifier file, boolean flipV) {
        try {
            Resource resource =
                    Minecraft.getInstance().getResourceManager().getResourceOrThrow(file);
            try (InputStream stream = resource.open()) {
                Document doc =
                        DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(stream);
                return parse(doc.getDocumentElement(), flipV);
            }
        } catch (IOException | SAXException | ParserConfigurationException e) {
            LOGGER.error("FAILED TO LOAD MODEL: {}", file, e);
            return null;
        }
    }

    public static Animation loadAnim(int length, Identifier file) {
        try {
            Resource resource =
                    Minecraft.getInstance().getResourceManager().getResourceOrThrow(file);
            try (InputStream stream = resource.open()) {
                Document doc =
                        DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(stream);
                return parseAnim(doc.getDocumentElement(), length);
            }
        } catch (IOException | SAXException | ParserConfigurationException e) {
            LOGGER.error("FAILED TO LOAD MODEL: {}", file, e);
            return null;
        }
    }

    private static AnimatedModel parse(Element root, boolean flipV) {
        Element scene = getFirstElement(root.getElementsByTagName("library_visual_scenes").item(0));
        AnimatedModel structure = new AnimatedModel();

        List<Element> sceneNodes = getChildElements(scene);
        int childCount = 0;
        for (Element node : sceneNodes) {
            if (node.getElementsByTagName("instance_geometry").getLength() > 0) childCount++;
        }
        AnimatedModel[] children = new AnimatedModel[childCount];
        int childIndex = 0;
        for (Element node : sceneNodes) {
            if (node.getElementsByTagName("instance_geometry").getLength() > 0) {
                AnimatedModel child = parseStructure(node);
                child.parent = structure;
                children[childIndex++] = child;
            }
        }
        structure.setChildren(children);

        Map<String, AnimatedModel.Mesh> geometry =
                parseGeometry(
                        (Element) root.getElementsByTagName("library_geometries").item(0), flipV);
        addGeometry(structure, geometry);
        return structure;
    }

    private static Element getFirstElement(Node root) {
        NodeList nodes = root.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) return (Element) node;
        }
        return null;
    }

    private static List<Element> getElementsByName(Element root, String name) {
        List<Element> elements = new ArrayList<>();
        NodeList nodes = root.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE && node.getNodeName().equals(name)) {
                elements.add((Element) node);
            }
        }
        return elements;
    }

    private static List<Element> getChildElements(Element root) {
        List<Element> elements = new ArrayList<>();
        if (root == null) return elements;
        NodeList nodes = root.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) elements.add((Element) node);
        }
        return elements;
    }

    private static AnimatedModel parseStructure(Element root) {
        AnimatedModel model = new AnimatedModel();
        model.name = root.getAttribute("name");

        List<Element> children = getChildElements(root);
        int childCount = 0;
        for (Element element : children) {
            if (element.getElementsByTagName("instance_geometry").getLength() > 0
                    && !"instance_geometry".equals(element.getTagName())) {
                childCount++;
            }
        }
        AnimatedModel[] modelChildren = new AnimatedModel[childCount];
        int childIndex = 0;

        for (Element element : children) {
            if ("transform".equals(element.getAttribute("sid"))) {
                model.transform.set(
                        Transform.matrixFromArray(
                                flipMatrix(parseFloatArray(element.getTextContent()))));
                model.hasTransform = true;
            } else if ("instance_geometry".equals(element.getTagName())) {
                model.geoName = element.getAttribute("url").substring(1);
            } else if (element.getElementsByTagName("instance_geometry").getLength() > 0) {
                AnimatedModel child = parseStructure(element);
                child.parent = model;
                modelChildren[childIndex++] = child;
            }
        }
        model.setChildren(modelChildren);
        return model;
    }

    private static Map<String, AnimatedModel.Mesh> parseGeometry(Element root, boolean flipV) {
        Map<String, AnimatedModel.Mesh> allGeometry = new HashMap<>();
        for (Element geometry : getElementsByName(root, "geometry")) {
            String name = geometry.getAttribute("id");
            Element mesh = getElementsByName(geometry, "mesh").get(0);

            float[] positions = new float[0];
            float[] normals = new float[0];
            float[] texCoords = new float[0];
            int[] indices = new int[0];

            for (Element section : getChildElements(mesh)) {
                String id = section.getAttribute("id");
                if (id.endsWith("mesh-positions")) {
                    positions =
                            parseFloatArray(
                                    getElementsByName(section, "float_array")
                                            .get(0)
                                            .getTextContent());
                } else if (id.endsWith("mesh-normals")) {
                    normals =
                            parseFloatArray(
                                    getElementsByName(section, "float_array")
                                            .get(0)
                                            .getTextContent());
                } else if (id.endsWith("mesh-map-0")) {
                    texCoords =
                            parseFloatArray(
                                    getElementsByName(section, "float_array")
                                            .get(0)
                                            .getTextContent());
                } else if ("triangles".equals(section.getNodeName())) {
                    indices =
                            concat(
                                    indices,
                                    parseIntegerArray(
                                            section.getElementsByTagName("p")
                                                    .item(0)
                                                    .getTextContent()));
                }
            }
            if (positions.length == 0 || indices.length == 0) continue;
            allGeometry.put(
                    name,
                    new AnimatedModel.Mesh(
                            name,
                            interleaveTriangles(positions, normals, texCoords, indices, flipV)));
        }
        return allGeometry;
    }

    private static void addGeometry(AnimatedModel model, Map<String, AnimatedModel.Mesh> geometry) {
        model.setMesh(geometry.get(model.geoName));
        for (AnimatedModel child : model.children) {
            addGeometry(child, geometry);
        }
    }

    private static float[] interleaveTriangles(
            float[] positions, float[] normals, float[] texCoords, int[] indices, boolean flipV) {
        int vertexCount = indices.length / 3;
        float[] vertexData = new float[vertexCount * 8];
        int out = 0;
        for (int i = 0; i < indices.length; i += 3) {
            int posIndex = indices[i] * 3;
            int normalIndex = indices[i + 1] * 3;
            int uvIndex = indices[i + 2] * 2;

            vertexData[out++] = positions[posIndex];
            vertexData[out++] = positions[posIndex + 1];
            vertexData[out++] = positions[posIndex + 2];
            vertexData[out++] = texCoords[uvIndex];
            float v = texCoords[uvIndex + 1];
            vertexData[out++] = flipV ? 1F - v : v;
            vertexData[out++] = normals[normalIndex];
            vertexData[out++] = normals[normalIndex + 1];
            vertexData[out++] = normals[normalIndex + 2];
        }
        return vertexData;
    }

    private static int[] concat(int[] left, int[] right) {
        int[] joined = new int[left.length + right.length];
        System.arraycopy(left, 0, joined, 0, left.length);
        System.arraycopy(right, 0, joined, left.length, right.length);
        return joined;
    }

    private static Animation parseAnim(Element root, int length) {
        Element animSection = (Element) root.getElementsByTagName("library_animations").item(0);
        Animation anim = new Animation();
        anim.length = length;
        for (Element section : getChildElements(animSection)) {
            if ("animation".equals(section.getNodeName())) {
                String name = section.getAttribute("name");
                Transform[] transforms = null;
                List<Element> children = getChildElements(section);
                if (children.isEmpty()) continue;
                for (Element child : children) {
                    if (child.getAttribute("id").endsWith("transform")) {
                        transforms = parseTransforms(child);
                    } else if (child.getAttribute("id").endsWith("hide_viewport")) {
                        setViewportHiddenKeyframes(transforms, child);
                    }
                }
                anim.objectTransforms.put(name, transforms);
                anim.numKeyFrames = transforms.length;
            }
        }
        return anim;
    }

    private static Transform[] parseTransforms(Element root) {
        String output = getOutputLocation(root);
        for (Element child : getChildElements(root)) {
            if (child.getAttribute("id").equals(output)) {
                return parseTransformsFromText(
                        child.getElementsByTagName("float_array").item(0).getTextContent());
            }
        }
        throw new IllegalStateException(
                "Failed to parse transforms for node " + root.getAttribute("id"));
    }

    private static void setViewportHiddenKeyframes(Transform[] transforms, Element root) {
        String output = getOutputLocation(root);
        for (Element child : getChildElements(root)) {
            if (child.getAttribute("id").equals(output)) {
                int[] hiddenFrames =
                        parseIntegerArray(
                                child.getElementsByTagName("float_array").item(0).getTextContent());
                for (int i = 0; i < hiddenFrames.length; i++) {
                    transforms[i].hidden = hiddenFrames[i] > 0;
                }
            }
        }
    }

    private static String getOutputLocation(Element root) {
        Element sampler = (Element) root.getElementsByTagName("sampler").item(0);
        for (Element child : getChildElements(sampler)) {
            if ("OUTPUT".equals(child.getAttribute("semantic"))) {
                return child.getAttribute("source").substring(1);
            }
        }
        return null;
    }

    private static Transform[] parseTransformsFromText(String data) {
        float[] floats = parseFloatArray(data);
        Transform[] transforms = new Transform[floats.length / 16];
        for (int i = 0; i < transforms.length; i++) {
            float[] rawTransform = new float[16];
            System.arraycopy(floats, i * 16, rawTransform, 0, 16);
            transforms[i] = new Transform(flipMatrix(rawTransform));
        }
        return transforms;
    }

    private static float[] parseFloatArray(String data) {
        String text = data.trim();
        if (text.isEmpty()) return new float[0];
        String[] numbers = text.split("\\s+");
        float[] out = new float[numbers.length];
        for (int i = 0; i < numbers.length; i++) {
            out[i] = Float.parseFloat(numbers[i]);
        }
        return out;
    }

    private static int[] parseIntegerArray(String data) {
        String text = data.trim();
        if (text.isEmpty()) return new int[0];
        String[] numbers = text.split("\\s+");
        int[] out = new int[numbers.length];
        for (int i = 0; i < numbers.length; i++) {
            out[i] = Integer.parseInt(numbers[i]);
        }
        return out;
    }

    private static float[] flipMatrix(float[] matrix) {
        if (matrix.length != 16) throw new IllegalStateException("Matrix length must be 16");
        return new float[] {
            matrix[0], matrix[4], matrix[8], matrix[12],
            matrix[1], matrix[5], matrix[9], matrix[13],
            matrix[2], matrix[6], matrix[10], matrix[14],
            matrix[3], matrix[7], matrix[11], matrix[15]
        };
    }
}
