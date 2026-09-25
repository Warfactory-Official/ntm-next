// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.config;

public final class BalanceConfig {

    public static boolean enable528 = false;
    public static boolean enable528ReasimBoilers = false;
    public static boolean enable528PressurizedRecipes = false;
    public static boolean enable528MachineGravity = false;

    public static boolean enableLBSM = false;
    public static boolean enableLBSMFullSchrab = false;
    public static boolean enableLBSMShorterDecay = false;
    public static boolean enableLBSMSimpleArmorRecipes = false;
    public static boolean enableLBSMSimpleToolRecipes = false;
    public static boolean enableLBSMSimpleCrafting = false;
    public static boolean enableLBSMSimpleChemistry = false;
    public static boolean enableLBSMSimpleCentrifuge = false;
    public static boolean enableLBSMUnlockAnvil = false;
    public static boolean enableLBSMSimpleMedicineRecipes = false;
    public static boolean enableLBSMSafeCrates = false;

    public static boolean enableExpensiveMode = false;

    private BalanceConfig() {}

    public static void loadFrom(ConfigStore c) {
        enable528 = c.get(ConfigSchema.ENABLE_528);
        enable528ReasimBoilers = enable528 && c.get(ConfigSchema.ENABLE_528_FORCE_REASIM_BOILERS);
        enable528PressurizedRecipes =
                enable528 && c.get(ConfigSchema.ENABLE_528_PRESSURIZED_RECIPES);
        enable528MachineGravity = enable528 && c.get(ConfigSchema.ENABLE_528_MACHINE_GRAVITY);

        enableLBSM = !enable528 && c.get(ConfigSchema.ENABLE_LBSM);
        enableLBSMFullSchrab = c.get(ConfigSchema.ENABLE_LBSM_FULL_SCHRAB);
        enableLBSMShorterDecay = c.get(ConfigSchema.ENABLE_LBSM_SHORTER_DECAY);
        enableLBSMSimpleArmorRecipes = c.get(ConfigSchema.ENABLE_LBSM_SIMPLE_ARMOR_RECIPES);
        enableLBSMSimpleToolRecipes = c.get(ConfigSchema.ENABLE_LBSM_SIMPLE_TOOL_RECIPES);
        enableLBSMSimpleCrafting = c.get(ConfigSchema.ENABLE_LBSM_SIMPLE_CRAFTING);
        enableLBSMSimpleChemistry = c.get(ConfigSchema.ENABLE_LBSM_SIMPLE_CHEMISTRY);
        enableLBSMSimpleCentrifuge = c.get(ConfigSchema.ENABLE_LBSM_SIMPLE_CENTRIFUGE);
        enableLBSMUnlockAnvil = c.get(ConfigSchema.ENABLE_LBSM_UNLOCK_ANVIL);
        enableLBSMSimpleMedicineRecipes = c.get(ConfigSchema.ENABLE_LBSM_SIMPLE_MEDICINE);
        enableLBSMSafeCrates = c.get(ConfigSchema.ENABLE_LBSM_SAFE_CRATES);

        enableExpensiveMode = c.get(ConfigSchema.ENABLE_EXPENSIVE_MODE);
    }
}
