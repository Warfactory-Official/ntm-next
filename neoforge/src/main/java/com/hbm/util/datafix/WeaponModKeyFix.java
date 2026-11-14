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

package com.hbm.util.datafix;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.Map;
import net.minecraft.nbt.IntArrayTag;

final class WeaponModKeyFix extends ItemComponentFix {

    private static final String STATE = "hbm:gun_state";
    private static final String MODS = "KEY_MOD_LIST_";
    private static final Map<Integer, String> KEYS =
            Map.ofEntries(
                    Map.entry(0, "hbm:weapon_mod_test_firerate"),
                    Map.entry(1, "hbm:weapon_mod_test_damage"),
                    Map.entry(2, "hbm:weapon_mod_test_multi"),
                    Map.entry(3, "hbm:weapon_mod_test_override_2_5"),
                    Map.entry(4, "hbm:weapon_mod_test_override_5"),
                    Map.entry(5, "hbm:weapon_mod_test_override_7_5"),
                    Map.entry(6, "hbm:weapon_mod_test_override_10"),
                    Map.entry(7, "hbm:weapon_mod_test_override_12_5"),
                    Map.entry(8, "hbm:weapon_mod_test_override_15"),
                    Map.entry(9, "hbm:weapon_mod_test_override_20"),
                    Map.entry(100, "hbm:weapon_mod_generic_iron_damage"),
                    Map.entry(101, "hbm:weapon_mod_generic_iron_dura"),
                    Map.entry(102, "hbm:weapon_mod_generic_steel_damage"),
                    Map.entry(103, "hbm:weapon_mod_generic_steel_dura"),
                    Map.entry(104, "hbm:weapon_mod_generic_dura_damage"),
                    Map.entry(105, "hbm:weapon_mod_generic_dura_dura"),
                    Map.entry(106, "hbm:weapon_mod_generic_desh_damage"),
                    Map.entry(107, "hbm:weapon_mod_generic_desh_dura"),
                    Map.entry(108, "hbm:weapon_mod_generic_wsteel_damage"),
                    Map.entry(109, "hbm:weapon_mod_generic_wsteel_dura"),
                    Map.entry(110, "hbm:weapon_mod_generic_ferro_damage"),
                    Map.entry(111, "hbm:weapon_mod_generic_ferro_dura"),
                    Map.entry(112, "hbm:weapon_mod_generic_tcalloy_damage"),
                    Map.entry(113, "hbm:weapon_mod_generic_tcalloy_dura"),
                    Map.entry(114, "hbm:weapon_mod_generic_bigmt_damage"),
                    Map.entry(115, "hbm:weapon_mod_generic_bigmt_dura"),
                    Map.entry(116, "hbm:weapon_mod_generic_bronze_damage"),
                    Map.entry(117, "hbm:weapon_mod_generic_bronze_dura"),
                    Map.entry(200, "hbm:weapon_mod_special_speedloader"),
                    Map.entry(201, "hbm:weapon_mod_special_silencer"),
                    Map.entry(202, "hbm:weapon_mod_special_scope"),
                    Map.entry(203, "hbm:weapon_mod_special_saw/sawed_off"),
                    Map.entry(204, "hbm:weapon_mod_special_saw/no_shield"),
                    Map.entry(205, "hbm:weapon_mod_special_saw/no_stock"),
                    Map.entry(206, "hbm:weapon_mod_special_greasegun"),
                    Map.entry(207, "hbm:weapon_mod_special_slowdown"),
                    Map.entry(208, "hbm:weapon_mod_special_speedup/minigun"),
                    Map.entry(209, "hbm:weapon_mod_special_speedup/shredder"),
                    Map.entry(210, "hbm:weapon_mod_special_choke"),
                    Map.entry(211, "hbm:weapon_mod_special_furniture_green"),
                    Map.entry(212, "hbm:weapon_mod_special_furniture_black"),
                    Map.entry(213, "hbm:weapon_mod_special_bayonet/mas36"),
                    Map.entry(214, "hbm:weapon_mod_special_stack_mag"),
                    Map.entry(215, "hbm:weapon_mod_special_skin_saturnite"),
                    Map.entry(216, "hbm:weapon_mod_special_las_shotgun"),
                    Map.entry(217, "hbm:weapon_mod_special_las_capacitor"),
                    Map.entry(218, "hbm:weapon_mod_special_las_auto"),
                    Map.entry(219, "hbm:weapon_mod_special_bayonet/carbine"),
                    Map.entry(220, "hbm:weapon_mod_special_nickel"),
                    Map.entry(221, "hbm:weapon_mod_special_doubloons"),
                    Map.entry(222, "hbm:weapon_mod_special_drill_hss"),
                    Map.entry(223, "hbm:weapon_mod_special_drill_weaponsteel"),
                    Map.entry(224, "hbm:weapon_mod_special_drill_tcalloy"),
                    Map.entry(225, "hbm:weapon_mod_special_drill_saturnite"),
                    Map.entry(226, "hbm:weapon_mod_special_engine_diesel"),
                    Map.entry(227, "hbm:weapon_mod_special_engine_aviation"),
                    Map.entry(228, "hbm:weapon_mod_special_engine_electric"),
                    Map.entry(229, "hbm:weapon_mod_special_engine_turbo"),
                    Map.entry(230, "hbm:weapon_mod_special_magnet"),
                    Map.entry(231, "hbm:weapon_mod_special_sifter"),
                    Map.entry(232, "hbm:weapon_mod_special_canisters"),
                    Map.entry(300, "hbm:weapon_mod_caliber_p9/henry"),
                    Map.entry(301, "hbm:weapon_mod_caliber_p9/star_f"),
                    Map.entry(302, "hbm:weapon_mod_caliber_p9/star_f_akimbo"),
                    Map.entry(310, "hbm:weapon_mod_caliber_p45/henry"),
                    Map.entry(311, "hbm:weapon_mod_caliber_p45/greasegun"),
                    Map.entry(312, "hbm:weapon_mod_caliber_p45/uzi"),
                    Map.entry(313, "hbm:weapon_mod_caliber_p45/uzi_akimbo"),
                    Map.entry(314, "hbm:weapon_mod_caliber_p45/lag"),
                    Map.entry(320, "hbm:weapon_mod_caliber_p22/henry"),
                    Map.entry(321, "hbm:weapon_mod_caliber_p22/uzi"),
                    Map.entry(322, "hbm:weapon_mod_caliber_p22/uzi_akimbo"),
                    Map.entry(330, "hbm:weapon_mod_caliber_m357/henry"),
                    Map.entry(331, "hbm:weapon_mod_caliber_m357/lag"),
                    Map.entry(340, "hbm:weapon_mod_caliber_m44/lag"),
                    Map.entry(350, "hbm:weapon_mod_caliber_r556/henry"),
                    Map.entry(351, "hbm:weapon_mod_caliber_r556/carbine"),
                    Map.entry(352, "hbm:weapon_mod_caliber_r556/minigun"),
                    Map.entry(360, "hbm:weapon_mod_caliber_r762/henry"),
                    Map.entry(361, "hbm:weapon_mod_caliber_r762/g3"),
                    Map.entry(370, "hbm:weapon_mod_caliber_bmg50/henry"),
                    Map.entry(371, "hbm:weapon_mod_caliber_bmg50/minigun"));

    WeaponModKeyFix(Schema outputSchema) {
        super(
                outputSchema,
                _ -> true,
                STATE,
                WeaponModKeyFix::fixState,
                Traversal.NESTED_FIRST);
    }

    private static Dynamic<?> fixState(Dynamic<?> state) {
        var fields = state.getMapValues().result();
        if (fields.isEmpty()) return state;
        var fixed = state;
        for (var field : fields.get().entrySet()) {
            String key = field.getKey().asString().getOrThrow();
            if (!key.startsWith(MODS) || !(field.getValue().getValue() instanceof IntArrayTag))
                continue;
            var ids =
                    field.getValue()
                            .asIntStream()
                            .mapToObj(
                                    id ->
                                            state.createString(
                                                    KEYS.getOrDefault(
                                                            id, "hbm:legacy_weapon_mod/" + id)));
            fixed = fixed.set(key, state.createList(ids));
        }
        return fixed;
    }
}
