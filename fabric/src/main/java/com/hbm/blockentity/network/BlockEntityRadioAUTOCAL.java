// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blockentity.network;

import com.hbm.api.control.IControlReceiver;
import com.hbm.blockentity.FoldedCoreResident;
import com.hbm.blockentity.GraphResident;
import com.hbm.blockentity.Synced;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.module.IParse.EnumStatementReturn;
import com.hbm.module.IParse.ParseContext;
import com.hbm.module.IParse;
import com.hbm.module.ParseMSES1Ext1;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.util.TickPhase;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import java.util.Arrays;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class BlockEntityRadioAUTOCAL extends BlockEntity
        implements Synced, GraphResident, FoldedCoreResident, IControlReceiver, SyncUnitSchema {
    public static final int MAX_SCRIPT_BYTES = Short.MAX_VALUE;
    private static final String LANG = "desc.gui.radioAUTOCAL.";
    private static final Component TERMINATED =
            Component.translatable(LANG + "programHasTerminated");
    private static final Component OUT_OF_BOUNDS =
            Component.translatable(LANG + "programIndexIsOut");
    private static final Component SHUTDOWN_REQUESTED =
            Component.translatable(LANG + "programRequestedShutdown");
    private static final Component UNRECOGNIZED_COMMAND =
            Component.translatable(LANG + "unrecognizedCommand");
    private static final Component PARAMETER_ERROR =
            Component.translatable(LANG + "parameterError");
    private static final Component UNDEFINED_BEHAVIOR =
            Component.translatable(LANG + "undefinedBehavior");
    private static final Component STACK_EXCEEDED =
            Component.translatable(LANG + "stackExceededCapacity");
    private static final Component EVALUATION_UNSUCCESSFUL =
            Component.translatable(LANG + "evaluationUnsuccessful");
    private static final Component USER_SHUTDOWN =
            Component.translatable(LANG + "userRequestedShutdown");
    private static final Component SCRIPT_CHANGED =
            Component.translatable(LANG + "scriptHasChanged");

    private static final IParse MSES1_EXT1 = new ParseMSES1Ext1();

    @SyncField(units = 1L)
    public boolean isOn;

    @SyncField(units = 1L)
    public boolean ignoreError;

    @SyncField(units = 1L)
    public boolean autoReboot;

    @SyncField(units = 1L << 1)
    public final Component[] history = {
        CommonComponents.EMPTY,
        CommonComponents.EMPTY,
        CommonComponents.EMPTY,
        CommonComponents.EMPTY,
        CommonComponents.EMPTY,
        CommonComponents.EMPTY
    };

    public String[] script = new String[0];
    public ParseContext ctx = new ParseContext();
    private String shownBuffer;

    public BlockEntityRadioAUTOCAL(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RTTY_AUTOCAL.get(), pos, state);
    }

    public void tickServer() {
        if (TickPhase.every(this, 60)) setChanged();

        ctx.world = level;

        if (!isOn && autoReboot) isOn = true;

        if (isOn) {
            int emergencyBrake = 100;
            for (int i = 0; i < ctx.clockSpeed && emergencyBrake > 0; i++) {
                emergencyBrake--;

                if (ctx.current == script.length) {
                    stop(TERMINATED);
                    break;
                }
                if (ctx.current < 0 || ctx.current >= script.length) {
                    stop(OUT_OF_BOUNDS);
                    break;
                }

                try {
                    int index = ctx.current;
                    ctx.current++;
                    String line = script[index];
                    EnumStatementReturn ret = MSES1_EXT1.eval(ctx, line);
                    if (ret != EnumStatementReturn.SKIP)
                        pushMsg(Component.literal(index + ": " + line));
                    showBuffer();
                    if (ret == EnumStatementReturn.END_TICK) break;
                    if (ret == EnumStatementReturn.SHUTDOWN) stop(SHUTDOWN_REQUESTED);
                    if (!ignoreError) {
                        if (ret == EnumStatementReturn.UNRECOGNIZED_COMMAND)
                            stop(UNRECOGNIZED_COMMAND);
                        if (ret == EnumStatementReturn.PARAMETER_ERROR) stop(PARAMETER_ERROR);
                        if (ret == EnumStatementReturn.UNDEFINED) stop(UNDEFINED_BEHAVIOR);
                        if (ret == EnumStatementReturn.STACK_EXCEEDED) stop(STACK_EXCEEDED);
                    }
                    if (ret == EnumStatementReturn.SKIP) i--;
                } catch (Exception ex) {
                    stop(EVALUATION_UNSUCCESSFUL);
                }
            }
            if (emergencyBrake < 100) markChanged();
        }

        networkPackNT(15);
    }

    private void showBuffer() {
        String buffer = ctx.readBuffer();
        if (buffer.equals(shownBuffer)) return;
        shownBuffer = buffer;
        history[0] = Component.translatable(LANG + "buffer", buffer);
    }

    public void pushMsg(Component msg) {
        for (int i = 2; i < history.length; i++) {
            history[i - 1] = history[i];
        }

        history[history.length - 1] = msg;
    }

    public void stop(Component reason) {
        isOn = false;
        ctx.turnOff();
        pushMsg(reason);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        isOn = input.getBooleanOr("isOn", false);
        ignoreError = input.getBooleanOr("ignoreError", false);
        autoReboot = input.getBooleanOr("autoReboot", false);
        script =
                input.read("script", Codec.STRING.listOf())
                        .orElse(List.of())
                        .toArray(String[]::new);
        ctx = new ParseContext();
        ctx.load(input, script, MSES1_EXT1);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("isOn", isOn);
        output.putBoolean("ignoreError", ignoreError);
        output.putBoolean("autoReboot", autoReboot);
        output.store("script", Codec.STRING.listOf(), Arrays.asList(script));
        ctx.save(output);
    }

    @Override
    public boolean hasPermission(Player player) {
        return player.getEyePosition()
                        .distanceToSqr(
                                worldPosition.getX() + 0.5D,
                                worldPosition.getY() + 1D,
                                worldPosition.getZ() + 0.5D)
                <= 15D * 15D;
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("on")) {
            if (isOn) stop(USER_SHUTDOWN);
            else isOn = true;
        }
        if (data.contains("ignore")) ignoreError = !ignoreError;
        if (data.contains("auto")) autoReboot = !autoReboot;

        String payload = data.getStringOr("payload", null);
        if (payload != null && ByteBufUtil.utf8Bytes(payload) <= MAX_SCRIPT_BYTES) {
            setComputerScript(payload);
        }
    }

    public boolean setComputerBuffer(String text) {
        boolean complete = ctx.writeBuffer(text);
        setChanged();
        return complete;
    }

    public void setComputerScript(String text) {
        ctx.jmp.clear();
        script = text.split("\n");
        for (int i = 0; i < script.length; i++) {
            script[i] = script[i].trim();
            MSES1_EXT1.generateJumpPoints(ctx, script[i], i);
        }
        if (isOn) stop(SCRIPT_CHANGED);
        setChanged();
    }

    public void setComputerState(boolean on) {
        if (on) isOn = true;
        else stop(USER_SHUTDOWN);
        setChanged();
    }

    @Override
    public long syncUnitMask() {
        return 0x3L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> {
                output.writeBoolean(isOn);
                output.writeBoolean(ignoreError);
                output.writeBoolean(autoReboot);
            }
            case 1 -> {
                for (Component line : history)
                    ComponentSerialization.TRUSTED_CONTEXT_FREE_STREAM_CODEC.encode(output, line);
            }
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> {
                isOn = input.readBoolean();
                ignoreError = input.readBoolean();
                autoReboot = input.readBoolean();
            }
            case 1 -> {
                for (int i = 0; i < history.length; i++) {
                    history[i] =
                            ComponentSerialization.TRUSTED_CONTEXT_FREE_STREAM_CODEC.decode(input);
                }
            }
            default -> throw new IllegalArgumentException();
        }
    }
}
