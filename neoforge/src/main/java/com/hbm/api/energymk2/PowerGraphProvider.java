// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.api.energymk2;

import com.hbm.uninos.graph.IGraphProvider;
import com.hbm.uninos.graph.NodeNetwork;
import com.mojang.serialization.Codec;
import net.minecraft.world.level.block.state.BlockState;

public final class PowerGraphProvider implements IGraphProvider<CableData> {

    public static final PowerGraphProvider INSTANCE = new PowerGraphProvider();

    private PowerGraphProvider() {}

    @Override
    public CableData createData(BlockState state) {
        return CableData.INSTANCE;
    }

    @Override
    public boolean dataCompatible(CableData a, CableData b) {
        return true;
    }

    @Override
    public Codec<CableData> dataCodec() {
        return CableData.CODEC;
    }

    @Override
    public void onNetsMerged(NodeNetwork<CableData> keep, NodeNetwork<CableData> drain) {
        PowerNetwork.mergeBridges(keep, drain);
    }
}
