// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine.storage;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.inventory.container.MenuFileCabinet;
import com.hbm.packet.SyncField;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.machine.BlockEntityLockableBase;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class BlockEntityFileCabinet extends BlockEntityLockableBase implements MenuProvider {

    private static final int SLOTS = 8;

    private static final int UPPER_DELAY = 10;
    private static final float MAX_EXTENT = 0.8F;

    public float lowerExtent;
    public float prevLowerExtent;
    public float upperExtent;
    public float prevUpperExtent;

    @SyncField(units = 1L << 4)
    private int timer;

    @SyncField(units = 1L << 5)
    private int playersUsing;

    public BlockEntityFileCabinet(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FILE_CABINET.get(), pos, state, SLOTS);
    }

    @Override
    protected double interactionRangeSq() {
        return 64;
    }

    @Override
    public void startOpen(ContainerUser user) {
        if (!level.isClientSide()) this.playersUsing++;
    }

    @Override
    public void stopOpen(ContainerUser user) {
        if (!level.isClientSide()) this.playersUsing--;
    }

    @Override
    public void tickServer() {
        if (this.playersUsing > 0) {
            if (timer < UPPER_DELAY) timer++;
        } else {
            timer = 0;
        }

        networkPackNT(25);
    }

    @Override
    public void tickClient() {
        this.prevLowerExtent = lowerExtent;
        this.prevUpperExtent = upperExtent;

        float openSpeed = playersUsing > 0 ? 1F / 16F : 1F / 25F;

        if (this.playersUsing > 0) {
            if (lowerExtent == 0F && upperExtent == 0F) {
                playSound(ModSounds.CRATE_OPEN.get(), 0.8F, 1.0F);
            } else {

                if (upperExtent + openSpeed >= MAX_EXTENT && lowerExtent < MAX_EXTENT) {
                    playSound(
                            ModSounds.CRATE_OPEN.get(),
                            0.5F,
                            level.getRandom().nextFloat() * 0.1F + 0.7F);
                }

                if (lowerExtent + openSpeed >= MAX_EXTENT && lowerExtent < MAX_EXTENT) {
                    playSound(
                            ModSounds.CRATE_OPEN.get(),
                            0.5F,
                            level.getRandom().nextFloat() * 0.1F + 0.7F);
                }
            }

            this.lowerExtent += openSpeed;

            if (timer >= UPPER_DELAY) this.upperExtent += openSpeed;

        } else if (lowerExtent > 0) {
            if (upperExtent - openSpeed < MAX_EXTENT / 2
                    && upperExtent >= MAX_EXTENT / 2
                    && upperExtent != lowerExtent) {
                playSound(ModSounds.CRATE_CLOSE.get(), 0.8F, 1.0F);
            }

            if (lowerExtent - openSpeed < MAX_EXTENT / 2 && lowerExtent >= MAX_EXTENT / 2) {
                playSound(ModSounds.CRATE_CLOSE.get(), 0.8F, 1.0F);
            }

            this.upperExtent -= openSpeed;
            this.lowerExtent -= openSpeed;
        }

        this.lowerExtent = Mth.clamp(lowerExtent, 0F, MAX_EXTENT);
        this.upperExtent = Mth.clamp(upperExtent, 0F, MAX_EXTENT);
    }

    private void playSound(SoundEvent sound, float volume, float pitch) {
        level.playLocalSound(
                worldPosition.getX() + 0.5,
                worldPosition.getY() + 0.5,
                worldPosition.getZ() + 0.5,
                sound,
                SoundSource.BLOCKS,
                volume,
                pitch,
                false);
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.fileCabinet");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        if (!canAccess(player)) return null;
        return new MenuFileCabinet(containerId, playerInventory, this);
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 0x30L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 4 -> output.writeInt(this.timer);
            case 5 -> output.writeInt(this.playersUsing);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 4 -> this.timer = input.readInt();
            case 5 -> this.playersUsing = input.readInt();
            default -> super.readSyncUnit(unit, input);
        }
    }
}
