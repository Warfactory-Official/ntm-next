/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: HbmMods (The Bobcat)
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) HbmMods (The Bobcat). All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only,
 * as published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this code. If not, see <https://www.gnu.org/licenses/>.
 *
 * Under GPLv3 §7(c) and §7(e), these additional terms apply to LGPL-covered
 * material authored by movblock:
 *
 * - the license grants no rights to the name "NTM NEXT";
 * - a modified version must not be named NTM NEXT or presented as the
 *   official release, and must be marked as different from it.
 *
 * Accurately identifying a work as derived from NTM NEXT does not violate
 * these additional terms, provided it is clearly distinguished from the
 * official release and does not imply endorsement. Ports are encouraged to
 * acknowledge NTM NEXT as their upstream project and provide a link to it.
 */

package com.hbm.blockentity.machine.storage;

import com.hbm.util.RegistryUtil;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public enum CrateType {
    IRON(
            "crate_iron",
            "container.crateIron",
            36,
            9,
            4,
            8,
            18,
            8,
            104,
            162,
            176,
            186,
            8,
            0x404040,
            0x404040),
    STEEL(
            "crate_steel",
            "container.crateSteel",
            54,
            9,
            6,
            8,
            18,
            8,
            140,
            198,
            176,
            222,
            8,
            0x1C1C1C,
            0x1C1C1C),
    DESH(
            "crate_desh",
            "container.crateDesh",
            104,
            13,
            8,
            8,
            18,
            44,
            174,
            232,
            248,
            256,
            44,
            0x3F1515,
            0x3F1515),
    TUNGSTEN(
            "crate_tungsten",
            "container.crateTungsten",
            27,
            9,
            3,
            8,
            18,
            8,
            86,
            144,
            176,
            168,
            8,
            0xFFFFFF,
            0xFFFFFF) {
        @Override
        public CrateBlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new CrateTungstenBlockEntity(pos, state);
        }
    },
    SAFE("safe", "container.safe", 15, 5, 3, 44, 18, 8, 86, 144, 176, 168, 8, 0x404040, 0x404040);

    public final String id;
    public final String containerKey;
    public final int slots;
    public final int columns;
    public final int rows;
    public final int crateX;
    public final int crateY;
    public final int playerInventoryX;
    public final int playerInventoryY;
    public final int hotbarY;
    public final int guiWidth;
    public final int guiHeight;
    public final int inventoryLabelX;
    public final int titleColor;
    public final int inventoryLabelColor;
    public final Identifier texture;

    private Supplier<? extends BlockEntityType<? extends CrateBlockEntity>> typeSupplier;

    CrateType(
            String id,
            String containerKey,
            int slots,
            int columns,
            int rows,
            int crateX,
            int crateY,
            int playerInventoryX,
            int playerInventoryY,
            int hotbarY,
            int guiWidth,
            int guiHeight,
            int inventoryLabelX,
            int titleColor,
            int inventoryLabelColor) {
        this.id = id;
        this.containerKey = containerKey;
        this.slots = slots;
        this.columns = columns;
        this.rows = rows;
        this.crateX = crateX;
        this.crateY = crateY;
        this.playerInventoryX = playerInventoryX;
        this.playerInventoryY = playerInventoryY;
        this.hotbarY = hotbarY;
        this.guiWidth = guiWidth;
        this.guiHeight = guiHeight;
        this.inventoryLabelX = inventoryLabelX;
        this.titleColor = titleColor;
        this.inventoryLabelColor = inventoryLabelColor;
        this.texture = RegistryUtil.id("textures/gui/storage/gui_" + id + ".png");
    }

    public void setTypeSupplier(
            Supplier<? extends BlockEntityType<? extends CrateBlockEntity>> typeSupplier) {
        this.typeSupplier = typeSupplier;
    }

    public BlockEntityType<? extends CrateBlockEntity> blockEntityType() {
        return typeSupplier.get();
    }

    public CrateBlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CrateBlockEntity(this, pos, state);
    }
}
