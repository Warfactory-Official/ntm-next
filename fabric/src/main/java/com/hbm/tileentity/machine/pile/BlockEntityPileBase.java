// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine.pile;

import com.hbm.handler.neutron.NeutronNodeWorld.StreamWorld;
import com.hbm.handler.neutron.NeutronNodeWorld;
import com.hbm.handler.neutron.PileNeutronHandler.PileNeutronNode;
import com.hbm.handler.neutron.PileNeutronHandler.PileNeutronStream;
import com.hbm.handler.neutron.PileNeutronHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public abstract class BlockEntityPileBase extends BlockEntity {

    protected BlockEntityPileBase(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public abstract void tickServer();

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null) NeutronNodeWorld.removeNode(level, worldPosition);
    }

    protected void castRay(int flux) {

        if (flux == 0) {
            NeutronNodeWorld.removeNode(level, worldPosition);
            return;
        }

        StreamWorld streamWorld = NeutronNodeWorld.getOrAddWorld(level);
        PileNeutronNode node = (PileNeutronNode) streamWorld.getNode(worldPosition);

        if (node == null) {
            node = PileNeutronHandler.makeNode(streamWorld, this);
            streamWorld.addNode(node);
        }

        RandomSource rand = level.getRandom();
        Vec3 neutronVector =
                new Vec3(1, 0, 0)
                        .zRot((float) (Math.PI * 2D * rand.nextDouble()))
                        .yRot((float) (Math.PI * 2D * rand.nextDouble()))
                        .xRot((float) (Math.PI * 2D * rand.nextDouble()));

        new PileNeutronStream(node, neutronVector, flux);
    }
}
