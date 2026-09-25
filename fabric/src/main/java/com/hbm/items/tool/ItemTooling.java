// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.tool;

import com.hbm.capability.NtmContracts;
import com.hbm.interfaces.IToolable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public class ItemTooling extends ItemCraftingDegradation implements IToolable.Tool {

    protected final IToolable.ToolType type;

    public ItemTooling(Item.Properties properties, IToolable.ToolType type) {
        super(properties);
        this.type = type;
    }

    public static Item.Properties weaponProperties(int durability, float attackDamage) {
        return new Item.Properties()
                .stacksTo(1)
                .durability(durability)
                .attributes(ItemToolAbility.attributes(attackDamage, 0));
    }

    @Override
    public IToolable.ToolType toolType() {
        return type;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        IToolable toolable = NtmContracts.TOOLABLE.at(level, pos);
        if (toolable == null) return InteractionResult.PASS;

        if (!toolable.onScrew(
                level,
                ctx.getPlayer(),
                pos,
                ctx.getClickedFace(),
                ctx.getClickLocation(),
                this.type)) {
            return InteractionResult.PASS;
        }

        ItemStack stack = ctx.getItemInHand();
        Player player = ctx.getPlayer();
        if (!level.isClientSide() && stack.isDamageableItem() && player != null) {
            stack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
        }
        return InteractionResult.SUCCESS;
    }
}
