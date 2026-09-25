// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.render.item.weapon.sedna;

import com.hbm.items.ModItems;
import com.hbm.items.armor.IPAMelee;
import com.hbm.items.armor.IPAWeaponsProvider;
import com.hbm.registration.RegistryHandle;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class ItemRenderPAMelee extends ItemRenderWeaponBase {

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
    public void setupFirstPerson(ItemStack stack, PoseStack pose) {
        IPAMelee component = IPAWeaponsProvider.getMeleeComponentClient();
        if (component != null) component.setupFirstPerson(stack, pose);
    }

    @Override
    public void renderFirstPerson(
            ItemStack stack, PoseStack pose, SubmitNodeCollector collector, int light) {
        IPAMelee component = IPAWeaponsProvider.getMeleeComponentClient();
        if (component != null) component.renderFirstPerson(stack, pose, collector, light);
    }

    @Override
    public void restBody(ItemStack stack, RestBody body) {}
}
