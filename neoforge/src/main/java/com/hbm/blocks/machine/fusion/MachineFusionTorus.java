// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine.fusion;

import com.hbm.blocks.IBlockHighlight;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.tileentity.Tiltable;
import com.hbm.tileentity.machine.fusion.BlockEntityFusionTorus;
import com.hbm.tileentity.machine.fusion.FusionPorts;
import com.hbm.world.phys.SilhouetteMesh;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class MachineFusionTorus extends BlockFusionMachine
        implements ICapabilityBlock, IBlockHighlight {

    private static final int[] PLACEMENT_DIMENSIONS = {
        4, 0, 7, 7, 3, 3, 0, 0, 0,
        4, 0, 6, 6, 4, 4, 0, 0, 0,
        4, 0, 5, 5, 5, 5, 0, 0, 0,
        4, 0, 4, 4, 6, 6, 0, 0, 0,
        4, 0, 3, 3, 7, 7, 0, 0, 0,
    };

    @Override
    protected int[] placementDimensionBoxes() {
        return PLACEMENT_DIMENSIONS;
    }

    private static final double[] PLACEMENT_EXTRAS = {
        3.5, 1.5, 7.5, 7.5, 1, -1, 3.5, 1.5, -7.5, -7.5, 1, -1, 3.5, 1.5, 1, -1, 7.5, 7.5, 3.5, 1.5,
        1, -1, -7.5, -7.5
    };

    @Override
    protected double[] placementExtraBoxes() {
        return PLACEMENT_EXTRAS;
    }

    private static final SilhouetteMesh FUSION_TORUS_LEVEL =
            SilhouetteMesh.octagonalPrism(
                    new float[] {
                        8F, 0F, 3.6066017F,
                        3.6066017F, 0F, 8F,
                        -2.6066017F, 0F, 8F,
                        -7F, 0F, 3.6066017F,
                        -7F, 0F, -2.6066017F,
                        -2.6066017F, 0F, -7F,
                        3.6066017F, 0F, -7F,
                        8F, 0F, -2.6066017F,
                        8F, 5F, 3.6066017F,
                        3.6066017F, 5F, 8F,
                        -2.6066017F, 5F, 8F,
                        -7F, 5F, 3.6066017F,
                        -7F, 5F, -2.6066017F,
                        -2.6066017F, 5F, -7F,
                        3.6066017F, 5F, -7F,
                        8F, 5F, -2.6066017F
                    },
                    new float[] {
                        0F, -1F, 0F, 0F,
                        0F, 1F, 0F, -5F,
                        0.70710677F, 0F, 0.70710677F, -8.2071066F,
                        0F, 0F, 1F, -8F,
                        -0.70710677F, 0F, 0.70710677F, -7.5F,
                        -1F, 0F, 0F, -7F,
                        -0.70710677F, 0F, -0.70710677F, -6.7928934F,
                        0F, 0F, -1F, -7F,
                        0.70710677F, 0F, -0.70710677F, -7.5F,
                        1F, 0F, 0F, -8F
                    });
    private static final SilhouetteMesh FUSION_TORUS_TILTED =
            SilhouetteMesh.octagonalPrism(
                    new float[] {
                        8.1245966F, 0.34442213F, 2.941112F,
                        4.1915007F, -0.3490888F, 7.700702F,
                        -1.9040262F, -1.4238946F, 8.242218F,
                        -6.591307F, -2.2503889F, 4.2484484F,
                        -7.1245966F, -2.344422F, -1.941112F,
                        -3.191501F, -1.6509112F, -6.700702F,
                        2.904026F, -0.57610536F, -7.2422185F,
                        7.591307F, 0.2503888F, -3.2484481F,
                        7.2563558F, 5.2684608F, 2.941112F,
                        3.32326F, 4.5749497F, 7.700702F,
                        -2.772267F, 3.500144F, 8.242218F,
                        -7.459548F, 2.67365F, 4.2484484F,
                        -7.9928374F, 2.5796165F, -1.941112F,
                        -4.059742F, 3.2731276F, -6.700702F,
                        2.0357852F, 4.3479333F, -7.2422185F,
                        6.7230663F, 5.1744275F, -3.2484481F
                    },
                    new float[] {
                        0.17364818F, -0.98480773F, 0F, -1.0716318F,
                        -0.17364818F, 0.98480773F, 0F, -3.928368F,
                        0.7544065F, 0.13302222F, 0.64278764F, -8.065575F,
                        0.08583165F, 0.015134436F, 0.9961947F, -8.025879F,
                        -0.63302225F, -0.1116189F, 0.76604444F, -7.67813F,
                        -0.98106027F, -0.17298739F, 0.087155744F, -7.226035F,
                        -0.7544065F, -0.13302222F, -0.64278764F, -6.9344254F,
                        -0.08583165F, -0.015134436F, -0.9961947F, -6.974121F,
                        0.63302225F, 0.1116189F, -0.76604444F, -7.32187F,
                        0.98106027F, 0.17298739F, -0.087155744F, -7.773965F
                    });

    @Override
    public SilhouetteMesh visualOutline(BlockState state) {
        return state.getValue(Tiltable.TILTED) ? FUSION_TORUS_TILTED : FUSION_TORUS_LEVEL;
    }

    public static final int[][][] LAYOUT = {
        {
            {0, 0, 0, 0, 3, 3, 3, 3, 3, 3, 3, 0, 0, 0, 0},
            {0, 0, 0, 3, 1, 1, 1, 1, 1, 1, 1, 3, 0, 0, 0},
            {0, 0, 3, 1, 1, 1, 1, 1, 1, 1, 1, 1, 3, 0, 0},
            {0, 3, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 3, 0},
            {3, 1, 1, 1, 1, 3, 3, 3, 3, 3, 1, 1, 1, 1, 3},
            {3, 1, 1, 1, 3, 3, 3, 3, 3, 3, 3, 1, 1, 1, 3},
            {3, 1, 1, 1, 3, 3, 3, 3, 3, 3, 3, 1, 1, 1, 3},
            {3, 1, 1, 1, 3, 3, 3, 3, 3, 3, 3, 1, 1, 1, 3},
            {3, 1, 1, 1, 3, 3, 3, 3, 3, 3, 3, 1, 1, 1, 3},
            {3, 1, 1, 1, 3, 3, 3, 3, 3, 3, 3, 1, 1, 1, 3},
            {3, 1, 1, 1, 1, 3, 3, 3, 3, 3, 1, 1, 1, 1, 3},
            {0, 3, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 3, 0},
            {0, 0, 3, 1, 1, 1, 1, 1, 1, 1, 1, 1, 3, 0, 0},
            {0, 0, 0, 3, 1, 1, 1, 1, 1, 1, 1, 3, 0, 0, 0},
            {0, 0, 0, 0, 3, 3, 3, 3, 3, 3, 3, 0, 0, 0, 0},
        },
        {
            {0, 0, 0, 0, 1, 1, 3, 3, 3, 1, 1, 0, 0, 0, 0},
            {0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0},
            {0, 0, 1, 1, 2, 2, 2, 2, 2, 2, 2, 1, 1, 0, 0},
            {0, 1, 1, 2, 1, 1, 1, 1, 1, 1, 1, 2, 1, 1, 0},
            {1, 1, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 1, 1},
            {1, 1, 2, 1, 1, 3, 3, 3, 3, 3, 1, 1, 2, 1, 1},
            {3, 1, 2, 1, 1, 3, 3, 3, 3, 3, 1, 1, 2, 1, 3},
            {3, 1, 2, 1, 1, 3, 3, 3, 3, 3, 1, 1, 2, 1, 3},
            {3, 1, 2, 1, 1, 3, 3, 3, 3, 3, 1, 1, 2, 1, 3},
            {1, 1, 2, 1, 1, 3, 3, 3, 3, 3, 1, 1, 2, 1, 1},
            {1, 1, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 1, 1},
            {0, 1, 1, 2, 1, 1, 1, 1, 1, 1, 1, 2, 1, 1, 0},
            {0, 0, 1, 1, 2, 2, 2, 2, 2, 2, 2, 1, 1, 0, 0},
            {0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0},
            {0, 0, 0, 0, 1, 1, 3, 3, 3, 1, 1, 0, 0, 0, 0},
        },
        {
            {0, 0, 0, 0, 1, 1, 3, 3, 3, 1, 1, 0, 0, 0, 0},
            {0, 0, 0, 1, 2, 2, 2, 2, 2, 2, 2, 1, 0, 0, 0},
            {0, 0, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 1, 0, 0},
            {0, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 1, 0},
            {1, 2, 2, 2, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 1},
            {1, 2, 2, 2, 1, 3, 3, 3, 3, 3, 1, 2, 2, 2, 1},
            {3, 2, 2, 2, 1, 3, 3, 3, 3, 3, 1, 2, 2, 2, 3},
            {3, 2, 2, 2, 1, 3, 3, 3, 3, 3, 1, 2, 2, 2, 3},
            {3, 2, 2, 2, 1, 3, 3, 3, 3, 3, 1, 2, 2, 2, 3},
            {1, 2, 2, 2, 1, 3, 3, 3, 3, 3, 1, 2, 2, 2, 1},
            {1, 2, 2, 2, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 1},
            {0, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 1, 0},
            {0, 0, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 1, 0, 0},
            {0, 0, 0, 1, 2, 2, 2, 2, 2, 2, 2, 1, 0, 0, 0},
            {0, 0, 0, 0, 1, 1, 3, 3, 3, 1, 1, 0, 0, 0, 0},
        }
    };

    private static final int[] DIMENSIONS = {4, 0, 7, 7, 7, 7};

    public MachineFusionTorus(Properties props) {
        super(props);
    }

    @Override
    protected boolean tilts() {
        return true;
    }

    private static int[][] layerAt(int iy) {
        return LAYOUT[iy > 2 ? 4 - iy : iy];
    }

    @Override
    public int[] getDimensions() {
        return DIMENSIONS;
    }

    @Override
    public int getOffset() {
        return 7;
    }

    @Override
    public boolean checkRequirement(Level level, BlockPos placed, Direction dir, int o) {
        BlockPos core = placed.offset(dir.getStepX() * o, 0, dir.getStepZ() * o);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int iy = 0; iy < 5; iy++) {
            int[][] layer = layerAt(iy);
            for (int ix = 0; ix < layer.length; ix++) {
                for (int iz = 0; iz < layer.length; iz++) {
                    if (layer[ix][iz] <= 0) continue;
                    pos.set(
                            core.getX() + ix - layer.length / 2,
                            core.getY() + iy,
                            core.getZ() + iz - layer.length / 2);
                    if (pos.equals(placed)) continue;
                    if (!level.getBlockState(pos).canBeReplaced()) return false;
                }
            }
        }

        return true;
    }

    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int iy = 0; iy < 5; iy++) {
            int[][] layer = layerAt(iy);
            for (int ix = 0; ix < layer.length; ix++) {
                for (int iz = 0; iz < layer[0].length; iz++) {
                    if (layer[ix][iz] <= 0) continue;

                    int ex = ix - layer.length / 2;
                    int ez = iz - layer.length / 2;
                    if (iy == 0 && ex == 0 && ez == 0) continue;

                    visitor.cell(
                            pos.set(core.getX() + ex, core.getY() + iy, core.getZ() + ez),
                            MASK_NONE);
                }
            }
        }

        visitor.cell(core.offset(0, 4, 0), MASK_UP);

        for (int side = -1; side <= 1; side += 2) {
            for (int off = -2; off <= 2; off += 2) {
                visitor.cell(core.offset(6 * side, 0, off), MASK_DOWN);
                visitor.cell(core.offset(6 * side, 4, off), MASK_UP);
                visitor.cell(core.offset(off, 0, 6 * side), MASK_DOWN);
                visitor.cell(core.offset(off, 4, 6 * side), MASK_UP);
            }
        }
    }

    @Override
    public int coreMask() {
        return MASK_DOWN;
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        int domains = PASSIVE_POWER_IN | PASSIVE_FLUID_IN | PASSIVE_ITEMS;
        visitor.passiveCell(core.offset(0, 4, 0), MASK_ALL, domains);
        for (int side = -1; side <= 1; side += 2) {
            for (int off = -2; off <= 2; off += 2) {
                visitor.passiveCell(core.offset(6 * side, 0, off), MASK_ALL, domains);
                visitor.passiveCell(core.offset(6 * side, 4, off), MASK_ALL, domains);
                visitor.passiveCell(core.offset(off, 0, 6 * side), MASK_ALL, domains);
                visitor.passiveCell(core.offset(off, 4, 6 * side), MASK_ALL, domains);
            }
        }
    }

    @Override
    protected InteractionResult useAtCore(
            BlockState coreState, Level level, BlockPos core, Player player, BlockHitResult hit) {
        if (player.isSecondaryUseActive()) return InteractionResult.SUCCESS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(core) instanceof MenuProvider menu)
            openCoreMenu(player, core, menu);
        return InteractionResult.SUCCESS;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityFusionTorus(pos, state);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.blockKeyed().powerIn().fluidIn().fluidOut().itemsAtCells().fe();
    }

    @Override
    public List<FusionPorts.Port> links(BlockPos core, Direction facing) {
        return BlockEntityFusionTorus.links(core, facing);
    }
}
