// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.tool;

import com.hbm.NuclearTech;
import com.hbm.capability.NtmContracts;
import com.hbm.interfaces.IBomb.BombReturnCode;
import com.hbm.interfaces.IBomb;
import com.hbm.platform.Services;
import com.hbm.sound.ModSounds;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
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
import net.minecraft.world.phys.Vec3;

public class ItemLaserDetonator extends Item {

    public ItemLaserDetonator(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        HitResult hit = player.pick(500.0D, 1.0F, false);

        if (level.isClientSide()) {
            Vec3 eye = player.getEyePosition();
            Vec3 aim = Vec3.atCenterOf(((BlockHitResult) hit).getBlockPos()).subtract(eye);
            double len = Math.min(aim.length(), 15D);
            aim = aim.normalize();
            for (int i = 0; i < len; i++) {
                double at = level.getRandom().nextDouble() * len + 3;
                level.addParticle(
                        DustParticleOptions.REDSTONE,
                        eye.x + aim.x * at,
                        eye.y + aim.y * at,
                        eye.z + aim.z * at,
                        0,
                        0,
                        0);
            }
        }

        BlockPos aimed =
                hit instanceof BlockHitResult b && b.getType() == HitResult.Type.BLOCK
                        ? ItemDetonator.bombTarget(level, b.getBlockPos())
                        : null;
        IBomb bomb = aimed == null ? null : NtmContracts.BOMB.at(level, aimed);
        if (bomb != null) {
            if (!level.isClientSide()) {
                level.playSound(
                        null,
                        player.getX(),
                        player.getY(),
                        player.getZ(),
                        ModSounds.TECH_BLEEP.get(),
                        SoundSource.PLAYERS,
                        1.0F,
                        1.0F);
                BombReturnCode ret = bomb.explode(level, aimed, player);
                if (Services.CONFIG.runtime().extendedLogging()) {
                    NuclearTech.LOGGER.info(
                            "[DET] Tried to detonate block at {} / {} / {} by {}!",
                            aimed.getX(),
                            aimed.getY(),
                            aimed.getZ(),
                            player.getDisplayName().getString());
                }
                player.sendSystemMessage(
                        Component.translatable(ret.getUnlocalizedMessage())
                                .withStyle(
                                        ret.wasSuccessful()
                                                ? ChatFormatting.YELLOW
                                                : ChatFormatting.RED));
            }
            return InteractionResult.SUCCESS;
        }
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
            player.sendSystemMessage(
                    Component.translatable(BombReturnCode.ERROR_NO_BOMB.getUnlocalizedMessage())
                            .withStyle(ChatFormatting.RED));
        }
        return InteractionResult.FAIL;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        adder.accept(Component.translatable("item.hbm.detonator_laser.desc"));
    }
}
