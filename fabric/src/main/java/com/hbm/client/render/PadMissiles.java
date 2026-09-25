// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.items.ModItems;
import com.hbm.items.weapon.ItemMissile;
import com.hbm.main.ResourceManager;
import com.hbm.registration.RegistryHandle;
import com.hbm.render.loader.HFRWavefrontObject;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import org.jspecify.annotations.Nullable;

public final class PadMissiles {

    private static @Nullable Map<Item, Missile> table;

    private PadMissiles() {}

    public static Map<Item, Missile> table() {
        Map<Item, Missile> built = table;
        if (built == null) {
            built = new IdentityHashMap<>();
            fill(built);
            table = built;
        }
        return built;
    }

    private static void fill(Map<Item, Missile> out) {
        put(
                out,
                ModItems.MISSILE_TEST,
                ResourceManager.missileMicro,
                ResourceManager.missileMicroTest_tex,
                1F);
        put(
                out,
                ModItems.MISSILE_TAINT,
                ResourceManager.missileMicro,
                ResourceManager.missileMicroTaint_tex,
                1F);
        put(
                out,
                ModItems.MISSILE_MICRO,
                ResourceManager.missileMicro,
                ResourceManager.missileMicro_tex,
                1F);
        put(
                out,
                ModItems.MISSILE_BHOLE,
                ResourceManager.missileMicro,
                ResourceManager.missileMicroBHole_tex,
                1F);
        put(
                out,
                ModItems.MISSILE_SCHRABIDIUM,
                ResourceManager.missileMicro,
                ResourceManager.missileMicroSchrab_tex,
                1F);
        put(
                out,
                ModItems.MISSILE_EMP,
                ResourceManager.missileMicro,
                ResourceManager.missileMicroEMP_tex,
                1F);

        put(
                out,
                ModItems.MISSILE_STEALTH,
                ResourceManager.missileStealth,
                ResourceManager.missileStealth_tex,
                1F);

        put(
                out,
                ModItems.MISSILE_GENERIC,
                ResourceManager.missileV2,
                ResourceManager.missileV2_HE_tex,
                1F);
        put(
                out,
                ModItems.MISSILE_INCENDIARY,
                ResourceManager.missileV2,
                ResourceManager.missileV2_IN_tex,
                1F);
        put(
                out,
                ModItems.MISSILE_CLUSTER,
                ResourceManager.missileV2,
                ResourceManager.missileV2_CL_tex,
                1F);
        put(
                out,
                ModItems.MISSILE_BUSTER,
                ResourceManager.missileV2,
                ResourceManager.missileV2_BU_tex,
                1F);
        put(
                out,
                ModItems.MISSILE_DECOY,
                ResourceManager.missileV2,
                ResourceManager.missileV2_decoy_tex,
                1F);
        put(
                out,
                ModItems.MISSILE_ANTI_BALLISTIC,
                ResourceManager.missileABM,
                ResourceManager.missileAA_tex,
                1F);

        put(
                out,
                ModItems.MISSILE_STRONG,
                ResourceManager.missileStrong,
                ResourceManager.missileStrong_HE_tex,
                1.5F);
        put(
                out,
                ModItems.MISSILE_INCENDIARY_STRONG,
                ResourceManager.missileStrong,
                ResourceManager.missileStrong_IN_tex,
                1.5F);
        put(
                out,
                ModItems.MISSILE_CLUSTER_STRONG,
                ResourceManager.missileStrong,
                ResourceManager.missileStrong_CL_tex,
                1.5F);
        put(
                out,
                ModItems.MISSILE_BUSTER_STRONG,
                ResourceManager.missileStrong,
                ResourceManager.missileStrong_BU_tex,
                1.5F);
        put(
                out,
                ModItems.MISSILE_EMP_STRONG,
                ResourceManager.missileStrong,
                ResourceManager.missileStrong_EMP_tex,
                1.5F);

        put(
                out,
                ModItems.MISSILE_BURST,
                ResourceManager.missileHuge,
                ResourceManager.missileHuge_HE_tex,
                1F);
        put(
                out,
                ModItems.MISSILE_INFERNO,
                ResourceManager.missileHuge,
                ResourceManager.missileHuge_IN_tex,
                1F);
        put(
                out,
                ModItems.MISSILE_RAIN,
                ResourceManager.missileHuge,
                ResourceManager.missileHuge_CL_tex,
                1F);
        put(
                out,
                ModItems.MISSILE_DRILL,
                ResourceManager.missileHuge,
                ResourceManager.missileHuge_BU_tex,
                1F);

        put(
                out,
                ModItems.MISSILE_NUCLEAR,
                ResourceManager.missileNuclear,
                ResourceManager.missileNuclear_tex,
                1F);
        put(
                out,
                ModItems.MISSILE_NUCLEAR_CLUSTER,
                ResourceManager.missileNuclear,
                ResourceManager.missileMIRV_tex,
                1F);
        put(
                out,
                ModItems.MISSILE_VOLCANO,
                ResourceManager.missileNuclear,
                ResourceManager.missileVolcano_tex,
                1F);
        put(
                out,
                ModItems.MISSILE_DOOMSDAY,
                ResourceManager.missileNuclear,
                ResourceManager.missileDoomsday_tex,
                1F);
        put(
                out,
                ModItems.MISSILE_DOOMSDAY_RUSTED,
                ResourceManager.missileNuclear,
                ResourceManager.missileDoomsdayRusted_tex,
                1F);

        put(
                out,
                ModItems.MISSILE_SHUTTLE,
                ResourceManager.missileShuttle,
                ResourceManager.missileShuttle_tex,
                1F);
    }

    private static void put(
            Map<Item, Missile> out,
            RegistryHandle<ItemMissile> item,
            HFRWavefrontObject mesh,
            Identifier texture,
            float scale) {
        out.put(item.get(), new Missile(mesh, RenderTypes.entityCutoutCull(texture), scale));
    }

    public record Missile(HFRWavefrontObject mesh, RenderType type, float scale) {}
}
