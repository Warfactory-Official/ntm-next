// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine;

import com.hbm.blockentity.machine.BlockEntityMachineStrandCaster;
import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.interfaces.IToolable;
import com.hbm.inventory.material.Mats.MaterialStack;
import com.hbm.inventory.material.Mats;
import com.hbm.items.machine.ItemMold;
import com.hbm.items.machine.ItemScraps;
import com.hbm.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class MachineStrandCaster extends BlockMultiblockCore
        implements ITickingBlock, ICapabilityBlock, ILookOverlay, IToolable {
    public MachineStrandCaster(Properties props) {
        super(props);
    }

    private static @Nullable BlockEntityMachineStrandCaster caster(Level level, BlockPos core) {
        return level.getBlockEntity(core) instanceof BlockEntityMachineStrandCaster be ? be : null;
    }

    @Override
    protected InteractionResult useItemOnAtCore(
            ItemStack held,
            BlockState coreState,
            Level level,
            BlockPos core,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        BlockEntityMachineStrandCaster caster = caster(level, core);
        if (caster == null) return InteractionResult.TRY_WITH_EMPTY_HAND;

        if (held.getItem() instanceof ItemMold
                && caster.inventory.get(BlockEntityMachineStrandCaster.SLOT_MOLD).isEmpty()) {
            if (!level.isClientSide()) {
                caster.inventory.set(
                        BlockEntityMachineStrandCaster.SLOT_MOLD, held.copyWithCount(1));
                held.shrink(1);
                level.playSound(
                        null, core, ModSounds.UPGRADE_PLUG.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
                caster.setChanged();
            }
            return InteractionResult.SUCCESS;
        }

        if (held.is(ItemTags.SHOVELS)) {
            if (!level.isClientSide() && caster.amount > 0 && caster.type != null) {
                ItemStack scrap = ItemScraps.create(new MaterialStack(caster.type, caster.amount));
                player.getInventory().placeItemBackInInventory(scrap);
                caster.amount = 0;
                caster.type = null;
                caster.setChanged();
            }
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useAtCore(
            BlockState coreState, Level level, BlockPos core, Player player, BlockHitResult hit) {
        if (player.isSecondaryUseActive()) return InteractionResult.SUCCESS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(core) instanceof MenuProvider menu) {
            openCoreMenu(player, core, menu);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
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

        BlockEntityMachineStrandCaster caster = caster(level, pos);
        if (caster == null) return false;

        ItemStack mold = caster.inventory.get(BlockEntityMachineStrandCaster.SLOT_MOLD);
        if (mold.isEmpty()) return false;

        if (!level.isClientSide()) {
            player.getInventory().placeItemBackInInventory(mold.copy());
            caster.inventory.set(BlockEntityMachineStrandCaster.SLOT_MOLD, ItemStack.EMPTY);
            caster.setChanged();
        }
        return true;
    }

    @Override
    public void buildLookOverlay(Level level, BlockPos pos, LookInfo info) {
        BlockEntityMachineStrandCaster caster = caster(level, pos);
        if (caster == null) return;

        info.title(
                Component.empty().append(Component.translatable(getDescriptionId())),
                0xFF4000,
                0x401000);

        if (caster.inventory.get(BlockEntityMachineStrandCaster.SLOT_MOLD).getItem()
                instanceof ItemMold mold) {
            info.line(mold.mold.getTitle(), 0x5555FF);
        } else {
            info.line(Component.empty().append(Component.translatable("foundry.noCast")), 0xFF5555);
        }

        if (caster.type != null && caster.amount > 0) {
            info.line(
                    caster.type
                            .getLocalizedName()
                            .copy()
                            .append(": ")
                            .append(Mats.formatAmount(caster.amount, false)),
                    0xFFFF55);
        }
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityMachineStrandCaster(pos, state);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.STRAND_CASTER)
                .fluidIn()
                .fluidOut()
                .items()
                .itemsAtCells();
    }
}
