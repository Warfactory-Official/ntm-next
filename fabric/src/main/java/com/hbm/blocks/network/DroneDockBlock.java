// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.network;

import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.inventory.IGUIProvider;
import com.hbm.tileentity.network.BlockEntityDroneDock;
import com.hbm.tileentity.network.BlockEntityDroneProvider;
import com.hbm.tileentity.network.BlockEntityDroneRequester;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class DroneDockBlock extends Block implements ITickingBlock, ICapabilityBlock {

    public static final MapCodec<DroneDockBlock> CODEC =
            simpleCodec(props -> new DroneDockBlock(props, Kind.DOCK));

    private final Kind kind;

    public DroneDockBlock(Properties props, Kind kind) {
        super(props);
        this.kind = kind;
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    public Kind kind() {
        return kind;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return switch (kind) {
            case DOCK -> new BlockEntityDroneDock(pos, state);
            case PROVIDER -> new BlockEntityDroneProvider(pos, state);
            case REQUESTER -> new BlockEntityDroneRequester(pos, state);
        };
    }

    @Override
    public MachineCaps caps() {
        return switch (kind) {
            case DOCK -> MachineCaps.of(ModBlockEntities.DRONE_DOCK);
            case PROVIDER -> MachineCaps.of(ModBlockEntities.DRONE_PROVIDER).items();
            case REQUESTER -> MachineCaps.of(ModBlockEntities.DRONE_REQUESTER).items();
        };
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isSecondaryUseActive()) return InteractionResult.PASS;
        if (!(level.getBlockEntity(pos) instanceof MenuProvider provider))
            return InteractionResult.PASS;
        if (!level.isClientSide()) IGUIProvider.openBlockMenu(player, provider, pos);
        return InteractionResult.SUCCESS;
    }

    public enum Kind {
        DOCK,
        PROVIDER,
        REQUESTER
    }
}
