// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.tool;

import com.hbm.extprop.HbmLivingProps;
import com.hbm.sound.ModSounds;
import com.hbm.util.ContaminationUtil;
import com.hbm.util.TickPhase;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class ItemGeigerCounter extends Item {

    public ItemGeigerCounter(Properties props) {
        super(props);
    }

    public static int pickGeiger(double x, RandomSource rand) {
        if (x <= 1e-5) return 0;
        List<Integer> list = new ArrayList<>();
        if (x < 1) list.add(0);
        if (x < 5) list.add(0);
        if (x < 10) list.add(1);
        if (x > 5 && x < 15) list.add(2);
        if (x > 10 && x < 20) list.add(3);
        if (x > 15 && x < 25) list.add(4);
        if (x > 20 && x < 30) list.add(5);
        if (x > 25) list.add(6);
        return list.get(rand.nextInt(list.size()));
    }

    @Override
    public void inventoryTick(
            ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
        if (!(owner instanceof LivingEntity entity)) return;
        if (!TickPhase.every(entity, 5)) return;

        double x = HbmLivingProps.getRadBuf(entity);
        int idx =
                x > 1e-5
                        ? pickGeiger(x, level.getRandom())
                        : (level.getRandom().nextInt(50) == 0 ? 1 : 0);
        if (idx > 0) {
            level.playSound(
                    null,
                    entity.getX(),
                    entity.getY(),
                    entity.getZ(),
                    ModSounds.GEIGER[idx - 1].get(),
                    SoundSource.PLAYERS,
                    1.0F,
                    1.0F);
        }
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide()) {
            level.playSound(
                    null,
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    ModSounds.TECH_BOOP.get(),
                    SoundSource.PLAYERS,
                    1.0F,
                    1.0F);
            ContaminationUtil.printGeigerData(player);
        }
        return InteractionResult.SUCCESS;
    }
}
