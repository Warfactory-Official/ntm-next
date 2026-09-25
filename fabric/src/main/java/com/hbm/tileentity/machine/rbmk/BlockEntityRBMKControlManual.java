// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine.rbmk;

import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.machine.rbmk.RBMKControl;
import com.hbm.interfaces.ICopiable;
import com.hbm.inventory.container.MenuRBMKControl;
import com.hbm.packet.SyncField;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class BlockEntityRBMKControlManual extends BlockEntityRBMKControl
        implements MenuProvider, ICopiable, IRORInteractive {

    @SyncField(units = 1L << 8)
    public @Nullable RBMKColor color;

    public double startingLevel;

    public BlockEntityRBMKControlManual(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RBMK_CONTROL.get(), pos, state);
    }

    @Override
    public boolean isModerated() {
        return getBlockState().getBlock() instanceof RBMKControl control && control.moderated;
    }

    @Override
    public void setTarget(double target) {
        this.startingLevel = this.level;
        super.setTarget(target);
    }

    @Override
    public double getMult() {
        if (this.targetLevel < this.startingLevel
                && Math.abs(this.level - this.targetLevel) > 0.01D) {
            return this.level
                    + Math.sin(Math.pow(1D - this.level, 15) * Math.PI)
                            * (this.startingLevel - this.targetLevel)
                            * RBMKConfig.getSurgeMod(getLevel());
        }
        return this.level;
    }

    @Override
    public void receiveControl(CompoundTag data) {
        super.receiveControl(data);
        data.getInt("color")
                .ifPresent(
                        c -> {
                            RBMKColor next =
                                    RBMKColor.VALUES[Math.abs(c) % RBMKColor.VALUES.length];
                            this.color = next == this.color ? null : next;
                        });
        setChanged();
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        int c = input.getIntOr("color", -1);
        this.color = c < 0 ? null : RBMKColor.VALUES[Math.clamp(c, 0, RBMKColor.VALUES.length - 1)];
        this.startingLevel = input.getDoubleOr("startingLevel", this.level);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("color", color == null ? -1 : color.ordinal());
        output.putDouble("startingLevel", startingLevel);
    }

    @Override
    public void writeDiagnostics(CompoundTag tag) {
        super.writeDiagnostics(tag);
        tag.putDouble("startingLevel", startingLevel);
        tag.putDouble("mult", getMult());
        if (color != null) tag.putInt("color", color.ordinal());
    }

    private void writeColor(ByteBuf output) {
        output.writeByte(color == null ? -1 : color.ordinal());
    }

    private void readColor(ByteBuf input) {
        int ordinal = input.readByte();
        if (ordinal < -1 || ordinal >= RBMKColor.VALUES.length)
            throw new DecoderException("Invalid RBMK color");
        color = ordinal < 0 ? null : RBMKColor.VALUES[ordinal];
    }

    @Override
    public RBMKColumnType getConsoleType() {
        return RBMKColumnType.CONTROL;
    }

    @Override
    public RBMKColumn getConsoleData(RBMKColumn reuse) {
        RBMKColumn.ControlColumn data = (RBMKColumn.ControlColumn) super.getConsoleData(reuse);
        data.color = this.color != null ? (short) this.color.ordinal() : -1;
        return data;
    }

    @Override
    public CompoundTag getSettings(Level level, BlockPos pos) {
        CompoundTag data = new CompoundTag();
        if (color != null) data.putInt("color", color.ordinal());
        return data;
    }

    @Override
    public void pasteSettings(
            CompoundTag nbt, int index, Level level, Player player, BlockPos pos) {
        if (nbt.getInt("color").isPresent()) {
            int c = nbt.getIntOr("color", 0);
            color = RBMKColor.VALUES[Math.abs(c % RBMKColor.VALUES.length)];
        } else {
            color = null;
        }
        markChanged();
        networkPackNT(50);
    }

    @Override
    public String[] getFunctionInfo() {
        return new String[] {
            PREFIX_VALUE + "extraction",
            PREFIX_FUNCTION + "setrods" + NAME_SEPARATOR + "percent",
            PREFIX_FUNCTION + "extendrods" + NAME_SEPARATOR + "percent"
        };
    }

    @Override
    public @Nullable String runRORFunction(String name, String[] params) {
        if ((PREFIX_FUNCTION + "setrods").equals(name) && params.length > 0) {
            int percent = IRORInteractive.parseInt(params[0], 0, 100);
            setTarget(percent / 100D);
            setChanged();
            return null;
        }
        if ((PREFIX_FUNCTION + "extendrods").equals(name) && params.length > 0) {
            int percent = IRORInteractive.parseInt(params[0], -100, 100);
            setTarget(targetLevel + percent / 100D);
            setChanged();
            return null;
        }
        return null;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.rbmkControl");
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new MenuRBMKControl(id, inv, this);
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 1L << 8;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 8 -> writeColor(output);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 8 -> readColor(input);
            default -> super.readSyncUnit(unit, input);
        }
    }
}
