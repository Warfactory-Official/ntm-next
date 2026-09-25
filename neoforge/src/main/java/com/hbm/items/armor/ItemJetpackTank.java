// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.armor;

import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.sound.ModSounds;
import com.hbm.util.I18nUtil;
import java.util.function.Consumer;
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

public class ItemJetpackTank extends Item {

    private static final int CHARGE = 1000;

    public ItemJetpackTank(Properties props) {
        super(props);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        for (String line : I18nUtil.loreLines(getDescriptionId() + ".desc")) {
            adder.accept(Component.literal(line));
        }
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;

        ItemStack pack = ArmorSuitEffects.wornPack(player);
        if (!(pack.getItem() instanceof ItemJetpack jetpack)) return InteractionResult.PASS;

        if (jetpack.fill(pack, NTMFluids.KEROSENE, CHARGE, 0) <= 0) return InteractionResult.PASS;
        ArmorSuitEffects.storeWornPack(player, pack);

        player.getItemInHand(hand).consume(1, player);
        server.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                ModSounds.JETPACK_TANK.get(),
                SoundSource.PLAYERS,
                1F,
                1F);
        return InteractionResult.SUCCESS;
    }
}
