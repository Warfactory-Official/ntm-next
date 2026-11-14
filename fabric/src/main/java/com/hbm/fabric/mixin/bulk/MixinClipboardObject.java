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

package com.hbm.fabric.mixin.bulk;

import com.hbm.fabric.integration.bulk.AxiomBulkData;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.moulberry.axiom.clipboard.ClipboardObject;
import com.moulberry.axiom.utils.IntMatrix;
import com.moulberry.axiom.world_modification.CompressedBlockEntity;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = ClipboardObject.class, remap = false)
public interface MixinClipboardObject {
    @WrapOperation(
            method = "applyMatrixToBlockEntities",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lit/unimi/dsi/fastutil/longs/Long2ObjectMap;put(JLjava/lang/Object;)Ljava/lang/Object;"))
    private static Object hbm$transform(
            Long2ObjectMap<Object> map,
            long key,
            Object value,
            Operation<Object> original,
            @Local(argsOnly = true) IntMatrix matrix) {
        return original.call(
                map, key, AxiomBulkData.transform((CompressedBlockEntity) value, matrix));
    }
}
