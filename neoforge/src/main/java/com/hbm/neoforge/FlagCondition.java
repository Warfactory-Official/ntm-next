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

package com.hbm.neoforge;

import com.hbm.inventory.recipes.loader.RecipeConditions;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.neoforged.neoforge.common.conditions.ICondition;

public record FlagCondition(String literal) implements ICondition {

    public static final MapCodec<FlagCondition> CODEC =
            RecordCodecBuilder.mapCodec(
                    instance ->
                            instance.group(
                                            RecipeConditions.LITERAL_CODEC
                                                    .fieldOf("name")
                                                    .forGetter(FlagCondition::literal))
                                    .apply(instance, FlagCondition::new));

    @Override
    public boolean test(IContext context) {
        return RecipeConditions.test(literal);
    }

    @Override
    public MapCodec<? extends ICondition> codec() {
        return CODEC;
    }
}
