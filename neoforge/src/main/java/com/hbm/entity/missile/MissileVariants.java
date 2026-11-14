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

package com.hbm.entity.missile;

import com.google.common.base.Suppliers;
import com.hbm.api.entity.RadarTargetType;
import com.hbm.entity.ModEntities;
import com.hbm.entity.missile.MissilePayload.Bunker;
import com.hbm.entity.missile.MissilePayload.Detonation;
import com.hbm.entity.missile.MissilePayload.Drill;
import com.hbm.entity.missile.MissilePayload.HighExplosive;
import com.hbm.entity.missile.MissilePayload.Ignition;
import com.hbm.entity.missile.MissilePayload.Nuclear;
import com.hbm.entity.missile.MissilePayload.Pulse;
import com.hbm.entity.missile.MissilePayload.Sequence;
import com.hbm.entity.missile.MissilePayload.Shuttle;
import com.hbm.entity.missile.MissilePayload.Smoke;
import com.hbm.entity.missile.MissilePayload.Submunitions;
import com.hbm.entity.missile.MissilePayload.Volcano;
import com.hbm.entity.missile.MissileVariant.Airframe;
import com.hbm.inventory.material.Material;
import com.hbm.inventory.material.MaterialShape;
import com.hbm.inventory.material.Materials;
import com.hbm.items.ModItems;
import com.hbm.items.machine.AshType;
import com.hbm.items.special.Autogen;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class MissileVariants {

    private static final Airframe TIER1 =
            new Airframe(
                    0.5F,
                    PlumeLayout.SINGLE,
                    debris(
                            () ->
                                    List.of(
                                            plate(Materials.TITANIUM, 4),
                                            new ItemStack(ModItems.THRUSTER_SMALL.get()))));
    private static final Airframe TIER2 =
            new Airframe(
                    1F,
                    PlumeLayout.SINGLE,
                    debris(
                            () ->
                                    List.of(
                                            plate(Materials.STEEL, 10),
                                            plate(Materials.TITANIUM, 6),
                                            new ItemStack(ModItems.THRUSTER_MEDIUM.get()))));
    private static final Airframe TIER3 =
            new Airframe(
                    1F,
                    PlumeLayout.CROSS,
                    debris(
                            () ->
                                    List.of(
                                            plate(Materials.STEEL, 16),
                                            plate(Materials.TITANIUM, 10),
                                            new ItemStack(ModItems.THRUSTER_LARGE.get()))));
    private static final Airframe TIER4 =
            new Airframe(
                    1F,
                    PlumeLayout.TRIO,
                    debris(
                            () ->
                                    List.of(
                                            plate(Materials.TITANIUM, 16),
                                            plate(Materials.STEEL, 20),
                                            plate(Materials.ALUMINUM, 12),
                                            new ItemStack(ModItems.THRUSTER_LARGE.get()))));
    private static final Airframe DOOMSDAY_FRAME = new Airframe(1F, PlumeLayout.TRIO, List::of);
    private static final Airframe STEALTH_FRAME =
            new Airframe(
                    1F,
                    PlumeLayout.SINGLE,
                    debris(
                            () ->
                                    List.of(
                                            new ItemStack(
                                                    Autogen.require(
                                                            MaterialShape.BOLT, Materials.STEEL),
                                                    4))));
    private static final Airframe SHUTTLE_FRAME =
            new Airframe(
                    1F,
                    PlumeLayout.SINGLE,
                    debris(
                            () ->
                                    List.of(
                                            plate(Materials.STEEL, 8),
                                            new ItemStack(ModItems.THRUSTER_MEDIUM.get(), 2),
                                            new ItemStack(ModItems.CANISTER.get()),
                                            new ItemStack(Items.GLASS_PANE, 2))));

    private static final Supplier<ItemStack> NO_RARE = () -> ItemStack.EMPTY;

    public static final MissileVariant GENERIC =
            new MissileVariant(
                    () -> ModEntities.MISSILE_GENERIC.get(),
                    "EntityMissileGeneric",
                    ModItems.MISSILE_GENERIC,
                    TIER1,
                    rare(ModItems.WARHEAD_GENERIC_SMALL),
                    new HighExplosive(15F, 24, false, Smoke.SMALL));
    public static final MissileVariant INCENDIARY =
            new MissileVariant(
                    () -> ModEntities.MISSILE_INCENDIARY.get(),
                    "EntityMissileIncendiary",
                    ModItems.MISSILE_INCENDIARY,
                    TIER1,
                    rare(ModItems.WARHEAD_INCENDIARY_SMALL),
                    new HighExplosive(15F, 24, true, Smoke.SMALL));
    public static final MissileVariant DECOY =
            new MissileVariant(
                            () -> ModEntities.MISSILE_DECOY.get(),
                            "EntityMissileDecoy",
                            ModItems.MISSILE_DECOY,
                            TIER1,
                            rare(() -> ModItems.ingot(Materials.STEEL)),
                            new Detonation(4F, false))
                    .radar("radar.target.tier4", RadarTargetType.TIER4);
    public static final MissileVariant BUSTER =
            new MissileVariant(
                    () -> ModEntities.MISSILE_BUSTER.get(),
                    "EntityMissileBunkerBuster",
                    ModItems.MISSILE_BUSTER,
                    TIER1,
                    rare(ModItems.WARHEAD_BUSTER_SMALL),
                    new Bunker(15, 5F, 5));
    public static final MissileVariant CLUSTER =
            new MissileVariant(
                            () -> ModEntities.MISSILE_CLUSTER.get(),
                            "EntityMissileCluster",
                            ModItems.MISSILE_CLUSTER,
                            TIER1,
                            rare(ModItems.WARHEAD_CLUSTER_SMALL),
                            new Sequence(new Detonation(5F, true), new Submunitions(25)))
                    .airburst();

    public static final MissileVariant STRONG =
            new MissileVariant(
                    () -> ModEntities.MISSILE_STRONG.get(),
                    "EntityMissileStrong",
                    ModItems.MISSILE_STRONG,
                    TIER2,
                    rare(ModItems.WARHEAD_GENERIC_MEDIUM),
                    new HighExplosive(30F, 32, false, Smoke.STANDARD));
    public static final MissileVariant INCENDIARY_STRONG =
            new MissileVariant(
                    () -> ModEntities.MISSILE_INCENDIARY_STRONG.get(),
                    "EntityMissileIncendiaryStrong",
                    ModItems.MISSILE_INCENDIARY_STRONG,
                    TIER2,
                    rare(ModItems.WARHEAD_INCENDIARY_MEDIUM),
                    new Sequence(
                            new HighExplosive(30F, 32, true, Smoke.STANDARD),
                            new Ignition(false, 25, true)));
    public static final MissileVariant BUSTER_STRONG =
            new MissileVariant(
                    () -> ModEntities.MISSILE_BUSTER_STRONG.get(),
                    "EntityMissileBusterStrong",
                    ModItems.MISSILE_BUSTER_STRONG,
                    TIER2,
                    rare(ModItems.WARHEAD_BUSTER_MEDIUM),
                    new Bunker(20, 7.5F, 8));
    public static final MissileVariant CLUSTER_STRONG =
            new MissileVariant(
                            () -> ModEntities.MISSILE_CLUSTER_STRONG.get(),
                            "EntityMissileClusterStrong",
                            ModItems.MISSILE_CLUSTER_STRONG,
                            TIER2,
                            rare(ModItems.WARHEAD_CLUSTER_MEDIUM),
                            new Sequence(new Detonation(15F, true), new Submunitions(50)))
                    .airburst();
    public static final MissileVariant EMP_STRONG =
            new MissileVariant(
                    () -> ModEntities.MISSILE_EMP_STRONG.get(),
                    "EntityMissileEMPStrong",
                    ModItems.MISSILE_EMP_STRONG,
                    TIER2,
                    rare(ModItems.WARHEAD_GENERIC_MEDIUM),
                    new Pulse());

    public static final MissileVariant BURST =
            new MissileVariant(
                    () -> ModEntities.MISSILE_BURST.get(),
                    "EntityMissileBurst",
                    ModItems.MISSILE_BURST,
                    TIER3,
                    rare(ModItems.WARHEAD_GENERIC_LARGE),
                    new HighExplosive(50F, 48, false, Smoke.LARGE));
    public static final MissileVariant INFERNO =
            new MissileVariant(
                    () -> ModEntities.MISSILE_INFERNO.get(),
                    "EntityMissileInferno",
                    ModItems.MISSILE_INFERNO,
                    TIER3,
                    rare(ModItems.WARHEAD_INCENDIARY_LARGE),
                    new Sequence(
                            new HighExplosive(50F, 48, true, Smoke.LARGE),
                            new Ignition(true, 10, false),
                            new Ignition(false, 25, false)));
    public static final MissileVariant RAIN =
            new MissileVariant(
                            () -> ModEntities.MISSILE_RAIN.get(),
                            "EntityMissileRain",
                            ModItems.MISSILE_RAIN,
                            TIER3,
                            rare(ModItems.WARHEAD_CLUSTER_LARGE),
                            new Sequence(new Detonation(25F, true), new Submunitions(100)))
                    .airburst();
    public static final MissileVariant DRILL =
            new MissileVariant(
                    () -> ModEntities.MISSILE_DRILL.get(),
                    "EntityMissileDrill",
                    ModItems.MISSILE_DRILL,
                    TIER3,
                    rare(ModItems.WARHEAD_BUSTER_LARGE),
                    new Drill(30));

    public static final MissileVariant NUCLEAR =
            new MissileVariant(
                    () -> ModEntities.MISSILE_NUCLEAR.get(),
                    "EntityMissileNuclear",
                    ModItems.MISSILE_NUCLEAR,
                    TIER4,
                    rare(ModItems.WARHEAD_NUCLEAR),
                    new Nuclear(1, 0));
    public static final MissileVariant MIRV =
            new MissileVariant(
                    () -> ModEntities.MISSILE_NUCLEAR_CLUSTER.get(),
                    "EntityMissileMirv",
                    ModItems.MISSILE_NUCLEAR_CLUSTER,
                    TIER4,
                    rare(ModItems.WARHEAD_MIRV),
                    new Nuclear(2, 0));
    public static final MissileVariant VOLCANO =
            new MissileVariant(
                    () -> ModEntities.MISSILE_VOLCANO.get(),
                    "EntityMissileVolcano",
                    ModItems.MISSILE_VOLCANO,
                    TIER4,
                    rare(ModItems.WARHEAD_VOLCANO),
                    new Volcano());
    public static final MissileVariant DOOMSDAY =
            new MissileVariant(
                            () -> ModEntities.MISSILE_DOOMSDAY.get(),
                            "EntityMissileDoomsday",
                            ModItems.MISSILE_DOOMSDAY,
                            DOOMSDAY_FRAME,
                            NO_RARE,
                            new Nuclear(2, 100))
                    .radar("radar.target.doomsday", null);
    public static final MissileVariant DOOMSDAY_RUSTED =
            new MissileVariant(
                            () -> ModEntities.MISSILE_DOOMSDAY_RUSTED.get(),
                            "EntityMissileDoomsdayRusted",
                            ModItems.MISSILE_DOOMSDAY_RUSTED,
                            DOOMSDAY_FRAME,
                            NO_RARE,
                            new Nuclear(1, 100))
                    .radar("radar.target.doomsday", null);

    public static final MissileVariant STEALTH =
            new MissileVariant(
                            () -> ModEntities.MISSILE_STEALTH.get(),
                            "EntityMissileStealth",
                            ModItems.MISSILE_STEALTH,
                            STEALTH_FRAME,
                            rare(() -> ModItems.POWDER_ASH.get(AshType.MISC)),
                            new HighExplosive(20F, 24, false, Smoke.STANDARD))
                    .stealthy();
    public static final MissileVariant SHUTTLE =
            new MissileVariant(
                            () -> ModEntities.MISSILE_SHUTTLE.get(),
                            "EntityMissileShuttle",
                            ModItems.MISSILE_SHUTTLE,
                            SHUTTLE_FRAME,
                            rare(ModItems.MISSILE_GENERIC),
                            new Shuttle())
                    .radar("radar.target.shuttle", null);

    private MissileVariants() {}

    private static Supplier<List<ItemStack>> debris(Supplier<List<ItemStack>> parts) {
        return Suppliers.memoize(parts::get);
    }

    private static Supplier<ItemStack> rare(Supplier<? extends Item> item) {
        return Suppliers.memoize(() -> new ItemStack(item.get()));
    }

    private static ItemStack plate(Material material, int count) {
        return new ItemStack(ModItems.plate(material), count);
    }
}
