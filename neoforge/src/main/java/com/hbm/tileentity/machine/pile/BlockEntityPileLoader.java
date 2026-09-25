// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine.pile;

import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.machine.pile.BlockPile;
import com.hbm.items.machine.ItemPileRodMK2;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.machine.pile.BlockEntityPileCore.PileChannel;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class BlockEntityPileLoader extends BlockEntityPileDeviceBase
        implements SyncUnitSchema, IRORValueProvider {
    public static final double SPEED = 1D / 7D;
    private static final int[] SLOT = {0};
    private static final String[] ROR = {
        PREFIX_VALUE + "meta",
        PREFIX_VALUE + "depletion",
        PREFIX_VALUE + "deppercent",
        PREFIX_VALUE + "lifetime",
        PREFIX_VALUE + "temp"
    };

    @SyncField(units = 1L)
    public double extension;

    public double lastExtension;
    private double syncExtension;
    private int turnProgress;
    public boolean loading;
    public int delay;
    private boolean wasRedstone;

    @SyncField(units = 1L << 1)
    public ItemStack channelStack = ItemStack.EMPTY;

    @SyncField(units = 1L << 1)
    public double channelDepletion;

    @SyncField(units = 1L << 1)
    public double channelTemp;

    public BlockEntityPileLoader(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PILE_LOADER.get(), pos, state, 1);
    }

    @Override
    public void tickServer() {
        PileChannel channel = attachedChannel(BlockPile.Role.FUEL_IN);
        channelStack = ItemStack.EMPTY;
        channelDepletion = 0D;
        channelTemp = 0D;
        if (channel != null) {
            channelStack = channel.rods[channel.rods.length - 1];
            channelDepletion = ItemPileRodMK2.getDepletionPercent(channelStack);
            channelTemp = channel.heat;
        }

        Direction facing = orientation();
        boolean redstone =
                level.getSignal(worldPosition.relative(facing), facing.getOpposite()) > 0;
        if (redstone && !wasRedstone && delay <= 0 && extension <= 0D) loading = true;
        wasRedstone = redstone;

        if (delay > 0) delay--;
        else if (loading) {
            if (extension == 0D) playBolt(1F);
            extension += SPEED;
            if (extension >= 1D) {
                extension = 1D;
                loading = false;
                delay = 5;
            }
        } else {
            if (extension == 1D) {
                playBolt(0.75F);
                if (channel != null) {
                    channel.loadItem(getItem(0));
                    setItem(0, ItemStack.EMPTY);
                }
            }
            if (extension > 0D) extension = Math.max(0D, extension - SPEED);
        }

        networkPackNT(35);
        setChanged();
    }

    @Override
    public void tickClient() {
        lastExtension = extension;
        if (turnProgress > 0) {
            extension += (syncExtension - extension) / turnProgress;
            turnProgress--;
        } else extension = syncExtension;
    }

    private void playBolt(float pitch) {
        level.playSound(
                null, worldPosition, ModSounds.GUN_BOLT_OPEN.get(), SoundSource.BLOCKS, 1F, pitch);
    }

    public void startLoading() {
        if (delay <= 0 && extension <= 0D) {
            loading = true;
            setChanged();
        }
    }

    public static boolean isItemLoadable(ItemStack stack) {
        return stack.getItem() instanceof ItemPileRodMK2;
    }

    @Override
    public String[] getFunctionInfo() {
        return ROR;
    }

    @Override
    public String provideRORValue(String name) {
        return switch (name) {
            case PREFIX_VALUE + "meta" ->
                    channelStack.getItem() instanceof ItemPileRodMK2 rod
                            ? Integer.toString(rod.type.ordinal())
                            : "-1";

            case PREFIX_VALUE + "depletion" ->
                    Long.toString(Math.round(ItemPileRodMK2.getDepletion(channelStack)));
            case PREFIX_VALUE + "deppercent" -> Long.toString(Math.round(channelDepletion));
            case PREFIX_VALUE + "lifetime" ->
                    channelStack.getItem() instanceof ItemPileRodMK2 rod
                            ? Long.toString(Math.round(rod.type.life))
                            : "0";
            case PREFIX_VALUE + "temp" -> Long.toString(Math.round(channelTemp));
            default -> null;
        };
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return SLOT;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == 0 && isItemLoadable(stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return false;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        loading = input.getBooleanOr("loading", false);
        extension = input.getDoubleOr("level", 0D);
        delay = input.getIntOr("delay", 0);
        wasRedstone = input.getBooleanOr("wasRedstone", false);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("loading", loading);
        output.putDouble("level", extension);
        output.putInt("delay", delay);
        output.putBoolean("wasRedstone", wasRedstone);
    }

    @Override
    public long syncUnitMask() {
        return 7L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeDouble(extension);
            case 1 -> {
                ItemStack.OPTIONAL_STREAM_CODEC.encode(
                        new RegistryFriendlyByteBuf(output, level.registryAccess()), channelStack);
                output.writeDouble(channelDepletion);
                output.writeDouble(channelTemp);
            }
            case 2 -> output.writeInt(channelNumber);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> {
                double next = input.readDouble();
                if (syncExtension != next) turnProgress = 2;
                syncExtension = next;
            }
            case 1 -> {
                channelStack =
                        ItemStack.OPTIONAL_STREAM_CODEC.decode(
                                new RegistryFriendlyByteBuf(input, level.registryAccess()));
                channelDepletion = input.readDouble();
                channelTemp = input.readDouble();
            }
            case 2 -> channelNumber = input.readInt();
            default -> throw new IllegalArgumentException();
        }
    }
}
