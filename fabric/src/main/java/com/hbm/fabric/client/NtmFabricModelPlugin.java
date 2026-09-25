// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.fabric.client;

import com.hbm.client.model.BedrockOreModel;
import com.hbm.client.model.BlockModel;
import com.hbm.client.model.ConveyorChuteModel;
import com.hbm.client.model.DetCordModel;
import com.hbm.client.model.FluoroModel;
import com.hbm.client.model.FoundryTankModel;
import com.hbm.client.model.HangingVineModel;
import com.hbm.client.model.PaintableDuctModel;
import com.hbm.client.model.PneumoTubeModel;
import com.hbm.client.model.ReedsModel;
import com.hbm.registration.Reg;
import java.util.HashMap;
import java.util.Map;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public final class NtmFabricModelPlugin implements ModelLoadingPlugin {

    @Override
    public void initialize(Context ctx) {

        PneumoTubeModel.FACTORY = FabricPneumoTubeModel::new;
        FoundryTankModel.FACTORY = FabricFoundryTankModel::new;
        ConveyorChuteModel.FACTORY = FabricConveyorChuteModel::new;
        DetCordModel.FACTORY = FabricDetCordModel::new;
        FluoroModel.FACTORY = FabricFluoroModel::new;
        HangingVineModel.FACTORY = FabricHangingVineModel::new;
        BedrockOreModel.FACTORY = FabricBedrockOreModel::new;
        ReedsModel.FACTORY = FabricReedsModel::new;
        PaintableDuctModel.Baked.FACTORY = FabricPaintableDuctModel::new;

        Map<BlockModel<?>, BlockModel.Prepared> hoisted = new HashMap<>();
        Reg.bakedBy()
                .forEach(
                        (blockId, supplier) -> {
                            BlockModel<?> model;
                            try {
                                model = supplier.get();
                            } catch (RuntimeException failure) {
                                throw new IllegalStateException(
                                        blockId + " could not build the block model that bakes it",
                                        failure);
                            }
                            if (model == null) {
                                throw new IllegalStateException(
                                        blockId
                                                + " states .bakedBy with a supplier that yields null, "
                                                + "so nothing would bake it and the block would draw as an invisible hole");
                            }
                            BlockModel.Prepared prepared =
                                    hoisted.computeIfAbsent(model, BlockModel::prepared);
                            Block block = BuiltInRegistries.BLOCK.getValue(blockId);
                            ctx.registerBlockStateResolver(
                                    block,
                                    resolverCtx -> {
                                        for (BlockState state :
                                                block.getStateDefinition().getPossibleStates()) {
                                            resolverCtx.setModel(
                                                    state, prepared.root(block, state));
                                        }
                                    });
                        });
    }
}
