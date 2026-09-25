// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
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
import org.jspecify.annotations.Nullable;

public class ItemDosimeter extends Item {

    public ItemDosimeter(Properties props) {
        super(props);
    }

    private static int pickGeiger(double x, RandomSource rand) {
        List<Integer> list = new ArrayList<>();
        if (x < 0.5) list.add(0);
        if (x < 1) list.add(1);
        if (x >= 0.5 && x < 2) list.add(2);

        if (x >= 2) list.add(3);
        return list.isEmpty() ? 0 : list.get(rand.nextInt(list.size()));
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
                        : (level.getRandom().nextInt(100) == 0 ? 1 : 0);
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
            ContaminationUtil.printDosimeterData(player);
        }
        return InteractionResult.SUCCESS;
    }
}
