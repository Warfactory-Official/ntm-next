// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.generic;

import com.hbm.items.ModItems;
import com.hbm.items.weapon.sedna.factory.GunFactory.EnumAmmo;
import com.hbm.lib.Library;
import com.hbm.sound.ModSounds;
import com.hbm.world.NtmWorldgenFields;
import com.hbm.world.WorldgenHash;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class BlockCrateAmmo extends Block {

    private static final long CONTENTS = WorldgenHash.identifier(Library.id("crate_ammo"));

    private static final EnumAmmo[] SMALL_ARMS = {
        EnumAmmo.P9_SP,
        EnumAmmo.P9_FMJ,
        EnumAmmo.M357_SP,
        EnumAmmo.M357_FMJ,
        EnumAmmo.M44_SP,
        EnumAmmo.M44_FMJ,
        EnumAmmo.R556_SP,
        EnumAmmo.R556_FMJ,
        EnumAmmo.R762_SP,
        EnumAmmo.R762_FMJ,
        EnumAmmo.G12,
        EnumAmmo.G12_SLUG
    };
    private static final EnumAmmo[] ORDNANCE = {EnumAmmo.G40_HE, EnumAmmo.ROCKET_HE};

    public BlockCrateAmmo(Properties props) {
        super(props);
    }

    @Override
    protected InteractionResult useItemOn(
            ItemStack held,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        if (!held.is(ModItems.CROWBAR.get())) return InteractionResult.PASS;
        if (level instanceof ServerLevel server) {
            RandomSource rand = NtmWorldgenFields.get(server).random(CONTENTS, pos);
            popResource(level, pos, new ItemStack(ModItems.CAP_NUKA, 12 + rand.nextInt(21)));
            popResource(
                    level, pos, new ItemStack(ModItems.SYRINGE_METAL_STIMPAK, 1 + rand.nextInt(3)));
            for (EnumAmmo ammo : SMALL_ARMS) {
                if (rand.nextBoolean()) {
                    popResource(
                            level, pos, ModItems.AMMO_STANDARD.stack(ammo, 16 + rand.nextInt(17)));
                }
            }
            for (EnumAmmo ammo : ORDNANCE) {
                if (rand.nextBoolean()) {
                    popResource(
                            level, pos, ModItems.AMMO_STANDARD.stack(ammo, 2 + rand.nextInt(3)));
                }
            }
            if (rand.nextInt(10) == 0)
                popResource(level, pos, new ItemStack(ModItems.SYRINGE_METAL_SUPER, 2));
            level.removeBlock(pos, false);
            level.playSound(null, pos, ModSounds.CRATE_BREAK.get(), SoundSource.BLOCKS, 0.5F, 1.0F);
        }
        return InteractionResult.SUCCESS;
    }
}
