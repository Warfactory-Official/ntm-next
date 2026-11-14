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

package com.hbm.items.special;

import com.hbm.inventory.material.Material;
import com.hbm.inventory.material.MaterialShape;
import com.hbm.inventory.material.Materials;
import java.util.Map;
import org.jspecify.annotations.Nullable;

public final class AutogenIcons {
    private static final Map<MaterialShape, Map<Material, String>> AUTHORED =
            Map.of(
                    MaterialShape.FRAGMENT,
                            Map.of(Materials.BISMUTH, "bedrock_ore_fragment_bismuth"),
                    MaterialShape.FINE_WIRE,
                            Map.of(
                                    Materials.ALUMINUM, "wire_aluminium",
                                    Materials.COPPER, "wire_copper",
                                    Materials.MINGRADE, "wire_red_copper",
                                    Materials.GOLD, "wire_gold",
                                    Materials.TUNGSTEN, "wire_tungsten",
                                    Materials.CARBON, "wire_carbon",
                                    Materials.SCHRABIDIUM, "wire_schrabidium",
                                    Materials.MAGNETIZED_TUNGSTEN, "wire_magnetized_tungsten"));

    private static final Map<Material, String> AUTHORED_SCRAPS =
            Map.of(Materials.BISMUTH, "scraps_bismuth");

    private AutogenIcons() {}

    public static @Nullable String authored(MaterialShape shape, Material mat) {
        return AUTHORED.getOrDefault(shape, Map.of()).get(mat);
    }

    public static @Nullable String authoredScraps(Material mat) {
        return AUTHORED_SCRAPS.get(mat);
    }

    public static boolean remapped(MaterialShape shape, Material mat) {
        return authored(shape, mat) == null && remappable(mat);
    }

    public static boolean remappedScraps(Material mat) {
        return authoredScraps(mat) == null && remappable(mat);
    }

    public static boolean tinted(MaterialShape shape, Material mat) {
        return authored(shape, mat) == null && !remappable(mat);
    }

    public static String sprite(MaterialShape shape, Material mat) {
        String override = authored(shape, mat);
        return override != null ? override : shape.autogenItemId(mat.name());
    }

    private static boolean remappable(Material mat) {
        return mat.lightColor() != mat.darkColor();
    }
}
