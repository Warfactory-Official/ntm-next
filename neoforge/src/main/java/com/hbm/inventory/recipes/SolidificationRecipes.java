/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: HbmMods (The Bobcat)
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) HbmMods (The Bobcat). All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only,
 * as published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this code. If not, see <https://www.gnu.org/licenses/>.
 *
 * Under GPLv3 §7(c) and §7(e), these additional terms apply to LGPL-covered
 * material authored by movblock:
 *
 * - the license grants no rights to the name "NTM NEXT";
 * - a modified version must not be named NTM NEXT or presented as the
 *   official release, and must be marked as different from it.
 *
 * Accurately identifying a work as derived from NTM NEXT does not violate
 * these additional terms, provided it is clearly distinguished from the
 * official release and does not imply endorsement. Ports are encouraged to
 * acknowledge NTM NEXT as their upstream project and provide a link to it.
 */

package com.hbm.inventory.recipes;

import com.hbm.inventory.recipes.loader.SimpleGenericRecipes;
import java.util.Collection;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

public class SolidificationRecipes extends SimpleGenericRecipes<SolidificationRecipe> {

    public static final SolidificationRecipes INSTANCE = new SolidificationRecipes();

    public static final int SF_OIL = 200, SF_CRACK = 200, SF_HEAVY = 150, SF_BITUMEN = 100;
    public static final int SF_COALOIL = 200, SF_CREOSOTE = 200, SF_WOOD = 1000, SF_LUBE = 100;

    @Override
    protected String registryName() {
        return "solidification";
    }

    @Override
    protected RecipeSerializer<SolidificationRecipe> serializer() {
        return SolidificationRecipe.SERIALIZER;
    }

    public @Nullable SolidificationRecipe getOutput(@Nullable Fluid type) {
        return byInputFluid().findLast(type);
    }

    public Collection<SolidificationRecipe> reachable() {
        return byInputFluid().reachable();
    }
}
