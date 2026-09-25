// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.armor;

import com.hbm.items.weapon.sedna.ItemGunBaseNT.LambdaContext;
import com.hbm.render.anim.AnimationEnums.GunAnimation;
import com.hbm.render.anim.BusAnimation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.item.ItemStack;

public interface IPAMelee {

    void setupFirstPerson(ItemStack stack, PoseStack pose);

    void renderFirstPerson(
            ItemStack stack, PoseStack pose, SubmitNodeCollector collector, int light);

    BusAnimation playAnim(ItemStack stack, GunAnimation type);

    void orchestra(ItemStack stack, LambdaContext ctx);

    void clickPrimary(ItemStack stack, LambdaContext ctx);

    void clickSecondary(ItemStack stack, LambdaContext ctx);
}
