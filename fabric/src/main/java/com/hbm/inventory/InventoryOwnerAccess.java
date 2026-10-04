// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;

public enum InventoryOwnerAccess {
    BLOCK {
        @Override
        public ServerLevel level(Object owner) {
            return ((BlockEntity) owner).getLevel() instanceof ServerLevel level ? level : null;
        }

        @Override
        public boolean live(Object owner) {
            BlockEntity block = (BlockEntity) owner;
            ServerLevel level = level(owner);
            if (level == null || block.isRemoved()) return false;
            var chunk =
                    level.getChunkSource()
                            .getChunkNow(
                                    block.getBlockPos().getX() >> 4,
                                    block.getBlockPos().getZ() >> 4);
            return chunk != null && chunk.getBlockEntities().get(block.getBlockPos()) == block;
        }

        @Override
        public double x(Object owner) {
            return ((BlockEntity) owner).getBlockPos().getX() + 0.5;
        }

        @Override
        public double y(Object owner) {
            return ((BlockEntity) owner).getBlockPos().getY() + 0.5;
        }

        @Override
        public double z(Object owner) {
            return ((BlockEntity) owner).getBlockPos().getZ() + 0.5;
        }

        @Override
        public void publish(Object owner) {
            BlockEntity block = (BlockEntity) owner;
            if (block.isRemoved()) return;
            block.setChanged();
            ((ServerLevel) block.getLevel())
                    .sendBlockUpdated(
                            block.getBlockPos(), block.getBlockState(), block.getBlockState(), 3);
        }
    },
    ENTITY {
        @Override
        public ServerLevel level(Object owner) {
            return ((Entity) owner).level() instanceof ServerLevel level ? level : null;
        }

        @Override
        public boolean live(Object owner) {
            Entity entity = (Entity) owner;
            ServerLevel level = level(owner);
            return level != null
                    && !entity.isRemoved()
                    && level.getEntity(entity.getId()) == entity;
        }

        @Override
        public double x(Object owner) {
            return ((Entity) owner).getX();
        }

        @Override
        public double y(Object owner) {
            return ((Entity) owner).getY();
        }

        @Override
        public double z(Object owner) {
            return ((Entity) owner).getZ();
        }
    },
    LOCATION {
        @Override
        public Object carrier(Object owner) {
            return ((InventoryLocation) owner).inventoryCarrier();
        }

        @Override
        public ServerLevel level(Object owner) {
            Object carrier = carrier(owner);
            return carrier == null ? null : of(carrier).level(carrier);
        }

        @Override
        public boolean live(Object owner) {
            Object carrier = carrier(owner);
            return carrier != null && of(carrier).live(carrier);
        }

        @Override
        public double x(Object owner) {
            Object carrier = carrier(owner);
            return of(carrier).x(carrier);
        }

        @Override
        public double y(Object owner) {
            Object carrier = carrier(owner);
            return of(carrier).y(carrier);
        }

        @Override
        public double z(Object owner) {
            Object carrier = carrier(owner);
            return of(carrier).z(carrier);
        }
    };

    private static final ClassValue<InventoryOwnerAccess> ACCESS =
            new ClassValue<>() {
                @Override
                protected InventoryOwnerAccess computeValue(Class<?> type) {
                    if (InventoryLocation.class.isAssignableFrom(type)) return LOCATION;
                    if (BlockEntity.class.isAssignableFrom(type)) return BLOCK;
                    if (Entity.class.isAssignableFrom(type)) return ENTITY;
                    throw new IllegalArgumentException(type.getName());
                }
            };

    public static InventoryOwnerAccess of(Object owner) {
        return ACCESS.get(owner.getClass());
    }

    public Object carrier(Object owner) {
        return owner;
    }

    public void publish(Object owner) {}

    public abstract ServerLevel level(Object owner);

    public abstract boolean live(Object owner);

    public abstract double x(Object owner);

    public abstract double y(Object owner);

    public abstract double z(Object owner);
}
