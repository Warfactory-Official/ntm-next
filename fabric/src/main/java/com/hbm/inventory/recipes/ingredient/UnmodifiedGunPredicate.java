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

package com.hbm.inventory.recipes.ingredient;

import com.hbm.items.ModDataComponents;
import com.hbm.items.weapon.sedna.GunStateData;
import com.hbm.registration.Registrar;
import com.hbm.registration.RegistryHandle;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.advancements.predicates.DataComponentMatchers;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.predicates.DataComponentPredicate;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import static com.hbm.items.weapon.sedna.GunBaseNTItem.*;
import static com.hbm.items.weapon.sedna.mags.MagazineSingleTypeBase.*;
import static com.hbm.items.weapon.sedna.mods.XWeaponModManager.KEY_MOD_LIST;

public record UnmodifiedGunPredicate() implements ComponentStatePredicate {
    public static final UnmodifiedGunPredicate INSTANCE = new UnmodifiedGunPredicate();
    public static final Codec<UnmodifiedGunPredicate> CODEC = MapCodec.unit(INSTANCE).codec();
    public static RegistryHandle<DataComponentPredicate.Type<UnmodifiedGunPredicate>> TYPE;

    public static void register() {
        TYPE = Registrar.registerDataComponentPredicate("unmodified_gun", CODEC);
    }

    public static DataComponentMatchers matchers() {
        return DataComponentMatchers.Builder.components().partial(TYPE.get(), INSTANCE).build();
    }

    @Override
    public boolean matches(DataComponentGetter components) {
        GunStateData state = components.get(ModDataComponents.GUN_STATE.get());
        if (state == null) return true;
        for (String key : state.tag().keySet()) {
            Tag value = state.tag().get(key);
            boolean allowed =
                    switch (key) {
                        case KEY_DRAWN,
                                KEY_AIMING,
                                KEY_EQUIPPED,
                                KEY_CANCELRELOAD,
                                KEY_LOCKEDON,
                                KEY_PRIMARY + "0",
                                KEY_SECONDARY + "0",
                                KEY_TERTIARY + "0",
                                KEY_RELOAD + "0" ->
                                value instanceof ByteTag flag
                                        && (flag.value() == 0 || flag.value() == 1);
                        case KEY_TIMER + "0",
                                KEY_ANIMTIMER + "0",
                                KEY_LASTANIM + "0",
                                KEY_LOCKONTARGET ->
                                value instanceof IntTag;
                        case KEY_MAG_TYPE + "0" ->
                                value instanceof IntTag || value instanceof StringTag;
                        case KEY_STATE + "0" ->
                                value instanceof ByteTag status
                                        && (status.value() == GunState.DRAWING.ordinal()
                                                || status.value() == GunState.IDLE.ordinal());
                        case KEY_MODE + "0",
                                KEY_MAG_COUNT + "0",
                                KEY_MAG_PREV + "0",
                                KEY_MAG_AFTER + "0" ->
                                value instanceof IntTag amount && amount.value() == 0;
                        case KEY_WEAR + "0" -> value instanceof FloatTag wear && wear.value() == 0F;
                        case KEY_MOD_LIST + "0" -> value instanceof ListTag mods && mods.isEmpty();
                        default -> false;
                    };
            if (!allowed) return false;
        }
        return true;
    }

    @Override
    public boolean claims(DataComponentType<?> component, ItemStack stack) {
        return component == ModDataComponents.GUN_STATE.get();
    }
}
