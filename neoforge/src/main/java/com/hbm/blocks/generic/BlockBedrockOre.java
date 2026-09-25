// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.generic;

import com.hbm.interfaces.ILookOverlay;
import com.hbm.inventory.fluid.FluidStackNTM;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.machine.IFluidContainerItem;
import com.hbm.items.machine.ItemDrillbit;
import com.hbm.tileentity.BlockEntityBedrockOre;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class BlockBedrockOre extends Block implements EntityBlock, ILookOverlay {

    public static final MapCodec<BlockBedrockOre> CODEC = simpleCodec(BlockBedrockOre::new);

    public BlockBedrockOre(Properties props) {
        super(props);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityBedrockOre(pos, state);
    }

    @Override
    protected InteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        if (stack.isEmpty() || !player.hasInfiniteMaterials()) {
            return super.useItemOn(stack, state, level, pos, player, hand, hit);
        }
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(pos) instanceof BlockEntityBedrockOre ore) {
            Fluid contained = containedFluid(stack);
            if (stack.getItem() instanceof ItemDrillbit drill) {
                ore.setData(ore.resource, ore.acid, ore.color, drill.type.tier, ore.shape);
            } else if (contained != null) {
                ore.setData(
                        ore.resource,
                        new FluidStackNTM(
                                contained, FluidTankNTM.containerContent(stack, contained)),
                        ore.color,
                        ore.tier,
                        ore.shape);
            } else if (stack.getItem() instanceof IFluidContainerItem item
                    && item.machineFillable()) {
                Fluid type = item.firstFluidType(stack);
                if (type != null) {
                    ore.setData(
                            ore.resource,
                            new FluidStackNTM(type, item.getFill(stack)),
                            ore.color,
                            ore.tier,
                            ore.shape);
                }
            } else {
                ore.setData(
                        stack.copy(), ore.acid, ore.color, ore.tier, level.getRandom().nextInt(10));
            }
        }
        return InteractionResult.SUCCESS;
    }

    private static @Nullable Fluid containedFluid(ItemStack stack) {
        for (Fluid fluid : NTMFluids.displayOrder()) {
            if (FluidTankNTM.containerContent(stack, fluid) > 0) return fluid;
        }
        return null;
    }

    @Override
    public void buildLookOverlay(Level level, BlockPos pos, LookInfo info) {
        if (!(level.getBlockEntity(pos) instanceof BlockEntityBedrockOre ore)) return;
        info.title(getName().getString(), 0xFFFF00, 0x404000);
        if (!ore.resource.isEmpty()) info.line(ore.resource.getHoverName().getString());
        info.line(Component.translatable("hbm.desc.bedrock_tier", ore.tier).getString());
        if (ore.acid != null) {
            info.line(
                    Component.translatable(
                                    "hbm.desc.bedrock_requires",
                                    ore.acid.amount(),
                                    NTMFluidProperties.clientName(ore.acid.type()))
                            .getString());
        }
    }
}
