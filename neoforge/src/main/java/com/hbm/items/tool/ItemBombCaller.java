// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.tool;

import com.hbm.entity.logic.EntityBomber;
import com.hbm.sound.ModSounds;
import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class ItemBombCaller extends Item {

    public final EnumCallerType type;

    public ItemBombCaller(Properties properties, EnumCallerType type) {
        super(properties);
        this.type = type;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        HitResult hit = player.pick(500D, 1F, false);
        if (!(hit instanceof BlockHitResult blockHit)) return InteractionResult.PASS;
        if (!level.isClientSide()) {
            double x = blockHit.getBlockPos().getX();
            double y = blockHit.getBlockPos().getY();
            double z = blockHit.getBlockPos().getZ();
            EntityBomber bomber =
                    switch (type) {
                        case CARPET_BOMBING -> EntityBomber.statFacCarpet(level, x, y, z);
                        case NAPALM -> EntityBomber.statFacNapalm(level, x, y, z);
                        case POISON_GAS -> EntityBomber.statFacChlorine(level, x, y, z);
                        case AGENT_ORANGE -> EntityBomber.statFacOrange(level, x, y, z);
                        case ATOMIC_BOMB -> EntityBomber.statFacABomb(level, x, y, z);
                        case VT_STINGER_ROCKETS -> EntityBomber.statFacStinger(level, x, y, z);
                        case PIP_OH_GOD -> EntityBomber.statFacBoxcar(level, x, y, z);
                        case CLOUD_THE_CLOUD -> EntityBomber.statFacPC(level, x, y, z);
                    };
            level.addFreshEntity(bomber);
            player.sendSystemMessage(
                    Component.translatable("desc.item.bombCaller.calledInAirstrike"));
            level.playSound(
                    null,
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    ModSounds.TECH_BLEEP.get(),
                    SoundSource.PLAYERS,
                    1F,
                    1F);
        }
        stack.shrink(1);
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return type.ordinal() >= EnumCallerType.ATOMIC_BOMB.ordinal();
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        adder.accept(Component.translatable("desc.item.bombCaller.aimClickToCall"));
        adder.accept(
                Component.translatable("desc.shared.type", Component.translatable(type.key())));
    }

    public enum EnumCallerType {
        CARPET_BOMBING,
        NAPALM,
        POISON_GAS,
        AGENT_ORANGE,
        ATOMIC_BOMB,
        VT_STINGER_ROCKETS,
        PIP_OH_GOD,
        CLOUD_THE_CLOUD;

        public String key() {
            String[] words = name().toLowerCase(Locale.ROOT).split("_");
            StringBuilder key = new StringBuilder("desc.item.bombCaller.").append(words[0]);
            for (int i = 1; i < words.length; i++) {
                key.append(Character.toUpperCase(words[i].charAt(0))).append(words[i].substring(1));
            }
            return key.toString();
        }
    }
}
