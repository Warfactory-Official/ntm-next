// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.serialization.JsonOps;
import java.util.*;
import net.minecraft.client.resources.model.cuboid.CuboidFace;
import net.minecraft.client.resources.model.geometry.UnbakedGeometry;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

public final class HbmModelJson {

    private static final Set<String> KNOWN_KEYS =
            Set.of(
                    "loader",
                    "fabric:type",
                    "model",
                    "skins",
                    "hidden",
                    "offset_x",
                    "offset_y",
                    "offset_z",
                    "overlay",
                    "overlay_tint",
                    "translucent",
                    "emissive",
                    "tiled",
                    "blackout",
                    "double_sided",
                    "groups",
                    "boxes",
                    "parent",
                    "textures",
                    "display",
                    "ambientocclusion",
                    "gui_light",
                    "visibility",
                    "smooth",
                    "uv_scale");

    private static final Set<String> SINGLE_GROUP_KEYS =
            Set.of("skins", "blackout", "overlay", "overlay_tint", "translucent", "smooth");

    private static final Set<String> GROUP_KEYS =
            Set.of("model", "parts", "texture", "matrix", "smooth", "color");

    private static final Set<String> BOX_KEYS =
            Set.of("from", "to", "texture", "faces", "uvs", "color");

    private HbmModelJson() {}

    public static UnbakedGeometry obj(JsonObject json) {
        checkKeys(json);
        if (json.has("groups") || json.has("boxes")) return objGroups(json);
        if (!json.has("model")) {
            throw new JsonParseException("hbm:obj model requires a 'model' key pointing to a .obj");
        }
        Identifier objId = Identifier.parse(json.get("model").getAsString());
        Map<String, String[]> skins = new LinkedHashMap<>();
        if (json.has("skins")) {
            for (Map.Entry<String, JsonElement> e : json.getAsJsonObject("skins").entrySet()) {
                skins.put(e.getKey(), toArray(e.getValue().getAsJsonArray()));
            }
        }
        if (skins.isEmpty()) {
            throw new JsonParseException(
                    "hbm:obj model "
                            + objId
                            + " states no 'skins', so no group of its "
                            + "mesh wears a sheet and it would draw nothing");
        }
        float offsetX = json.has("offset_x") ? json.get("offset_x").getAsFloat() : 0.0F;
        float offsetY = json.has("offset_y") ? json.get("offset_y").getAsFloat() : 0.0F;
        float offsetZ = json.has("offset_z") ? json.get("offset_z").getAsFloat() : 0.0F;
        boolean overlay = json.has("overlay") && json.get("overlay").getAsBoolean();
        int overlayTintIndex = json.has("overlay_tint") ? json.get("overlay_tint").getAsInt() : 1;
        boolean translucentBase = json.has("translucent") && json.get("translucent").getAsBoolean();
        String[] emissive = strings(json, "emissive");
        String[] blackout = strings(json, "blackout");
        if (skins.size() != 1) {
            if (emissive != null) {
                throw new JsonParseException(
                        "hbm:obj model "
                                + objId
                                + " combines 'emissive' with "
                                + skins.size()
                                + " skin slots; the multi-slot bake does not carry it");
            }
            if (blackout != null) {
                throw new JsonParseException(
                        "hbm:obj model "
                                + objId
                                + " combines 'blackout' with "
                                + skins.size()
                                + " skin slots; the multi-slot bake does not carry it");
            }
            if (overlay) {
                throw new JsonParseException(
                        "hbm:obj model "
                                + objId
                                + " combines 'overlay' with "
                                + skins.size()
                                + " skin slots, so which sheet the coincident pass covers is unstated");
            }
        }
        if (overlay && json.has("double_sided")) {
            throw new JsonParseException(
                    "hbm:obj model "
                            + objId
                            + " combines 'double_sided' with 'overlay'; "
                            + "the coincident pass draws front faces alone, so the back copy would lose its overlay");
        }
        boolean smooth = !json.has("smooth") || json.get("smooth").getAsBoolean();
        return new ObjUnbakedGeometry(
                objId,
                skins,
                hidden(json),
                offsetX,
                offsetY,
                offsetZ,
                overlay,
                overlayTintIndex,
                translucentBase,
                new ObjUnbakedGeometry.Overrides(
                        emissive, strings(json, "tiled"), blackout, strings(json, "double_sided")),
                smooth,
                uvScales(json));
    }

    private static String[] hidden(JsonObject json) {
        String[] declared = strings(json, "hidden");
        return declared == null ? new String[0] : declared;
    }

    private static Map<String, float[]> uvScales(JsonObject json) {
        if (!json.has("uv_scale")) return Map.of();
        Map<String, float[]> scales = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> e : json.getAsJsonObject("uv_scale").entrySet()) {
            JsonArray pair = e.getValue().getAsJsonArray();
            if (pair.size() != 2) {
                throw new JsonParseException(
                        "hbm:obj 'uv_scale' slot '"
                                + e.getKey()
                                + "' states "
                                + pair.size()
                                + " number(s); it takes exactly [u, v]");
            }
            scales.put(
                    e.getKey(), new float[] {pair.get(0).getAsFloat(), pair.get(1).getAsFloat()});
        }
        return scales;
    }

    private static UnbakedGeometry objGroups(JsonObject json) {
        List<String> conflicting = SINGLE_GROUP_KEYS.stream().filter(json::has).sorted().toList();
        if (!conflicting.isEmpty()) {
            throw new JsonParseException(
                    "hbm:obj model states 'groups' alongside "
                            + conflicting
                            + ", which the group bake does not read; state those per group instead");
        }
        Identifier fallback =
                json.has("model") ? Identifier.parse(json.get("model").getAsString()) : null;
        List<ObjUnbakedGeometry.GroupSpec> specs = new ArrayList<>();
        if (json.has("groups"))
            for (JsonElement element : json.getAsJsonArray("groups")) {
                JsonObject group = element.getAsJsonObject();
                List<String> unknown =
                        group.keySet().stream()
                                .filter(k -> !GROUP_KEYS.contains(k))
                                .sorted()
                                .toList();
                if (!unknown.isEmpty()) {
                    throw new JsonParseException(
                            "hbm:obj group has unrecognised key(s) "
                                    + unknown
                                    + "; known keys are "
                                    + new TreeSet<>(GROUP_KEYS));
                }
                Identifier mesh =
                        group.has("model")
                                ? Identifier.parse(group.get("model").getAsString())
                                : fallback;
                if (mesh == null) {
                    throw new JsonParseException(
                            "hbm:obj group states no 'model' and the model states no default one");
                }
                String[] parts = strings(group, "parts");
                if (parts == null || parts.length == 0) {
                    throw new JsonParseException(
                            "hbm:obj group in " + mesh + " requires a non-empty 'parts'");
                }
                if (!group.has("texture")) {
                    throw new JsonParseException(
                            "hbm:obj group "
                                    + List.of(parts)
                                    + " in "
                                    + mesh
                                    + " states no 'texture', so the sheet it draws under is unstated");
                }
                String slot = group.get("texture").getAsString();
                Matrix4fc transform = group.has("matrix") ? matrix(group.get("matrix")) : null;
                boolean smooth = !group.has("smooth") || group.get("smooth").getAsBoolean();
                specs.add(
                        new ObjUnbakedGeometry.GroupSpec(
                                mesh, parts, slot, transform, smooth, color(group)));
            }
        if (json.has("groups") && specs.isEmpty()) {
            throw new JsonParseException("hbm:obj model states an empty 'groups'");
        }
        float offsetX = json.has("offset_x") ? json.get("offset_x").getAsFloat() : 0.0F;
        float offsetY = json.has("offset_y") ? json.get("offset_y").getAsFloat() : 0.0F;
        float offsetZ = json.has("offset_z") ? json.get("offset_z").getAsFloat() : 0.0F;
        return new ObjUnbakedGeometry(
                fallback,
                specs,
                boxes(json),
                hidden(json),
                offsetX,
                offsetY,
                offsetZ,
                uvScales(json),
                new ObjUnbakedGeometry.Overrides(
                        strings(json, "emissive"),
                        strings(json, "tiled"),
                        null,
                        strings(json, "double_sided")));
    }

    private static List<ObjUnbakedGeometry.BoxSpec> boxes(JsonObject json) {
        if (!json.has("boxes")) return List.of();
        List<ObjUnbakedGeometry.BoxSpec> boxes = new ArrayList<>();
        for (JsonElement element : json.getAsJsonArray("boxes")) {
            JsonObject box = element.getAsJsonObject();
            List<String> unknown =
                    box.keySet().stream().filter(k -> !BOX_KEYS.contains(k)).sorted().toList();
            if (!unknown.isEmpty()) {
                throw new JsonParseException(
                        "hbm:obj box has unrecognised key(s) "
                                + unknown
                                + "; known keys are "
                                + new TreeSet<>(BOX_KEYS));
            }
            Map<Direction, String> faces = boxFaces(box);
            boxes.add(
                    new ObjUnbakedGeometry.BoxSpec(
                            corner(box, "from"),
                            corner(box, "to"),
                            faces,
                            boxUvs(box, faces.keySet()),
                            color(box)));
        }
        if (boxes.isEmpty()) throw new JsonParseException("hbm:obj model states an empty 'boxes'");
        return boxes;
    }

    private static Map<Direction, String> boxFaces(JsonObject box) {
        boolean hasFaces = box.has("faces");
        if (box.has("texture") == hasFaces) {
            throw new JsonParseException(
                    "hbm:obj box states "
                            + (hasFaces
                                    ? "both 'texture' and 'faces'"
                                    : "neither 'texture' nor 'faces'")
                            + "; it takes exactly one - 'texture' for all six "
                            + "faces under one slot, 'faces' for a subset naming a slot each");
        }
        if (!hasFaces) {
            String slot = box.get("texture").getAsString();
            Map<Direction, String> all = new LinkedHashMap<>();
            for (Direction dir : Direction.VALUES) all.put(dir, slot);
            return all;
        }
        Map<Direction, String> faces = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : box.getAsJsonObject("faces").entrySet()) {
            Direction dir = Direction.byName(entry.getKey());
            if (dir == null) {
                throw new JsonParseException(
                        "hbm:obj box face '" + entry.getKey() + "' is not a direction");
            }
            faces.put(dir, entry.getValue().getAsString());
        }
        if (faces.isEmpty()) throw new JsonParseException("hbm:obj box states an empty 'faces'");
        return faces;
    }

    private static Map<Direction, CuboidFace.UVs> boxUvs(JsonObject box, Set<Direction> faces) {
        if (!box.has("uvs")) return Map.of();
        Map<Direction, CuboidFace.UVs> values = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : box.getAsJsonObject("uvs").entrySet()) {
            Direction face = Direction.byName(entry.getKey());
            if (!faces.contains(face)) {
                throw new JsonParseException(
                        "hbm:obj box UV names an undrawn face '" + entry.getKey() + "'");
            }
            JsonArray array = entry.getValue().getAsJsonArray();
            if (array.size() != 4) {
                throw new JsonParseException(
                        "hbm:obj box UV for '"
                                + entry.getKey()
                                + "' has "
                                + array.size()
                                + " values, expected four");
            }
            float u0 = array.get(0).getAsFloat(), v0 = array.get(1).getAsFloat();
            float u1 = array.get(2).getAsFloat(), v1 = array.get(3).getAsFloat();
            if (!Float.isFinite(u0)
                    || !Float.isFinite(v0)
                    || !Float.isFinite(u1)
                    || !Float.isFinite(v1)) {
                throw new JsonParseException(
                        "hbm:obj box UV for '" + entry.getKey() + "' is not finite");
            }
            values.put(face, new CuboidFace.UVs(u0, v0, u1, v1));
        }
        if (!values.keySet().equals(faces)) {
            throw new JsonParseException(
                    "hbm:obj box UV faces "
                            + values.keySet()
                            + " do not cover its drawn faces "
                            + faces);
        }
        return values;
    }

    private static int color(JsonObject json) {
        if (!json.has("color")) return ObjUnbakedGeometry.WHITE;
        String hex = json.get("color").getAsString().replaceFirst("^(0x|#)", "");
        if (hex.length() != 6 && hex.length() != 8) {
            throw new JsonParseException(
                    "hbm:obj 'color' is '"
                            + json.get("color").getAsString()
                            + "'; it takes 6 hex digits (opaque RGB) or 8 (ARGB)");
        }
        try {
            long argb = Long.parseUnsignedLong(hex, 16);
            return hex.length() == 6 ? (int) (argb | 0xFF000000L) : (int) argb;
        } catch (NumberFormatException e) {
            throw new JsonParseException(
                    "hbm:obj 'color' is not hex: " + json.get("color").getAsString());
        }
    }

    private static Matrix4fc matrix(JsonElement element) {
        return ExtraCodecs.MATRIX4F
                .parse(JsonOps.INSTANCE, element)
                .getOrThrow(error -> new JsonParseException("hbm:obj group 'matrix': " + error));
    }

    private static Vector3f corner(JsonObject box, String key) {
        if (!box.has(key)) throw new JsonParseException("hbm:obj box states no '" + key + "'");
        JsonArray triple = box.getAsJsonArray(key);
        if (triple.size() != 3) {
            throw new JsonParseException(
                    "hbm:obj box '"
                            + key
                            + "' states "
                            + triple.size()
                            + " number(s); it takes exactly [x, y, z]");
        }
        return new Vector3f(
                triple.get(0).getAsFloat(), triple.get(1).getAsFloat(), triple.get(2).getAsFloat());
    }

    private static void checkKeys(JsonObject json) {
        List<String> unknown =
                json.keySet().stream().filter(k -> !KNOWN_KEYS.contains(k)).sorted().toList();
        if (!unknown.isEmpty()) {
            throw new JsonParseException(
                    "hbm model has unrecognised key(s) "
                            + unknown
                            + "; known keys are "
                            + new TreeSet<>(KNOWN_KEYS));
        }
    }

    private static @Nullable String[] strings(JsonObject json, String key) {
        return json.has(key) ? toArray(json.getAsJsonArray(key)) : null;
    }

    private static String[] toArray(JsonArray arr) {
        String[] names = new String[arr.size()];
        for (int i = 0; i < arr.size(); i++) names[i] = arr.get(i).getAsString();
        return names;
    }
}
