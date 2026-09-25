// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.special;

import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.NTMMaterial;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SignApplicator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.SignBlockEntity;

public final class MaterialDyeItem extends MaterialShapeItem implements SignApplicator {

    public MaterialDyeItem(
            Properties properties, NTMMaterial material, MaterialShapes shape, DyeColor dye) {
        super(properties.component(DataComponents.DYE, dye), material, shape);
    }

    @Override
    public InteractionResult interactLivingEntity(
            ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        return Items.DYE.white().interactLivingEntity(stack, player, target, hand);
    }

    @Override
    public boolean tryApplyToSign(
            Level level, SignBlockEntity sign, boolean front, ItemStack stack, Player player) {
        return ((SignApplicator) Items.DYE.white())
                .tryApplyToSign(level, sign, front, stack, player);
    }
}
