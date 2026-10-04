// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

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
        Identifier id =
                context.registryAccess()
                        .lookupOrThrow(Registries.STRUCTURE)
                        .getKey((Structure) (Object) this);
        if (id == null || !id.getNamespace().equals(NuclearTech.MOD_ID)) return;
        cir.setReturnValue(
                findGenerationPoint(context).filter(stub -> ColumnBiome.valid(stub, context)));
    }
}
