// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.network;

import com.hbm.api.control.IControlReceiver;
import com.hbm.api.redstoneoverradio.IRORInfo;
import com.hbm.blocks.network.RadioTorchBlock;
import com.hbm.capability.ContractLink;
import com.hbm.capability.NtmContracts.Contract;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.EntityProcessorCrossSmooth;
import com.hbm.explosion.vanillant.standard.ExplosionEffectWeapon;
import com.hbm.explosion.vanillant.standard.PlayerProcessorStandard;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.tileentity.FoldedCoreResident;
import com.hbm.tileentity.GraphResident;
import com.hbm.tileentity.Synced;
import io.netty.buffer.ByteBuf;
import java.util.Arrays;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public abstract class BlockEntityRadioTorch extends BlockEntity
        implements Synced, GraphResident, FoldedCoreResident, IControlReceiver, SyncUnitSchema {

    @SyncField(units = 1L << 2)
    public final String[] mapping = new String[16];

    @SyncField(units = 1L << 1)
    public String channel = "";

    public int lastState;
    public long lastUpdate;

    @SyncField(units = 1L)
    public boolean polling;

    @SyncField(units = 1L << 3)
    public boolean customMap;

    private final @Nullable ContractLink<?> attachment;

    protected BlockEntityRadioTorch(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        this(type, pos, state, null);
    }

    protected BlockEntityRadioTorch(
            BlockEntityType<?> type,
            BlockPos pos,
            BlockState state,
            @Nullable Contract<?> attachment) {
        super(type, pos, state);
        this.attachment = attachment == null ? null : new ContractLink<>(attachment);
        Arrays.fill(mapping, "");
    }

    protected static void writeString(ByteBuf buffer, String value) {
        ByteBufCodecs.STRING_UTF8.encode(buffer, value);
    }

    protected static String readString(ByteBuf buffer) {
        return ByteBufCodecs.STRING_UTF8.decode(buffer);
    }

    public static BlockEntityRadioTorch create(
            RadioTorchBlock.Kind kind, BlockPos pos, BlockState state) {
        return switch (kind) {
            case SENDER -> new BlockEntityRadioTorchSender(pos, state);
            case RECEIVER -> new BlockEntityRadioTorchReceiver(pos, state);
            case COUNTER -> new BlockEntityRadioTorchCounter(pos, state);
            case LOGIC -> new BlockEntityRadioTorchLogic(pos, state);
            case READER -> new BlockEntityRadioTorchReader(pos, state);
            case CONTROLLER -> new BlockEntityRadioTorchController(pos, state);
        };
    }

    protected Direction supportDirection() {
        return getBlockState().getValue(RadioTorchBlock.FACING).getOpposite();
    }

    protected final <T> @Nullable T attached(Class<T> type) {
        if (level == null || attachment == null) return null;
        Object value = attachment.get(level, worldPosition.relative(supportDirection()));
        return type.isInstance(value) ? type.cast(value) : null;
    }

    public final @Nullable IRORInfo attachedInfo() {
        return attached(IRORInfo.class);
    }

    public void refreshAttached() {
        if (level != null && attachment != null) {
            attachment.refresh(level, worldPosition.relative(supportDirection()));
        }
    }

    protected void setState(int state) {
        setState(state, true);
    }

    protected void setState(int state, boolean notifyNeighbors) {
        if (lastState == state) return;
        lastState = state;
        BlockState blockState = getBlockState();
        level.setBlock(
                worldPosition,
                blockState.setValue(RadioTorchBlock.LIT, state > 0),
                notifyNeighbors ? Block.UPDATE_ALL : Block.UPDATE_CLIENTS);
        setChanged();
    }

    public void tickServer() {
        networkPackNT(50);
    }

    protected final void destroyWithExplosion() {
        double x = worldPosition.getX() + 0.5D;
        double y = worldPosition.getY() + 0.5D;
        double z = worldPosition.getZ() + 0.5D;
        level.destroyBlock(worldPosition, false);
        ExplosionVNT explosion = new ExplosionVNT(level, x, y, z, 5F, null);
        explosion.setEntityProcessor(
                new EntityProcessorCrossSmooth(1D, 50F).setupPiercing(5F, 0.5F));
        explosion.setPlayerProcessor(new PlayerProcessorStandard());
        explosion.setSFX(new ExplosionEffectWeapon(10, 2.5F, 1F));
        explosion.explode();
    }

    @Override
    protected final void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        if (attachment != null) attachment.load(input, "attachment");
        loadRadio(input);
    }

    @Override
    protected final void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (attachment != null) attachment.save(output, "attachment");
        saveRadio(output);
    }

    protected abstract void loadRadio(ValueInput input);

    protected abstract void saveRadio(ValueOutput output);

    @Override
    public final void receiveControl(CompoundTag data) {
        receiveRadioControl(data);
        setChanged();
    }

    protected abstract void receiveRadioControl(CompoundTag data);

    @Override
    public boolean hasPermission(Player player) {
        return player.getEyePosition()
                        .distanceToSqr(
                                worldPosition.getX() + 0.5D,
                                worldPosition.getY() + 0.5D,
                                worldPosition.getZ() + 0.5D)
                < 256D;
    }

    protected final void loadMapped(ValueInput input) {
        polling = input.getBooleanOr("p", false);
        customMap = input.getBooleanOr("m", false);
        lastState = input.getIntOr("l", 0);
        lastUpdate = input.getLongOr("u", 0L);
        channel = input.getStringOr("c", "");
        for (int i = 0; i < mapping.length; i++) mapping[i] = input.getStringOr("m" + i, "");
    }

    protected final void saveMapped(ValueOutput output) {
        output.putBoolean("p", polling);
        output.putBoolean("m", customMap);
        output.putInt("l", lastState);
        output.putLong("u", lastUpdate);
        output.putString("c", channel);
        for (int i = 0; i < mapping.length; i++) output.putString("m" + i, mapping[i]);
    }

    protected final void receiveMapped(CompoundTag data) {
        if (data.contains("p")) polling = data.getBooleanOr("p", polling);
        if (data.contains("m")) customMap = data.getBooleanOr("m", customMap);
        if (data.contains("c")) channel = data.getStringOr("c", channel);
        for (int i = 0; i < mapping.length; i++) {
            if (data.contains("m" + i)) mapping[i] = data.getStringOr("m" + i, mapping[i]);
        }
    }

    private void writeChannel(ByteBuf output) {
        writeString(output, channel);
    }

    private void readChannel(ByteBuf input) {
        channel = readString(input);
    }

    private void writeMapping(ByteBuf output) {
        for (String value : mapping) writeString(output, value);
    }

    private void readMapping(ByteBuf input) {
        for (int i = 0; i < mapping.length; i++) mapping[i] = readString(input);
    }

    @Override
    public long syncUnitMask() {
        return 0xfL;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeBoolean(this.polling);
            case 1 -> writeChannel(output);
            case 2 -> writeMapping(output);
            case 3 -> output.writeBoolean(this.customMap);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.polling = input.readBoolean();
            case 1 -> readChannel(input);
            case 2 -> readMapping(input);
            case 3 -> this.customMap = input.readBoolean();
            default -> throw new IllegalArgumentException();
        }
    }
}
