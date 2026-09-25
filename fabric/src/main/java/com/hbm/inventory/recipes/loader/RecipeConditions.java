// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.recipes.loader;

import com.hbm.config.BalanceConfig;
import com.hbm.config.RadiationConfig;
import com.hbm.platform.Services;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;

public final class RecipeConditions {

    private static final Map<String, BooleanSupplier> FLAGS = new LinkedHashMap<>();

    static {
        FLAGS.put("enable_528", () -> BalanceConfig.enable528);
        FLAGS.put("lbsm", () -> BalanceConfig.enableLBSM);

        FLAGS.put(
                "lbsm_simple_armor",
                () -> BalanceConfig.enableLBSM && BalanceConfig.enableLBSMSimpleArmorRecipes);
        FLAGS.put(
                "lbsm_simple_tools",
                () -> BalanceConfig.enableLBSM && BalanceConfig.enableLBSMSimpleToolRecipes);
        FLAGS.put(
                "lbsm_simple_crafting",
                () -> BalanceConfig.enableLBSM && BalanceConfig.enableLBSMSimpleCrafting);
        FLAGS.put(
                "lbsm_simple_chemistry",
                () -> BalanceConfig.enableLBSM && BalanceConfig.enableLBSMSimpleChemistry);
        FLAGS.put(
                "lbsm_simple_centrifuge",
                () -> BalanceConfig.enableLBSM && BalanceConfig.enableLBSMSimpleCentrifuge);
        FLAGS.put(
                "lbsm_simple_medicine",
                () -> BalanceConfig.enableLBSM && BalanceConfig.enableLBSMSimpleMedicineRecipes);

        FLAGS.put("enable_528_pressurized", () -> BalanceConfig.enable528PressurizedRecipes);

        FLAGS.put("expensive_mode", () -> BalanceConfig.enableExpensiveMode);
    }

    private RecipeConditions() {}

    public static final Codec<String> LITERAL_CODEC =
            Codec.STRING.validate(
                    literal -> {
                        String flag = literal.startsWith("!") ? literal.substring(1) : literal;
                        if (FLAGS.containsKey(flag)) return DataResult.success(literal);
                        return DataResult.error(
                                () ->
                                        "Unknown recipe condition '"
                                                + literal
                                                + "'; known flags are "
                                                + FLAGS.keySet());
                    });

    public static Set<String> names() {
        return FLAGS.keySet();
    }

    public static boolean isKnown(String flag) {
        return FLAGS.containsKey(flag);
    }

    public static boolean test(String literal) {
        boolean negated = !literal.isEmpty() && literal.charAt(0) == '!';
        BooleanSupplier flag = FLAGS.get(negated ? literal.substring(1) : literal);

        if (flag == null) throw new IllegalStateException("Unknown recipe condition: " + literal);
        return flag.getAsBoolean() != negated;
    }

    public static boolean test(List<String> conditions) {
        for (String literal : conditions) {
            if (!test(literal)) return false;
        }
        return true;
    }
}
