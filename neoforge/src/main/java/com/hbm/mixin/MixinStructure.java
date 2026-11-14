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

package com.hbm.mixin;

import com.hbm.NuclearTech;
import com.hbm.world.structure.ColumnBiome;
import java.util.Optional;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Structure.class)
public abstract class MixinStructure {

    @Shadow
    protected abstract Optional<Structure.GenerationStub> findGenerationPoint(
            Structure.GenerationContext context);

    @Inject(method = "findValidGenerationPoint", at = @At("HEAD"), cancellable = true)
    private void hbm$columnBiome(
            Structure.GenerationContext context,
            CallbackInfoReturnable<Optional<Structure.GenerationStub>> cir) {
        if ((Object) this instanceof ColumnBiome.SelfTested) {
            cir.setReturnValue(findGenerationPoint(context));
            return;
        }
        Identifier id =
                context.registryAccess()
                        .lookupOrThrow(Registries.STRUCTURE)
                        .getKey((Structure) (Object) this);
        if (id == null || !id.getNamespace().equals(NuclearTech.MOD_ID)) return;
        cir.setReturnValue(
                findGenerationPoint(context).filter(stub -> ColumnBiome.valid(stub, context)));
    }
}
