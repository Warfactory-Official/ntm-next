// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.neoforge.mixin.storage;

import appeng.api.config.Settings;
import appeng.blockentity.crafting.CraftingBlockEntity;
import appeng.crafting.inv.ListCraftingInventory;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import com.hbm.inventory.InventoryChanges;
import com.hbm.inventory.InventoryOwnerAccess;
import com.hbm.neoforge.inventory.AE2CraftingSlotSource;
import com.mojang.serialization.MapCodec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CraftingBlockEntity.class, remap = false, priority = 500)
public abstract class MixinCraftingBlockEntity implements AE2CraftingSlotSource {
    @Shadow private @Nullable ValueInput previousState;

    @Shadow
    public abstract @Nullable CraftingCPUCluster getCluster();

    @Unique private @Nullable CompoundTag hbm$pendingTag;
    @Unique private @Nullable ListCraftingInventory hbm$pendingInventory;

    @Override
    public @Nullable ListCraftingInventory craftingInventory() {
        return hbm$pendingInventory;
    }

    @Override
    public boolean craftingInventoryResident() {
        return hbm$pendingInventory != null
                && previousState != null
                && inventoryOwner() != null
                && InventoryOwnerAccess.BLOCK.live((BlockEntity) (Object) this);
    }

    @Override
    public void publishCraftingInventory() {
        if (!craftingInventoryResident()) return;
        BlockEntity owner = (BlockEntity) (Object) this;
        var output =
                TagValueOutput.createWithContext(
                        ProblemReporter.DISCARDING, owner.getLevel().registryAccess());
        hbm$pendingInventory.serialize(output.childrenList("inventory"));
        hbm$pendingTag.put("inventory", output.buildResult().get("inventory"));
        previousState =
                TagValueInput.create(
                        ProblemReporter.DISCARDING,
                        owner.getLevel().registryAccess(),
                        hbm$pendingTag);
        owner.setChanged();
    }

    @Inject(method = "setPreviousState", at = @At("RETURN"))
    private void hbm$pendingMaterials(@Nullable ValueInput input, CallbackInfo ci) {
        if (input == null) {
            hbm$pendingTag = null;
            hbm$pendingInventory = null;
        } else {
            hbm$pendingTag = input.read(MapCodec.assumeMapUnsafe(CompoundTag.CODEC)).orElseThrow();
            hbm$pendingInventory = new ListCraftingInventory(this::craftingKeyChanged);
            hbm$pendingInventory.deserialize(input.childrenListOrEmpty("inventory"));
        }
        InventoryChanges.storageLayoutChanged((BlockEntity) (Object) this);
    }

    @Inject(method = "saveAdditional", at = @At("RETURN"))
    private void hbm$savePendingMaterials(ValueOutput output, CallbackInfo ci) {
        if (previousState == null || hbm$pendingInventory == null) return;
        output.putBoolean("core", true);
        hbm$pendingInventory.serialize(output.childrenList("inventory"));
        hbm$pendingTag
                .getCompound("job")
                .ifPresent(job -> output.store("job", CompoundTag.CODEC, job));
        String setting = Settings.CPU_SELECTION_MODE.getName();
        hbm$pendingTag.getString(setting).ifPresent(value -> output.putString(setting, value));
    }
}
