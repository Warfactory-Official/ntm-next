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

package com.hbm.inventory.fluid.trait;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

public record PoisonTrait(boolean withering, int level) implements FluidTrait {

    @Override
    public void describeDetails(Consumer<Component> lines) {
        lines.accept(FluidTraitText.label("trait.toxicfumes", ChatFormatting.GREEN));
    }

    public void expose(LivingEntity living, double intensity) {
        var effect = withering ? MobEffects.WITHER : MobEffects.POISON;
        living.addEffect(new MobEffectInstance(effect, (int) (100 * intensity)));
    }
}
