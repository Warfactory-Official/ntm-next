// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.fabric.client;

import com.hbm.client.model.PaintableDuctModel;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.ARGB;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public final class FabricPaintableDuctModel extends PaintableDuctModel.Baked {

    public FabricPaintableDuctModel(
            ModelBaker baker, ResolvedModel carrier, PaintableDuctModel.Root root) {
        super(baker, carrier, root);
    }

    @Override
    public void emitQuads(
            QuadEmitter emitter,
            BlockAndTintGetter level,
            BlockPos pos,
            BlockState state,
            RandomSource random,
            Predicate<@Nullable Direction> cullTest) {
        visit(
                level,
                pos,
                random,
                new PartVisitor() {
                    @Override
                    public void own(BlockStateModelPart part) {
                        part.emitQuads(emitter, cullTest);
                    }

                    @Override
                    public void camo(BlockStateModelPart part, BlockState camo) {
                        emitter.pushTransform(
                                quad -> {
                                    int tintIndex = quad.tintIndex();
                                    if (tintIndex == -1) return true;
                                    int tint = camoTint(camo, level, pos, tintIndex);
                                    for (int i = 0; i < 4; i++)
                                        quad.color(i, ARGB.multiply(quad.color(i), tint));
                                    quad.tintIndex(-1);
                                    return true;
                                });
                        part.emitQuads(emitter, cullTest);
                        emitter.popTransform();
                    }
                });
    }

    @Override
    public @Nullable Object createGeometryKey(
            BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
        return geometryKey(level, pos, state);
    }
}
