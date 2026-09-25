// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.armor;

import com.hbm.handler.ArmorUtil;
import com.hbm.hazard.HazardClass;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

public class ItemGasMask extends ModArmorItem implements IGasMask {

    private final Type type;

    public ItemGasMask(Properties properties, Type type) {
        this(properties, type, Suit.NONE);
    }

    protected ItemGasMask(Properties properties, Type type, Suit suit) {
        super(properties, suit);
        this.type = type;
    }

    public Type type() {
        return type;
    }

    @Override
    public HazardClass[] filterBlacklist() {
        return type == Type.MONO
                ? new HazardClass[] {
                    HazardClass.GAS_LUNG, HazardClass.GAS_BLISTERING, HazardClass.BACTERIA
                }
                : new HazardClass[] {HazardClass.GAS_BLISTERING};
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {

        if (ArmorUtil.ejectGasMaskFilter(player, player.getItemInHand(hand)))
            return InteractionResult.SUCCESS;
        return super.use(level, player, hand);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        ArmorUtil.addGasMaskTooltip(stack, context, flag, adder);
        ArmorUtil.addGasMaskBlacklist(this, adder);

        super.appendHoverText(stack, context, display, adder, flag);
    }

    public enum Type {
        GAS_MASK,
        M65,
        MONO,
        OLDE
    }
}
