// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.api.energymk2.ItemEnergyTransfer;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.machine.BlockCharger;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.tileentity.FoldedCoreResident;
import com.hbm.tileentity.GraphResident;
import com.hbm.tileentity.Synced;
import com.hbm.util.TickPhase;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class BlockEntityCharger extends BlockEntity
        implements Synced, GraphResident, FoldedCoreResident, IEnergyHandlerMK2, SyncUnitSchema {

    public static final int DELAY = 20;
    private static final EquipmentSlot[] SLOTS = {
        EquipmentSlot.MAINHAND,
        EquipmentSlot.FEET,
        EquipmentSlot.LEGS,
        EquipmentSlot.CHEST,
        EquipmentSlot.HEAD
    };
    public int usingTicks;
    public int lastUsingTicks;
    private List<Player> players = List.of();

    @SyncField(units = 1L << 0)
    private long charge;

    @SyncField(units = 1L << 1)
    private boolean particles;

    private int lastOp;

    public BlockEntityCharger(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NTM_CHARGER.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, BlockEntityCharger be) {
        be.serverTick((ServerLevel) level, pos, state);
        be.advanceArm(level, pos);
    }

    public static void tickClient(
            Level level, BlockPos pos, BlockState state, BlockEntityCharger be) {
        be.advanceArm(level, pos);
    }

    private void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
        Direction dir = state.getValue(HorizontalDirectionalBlock.FACING).getOpposite();

        AABB box =
                new AABB(
                                pos.getX() + 0.5,
                                pos.getY(),
                                pos.getZ() + 0.5,
                                pos.getX() + 0.5,
                                pos.getY() + 0.5,
                                pos.getZ() + 0.5)
                        .inflate(0.5, 0.0, 0.5);
        players = level.getEntitiesOfClass(Player.class, box);

        charge = 0L;
        for (Player player : players) {
            for (EquipmentSlot slot : SLOTS) {
                charge +=
                        ItemEnergyTransfer.insert(
                                player.getInventory(),
                                inventorySlot(player, slot),
                                Long.MAX_VALUE,
                                true);
            }
        }

        particles = lastOp > 0;
        if (particles) {
            lastOp--;
            if (TickPhase.every(this, 20)) {
                level.playSound(
                        null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.2F, 0.5F);
            }
        }

        networkPackNT(50);

        if (particles) {
            double px =
                    pos.getX()
                            + 0.5
                            + (level.getRandom().nextDouble() * 0.0625)
                            + dir.getStepX() * 0.75;
            double py = pos.getY() + 0.1;
            double pz =
                    pos.getZ()
                            + 0.5
                            + (level.getRandom().nextDouble() * 0.0625)
                            + dir.getStepZ() * 0.75;
            level.sendParticles(
                    ParticleTypes.ENCHANTED_HIT,
                    px,
                    py,
                    pz,
                    1,
                    -dir.getStepX() * 0.1,
                    0.0,
                    -dir.getStepZ() * 0.1,
                    0.0);
        }
    }

    private void advanceArm(Level level, BlockPos pos) {
        lastUsingTicks = usingTicks;
        if ((charge > 0L || particles) && usingTicks < DELAY) {
            usingTicks++;
            if (usingTicks == 2) {
                level.playSound(
                        null, pos, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 0.5F, 0.5F);
            }
        }
        if ((charge <= 0L && !particles) && usingTicks > 0) {
            usingTicks--;
            if (usingTicks == 4) {
                level.playSound(
                        null, pos, SoundEvents.PISTON_CONTRACT, SoundSource.BLOCKS, 0.5F, 0.5F);
            }
        }
    }

    @Override
    public long getPower() {
        return 0L;
    }

    @Override
    public void setPower(long power) {}

    @Override
    public long getMaxPower() {
        return charge;
    }

    @Override
    public long transferPower(long power, boolean simulate) {
        if (usingTicks < DELAY || power == 0L) return power;

        long remaining = power;
        for (Player player : players) {
            for (EquipmentSlot slot : SLOTS) {
                if (remaining <= 0L) return 0L;
                Inventory inventory = player.getInventory();
                int index = inventorySlot(player, slot);
                ItemStack stack = inventory.getItem(index);

                long allowance = Math.min(Math.max(remaining / 5L, 1L), remaining);
                long accepted = ItemEnergyTransfer.insert(inventory, index, allowance, simulate);

                if (!simulate && (accepted > 0L || stack.getItem() instanceof IBatteryItem)) {

                    lastOp = 4;
                }
                remaining -= accepted;
            }
        }
        return remaining;
    }

    private static int inventorySlot(Player player, EquipmentSlot slot) {
        return slot == EquipmentSlot.MAINHAND
                ? player.getInventory().getSelectedSlot()
                : slot.getIndex(Inventory.INVENTORY_SIZE);
    }

    public boolean acceptsFace(Direction dir) {
        return dir == getBlockState().getValue(HorizontalDirectionalBlock.FACING).getOpposite();
    }

    @Override
    public long syncUnitMask() {
        return 0x3L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeLong(this.charge);
            case 1 -> output.writeBoolean(this.particles);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.charge = input.readLong();
            case 1 -> this.particles = input.readBoolean();
            default -> throw new IllegalArgumentException();
        }
    }
}
