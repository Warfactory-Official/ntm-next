// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.missile;

import com.hbm.blocks.ModBlocks;
import com.hbm.interfaces.StoredItems;
import com.hbm.items.ModItems;
import com.hbm.tileentity.machine.storage.BlockEntitySoyuzCapsule;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

public class EntitySoyuzCapsule extends Entity implements StoredItems {

    public int soyuz;

    public ItemStack[] payload = new ItemStack[18];

    @Override
    public void visitStoredItems(Visitor visitor) {
        for (int i = 0; i < payload.length; i++) {
            ItemStack stack = payload[i];
            if (stack != null && visitor.visit(stack) && stack.isEmpty())
                payload[i] = ItemStack.EMPTY;
        }
    }

    public EntitySoyuzCapsule(EntityType<? extends EntitySoyuzCapsule> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {}

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public void tick() {
        Vec3 motion = getDeltaMovement();
        if (motion.y > -0.2D) motion = new Vec3(motion.x, motion.y - 0.02D, motion.z);
        this.setDeltaMovement(motion);

        if (getY() > 600) this.setPos(getX(), 600, getZ());

        this.xo = this.xOld = getX();
        this.yo = this.yOld = getY();
        this.zo = this.zOld = getZ();
        this.setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);

        BlockPos pos = BlockPos.containing(getX(), getY(), getZ());

        if (!level().getBlockState(pos).isAir()) {
            this.discard();

            if (!level().isClientSide()) {
                BlockPos above = pos.above();
                level().setBlockAndUpdate(above, ModBlocks.SOYUZ_CAPSULE.get().defaultBlockState());

                if (level().getBlockEntity(above) instanceof BlockEntitySoyuzCapsule capsule) {
                    for (int i = 0; i < payload.length; i++) {
                        capsule.setItem(i, payload[i] == null ? ItemStack.EMPTY : payload[i]);
                    }
                    capsule.setItem(
                            BlockEntitySoyuzCapsule.SLOT_ROCKET,
                            new ItemStack(ModItems.soyuzOfSkin(soyuz).get()));
                }
            }
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 500000;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        soyuz = input.getIntOr("soyuz", soyuz);

        input.read("items", SlotEntry.CODEC.listOf())
                .ifPresent(
                        list -> {
                            for (SlotEntry entry : list) {
                                byte b0 = entry.slot();
                                if (b0 >= 0 && b0 < payload.length) {
                                    payload[b0] = entry.stack();
                                }
                            }
                        });
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putInt("soyuz", soyuz);

        List<SlotEntry> list = new ArrayList<>();
        for (int i = 0; i < payload.length; i++) {
            if (payload[i] != null && !payload[i].isEmpty()) {
                list.add(new SlotEntry((byte) i, payload[i]));
            }
        }
        output.store("items", SlotEntry.CODEC.listOf(), list);
    }

    private record SlotEntry(byte slot, ItemStack stack) {
        private static final Codec<SlotEntry> CODEC =
                RecordCodecBuilder.create(
                        instance ->
                                instance.group(
                                                Codec.BYTE
                                                        .fieldOf("slot")
                                                        .forGetter(SlotEntry::slot),
                                                ItemStack.OPTIONAL_CODEC
                                                        .fieldOf("item")
                                                        .forGetter(SlotEntry::stack))
                                        .apply(instance, SlotEntry::new));
    }
}
