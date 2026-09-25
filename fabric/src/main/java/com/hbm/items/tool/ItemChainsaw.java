// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.tool;

import com.hbm.handler.ability.AvailableAbilities;
import com.hbm.items.IAnimatedItem;
import com.hbm.items.ISwingReceiver;
import com.hbm.render.anim.AnimationEnums.ToolAnimation;
import com.hbm.render.anim.BusAnimation;
import com.hbm.render.anim.BusAnimationSequence;
import com.hbm.render.anim.HbmAnimations;
import com.hbm.util.GameTime;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

public class ItemChainsaw extends ItemToolAbilityFueled implements ISwingReceiver, IAnimatedItem {

    public ItemChainsaw(
            Properties properties,
            AvailableAbilities abilities,
            Supplier<Fluid[]> acceptedFuels,
            int maxFuel,
            int consumption,
            int fillRate) {
        super(properties, abilities, true, acceptedFuels, maxFuel, consumption, fillRate);
    }

    @Override
    public void onEntitySwing(ServerPlayer player, ItemStack stack) {
        playAnimation(player, ToolAnimation.SWING);
    }

    @Override
    public @Nullable BusAnimation getAnimation(ToolAnimation type, ItemStack stack) {
        return getAnimation();
    }

    public static @Nullable BusAnimation getAnimation() {
        int forward = 150;
        int sideways = 100;
        int retire = 200;

        HbmAnimations.Animation current = HbmAnimations.getRelevantAnim();
        if (current == null) {
            return new BusAnimation()
                    .addBus(
                            "SWING_ROT",
                            new BusAnimationSequence()
                                    .addPos(0, 0, 90, forward)
                                    .addPos(45, 0, 90, sideways)
                                    .addPos(0, 0, 0, retire))
                    .addBus(
                            "SWING_TRANS",
                            new BusAnimationSequence()
                                    .addPos(0, 0, 3, forward)
                                    .addPos(2, 0, 2, sideways)
                                    .addPos(0, 0, 0, retire));
        }

        double[] rot = HbmAnimations.getRelevantTransformation("SWING_ROT");
        double[] trans = HbmAnimations.getRelevantTransformation("SWING_TRANS");

        if (GameTime.millis() - current.startMillis < 50) return null;

        return new BusAnimation()
                .addBus(
                        "SWING_ROT",
                        new BusAnimationSequence()
                                .addPos(rot[0], rot[1], rot[2], 0)
                                .addPos(0, 0, 90, forward)
                                .addPos(45, 0, 90, sideways)
                                .addPos(0, 0, 0, retire))
                .addBus(
                        "SWING_TRANS",
                        new BusAnimationSequence()
                                .addPos(trans[0], trans[1], trans[2], 0)
                                .addPos(0, 0, 3, forward)
                                .addPos(2, 0, 2, sideways)
                                .addPos(0, 0, 0, retire));
    }
}
