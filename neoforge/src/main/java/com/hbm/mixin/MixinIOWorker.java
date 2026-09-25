// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.handler.radiation.RadiationSystemNT;
import com.hbm.interfaces.injected.IChunkExtension;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.storage.IOWorker;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(IOWorker.class)
public abstract class MixinIOWorker {

    @Shadow
    public abstract RegionStorageInfo storageInfo();

    @Inject(method = "lambda$loadAsync$0", at = @At("RETURN"))
    private void hbm$readRadiationSidecar(
            ChunkPos pos, CallbackInfoReturnable<Optional<CompoundTag>> cir) {
        RegionStorageInfo info = this.storageInfo();
        if (!"chunk".equals(info.type())) return;
        Optional<CompoundTag> result = cir.getReturnValue();
        if (result == null || result.isEmpty()) return;
        byte[] rad = RadiationSystemNT.readSidecar(info.dimension(), pos);
        if (rad != null) result.get().putByteArray(IChunkExtension.RADIATION_NBT_KEY, rad);
    }
}
