// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine;

import com.hbm.advancement.AwardRegions;
import com.hbm.advancement.DetonationTrigger;
import com.hbm.advancement.HbmCriteria;
import com.hbm.blocks.IPersistentInfoProvider;
import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.client.RepairLookOverlay;
import com.hbm.entity.projectile.EntityBombletZeta;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.interfaces.IRepairable;
import com.hbm.interfaces.IToolable;
import com.hbm.items.ModDataComponents;
import com.hbm.tileentity.machine.oil.BlockEntityMachineRefinery;
import com.hbm.tileentity.machine.oil.RefineryContents;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class MachineRefinery extends BlockMultiblockCore
        implements ITickingBlock,
                ILookOverlay,
                IToolable,
                ICapabilityBlock,
                IPersistentInfoProvider {

    public static final BooleanProperty EXPLODED = BooleanProperty.create("exploded");
    private static final int[] DIMENSIONS = {8, 0, 1, 1, 1, 1};

    public MachineRefinery(Properties props) {
        super(props);
        registerDefaultState(defaultBlockState().setValue(EXPLODED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(EXPLODED);
    }

    @Override
    protected boolean tilts() {
        return true;
    }

    @Override
    public int[] getDimensions() {
        return DIMENSIONS;
    }

    @Override
    public int getOffset() {
        return 1;
    }

    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        for (int dx = -1; dx <= 1; dx += 2) {
            for (int dz = -1; dz <= 1; dz += 2) {
                visitor.cell(core.offset(dx, 0, dz), MachineChemicalPlant.ringMask(dx, dz));
            }
        }
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        for (int dx = -1; dx <= 1; dx += 2) {
            for (int dz = -1; dz <= 1; dz += 2) {
                visitor.passiveCell(
                        core.offset(dx, 0, dz),
                        MASK_ALL,
                        PASSIVE_POWER_IN | PASSIVE_FLUID_IN | PASSIVE_ITEMS);
            }
        }
    }

    @Override
    protected InteractionResult useAtCore(
            BlockState coreState, Level level, BlockPos core, Player player, BlockHitResult hit) {
        if (player.isSecondaryUseActive()) return InteractionResult.SUCCESS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        if (coreState.getValue(EXPLODED)) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(core) instanceof MenuProvider menu) {
            openCoreMenu(player, core, menu);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void onExplosionHit(
            BlockState state,
            ServerLevel level,
            BlockPos core,
            Explosion explosion,
            BiConsumer<ItemStack, BlockPos> onHit) {
        if (level.getBlockEntity(core) instanceof BlockEntityMachineRefinery be) {
            if (be.lastExplosion == explosion) return;
            be.lastExplosion = explosion;
            if (!be.hasExploded()) {
                be.explode(level, core);

                if (explosion.getDirectSourceEntity() instanceof EntityBombletZeta) {
                    AwardRegions.within(
                            level,
                            core.getX() + 0.5,
                            core.getY() + 0.5,
                            core.getZ() + 0.5,
                            100,
                            p -> HbmCriteria.detonation(p, DetonationTrigger.Kind.REFINERY));
                }
                return;
            }
            level.removeBlock(core, false);
            return;
        }
        super.onExplosionHit(state, level, core, explosion, onHit);
    }

    @Override
    public boolean dropFromExplosion(Explosion explosion) {
        return false;
    }

    @Override
    public boolean onScrew(
            Level level,
            Player player,
            BlockPos core,
            @Nullable Direction side,
            Vec3 hit,
            ToolType tool) {
        if (tool != ToolType.TORCH) return false;
        return IRepairable.tryRepairMultiblock(level, core, player);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityMachineRefinery(pos, state);
    }

    @Override
    public void buildLookOverlay(Level level, BlockPos pos, LookInfo info) {
        RepairLookOverlay.build(level, pos, info);
    }

    @Override
    public MachineCaps caps() {

        return MachineCaps.of(ModBlockEntities.REFINERY)
                .powerIn()
                .fluidIn()
                .fluidOut()
                .items()
                .itemsAtCells()
                .fe()
                .fluidFaces(
                        BlockEntityMachineRefinery.class,
                        (be, face) -> face.side() != Direction.DOWN);
    }

    @Override
    public void appendPersistentInfo(ItemStack stack, Consumer<Component> adder) {
        RefineryContents contents = stack.get(ModDataComponents.REFINERY_CONTENTS.get());
        if (contents == null) return;
        for (int i = 0; i < contents.tanks().size(); i++) {
            int capacity =
                    i == 0
                            ? BlockEntityMachineRefinery.TANK_CAPACITY_INPUT
                            : BlockEntityMachineRefinery.TANK_CAPACITY_OUTPUT;
            adder.accept(IPersistentInfoProvider.tankLine(contents.tanks().get(i), capacity));
        }
    }
}
