// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.network;

import com.hbm.api.control.IControlReceiver;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.tileentity.FoldedCoreResident;
import com.hbm.tileentity.GraphResident;
import com.hbm.tileentity.Synced;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class BlockEntityRadioTelex extends BlockEntity
        implements Synced, GraphResident, FoldedCoreResident, IControlReceiver, SyncUnitSchema {

    public static final int LINE_WIDTH = 33;
    public static final char EOL = '\n';
    public static final char EOT = '';
    public static final char BELL = '';
    public static final char PRINT = '\f';
    public static final char PAUSE = '';
    public static final char CLEAR = '';
    private static final int BUFFER_LINES = 5;

    @SyncField(units = 1L)
    public final String[] txBuffer = {"", "", "", "", ""};

    @SyncField(units = 1L << 1)
    public final String[] rxBuffer = {"", "", "", "", ""};

    @SyncField(units = 1L << 2)
    public String txChannel = "";

    @SyncField(units = 1L << 3)
    public String rxChannel = "";

    public int sendingLine;
    public int sendingIndex;
    public boolean isSending;
    public int sendingWait;
    public int writingLine;
    public boolean printAfterRx;
    public boolean deleteOnReceive = true;

    @SyncField(units = 1L << 4)
    public char sendingChar = ' ';

    public BlockEntityRadioTelex(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RTTY_TELEX.get(), pos, state);
    }

    public void tickServer() {
        sendingChar = ' ';

        if (isSending && txChannel.isEmpty()) isSending = false;

        if (isSending) {
            if (sendingWait > 0) {
                sendingWait--;
            } else {
                String line = txBuffer[sendingLine];
                if (line.length() > sendingIndex) {
                    char c = line.charAt(sendingIndex++);
                    if (c == PAUSE) {
                        sendingWait = 20;
                    } else {
                        RTTYSystem.broadcast(level, txChannel, c);
                        sendingChar = c;
                    }
                } else if (sendingLine >= BUFFER_LINES - 1) {
                    isSending = false;
                    RTTYSystem.broadcast(level, txChannel, EOT);
                    sendingLine = 0;
                    sendingIndex = 0;
                } else {
                    RTTYSystem.broadcast(level, txChannel, EOL);
                    sendingLine++;
                    sendingIndex = 0;
                }
            }
        }

        if (!rxChannel.isEmpty()) {
            RTTYSystem.RTTYChannel channel = RTTYSystem.listen(level, rxChannel);
            if (channel != null
                    && channel.signal instanceof Character
                    && channel.timeStamp > level.getGameTime() - 2
                    && channel.timeStamp != -1) {
                receiveCharacter((char) channel.signal);
            }
        }

        networkPackNT(16);
    }

    private void receiveCharacter(char c) {
        if (deleteOnReceive) {
            deleteOnReceive = false;
            clearReceiveBuffer();
        }

        if (c == EOT) {
            if (printAfterRx) {
                printAfterRx = false;
                print();
            }
            deleteOnReceive = true;
        } else if (c == EOL) {
            if (writingLine < BUFFER_LINES - 1) writingLine++;
            setChanged();
        } else if (c == BELL) {
            level.playSound(
                    null,
                    worldPosition,
                    SoundEvents.EXPERIENCE_ORB_PICKUP,
                    SoundSource.BLOCKS,
                    2.0F,
                    0.5F);
        } else if (c == PRINT) {
            printAfterRx = true;
        } else if (c == CLEAR) {
            clearReceiveBuffer();
        } else {
            rxBuffer[writingLine] += c;
            setChanged();
        }
    }

    public void clearReceiveBuffer() {
        for (int i = 0; i < BUFFER_LINES; i++) rxBuffer[i] = "";
        writingLine = 0;
    }

    public void print() {
        List<Component> text = new ArrayList<>();
        for (String line : rxBuffer) {
            if (!line.isEmpty())
                text.add(Component.literal(ChatFormatting.RESET + "" + ChatFormatting.GRAY + line));
        }

        ItemStack stack = new ItemStack(Items.PAPER);
        stack.set(DataComponents.LORE, new ItemLore(text));
        stack.set(
                DataComponents.CUSTOM_NAME,
                Component.translatable("chat.entityRadioTelex.message"));
        level.addFreshEntity(
                new ItemEntity(
                        level,
                        worldPosition.getX() + 0.5D,
                        worldPosition.getY() + 1D,
                        worldPosition.getZ() + 0.5D,
                        stack));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        for (int i = 0; i < BUFFER_LINES; i++) {
            txBuffer[i] = input.getStringOr("tx" + i, txBuffer[i]);
            rxBuffer[i] = input.getStringOr("rx" + i, rxBuffer[i]);
        }
        txChannel = input.getStringOr("txChan", txChannel);
        rxChannel = input.getStringOr("rxChan", rxChannel);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        for (int i = 0; i < BUFFER_LINES; i++) {
            output.putString("tx" + i, txBuffer[i]);
            output.putString("rx" + i, rxBuffer[i]);
        }
        output.putString("txChan", txChannel);
        output.putString("rxChan", rxChannel);
    }

    @Override
    public boolean hasPermission(Player player) {
        return player.getEyePosition()
                        .distanceToSqr(
                                worldPosition.getX() + 0.5D,
                                worldPosition.getY() + 0.5D,
                                worldPosition.getZ() + 0.5D)
                < 16D * 16D;
    }

    @Override
    public void receiveControl(CompoundTag data) {
        for (int i = 0; i < BUFFER_LINES; i++) {
            String key = "tx" + i;
            if (data.contains(key)) txBuffer[i] = data.getStringOr(key, txBuffer[i]);
        }

        String command = data.getStringOr("cmd", "");
        if ("snd".equals(command) && !isSending) {
            isSending = true;
            sendingLine = 0;
            sendingIndex = 0;
        }
        if ("rxprt".equals(command)) print();
        if ("rxcls".equals(command)) clearReceiveBuffer();
        if ("sve".equals(command)) {
            txChannel = data.getStringOr("txChan", txChannel);
            rxChannel = data.getStringOr("rxChan", rxChannel);
            setChanged();
        }
    }

    private void writeTransmitBuffer(ByteBuf output) {
        for (String line : txBuffer) ByteBufCodecs.STRING_UTF8.encode(output, line);
    }

    private void readTransmitBuffer(ByteBuf input) {
        for (int i = 0; i < txBuffer.length; i++)
            txBuffer[i] = ByteBufCodecs.STRING_UTF8.decode(input);
    }

    private void writeReceiveBuffer(ByteBuf output) {
        for (String line : rxBuffer) ByteBufCodecs.STRING_UTF8.encode(output, line);
    }

    private void readReceiveBuffer(ByteBuf input) {
        for (int i = 0; i < rxBuffer.length; i++)
            rxBuffer[i] = ByteBufCodecs.STRING_UTF8.decode(input);
    }

    private void writeTransmitChannel(ByteBuf output) {
        ByteBufCodecs.STRING_UTF8.encode(output, txChannel);
    }

    private void readTransmitChannel(ByteBuf input) {
        txChannel = ByteBufCodecs.STRING_UTF8.decode(input);
    }

    private void writeReceiveChannel(ByteBuf output) {
        ByteBufCodecs.STRING_UTF8.encode(output, rxChannel);
    }

    private void readReceiveChannel(ByteBuf input) {
        rxChannel = ByteBufCodecs.STRING_UTF8.decode(input);
    }

    @Override
    public long syncUnitMask() {
        return 0x1fL;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> writeTransmitBuffer(output);
            case 1 -> writeReceiveBuffer(output);
            case 2 -> writeTransmitChannel(output);
            case 3 -> writeReceiveChannel(output);
            case 4 -> output.writeChar(this.sendingChar);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> readTransmitBuffer(input);
            case 1 -> readReceiveBuffer(input);
            case 2 -> readTransmitChannel(input);
            case 3 -> readReceiveChannel(input);
            case 4 -> this.sendingChar = input.readChar();
            default -> throw new IllegalArgumentException();
        }
    }
}
