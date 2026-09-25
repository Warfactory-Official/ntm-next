// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.multiblock;

import com.hbm.capability.NtmCapabilities.CapRole;
import java.util.Arrays;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.jspecify.annotations.Nullable;

public final class MultiblockMaskTable {

    public static final int NOT_A_CELL = -1;

    public final int declaredCaps;

    private final byte[] masks;

    private final byte[][] passive;

    private final byte[] @Nullable [] byRole;
    private final int minX, minY, minZ;
    private final int sizeX, sizeY, sizeZ;

    private MultiblockMaskTable(
            byte[] masks,
            byte[][] passive,
            byte[][] byRole,
            int minX,
            int minY,
            int minZ,
            int sizeX,
            int sizeY,
            int sizeZ,
            int declaredCaps) {
        this.masks = masks;
        this.passive = passive;
        this.byRole = byRole;
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
        this.declaredCaps = declaredCaps;
    }

    public record Geometry(
            Class<?> maskAtOwner,
            CellSource cells,
            PassiveCellSource passiveCells,
            int coreMask,
            int passiveCoreMask,
            int passiveCoreDomains) {}

    @FunctionalInterface
    public interface CellSource {
        void visit(BlockPos core, Direction facing, BlockMultiblockCore.CellVisitor visitor);
    }

    @FunctionalInterface
    public interface PassiveCellSource {
        void visit(BlockPos core, Direction facing, BlockMultiblockCore.PassiveCellVisitor visitor);
    }

    public static MultiblockMaskTable bake(BlockMultiblockCore block, int declaredCaps) {
        return bake(
                new Geometry(
                        block.getClass(),
                        block::visitCells,
                        block::visitPassiveCells,
                        block.coreMask(),
                        block.passiveCoreMask(),
                        block.passiveCoreDomains(declaredCaps)),
                declaredCaps);
    }

    public static MultiblockMaskTable bake(Geometry geometry, int declaredCaps) {
        Bounds bounds = new Bounds();
        geometry.cells().visit(BlockPos.ZERO, Direction.SOUTH, bounds);
        bounds.include(0, 0, 0);

        int sizeX = bounds.maxX - bounds.minX + 1;
        int sizeY = bounds.maxY - bounds.minY + 1;
        int sizeZ = bounds.maxZ - bounds.minZ + 1;
        byte[] masks = new byte[sizeX * sizeY * sizeZ];
        Arrays.fill(masks, (byte) NOT_A_CELL);
        byte[][] passive = new byte[BlockMultiblockCore.PASSIVE_DOMAINS][];
        for (int d = 0; d < passive.length; d++) {
            passive[d] = new byte[masks.length];
            Arrays.fill(passive[d], (byte) NOT_A_CELL);
        }

        byte[][] byRole = new byte[CapRole.COUNT][];
        for (int r = 0; r < byRole.length; r++) {
            byRole[r] = new byte[masks.length];
            Arrays.fill(byRole[r], (byte) NOT_A_CELL);
        }

        MultiblockMaskTable table =
                new MultiblockMaskTable(
                        masks,
                        passive,
                        byRole,
                        bounds.minX,
                        bounds.minY,
                        bounds.minZ,
                        sizeX,
                        sizeY,
                        sizeZ,
                        declaredCaps);
        geometry.cells()
                .visit(
                        BlockPos.ZERO,
                        Direction.SOUTH,
                        new BlockMultiblockCore.CellVisitor() {
                            @Override
                            public void cell(BlockPos pos, int mask) {
                                table.put(
                                        pos.getX(),
                                        pos.getY(),
                                        pos.getZ(),
                                        mask,
                                        BlockMultiblockCore.ROLE_ALL);
                            }

                            @Override
                            public void cell(BlockPos pos, int mask, int roles) {
                                table.put(pos.getX(), pos.getY(), pos.getZ(), mask, roles);
                            }
                        });
        table.put(0, 0, 0, geometry.coreMask(), BlockMultiblockCore.ROLE_ALL);
        for (int i = 0; i < masks.length; i++) {
            if (masks[i] == (byte) NOT_A_CELL) continue;
            for (byte[] plane : passive) plane[i] = 0;
        }
        geometry.passiveCells()
                .visit(
                        BlockPos.ZERO,
                        Direction.SOUTH,
                        (pos, faces, domains) ->
                                table.putPassive(
                                        pos.getX(), pos.getY(), pos.getZ(), faces, domains));
        table.putPassive(0, 0, 0, geometry.passiveCoreMask(), geometry.passiveCoreDomains());

        for (byte[] plane : byRole) {
            if (!Arrays.equals(plane, masks)) {
                assertAnswersFromTheTable(geometry.maskAtOwner());
                return table;
            }
        }

        return new MultiblockMaskTable(
                masks,
                passive,
                null,
                bounds.minX,
                bounds.minY,
                bounds.minZ,
                sizeX,
                sizeY,
                sizeZ,
                declaredCaps);
    }

    private static void assertAnswersFromTheTable(Class<?> maskAtOwner) {
        for (Class<?>[] signature :
                new Class<?>[][] {
                    {int.class, int.class, int.class},
                    {int.class, int.class, int.class, Direction.class}
                }) {
            Class<?> owner;
            try {
                owner = maskAtOwner.getMethod("maskAt", signature).getDeclaringClass();
            } catch (NoSuchMethodException e) {
                throw new AssertionError(e);
            }
            if (owner != BlockMultiblockCore.class) {
                throw new IllegalStateException(
                        maskAtOwner.getName()
                                + " emits role-qualified cells and "
                                + "takes maskAt/"
                                + signature.length
                                + " from "
                                + owner.getName()
                                + "; the role planes bypass the override");
            }
        }
    }

    public static int localX(int dx, int dy, int dz, Direction facing) {
        return switch (facing) {
            case WEST -> dz;
            case NORTH -> -dx;
            case EAST -> -dz;
            default -> dx;
        };
    }

    public static int localY(int dx, int dy, int dz, Direction facing) {
        return dy;
    }

    public static int localZ(int dx, int dy, int dz, Direction facing) {
        return switch (facing) {
            case WEST -> -dx;
            case NORTH -> -dz;
            case EAST -> dx;
            default -> dz;
        };
    }

    public static int worldX(int lx, int ly, int lz, Direction facing) {
        return switch (facing) {
            case WEST -> -lz;
            case NORTH -> -lx;
            case EAST -> lz;
            default -> lx;
        };
    }

    public static int worldZ(int lx, int ly, int lz, Direction facing) {
        return switch (facing) {
            case WEST -> lx;
            case NORTH -> -lz;
            case EAST -> -lx;
            default -> lz;
        };
    }

    public static Direction toLocal(Direction side, Direction facing) {
        if (side.getAxis() == Direction.Axis.Y) return side;
        return switch (facing) {
            case WEST -> side.getCounterClockWise();
            case NORTH -> side.getOpposite();
            case EAST -> side.getClockWise();
            default -> side;
        };
    }

    public boolean declares(int bits) {
        return (declaredCaps & bits) == bits;
    }

    private void put(int lx, int ly, int lz, int mask, int roles) {
        if (mask < 0 || mask > 0b111111) {
            throw new IllegalStateException(
                    "multiblock mask table: mask "
                            + mask
                            + " at local "
                            + lx
                            + ","
                            + ly
                            + ","
                            + lz
                            + " is not a 6-bit face set");
        }

        if (roles == 0 || (roles & ~BlockMultiblockCore.ROLE_ALL) != 0) {
            throw new IllegalStateException(
                    "multiblock mask table: roles 0b"
                            + Integer.toBinaryString(roles)
                            + " at local "
                            + lx
                            + ","
                            + ly
                            + ","
                            + lz
                            + " is not a non-empty subset of ROLE_ALL");
        }
        int index = index(lx, ly, lz);
        int union = 0;
        for (CapRole role : CapRole.VALUES) {
            byte[] plane = byRole[role.planeIndex];
            if ((roles & role.bit) != 0) plane[index] = (byte) mask;
            else if (plane[index] == (byte) NOT_A_CELL) plane[index] = 0;
            union |= plane[index];
        }
        masks[index] = (byte) union;
    }

    private void putPassive(int lx, int ly, int lz, int faces, int domains) {
        if (domains == 0 || faces == 0) return;
        if (faces < 0 || faces > 0b111111) {
            throw new IllegalStateException(
                    "multiblock mask table: passive faces "
                            + faces
                            + " at local "
                            + lx
                            + ","
                            + ly
                            + ","
                            + lz
                            + " is not a 6-bit face set");
        }
        if (maskAtLocal(lx, ly, lz) == NOT_A_CELL) {
            throw new IllegalStateException(
                    "multiblock mask table: passive emission at local "
                            + lx
                            + ","
                            + ly
                            + ","
                            + lz
                            + " is not a cell of the footprint");
        }
        int index = index(lx, ly, lz);
        for (int d = 0; d < passive.length; d++) {
            if ((domains & (1 << d)) != 0) passive[d][index] |= (byte) faces;
        }
    }

    private int index(int lx, int ly, int lz) {
        return ((ly - minY) * sizeZ + (lz - minZ)) * sizeX + (lx - minX);
    }

    private boolean outside(int lx, int ly, int lz) {
        return lx < minX
                || ly < minY
                || lz < minZ
                || lx >= minX + sizeX
                || ly >= minY + sizeY
                || lz >= minZ + sizeZ;
    }

    public int maskAtLocal(int lx, int ly, int lz) {
        if (outside(lx, ly, lz)) return NOT_A_CELL;
        return masks[index(lx, ly, lz)];
    }

    public int maskAtLocal(int lx, int ly, int lz, CapRole role) {
        if (byRole == null) return maskAtLocal(lx, ly, lz);
        if (outside(lx, ly, lz)) return NOT_A_CELL;
        return byRole[role.planeIndex][index(lx, ly, lz)];
    }

    public boolean hasRolePlanes() {
        return byRole != null;
    }

    public int passiveMaskAtLocal(int lx, int ly, int lz, int domains) {
        int named = domains & ((1 << passive.length) - 1);
        if (named == 0) return 0;
        if (outside(lx, ly, lz)) return NOT_A_CELL;
        int index = index(lx, ly, lz);
        int mask = 0;
        for (int d = 0; d < passive.length; d++) {
            if ((named & (1 << d)) != 0) mask |= passive[d][index];
        }
        return mask;
    }

    public void forEachLocalCell(LocalCellConsumer consumer) {
        for (int ly = minY; ly < minY + sizeY; ly++) {
            for (int lz = minZ; lz < minZ + sizeZ; lz++) {
                for (int lx = minX; lx < minX + sizeX; lx++) {
                    if (masks[index(lx, ly, lz)] != (byte) NOT_A_CELL) consumer.accept(lx, ly, lz);
                }
            }
        }
    }

    @FunctionalInterface
    public interface LocalCellConsumer {
        void accept(int lx, int ly, int lz);
    }

    private static final class Bounds implements BlockMultiblockCore.CellVisitor {

        private int minX, minY, minZ, maxX, maxY, maxZ;

        @Override
        public void cell(BlockPos pos, int mask) {
            include(pos.getX(), pos.getY(), pos.getZ());
        }

        void include(int x, int y, int z) {
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            minZ = Math.min(minZ, z);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
            maxZ = Math.max(maxZ, z);
        }
    }
}
