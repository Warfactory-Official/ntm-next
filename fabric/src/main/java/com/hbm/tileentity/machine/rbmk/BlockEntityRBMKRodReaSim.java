// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine.rbmk;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.handler.neutron.NeutronNodeWorld.StreamWorld;
import com.hbm.handler.neutron.NeutronNodeWorld;
import com.hbm.handler.neutron.RBMKNeutronHandler.RBMKNeutronNode;
import com.hbm.handler.neutron.RBMKNeutronHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class BlockEntityRBMKRodReaSim extends BlockEntityRBMKRod {

    public BlockEntityRBMKRodReaSim(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RBMK_ROD_REASIM.get(), pos, state);
    }

    @Override
    public void spreadFlux(double flux, double ratio) {
        if (flux == 0) {
            NeutronNodeWorld.removeNode(level, worldPosition);
            return;
        }

        StreamWorld streamWorld = NeutronNodeWorld.getOrAddWorld(level);
        RBMKNeutronNode node = (RBMKNeutronNode) streamWorld.getNode(worldPosition);
        if (node == null) {
            node = RBMKNeutronHandler.makeNode(streamWorld, this);
            streamWorld.addNode(node);
        }

        Vec3 vec =
                new Vec3(1, 0, 0).yRot((float) Math.toRadians(level.getRandom().nextInt(4) * 9D));
        for (int i = 0; i < 8; i++) {
            new RBMKNeutronHandler.RBMKNeutronStream(node, vec, flux * 0.75, ratio);
            vec = vec.yRot((float) Math.toRadians(45D));
        }
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.rbmkReaSim");
    }

    @Override
    public RBMKColumnType getConsoleType() {
        return RBMKColumnType.FUEL_SIM;
    }
}
