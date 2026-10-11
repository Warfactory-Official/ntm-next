// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.multiblock;

import com.hbm.capability.NtmCapabilities.CapRole;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import java.util.Arrays;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import org.jspecify.annotations.Nullable;

public final class MultiblockMaskTable {
    public static final int NOT_A_CELL = -1;
    public static final int FLAG_HEAT = 1;
    public static final int FLAG_SOIL = 2;
    public static final int FLAG_PROXY = 4;
    private final byte[] masks;
    private final byte[][] passive;
    private final byte[] @Nullable [] byRole;
    private final byte @Nullable [] flags;
    private final byte @Nullable [] kinds;
    private final int minX, minY, minZ;
    private final int sizeX, sizeY, sizeZ;

    private MultiblockMaskTable(
            byte[] masks,
            byte[][] passive,
            byte[] @Nullable [] byRole,
            byte @Nullable [] flags,
            byte @Nullable [] kinds,
            int minX,
            int minY,
            int minZ,
            int sizeX,
            int sizeY,
            int sizeZ) {
        this.masks = masks;
        this.passive = passive;
        this.byRole = byRole;
        this.flags = flags;
        this.kinds = kinds;
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final IntArrayList active = new IntArrayList();
        private final IntArrayList passives = new IntArrayList();
        private final IntArrayList flagged = new IntArrayList();
        private final IntArrayList kinded = new IntArrayList();

        private Builder() {}

        public Builder cell(int lx, int ly, int lz, int mask, int roles) {
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
            active.addElements(active.size(), new int[] {lx, ly, lz, mask, roles});
            return this;
        }

        public Builder passive(int lx, int ly, int lz, int faces, int domains) {
            if (domains == 0 || faces == 0) return this;
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
            passives.addElements(passives.size(), new int[] {lx, ly, lz, faces, domains});
            return this;
        }

        public Builder flag(int lx, int ly, int lz, int bits) {
            if (bits != 0) flagged.addElements(flagged.size(), new int[] {lx, ly, lz, bits});
            return this;
        }

        public Builder kind(int lx, int ly, int lz, int kind) {
            if (kind < 0 || kind > Byte.MAX_VALUE) {
                throw new IllegalStateException(
                        "multiblock mask table: cell kind "
                                + kind
                                + " at local "
                                + lx
                                + ","
                                + ly
                                + ","
                                + lz
                                + " is not 0.."
                                + Byte.MAX_VALUE);
            }
            if (kind != 0) kinded.addElements(kinded.size(), new int[] {lx, ly, lz, kind});
            return this;
        }

        public MultiblockMaskTable build() {
            int minX = 0, minY = 0, minZ = 0, maxX = 0, maxY = 0, maxZ = 0;
            for (int i = 0; i < active.size(); i += 5) {
                minX = Math.min(minX, active.getInt(i));
                minY = Math.min(minY, active.getInt(i + 1));
                minZ = Math.min(minZ, active.getInt(i + 2));
                maxX = Math.max(maxX, active.getInt(i));
                maxY = Math.max(maxY, active.getInt(i + 1));
                maxZ = Math.max(maxZ, active.getInt(i + 2));
            }
            int sizeX = maxX - minX + 1, sizeY = maxY - minY + 1, sizeZ = maxZ - minZ + 1;
            int volume = sizeX * sizeY * sizeZ;
            byte[] masks = filled(volume);
            byte[][] passive = new byte[BlockMultiblockCore.PASSIVE_DOMAINS][];
            for (int d = 0; d < passive.length; d++) passive[d] = filled(volume);
            byte[][] byRole = new byte[CapRole.COUNT][];
            for (int r = 0; r < byRole.length; r++) byRole[r] = filled(volume);
            byte[] flags = flagged.isEmpty() ? null : new byte[volume];
            byte[] kinds = kinded.isEmpty() ? null : new byte[volume];
            MultiblockMaskTable table =
                    new MultiblockMaskTable(
                            masks, passive, byRole, flags, kinds, minX, minY, minZ, sizeX, sizeY,
                            sizeZ);
            for (int i = 0; i < active.size(); i += 5) {
                table.put(
                        active.getInt(i),
                        active.getInt(i + 1),
                        active.getInt(i + 2),
                        active.getInt(i + 3),
                        active.getInt(i + 4));
            }
            for (int i = 0; i < volume; i++) {
                if (masks[i] == (byte) NOT_A_CELL) continue;
                for (byte[] plane : passive) plane[i] = 0;
            }
            for (int i = 0; i < passives.size(); i += 5) {
                table.putPassive(
                        passives.getInt(i),
                        passives.getInt(i + 1),
                        passives.getInt(i + 2),
                        passives.getInt(i + 3),
                        passives.getInt(i + 4));
            }
            for (int i = 0; i < flagged.size(); i += 4) {
                int lx = flagged.getInt(i), ly = flagged.getInt(i + 1), lz = flagged.getInt(i + 2);
                if (table.maskAtLocal(lx, ly, lz) == NOT_A_CELL) {
                    throw new IllegalStateException(
                            "multiblock mask table: flags at local "
                                    + lx
                                    + ","
                                    + ly
                                    + ","
                                    + lz
                                    + " are not a cell of the footprint");
                }
                flags[table.index(lx, ly, lz)] |= (byte) flagged.getInt(i + 3);
            }
            for (int i = 0; i < kinded.size(); i += 4) {
                int lx = kinded.getInt(i), ly = kinded.getInt(i + 1), lz = kinded.getInt(i + 2);
                if (table.maskAtLocal(lx, ly, lz) == NOT_A_CELL) {
                    throw new IllegalStateException(
                            "multiblock mask table: kind at local "
                                    + lx
                                    + ","
                                    + ly
                                    + ","
                                    + lz
                                    + " is not a cell of the footprint");
                }
                kinds[table.index(lx, ly, lz)] = (byte) kinded.getInt(i + 3);
            }
            for (byte[] plane : byRole) {
                if (!Arrays.equals(plane, masks)) return table;
            }
            return new MultiblockMaskTable(
                    masks, passive, null, flags, kinds, minX, minY, minZ, sizeX, sizeY, sizeZ);
        }

        private static byte[] filled(int volume) {
            byte[] plane = new byte[volume];
            Arrays.fill(plane, (byte) NOT_A_CELL);
            return plane;
        }
    }

    public CompoundTag write() {
        CompoundTag tag = new CompoundTag();
        tag.putIntArray("min", new int[] {minX, minY, minZ});
        tag.putIntArray("size", new int[] {sizeX, sizeY, sizeZ});
        tag.putByteArray("masks", masks);
        for (int d = 0; d < passive.length; d++) tag.putByteArray("passive" + d, passive[d]);
        if (byRole != null) {
            for (int r = 0; r < byRole.length; r++) tag.putByteArray("role" + r, byRole[r]);
        }
        if (flags != null) tag.putByteArray("flags", flags);
        if (kinds != null) tag.putByteArray("kinds", kinds);
        return tag;
    }

    public static MultiblockMaskTable read(CompoundTag tag) {
        int[] min = tag.getIntArray("min").orElseThrow();
        int[] size = tag.getIntArray("size").orElseThrow();
        byte[][] passive = new byte[BlockMultiblockCore.PASSIVE_DOMAINS][];
        for (int d = 0; d < passive.length; d++)
            passive[d] = tag.getByteArray("passive" + d).orElseThrow();
        byte[][] byRole = null;
        if (tag.contains("role0")) {
            byRole = new byte[CapRole.COUNT][];
            for (int r = 0; r < byRole.length; r++)
                byRole[r] = tag.getByteArray("role" + r).orElseThrow();
        }
        return new MultiblockMaskTable(
                tag.getByteArray("masks").orElseThrow(),
                passive,
                byRole,
                tag.getByteArray("flags").orElse(null),
                tag.getByteArray("kinds").orElse(null),
                min[0],
                min[1],
                min[2],
                size[0],
                size[1],
                size[2]);
    }

    boolean sameAs(MultiblockMaskTable other) {
        return minX == other.minX
                && minY == other.minY
                && minZ == other.minZ
                && sizeX == other.sizeX
                && sizeY == other.sizeY
                && sizeZ == other.sizeZ
                && Arrays.equals(masks, other.masks)
                && Arrays.deepEquals(passive, other.passive)
                && Arrays.deepEquals(byRole, other.byRole)
                && Arrays.equals(flags, other.flags)
                && Arrays.equals(kinds, other.kinds);
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

    private void put(int lx, int ly, int lz, int mask, int roles) {
        int index = index(lx, ly, lz);
        int union = 0;
        for (CapRole role : CapRole.VALUES) {
            byte[] plane = byRole[role.planeIndex];
            if (plane[index] == (byte) NOT_A_CELL) plane[index] = 0;
            if ((roles & role.bit) != 0) plane[index] |= (byte) mask;
            union |= plane[index];
        }
        masks[index] = (byte) union;
    }

    private void putPassive(int lx, int ly, int lz, int faces, int domains) {
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

    public int flagsAtLocal(int lx, int ly, int lz) {
        if (flags == null || outside(lx, ly, lz)) return 0;
        return flags[index(lx, ly, lz)];
    }

    public int kindAtLocal(int lx, int ly, int lz) {
        if (kinds == null || outside(lx, ly, lz)) return 0;
        return kinds[index(lx, ly, lz)];
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
}
