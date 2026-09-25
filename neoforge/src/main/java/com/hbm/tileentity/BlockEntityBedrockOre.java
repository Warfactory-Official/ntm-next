// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.client.StackColor;
import com.hbm.inventory.fluid.FluidStackNTM;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.items.MaterialShapeRoster;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class BlockEntityBedrockOre extends BlockEntity implements Synced, SyncUnitSchema {

    @SyncField(units = 1L)
    public ItemStack resource = ItemStack.EMPTY;

    @SyncField(units = 2L)
    public @Nullable FluidStackNTM acid;

    @SyncField(units = 4L)
    public int tier;

    @SyncField(units = 8L)
    public int color;

    @SyncField(units = 8L)
    public int shape;

    public int tint;
    private int meshShape;

    public BlockEntityBedrockOre(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BEDROCK_ORE.get(), pos, state);
    }

    public void setData(
            ItemStack resource, @Nullable FluidStackNTM acid, int color, int tier, int shape) {
        this.resource = resource;
        this.acid = acid;
        this.color = color;
        this.tier = tier;
        this.shape = shape;
        setChanged();
    }

    private static ItemStack orIronPowder(ItemStack stack) {
        return stack.isEmpty()
                ? new ItemStack(MaterialShapeRoster.require(MaterialShapes.DUST, Mats.MAT_IRON))
                : stack;
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        if (!resource.isEmpty()) out.store("resource", ItemStack.CODEC, resource);
        if (acid != null) {
            out.putString("acid", BuiltInRegistries.FLUID.getKey(acid.type()).toString());
            out.putLong("acid_amount", acid.amount());
        }
        out.putInt("tier", tier);
        out.putInt("color", color);
        out.putInt("shape", shape);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        resource = orIronPowder(in.read("resource", ItemStack.CODEC).orElse(ItemStack.EMPTY));
        String acidName = in.getStringOr("acid", "");
        if (!acidName.isEmpty()) {
            Fluid f = BuiltInRegistries.FLUID.getValue(Identifier.parse(acidName));
            acid =
                    (f != Fluids.EMPTY)
                            ? new FluidStackNTM(f, in.getLongOr("acid_amount", 0))
                            : null;
        } else {
            acid = null;
        }
        tier = in.getIntOr("tier", 0);
        color = in.getIntOr("color", 0);
        shape = in.getIntOr("shape", 0);
    }

    @Override
    public long syncUnitMask() {
        return 15L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 ->
                    ItemStack.OPTIONAL_STREAM_CODEC.encode(
                            new RegistryFriendlyByteBuf(output, level.registryAccess()), resource);
            case 1 -> {
                output.writeInt(acid == null ? -1 : BuiltInRegistries.FLUID.getId(acid.type()));
                if (acid != null) output.writeLong(acid.amount());
            }
            case 2 -> output.writeInt(tier);
            case 3 -> {
                output.writeInt(color);
                output.writeInt(shape);
            }
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 ->
                    resource =
                            orIronPowder(
                                    ItemStack.OPTIONAL_STREAM_CODEC.decode(
                                            new RegistryFriendlyByteBuf(
                                                    input, level.registryAccess())));
            case 1 -> {
                int id = input.readInt();
                if (id < 0) acid = null;
                else {
                    Fluid type = BuiltInRegistries.FLUID.byId(id);
                    long amount = input.readLong();
                    acid = type == Fluids.EMPTY ? null : new FluidStackNTM(type, amount);
                }
            }
            case 2 -> tier = input.readInt();
            case 3 -> {
                color = input.readInt();
                shape = input.readInt();
            }
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void afterSyncUnits(long units) {
        if ((units & 9L) == 0 || !level.isClientSide()) return;
        int oldTint = tint;
        tint = color != 0 ? color : StackColor.amplified(resource);
        if (tint == oldTint && shape == meshShape) return;
        meshShape = shape;
        BlockState state = getBlockState();
        level.sendBlockUpdated(worldPosition, state, state, 0);
    }
}
