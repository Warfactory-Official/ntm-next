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

package com.hbm.render.item.weapon.sedna;

import com.hbm.items.armor.ArmorPAWeapons;
import com.hbm.items.armor.PowerArmorMeleeWeapon;
import com.hbm.render.item.weapon.sedna.WeaponItemRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public class PAMeleeItemRenderer extends WeaponItemRenderer {
    @Override
    protected float aimZoom(ItemStack stack) {
        return 0F;
    }

    @Override
    protected float getTurnMagnitude(ItemStack stack) {
        return 2.75F;
    }

    @Override
    public boolean isAkimbo(LivingEntity entity) {
        return true;
    }

    @Override
    protected float getSwayMagnitude(ItemStack stack) {
        return 2F;
    }

    @Override
    protected float getSwayPeriod(ItemStack stack) {
        return 0.5F;
    }

    @Override
    public void setupFirstPerson(GunRenderState frame, PoseStack pose) {}

    @Override
    public void renderFirstPerson(
            GunRenderState frame, PoseStack pose, SubmitNodeCollector collector, int light) {
        PowerArmorMeleeWeapon component = ArmorPAWeapons.localMelee();
        if (component != null) component.drawArms(frame.getItemStack(), pose, collector, light);
    }

    @Override
    public GunAnimationChannels rig() {
        return null;
    }

    @Override
    public void restBody(ItemStack stack, RestBody body) {}
}
