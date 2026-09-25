// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine.rbmk;

import com.hbm.api.control.IControlReceiver;
import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.EntityProcessorCrossSmooth;
import com.hbm.explosion.vanillant.standard.ExplosionEffectWeapon;
import com.hbm.explosion.vanillant.standard.PlayerProcessorStandard;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.tileentity.FoldedCoreResident;
import com.hbm.tileentity.GraphResident;
import com.hbm.tileentity.Synced;
import com.hbm.tileentity.network.RTTYSystem;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class BlockEntityRBMKTerminal extends BlockEntity
        implements Synced,
                GraphResident,
                FoldedCoreResident,
                IControlReceiver,
                IRORInteractive,
                SyncUnitSchema {

    public static final int HISTORY = 17;

    @SyncField(units = 1L)
    public final String[] history = new String[HISTORY];

    public String channel = "";

    @SyncField(units = 1L << 1)
    public String repeatCmd = "";

    public boolean doesRepeat = false;
    public boolean ocMode = false;

    public BlockEntityRBMKTerminal(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RBMK_TERMINAL.get(), pos, state);
    }

    public static void tickServer(
            Level level, BlockPos pos, BlockState state, BlockEntityRBMKTerminal terminal) {
        if (terminal.ocMode) {
            terminal.networkPackNT(10);
            return;
        }

        if (!terminal.channel.isEmpty() && !terminal.repeatCmd.isEmpty()) {
            RTTYSystem.broadcast(level, terminal.channel, terminal.repeatCmd);
        }
        terminal.networkPackNT(50);
    }

    public void eval(String cmd) {
        if (cmd == null) return;

        if (ocMode) {
            push(cmd);
            markChanged();
            return;
        }

        push(cmd);
        if (cmd.isEmpty()) return;

        if (cmd.startsWith("chan ")) {
            this.channel = cmd.substring(5);
            push("Set channel to " + (this.channel.isEmpty() ? "<none>" : this.channel));
            markChanged();
            return;
        }

        if (cmd.equals("chan")) {
            this.channel = "";
            push("Set channel to <none>");
            markChanged();
            return;
        }

        if (cmd.startsWith("start ")) {
            this.repeatCmd = cmd.substring(6);
            push("Repeating signal on " + this.channel);
            markChanged();
            return;
        }

        if (cmd.equals("stop")) {
            this.repeatCmd = "";
            push("Stopping repeat signal");
            markChanged();
            return;
        }

        if (cmd.startsWith("send ")) {
            if (this.channel.isEmpty()) {
                push("Cannot send - no channel set");
                return;
            }
            RTTYSystem.broadcast(level, this.channel, cmd.substring(5));
            push("Sent signal on " + this.channel);
            return;
        }

        if (cmd.equals("horse")) {
            push("Horse.");
            return;
        }

        if (cmd.equals("selfdestruct")) {
            level.destroyBlock(worldPosition, false);
            ExplosionVNT vnt =
                    new ExplosionVNT(
                            level,
                            worldPosition.getX() + 0.5D,
                            worldPosition.getY() + 0.5D,
                            worldPosition.getZ() + 0.5D,
                            5);
            vnt.setEntityProcessor(new EntityProcessorCrossSmooth(1, 50).setupPiercing(5F, 0.5F));
            vnt.setPlayerProcessor(new PlayerProcessorStandard());
            vnt.setSFX(new ExplosionEffectWeapon(10, 2.5F, 1F));
            vnt.explode();
            return;
        }

        if (cmd.equals("clear")) {
            for (int i = 0; i < history.length; i++) history[i] = "";
            return;
        }

        push("Unrecognized command!");
    }

    public void push(String msg) {
        for (int i = history.length - 2; i > 0; i--) history[i] = history[i - 1];
        history[0] = msg;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.channel = input.getStringOr("channel", "");
        this.repeatCmd = input.getStringOr("repeatCmd", "");
        this.ocMode = input.getBooleanOr("ocMode", false);
        for (int i = 0; i < history.length; i++) history[i] = input.getStringOr("history" + i, "");
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putString("channel", channel);
        output.putString("repeatCmd", repeatCmd);
        output.putBoolean("ocMode", ocMode);
        for (int i = 0; i < history.length; i++) {
            output.putString("history" + i, history[i] == null ? "" : history[i]);
        }
    }

    @Override
    public String[] getFunctionInfo() {
        return new String[] {
            PREFIX_FUNCTION + "clear",
            PREFIX_FUNCTION + "write" + NAME_SEPARATOR + "text",
            PREFIX_FUNCTION + "set<line#>" + NAME_SEPARATOR + "text",
            PREFIX_FUNCTION + "submit" + NAME_SEPARATOR + "command",
        };
    }

    @Override
    public String runRORFunction(String name, String[] params) {
        if ((PREFIX_FUNCTION + "clear").equals(name)) {
            for (int i = 0; i < history.length; i++) history[i] = "";
            markChanged();
            return null;
        }

        String allParams = String.join(" ", params);

        if ((PREFIX_FUNCTION + "write").equals(name)) {
            push(allParams);
            markChanged();
            return null;
        }

        if (name.startsWith(PREFIX_FUNCTION + "set")) {
            int line =
                    IRORInteractive.parseInt(
                                    name.substring((PREFIX_FUNCTION + "set").length()), 1, HISTORY)
                            - 1;
            history[line] = allParams;
            markChanged();
            return null;
        }

        if ((PREFIX_FUNCTION + "submit").equals(name)) {
            eval(allParams);
            return null;
        }

        return null;
    }

    @Override
    public void receiveControl(CompoundTag data) {
        data.getString("cmd")
                .ifPresent(
                        cmd -> {
                            eval(cmd);
                            markChanged();
                        });
    }

    @Override
    public boolean hasPermission(Player player) {
        return player.getEyePosition()
                        .distanceToSqr(
                                worldPosition.getX() + 0.5D,
                                worldPosition.getY() + 0.5D,
                                worldPosition.getZ() + 0.5D)
                < 15 * 15;
    }

    private void writeHistory(ByteBuf output) {
        for (String line : history)
            ByteBufCodecs.STRING_UTF8.encode(output, line == null ? "" : line);
    }

    private void readHistory(ByteBuf input) {
        for (int i = 0; i < history.length; i++)
            history[i] = ByteBufCodecs.STRING_UTF8.decode(input);
    }

    private void writeRepeat(ByteBuf output) {
        output.writeBoolean(!repeatCmd.isEmpty());
    }

    private void readRepeat(ByteBuf input) {
        doesRepeat = input.readBoolean();
    }

    @Override
    public long syncUnitMask() {
        return 0x3L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> writeHistory(output);
            case 1 -> writeRepeat(output);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> readHistory(input);
            case 1 -> readRepeat(input);
            default -> throw new IllegalArgumentException();
        }
    }
}
