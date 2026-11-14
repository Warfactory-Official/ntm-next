/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.blockentity.machine;

import com.hbm.blockentity.MachineBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.handler.MissileStruct;
import com.hbm.inventory.container.MissileAssemblyMenu;
import com.hbm.items.weapon.CustomMissileItem;
import com.hbm.items.weapon.CustomMissilePartItem.FuelType;
import com.hbm.items.weapon.CustomMissilePartItem.PartType;
import com.hbm.items.weapon.CustomMissilePartItem;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.sound.ModSounds;
import com.hbm.sound.WorldSounds;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import org.jspecify.annotations.Nullable;

public final class MissileWorkbenchBlockEntity extends MachineBlockEntity
        implements MenuProvider, SyncUnitSchema {
    public static final int GUIDANCE_SLOT = 0;
    public static final int PAYLOAD_SLOT = 1;
    public static final int BODY_SLOT = 2;
    public static final int STABILIZER_SLOT = 3;
    public static final int PROPULSION_SLOT = 4;
    public static final int PRODUCT_SLOT = 5;
    public static final int INVENTORY_SIZE = 6;

    private static final int[] AUTOMATION_SLOTS = {GUIDANCE_SLOT};
    private static final int PICTURE_UNIT = 0;
    private static final long PICTURE_MASK = 1L;

    private MissileStruct picture;

    {
        hbm$syncSet$MissileWorkbenchBlockEntity$picture(MissileStruct.EMPTY);
    }

    public MissileWorkbenchBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MISSILE_ASSEMBLY.get(), pos, state, INVENTORY_SIZE);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.missileAssembly");
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new MissileAssemblyMenu(containerId, playerInventory, this);
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return AUTOMATION_SLOTS;
    }

    public int guidanceStatus() {
        return hasCategory(componentAt(GUIDANCE_SLOT), PartType.CHIP) ? 1 : 0;
    }

    public int bodyStatus() {
        return hasCategory(componentAt(BODY_SLOT), PartType.FUSELAGE) ? 1 : 0;
    }

    public int payloadStatus() {
        var payload = componentAt(PAYLOAD_SLOT);
        var body = componentAt(BODY_SLOT);
        var propulsion = componentAt(PROPULSION_SLOT);
        if (!hasCategory(payload, PartType.WARHEAD)
                || !hasCategory(body, PartType.FUSELAGE)
                || !hasCategory(propulsion, PartType.THRUSTER)) {
            return 0;
        }

        float mass = (Float) payload.attributes[2];
        float lift = (Float) propulsion.attributes[2];
        return payload.bottom == body.top && mass <= lift ? 1 : 0;
    }

    public int stabilizerStatus() {
        if (getItem(STABILIZER_SLOT).isEmpty()) return -1;
        var stabilizer = componentAt(STABILIZER_SLOT);
        var body = componentAt(BODY_SLOT);
        if (!hasCategory(stabilizer, PartType.FINS) || body == null) return 0;
        return stabilizer.top == body.bottom ? 1 : 0;
    }

    public int propulsionStatus() {
        var propulsion = componentAt(PROPULSION_SLOT);
        var body = componentAt(BODY_SLOT);
        if (!hasCategory(propulsion, PartType.THRUSTER) || !hasCategory(body, PartType.FUSELAGE))
            return 0;

        var bodyFuel = (FuelType) body.attributes[0];
        var propulsionFuel = (FuelType) propulsion.attributes[0];
        return body.bottom == propulsion.top && bodyFuel == propulsionFuel ? 1 : 0;
    }

    public boolean canAssemble() {
        return getItem(PRODUCT_SLOT).isEmpty()
                && guidanceStatus() == 1
                && bodyStatus() == 1
                && payloadStatus() == 1
                && stabilizerStatus() != 0
                && propulsionStatus() == 1;
    }

    public void assembleFromMenu() {
        if (!canAssemble()) return;

        var product =
                CustomMissileItem.buildMissile(
                        getItem(GUIDANCE_SLOT),
                        getItem(PAYLOAD_SLOT),
                        getItem(BODY_SLOT),
                        getItem(STABILIZER_SLOT),
                        getItem(PROPULSION_SLOT));
        for (var slot = GUIDANCE_SLOT; slot < PRODUCT_SLOT; slot++) {
            inventory.set(slot, ItemStack.EMPTY);
        }
        inventory.set(PRODUCT_SLOT, product);
        setChanged();
        WorldSounds.machine(
                level,
                null,
                worldPosition,
                ModSounds.MISSILE_ASSEMBLY2.get(),
                SoundSource.BLOCKS,
                1F,
                1F);
    }

    public MissileStruct visibleParts() {
        return picture;
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (level instanceof ServerLevel) {
            rebuildPicture();
            syncToTracking();
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        rebuildPicture();
    }

    @Override
    public long syncUnitMask() {
        return PICTURE_MASK;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        if (unit != PICTURE_UNIT)
            throw new IllegalArgumentException("Unknown missile workbench sync unit " + unit);
        var buffer = new RegistryFriendlyByteBuf(output, level.registryAccess());
        MissileStruct.STREAM_CODEC.encode(buffer, picture);
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        if (unit != PICTURE_UNIT)
            throw new IllegalArgumentException("Unknown missile workbench sync unit " + unit);
        var buffer = new RegistryFriendlyByteBuf(input, level.registryAccess());
        hbm$syncSet$MissileWorkbenchBlockEntity$picture(
                MissileStruct.STREAM_CODEC.decode(buffer).sanitised());
    }

    private void rebuildPicture() {
        hbm$syncSet$MissileWorkbenchBlockEntity$picture(
                new MissileStruct(
                                inventory.get(PAYLOAD_SLOT),
                                inventory.get(BODY_SLOT),
                                inventory.get(STABILIZER_SLOT),
                                inventory.get(PROPULSION_SLOT))
                        .sanitised());
    }

    private @Nullable CustomMissilePartItem componentAt(int slot) {
        return CustomMissilePartItem.part(getItem(slot));
    }

    private static boolean hasCategory(
            @Nullable CustomMissilePartItem component, PartType category) {
        return component != null && component.type == category;
    }

    public final void hbm$syncSet$MissileWorkbenchBlockEntity$picture(MissileStruct next) {
        if (this.picture != next) {
            boolean bound = syncBound();
            if (bound) com.hbm.packet.SyncBindings.unbind(this, this.picture);
            this.picture = next;
            if (bound) bindSyncValue(next, 3, PICTURE_MASK);
            if ((PICTURE_MASK) == 0L) syncChanged(3);
            else syncUnitsChanged(3, PICTURE_MASK);
        }
    }

    static {
        com.hbm.packet.SyncBindings.field(
                MissileWorkbenchBlockEntity.class, "picture", 3, PICTURE_MASK);
    }
}
