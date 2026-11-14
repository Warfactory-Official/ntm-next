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

package com.hbm.blocks.machine;

import com.hbm.blockentity.machine.FoundryCastingBaseBlockEntity;
import com.hbm.blocks.TickingEntityBlock;
import com.hbm.interfaces.LookOverlay;
import com.hbm.interfaces.LookOverlayProvider;
import com.hbm.interfaces.ToolInteractionHandler;
import com.hbm.interfaces.ToolType;
import com.hbm.inventory.material.MaterialStack;
import com.hbm.items.machine.MoldItem;
import com.hbm.items.machine.ScrapsItem;
import com.hbm.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Prediction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public abstract class FoundryCastingBlock extends Block
        implements ToolInteractionHandler, LookOverlayProvider, TickingEntityBlock {

    protected FoundryCastingBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useItemOn(
            ItemStack held,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof FoundryCastingBaseBlockEntity casting)) {
            return InteractionResult.PASS;
        }

        if (!casting.getItem(1).isEmpty()) {
            if (!level.isClientSide()) returnSlot(casting, player, 1);
            return InteractionResult.SUCCESS;
        }

        if (casting.getItem(0).isEmpty()
                && held.getItem() instanceof MoldItem mold
                && mold.mold.size == casting.getMoldSize()) {
            if (!level.isClientSide()) {
                var installed = held.copyWithCount(1);
                casting.setItem(0, installed);
                held.shrink(1);
                level.playSound(
                        null, pos, ModSounds.UPGRADE_PLUG.get(), SoundSource.BLOCKS, 1F, 1F);
            }
            return InteractionResult.SUCCESS;
        }

        if (held.is(ItemTags.SHOVELS) && casting.amount > 0 && casting.type != null) {
            if (!level.isClientSide()) {
                var scraps = ScrapsItem.create(new MaterialStack(casting.type, casting.amount));
                casting.hbm$syncSet$FoundryBaseBlockEntity$amount(0);
                casting.hbm$syncSet$FoundryBaseBlockEntity$type(null);
                casting.setChanged();
                player.getInventory().placeItemBackInInventory(scraps, Prediction.SERVER_ONLY);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof FoundryCastingBaseBlockEntity casting)
                || casting.getItem(1).isEmpty()) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) returnSlot(casting, player, 1);
        return InteractionResult.SUCCESS;
    }

    private static void returnSlot(FoundryCastingBaseBlockEntity casting, Player player, int slot) {
        var returned = casting.removeItemNoUpdate(slot);
        casting.setChanged();
        player.getInventory().placeItemBackInInventory(returned, Prediction.SERVER_ONLY);
    }

    @Override
    public boolean onToolUse(
            Level level,
            @Nullable Player player,
            BlockPos pos,
            @Nullable Direction side,
            Vec3 hit,
            ToolType tool) {
        if (tool != ToolType.SCREWDRIVER || player == null || player.isSpectator()) return false;
        if (!(level.getBlockEntity(pos) instanceof FoundryCastingBaseBlockEntity casting))
            return false;
        if (casting.getItem(0).isEmpty() || casting.amount > 0) return false;

        if (!level.isClientSide()) returnSlot(casting, player, 0);
        return true;
    }

    @Override
    public void buildLookOverlay(Level level, BlockPos pos, LookOverlay info) {
        if (!(level.getBlockEntity(pos) instanceof FoundryCastingBaseBlockEntity casting)) return;

        info.title(Component.translatable(getDescriptionId()), 0xFF4000, 0x401000);
        var moldStack = casting.getItem(0);
        if (moldStack.isEmpty()) {
            info.row(Component.translatable("foundry.noCast"), 0xFF5555);
        } else if (moldStack.getItem() instanceof MoldItem mold) {
            info.row(mold.mold.getTitle(), 0x5555FF);
        }
        if (casting.amount > 0 && casting.type != null) {
            info.row(
                    Component.translatable(
                            "overlay.hbm.material_quantity",
                            casting.type.displayName(),
                            casting.amount,
                            casting.getCapacity()),
                    0xFFFF55);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!(level.getBlockEntity(pos) instanceof FoundryCastingBaseBlockEntity casting)) return;
        if (casting.amount <= 0 || casting.amount < casting.getCapacity()) return;

        double x = pos.getX() + 0.25 + random.nextDouble() * 0.5;
        double z = pos.getZ() + 0.25 + random.nextDouble() * 0.5;
        level.addParticle(ParticleTypes.SMOKE, x, pos.getY() + topY(), z, 0, 0, 0);
    }

    protected double topY() {
        return 0.5;
    }
}
