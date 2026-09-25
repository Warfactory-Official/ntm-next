// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine;

import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockHandlerXR;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.interfaces.IToolable;
import com.hbm.inventory.material.Mats.MaterialStack;
import com.hbm.inventory.material.Mats;
import com.hbm.items.machine.ItemMold;
import com.hbm.items.machine.ItemScraps;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.machine.BlockEntityMachineStrandCaster;
import com.hbm.util.I18nUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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

    private static final int[] PLACEMENT_DIMENSIONS = {
        0, 0, 6, 0, 1, 0, 0, 0, 0,
        2, 0, 1, 0, 1, 0, 0, 0, 0,
    };

    @Override
    protected int[] placementDimensionBoxes() {
        return PLACEMENT_DIMENSIONS;
    }

    private static final int[] DIMENSIONS = {0, 0, 6, 0, 1, 0};

    private static final int[] HEAD = {2, 0, 1, 0, 1, 0};

    private static final int PORT_DOMAINS = PASSIVE_FLUID_IN | PASSIVE_ITEMS;

    public MachineStrandCaster(Properties props) {
        super(props);
    }

    private static @Nullable BlockEntityMachineStrandCaster caster(Level level, BlockPos core) {
        return level.getBlockEntity(core) instanceof BlockEntityMachineStrandCaster be ? be : null;
    }

    @Override
    public int[] getDimensions() {
        return DIMENSIONS;
    }

    @Override
    public int getOffset() {
        return 0;
    }

    @Override
    public boolean checkRequirement(Level level, BlockPos placed, Direction dir, int o) {
        if (!super.checkRequirement(level, placed, dir, o)) return false;
        return MultiblockHandlerXR.checkSpace(level, placed, HEAD, placed, dir);
    }

    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        MultiblockHandlerXR.visitBox(core, HEAD, facing, visitor);

        Direction rot = facing.getClockWise();
        BlockPos head = core.relative(facing.getOpposite());
        BlockPos tail = core.relative(facing.getOpposite(), 5);

        visitor.cell(head.relative(rot), MASK_WEST);
        visitor.cell(head, MASK_EAST);
        visitor.cell(tail.relative(rot), MASK_WEST);
        visitor.cell(tail, MASK_EAST);
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        Direction rot = facing.getClockWise();
        BlockPos head = core.relative(facing.getOpposite());
        BlockPos tail = core.relative(facing.getOpposite(), 5);

        visitor.passiveCell(head.relative(rot), MASK_ALL, PORT_DOMAINS);
        visitor.passiveCell(head, MASK_ALL, PORT_DOMAINS);
        visitor.passiveCell(tail.relative(rot), MASK_ALL, PORT_DOMAINS);
        visitor.passiveCell(tail, MASK_ALL, PORT_DOMAINS);

        visitor.passiveCell(head.above(2).relative(rot), MASK_ALL, PORT_DOMAINS);
        visitor.passiveCell(head.above(2), MASK_ALL, PORT_DOMAINS);
        visitor.passiveCell(core.above(2).relative(rot), MASK_ALL, PORT_DOMAINS);
        visitor.passiveCell(core.above(2), MASK_ALL, PORT_DOMAINS);
    }

    @Override
    public int coreMask() {
        return MASK_NONE;
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

        info.title(I18nUtil.resolveKey(getDescriptionId()), 0xFF4000, 0x401000);

        if (caster.inventory.get(BlockEntityMachineStrandCaster.SLOT_MOLD).getItem()
                instanceof ItemMold mold) {
            info.line(mold.mold.getTitle(), 0x5555FF);
        } else {
            info.line(I18nUtil.resolveKey("foundry.noCast"), 0xFF5555);
        }

        if (caster.type != null && caster.amount > 0) {
            info.line(
                    caster.type.getLocalizedName() + ": " + Mats.formatAmount(caster.amount, false),
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
