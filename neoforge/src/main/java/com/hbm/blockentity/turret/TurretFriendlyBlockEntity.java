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

package com.hbm.blockentity.turret;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.handler.CasingEjector;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.factory.XFactory556mm;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class TurretFriendlyBlockEntity extends TurretChekhovBlockEntity {

    private static final CasingEjector EJECTOR =
            new CasingEjector().setMotion(-0.3, 0.6, 0).setAngleRange(0.02F, 0.05F);

    public TurretFriendlyBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TURRET_FRIENDLY.get(), pos, state);
    }

    @Override
    protected int cadence() {
        return 5;
    }

    @Override
    protected List<BulletConfig> getAmmoList() {
        return Rounds.R556;
    }

    @Override
    protected @Nullable CasingEjector getEjector() {
        return EJECTOR;
    }

    @Override
    public Component getName() {
        return Component.translatable("container.turretFriendly");
    }

    private static final class Rounds {
        static final List<BulletConfig> R556 =
                List.of(
                        XFactory556mm.r556_sp,
                        XFactory556mm.r556_fmj,
                        XFactory556mm.r556_jhp,
                        XFactory556mm.r556_ap);
    }
}
