// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine;

import com.hbm.blocks.ITickingBlock;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.interfaces.IToolable;
import com.hbm.inventory.material.Mats.MaterialStack;
import com.hbm.items.machine.ItemMold;
import com.hbm.items.machine.ItemScraps;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.machine.BlockEntityFoundryCastingBase;
import com.hbm.util.I18nUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
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

public abstract class FoundryCastingBase extends Block
        implements IToolable, ILookOverlay, ITickingBlock {

    protected FoundryCastingBase(Properties props) {
        super(props);
    }

    private static @Nullable BlockEntityFoundryCastingBase cast(Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof BlockEntityFoundryCastingBase be ? be : null;
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
        BlockEntityFoundryCastingBase cast = cast(level, pos);
        if (cast == null) return InteractionResult.PASS;

        if (!cast.inventory.get(1).isEmpty()) {
            return takeOutput(level, player, cast);
        }

        if (held.getItem() instanceof ItemMold moldItem && cast.inventory.get(0).isEmpty()) {
            if (moldItem.mold.size == cast.getMoldSize()) {
                if (!level.isClientSide()) {
                    cast.inventory.set(0, held.copyWithCount(1));
                    held.shrink(1);
                    level.playSound(
                            null,
                            pos,
                            ModSounds.UPGRADE_PLUG.get(),
                            SoundSource.PLAYERS,
                            1.0F,
                            1.0F);
                    cast.setChanged();
                }
                return InteractionResult.SUCCESS;
            }
        }

        if (held.is(ItemTags.SHOVELS) && cast.amount > 0 && cast.type != null) {
            if (!level.isClientSide()) {
                ItemStack scrap = ItemScraps.create(new MaterialStack(cast.type, cast.amount));
                player.getInventory().placeItemBackInInventory(scrap);
                cast.amount = 0;
                cast.type = null;
                cast.setChanged();
            }
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        BlockEntityFoundryCastingBase cast = cast(level, pos);
        if (cast == null || cast.inventory.get(1).isEmpty()) return InteractionResult.PASS;
        return takeOutput(level, player, cast);
    }

    private static InteractionResult takeOutput(
            Level level, Player player, BlockEntityFoundryCastingBase cast) {
        if (!level.isClientSide()) {
            ItemStack out = cast.inventory.get(1).copy();
            player.getInventory().placeItemBackInInventory(out);
            cast.inventory.set(1, ItemStack.EMPTY);
            cast.setChanged();
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean onScrew(
            Level level,
            Player player,
            BlockPos pos,
            @Nullable Direction side,
            Vec3 hit,
            ToolType tool) {
        if (tool != ToolType.SCREWDRIVER) return false;

        BlockEntityFoundryCastingBase cast = cast(level, pos);
        if (cast == null) return false;
        if (cast.inventory.get(0).isEmpty()) return false;
        if (cast.amount > 0) return false;

        if (!level.isClientSide()) {
            ItemStack mold = cast.inventory.get(0).copy();
            player.getInventory().placeItemBackInInventory(mold);
            cast.inventory.set(0, ItemStack.EMPTY);
            cast.setChanged();
        }
        return true;
    }

    @Override
    public void buildLookOverlay(Level level, BlockPos pos, ILookOverlay.LookInfo info) {
        BlockEntityFoundryCastingBase cast = cast(level, pos);
        if (cast == null) return;

        info.title(I18nUtil.resolveKey(getDescriptionId()), 0xFF4000, 0x401000);

        if (cast.inventory.get(0).getItem() instanceof ItemMold moldItem) {
            info.line(moldItem.mold.getTitle(), 0x5555FF);
        } else {
            info.line(I18nUtil.resolveKey("foundry.noCast"), 0xFF5555);
        }

        if (cast.type != null && cast.amount > 0) {
            info.line(
                    cast.type.getLocalizedName() + ": " + cast.amount + " / " + cast.getCapacity(),
                    0xFFFF55);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        BlockEntityFoundryCastingBase cast = cast(level, pos);
        if (cast != null && cast.amount > 0 && cast.amount >= cast.getCapacity()) {
            level.addParticle(
                    ParticleTypes.SMOKE,
                    pos.getX() + 0.25 + random.nextDouble() * 0.5,
                    pos.getY() + topY(),
                    pos.getZ() + 0.25 + random.nextDouble() * 0.5,
                    0.0,
                    0.0,
                    0.0);
        }
    }

    protected double topY() {
        return 0.5D;
    }
}
