// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.generic;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.handler.ArmorUtil;
import com.hbm.items.armor.ArmorSuitEffects;
import com.hbm.items.armor.ModArmorItem;
import com.hbm.sound.ModSounds;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class HEVBattery extends Block {

    private static final long CHARGE = 150_000L;

    public static final MapCodec<HEVBattery> CODEC = simpleCodec(HEVBattery::new);

    private static final VoxelShape SHAPE = Shapes.box(0.375, 0.0, 0.375, 0.625, 0.375, 0.625);

    public HEVBattery(Properties props) {
        super(props);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
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
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isSecondaryUseActive()) return InteractionResult.PASS;
        if (!(player.getItemBySlot(EquipmentSlot.HEAD).getItem() instanceof ModArmorItem helmet)
                || !helmet.isPowered()
                || !(player.getItemBySlot(EquipmentSlot.CHEST).getItem()
                        instanceof ModArmorItem plate)
                || !ArmorSuitEffects.hasFullSet(player, plate.suit(), false)) {
            return InteractionResult.SUCCESS;
        }
        if (!level.isClientSide()) {
            for (EquipmentSlot slot : ArmorUtil.ARMOR_SLOTS) {
                ItemStack worn = player.getItemBySlot(slot);
                if (worn.getItem() instanceof ModArmorItem armor) {
                    armor.setCharge(worn, armor.getCharge(worn) + CHARGE);
                } else if (worn.getItem() instanceof IBatteryItem battery) {
                    battery.chargeBattery(worn, CHARGE);
                }
            }
            level.playSound(
                    null, pos, ModSounds.ITEM_BATTERY.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
            level.removeBlock(pos, false);
        }
        return InteractionResult.SUCCESS;
    }
}
