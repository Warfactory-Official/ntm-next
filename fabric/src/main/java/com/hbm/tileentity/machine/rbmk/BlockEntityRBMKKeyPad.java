// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine.rbmk;

import com.hbm.api.control.IControlReceiver;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.packet.SyncBindings;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncSource;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.tileentity.FoldedCoreResident;
import com.hbm.tileentity.GraphResident;
import com.hbm.tileentity.Synced;
import com.hbm.tileentity.network.RTTYSystem;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class BlockEntityRBMKKeyPad extends BlockEntity
        implements Synced, GraphResident, FoldedCoreResident, IControlReceiver, SyncUnitSchema {

    public static final int KEYS = 4;

    public static final int CLICK_TICKS = 7;

    @SyncField(units = 0xfL)
    public final KeyUnit[] keys = new KeyUnit[KEYS];

    public BlockEntityRBMKKeyPad(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RBMK_KEYPAD.get(), pos, state);
        for (int i = 0; i < KEYS; i++) keys[i] = new KeyUnit(this, i);
    }

    public static void tickServer(
            Level level, BlockPos pos, BlockState state, BlockEntityRBMKKeyPad keypad) {
        for (KeyUnit unit : keypad.keys) unit.update(level);
        keypad.networkPackNT(50);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        for (int i = 0; i < KEYS; i++) keys[i].load(input, i);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        for (int i = 0; i < KEYS; i++) keys[i].save(output, i);
    }

    @Override
    public void receiveControl(CompoundTag data) {
        int active = data.getByteOr("active", (byte) 0);
        int polling = data.getByteOr("polling", (byte) 0);
        for (int i = 0; i < KEYS; i++) {
            keys[i].active = (active & (1 << i)) != 0;
            keys[i].polling = (polling & (1 << i)) != 0;
        }

        for (int i = 0; i < KEYS; i++) {
            KeyUnit key = keys[i];
            key.color = Mth.clamp(data.getIntOr("color" + i, 0), 0, 0xffffff);
            key.label = data.getStringOr("label" + i, "");
            key.rtty = data.getStringOr("rtty" + i, "");
            key.command = data.getStringOr("cmd" + i, "");
        }
        setChanged();
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

    public static class KeyUnit implements SyncSource {

        private final BlockEntityRBMKKeyPad panel;

        @SyncField public boolean polling;
        @SyncField public boolean isPressed;
        @SyncField public int color;
        @SyncField public String label = "";
        @SyncField public String rtty = "";
        @SyncField public String command = "";
        @SyncField public boolean active;
        public int clickTimer;

        public KeyUnit(BlockEntityRBMKKeyPad panel, int initialIndex) {
            this.panel = panel;
            if (initialIndex == 0) color = 0xff0000;
            if (initialIndex == 1) color = 0xffff00;
            if (initialIndex == 2) color = 0x0080ff;
            if (initialIndex == 3) color = 0x00ff00;
            label = "Button " + (initialIndex + 1);
        }

        public void click(Level level) {
            if (!active) return;

            if (!polling) {
                if (canSend()) RTTYSystem.broadcast(level, rtty, command);
                isPressed = true;
                clickTimer = CLICK_TICKS;
            } else {
                isPressed = !isPressed;
                panel.setChanged();
            }

            BlockPos pos = panel.getBlockPos();
            level.playSound(
                    null,
                    pos.getX() + 0.5D,
                    pos.getY() + 0.5D,
                    pos.getZ() + 0.5D,
                    SoundEvents.UI_BUTTON_CLICK,
                    SoundSource.BLOCKS,
                    1F,
                    isPressed ? 1F : 0.75F);
        }

        public void update(Level level) {
            if (!active) return;

            if (polling && isPressed) {
                if (canSend()) RTTYSystem.broadcast(level, rtty, command);
            }

            if (!polling && isPressed) {
                if (clickTimer-- <= 0) isPressed = false;
            }
        }

        public boolean canSend() {
            return rtty != null && !rtty.isEmpty() && command != null && !command.isEmpty();
        }

        public void serialize(ByteBuf buf) {
            buf.writeBoolean(active);
            buf.writeBoolean(polling);
            buf.writeBoolean(isPressed);
            buf.writeInt(color);
            ByteBufCodecs.STRING_UTF8.encode(buf, label);
            ByteBufCodecs.STRING_UTF8.encode(buf, rtty);
            ByteBufCodecs.STRING_UTF8.encode(buf, command);
        }

        public void deserialize(ByteBuf buf) {
            active = buf.readBoolean();
            polling = buf.readBoolean();
            isPressed = buf.readBoolean();
            color = buf.readInt();
            label = ByteBufCodecs.STRING_UTF8.decode(buf);
            rtty = ByteBufCodecs.STRING_UTF8.decode(buf);
            command = ByteBufCodecs.STRING_UTF8.decode(buf);
        }

        public void load(ValueInput input, int index) {
            this.active = input.getBooleanOr("active" + index, false);
            this.polling = input.getBooleanOr("polling" + index, false);
            this.isPressed = input.getBooleanOr("isPressed" + index, false);
            this.color = input.getIntOr("color" + index, 0);
            this.label = input.getStringOr("label" + index, "");
            this.rtty = input.getStringOr("rtty" + index, "");
            this.command = input.getStringOr("command" + index, "");
        }

        public void save(ValueOutput output, int index) {
            output.putBoolean("active" + index, active);
            output.putBoolean("polling" + index, polling);
            output.putBoolean("isPressed" + index, isPressed);
            output.putInt("color" + index, color);
            output.putString("label" + index, label);
            output.putString("rtty" + index, rtty);
            output.putString("command" + index, command);
        }
    }

    @Override
    public long syncUnitMask() {
        return 0xfL;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        if (unit < 0 || unit >= keys.length) throw new IllegalArgumentException();
        keys[unit].serialize(output);
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        if (unit < 0 || unit >= keys.length) throw new IllegalArgumentException();
        keys[unit].deserialize(input);
    }

    @Override
    public void bindSyncValue(Object value, int flags, long units) {
        if (value == this.keys) {
            SyncBindings.bindIndexed(this, value, flags, 0, 4, 0L);
            return;
        }
        SyncBindings.bindUnits(this, value, flags, units);
    }

    @Override
    public void syncArrayChanged(Object value, int index, int flags, long units) {
        if (value == keys) {
            long selected = index < 0 ? 0xfL : 1L << index;
            if (index >= 0 && syncBound())
                SyncBindings.bindUnits(this, keys[index], flags, selected);
            syncUnitsChanged(flags, selected);
            return;
        }
        if (units == 0) syncChanged(flags);
        else syncUnitsChanged(flags, units);
    }
}
