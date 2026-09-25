// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.network;

import com.hbm.NuclearTech;
import com.hbm.api.control.IControlReceiver;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.module.mses.CompiledMses;
import com.hbm.module.mses.MsesCompiler;
import com.hbm.module.mses.MsesProgram;
import com.hbm.module.mses.MsesState;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.tileentity.FoldedCoreResident;
import com.hbm.tileentity.GraphResident;
import com.hbm.tileentity.Synced;
import com.hbm.util.TickPhase;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class BlockEntityRadioAUTOCAL extends BlockEntity
        implements Synced,
                GraphResident,
                FoldedCoreResident,
                IControlReceiver,
                SyncUnitSchema,
                CompiledMses.Host {

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
    public MsesState ctx = new MsesState();
    private String shownBuffer;

    private @Nullable MsesProgram program;
    private String[] programOf;
    private @Nullable CompletableFuture<MsesProgram> compiling;
    private Component[] lineMessages;

    private final Component[] pushed = new Component[history.length - 1];
    private int pushes;
    private boolean bufferShown;

    public BlockEntityRadioAUTOCAL(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RTTY_AUTOCAL.get(), pos, state);
    }

    public void tickServer() {
        if (TickPhase.every(this, 60)) setChanged();

        ctx.world = level;

        if (!isOn && autoReboot) isOn = true;

        try {
            if (isOn) {
                MsesProgram running = program();
                if (running != null) running.code().run(ctx, this);
            }
        } finally {
            publish();
        }

        networkPackNT(15);
    }

    private void compileScript() {
        String[] source = script;
        programOf = source;
        program = null;
        compiling =
                CompletableFuture.supplyAsync(
                        () -> MsesCompiler.compile(source), Util.backgroundExecutor());
    }

    private @Nullable MsesProgram program() {
        if (programOf != script) compileScript();
        if (compiling != null) {
            try {
                program = compiling.join();
            } catch (CompletionException ex) {
                if (!(ex.getCause() instanceof IllegalArgumentException limit)) throw ex;
                NuclearTech.LOGGER.warn(
                        "AUTOCAL at {} cannot run its script: {}",
                        worldPosition,
                        limit.getMessage());
            }
            compiling = null;
            lineMessages = new Component[script.length];
            if (program != null) ctx.bind(program.slots());
        }
        if (program == null) stop(EVALUATION_UNSUCCESSFUL);
        return program;
    }

    @Override
    public void msesAfterInstruction(int index, int ret) {
        if (ret != CompiledMses.SKIP) pushMsg(lineMessage(index));
        bufferShown = true;
        if (ret == CompiledMses.END_TICK) return;
        if (ret == CompiledMses.SHUTDOWN) stop(SHUTDOWN_REQUESTED);
        if (!ignoreError) {
            if (ret == CompiledMses.UNRECOGNIZED_COMMAND) stop(UNRECOGNIZED_COMMAND);
            if (ret == CompiledMses.PARAMETER_ERROR) stop(PARAMETER_ERROR);
            if (ret == CompiledMses.UNDEFINED) stop(UNDEFINED_BEHAVIOR);
            if (ret == CompiledMses.STACK_EXCEEDED) stop(STACK_EXCEEDED);
        }
    }

    @Override
    public void msesEndOfProgram(boolean outOfBounds) {
        stop(outOfBounds ? OUT_OF_BOUNDS : TERMINATED);
    }

    @Override
    public void msesEvaluationFailed() {
        stop(EVALUATION_UNSUCCESSFUL);
    }

    private Component lineMessage(int index) {
        Component message = lineMessages[index];
        if (message == null)
            lineMessages[index] = message = Component.literal(index + ": " + script[index]);
        return message;
    }

    private void showBuffer() {
        bufferShown = false;
        String buffer = ctx.readBuffer();
        if (buffer.equals(shownBuffer)) return;
        shownBuffer = buffer;
        history[0] = Component.translatable(LANG + "buffer", buffer);
    }

    public void pushMsg(Component msg) {
        pushed[pushes++ % pushed.length] = msg;
    }

    private void publish() {
        if (bufferShown) showBuffer();
        int count = Math.min(pushes, pushed.length);
        if (count == 0) return;
        for (int i = 1; i + count < history.length; i++) history[i] = history[i + count];
        for (int i = 0; i < count; i++) {
            history[history.length - count + i] = pushed[(pushes - count + i) % pushed.length];
        }
        Arrays.fill(pushed, null);
        pushes = 0;
    }

    public void stop(Component reason) {
        if (bufferShown) showBuffer();
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
        ctx = new MsesState();
        ctx.load(input, script);
        if (script.length > 0) compileScript();
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
        publish();
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
            ctx.generateJumpPoint(script[i], i);
        }
        compileScript();
        if (isOn) stop(SCRIPT_CHANGED);
        setChanged();
    }

    public void setComputerState(boolean on) {
        if (on) isOn = true;
        else stop(USER_SHUTDOWN);
        publish();
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
