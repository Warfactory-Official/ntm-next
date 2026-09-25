// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.weapon;

import com.hbm.items.ModItems;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.factory.GunFactory;
import com.hbm.sound.ModSounds;
import com.hbm.util.I18nUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

public class ItemAmmoContainer extends Item {

    public final boolean constrained;

    public ItemAmmoContainer(Properties properties, boolean constrained) {
        super(properties.stacksTo(1));
        this.constrained = constrained;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack container = player.getItemInHand(hand);

        if (!(level instanceof ServerLevel serverLevel)) return InteractionResult.SUCCESS;

        List<ItemGunBaseNT> guns = new ArrayList<>();
        for (ItemStack carried : player.getInventory().getNonEquipmentItems()) {
            if (!(carried.getItem() instanceof ItemGunBaseNT gun)
                    || gun.defaultAmmoType == null
                    || constrained && gun.isDefaultExpensive) continue;
            guns.add(gun);
        }
        if (guns.isEmpty()) return InteractionResult.SUCCESS;

        Collections.shuffle(guns);
        for (int index = 0; index < Math.min(3, guns.size()); index++) {
            ItemGunBaseNT gun = guns.get(index);
            ItemStack ammo =
                    ModItems.AMMO_STANDARD.stack(
                            (GunFactory.EnumAmmo) gun.defaultAmmoType, gun.defaultAmmoAmount);
            if (constrained) ammo.setCount((int) Math.ceil(ammo.getCount() / 2D));
            player.getInventory().placeItemBackInInventory(ammo);
        }

        serverLevel.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                ModSounds.ITEM_UNPACK.get(),
                SoundSource.PLAYERS,
                1.0F,
                1.0F);
        container.shrink(1);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        for (String line :
                I18nUtil.loreLines(
                        "item.hbm.ammo_container" + (constrained ? ".1" : "") + ".desc")) {
            adder.accept(Component.literal(line).withStyle(ChatFormatting.YELLOW));
        }
    }
}
