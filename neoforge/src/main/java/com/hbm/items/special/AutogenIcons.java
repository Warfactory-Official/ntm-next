// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.special;

import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.NTMMaterial;
import java.util.Map;
import org.jspecify.annotations.Nullable;

public final class AutogenIcons {

    private static final Map<MaterialShapes, Map<NTMMaterial, String>> AUTHORED =
            Map.of(
                    MaterialShapes.FRAGMENT,
                            Map.of(Mats.MAT_BISMUTH, "bedrock_ore_fragment_bismuth"),
                    MaterialShapes.WIRE,
                            Map.of(
                                    Mats.MAT_ALUMINIUM, "wire_aluminium",
                                    Mats.MAT_COPPER, "wire_copper",
                                    Mats.MAT_MINGRADE, "wire_red_copper",
                                    Mats.MAT_GOLD, "wire_gold",
                                    Mats.MAT_TUNGSTEN, "wire_tungsten",
                                    Mats.MAT_CARBON, "wire_carbon",
                                    Mats.MAT_SCHRABIDIUM, "wire_schrabidium",
                                    Mats.MAT_MAGTUNG, "wire_magnetized_tungsten"));

    private static final Map<NTMMaterial, String> AUTHORED_SCRAPS =
            Map.of(Mats.MAT_BISMUTH, "scraps_bismuth");

    private AutogenIcons() {}

    public static @Nullable String authored(MaterialShapes shape, NTMMaterial mat) {
        return AUTHORED.getOrDefault(shape, Map.of()).get(mat);
    }

    public static @Nullable String authoredScraps(NTMMaterial mat) {
        return AUTHORED_SCRAPS.get(mat);
    }

    public static boolean remapped(MaterialShapes shape, NTMMaterial mat) {
        return authored(shape, mat) == null && remappable(mat);
    }

    public static boolean remappedScraps(NTMMaterial mat) {
        return authoredScraps(mat) == null && remappable(mat);
    }

    public static boolean ownsSprite(MaterialShapes shape, NTMMaterial mat) {
        return authored(shape, mat) != null || remapped(shape, mat);
    }

    public static String sprite(MaterialShapes shape, NTMMaterial mat) {
        String override = authored(shape, mat);
        return override != null ? override : shape.autogenItemId(mat.tagPath).getPath();
    }

    private static boolean remappable(NTMMaterial mat) {
        return mat.solidColorLight != mat.solidColorDark;
    }
}
