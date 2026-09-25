// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.armor;

import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.handler.ArmorModHandler;
import com.hbm.items.ModItems;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.factory.XFactory12ga;
import com.hbm.particle.helper.ParticleCreators;
import com.hbm.sound.ModSounds;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class ItemModTwoKick extends ItemArmorMod {

    public ItemModTwoKick(Properties properties) {
        super(properties, ArmorModHandler.SERVOS, false, true, false, false);
    }

    public static void onPunch(Player player) {
        if (!(player.level() instanceof ServerLevel level)) return;
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (chest.isEmpty() || !ArmorModHandler.hasMods(chest)) return;
        ItemStack held = player.getMainHandItem();
        if (carriesAttackDamage(held)) return;
        if (!ArmorModHandler.pryMod(chest, ArmorModHandler.SERVOS)
                .is(ModItems.BALLISTIC_GAUNTLET.get())) return;

        BulletConfig[] configs = {
            XFactory12ga.g12_bp,
            XFactory12ga.g12_bp_magnum,
            XFactory12ga.g12_bp_slug,
            XFactory12ga.g12,
            XFactory12ga.g12_slug,
            XFactory12ga.g12_flechette,
            XFactory12ga.g12_magnum,
            XFactory12ga.g12_explosive,
            XFactory12ga.g12_phosphorus
        };
        BulletConfig chosen = null;
        for (BulletConfig config : configs) {
            if (consumeShell(player.getInventory(), config)) {
                chosen = config;
                break;
            }
        }
        if (chosen == null) return;
        int projectiles = chosen.projectilesMin;
        if (chosen.projectilesMax > chosen.projectilesMin) {
            projectiles +=
                    player.getRandom().nextInt(chosen.projectilesMax - chosen.projectilesMin);
        }
        for (int index = 0; index < projectiles; index++) {
            EntityBulletBaseMK4 bullet =
                    new EntityBulletBaseMK4(player, chosen, 15F, 0F, -0.1875, -0.0625, 0.5);
            level.addFreshEntity(bullet);
            if (index == 0 && chosen.blackPowder) {
                var motion = bullet.getDeltaMovement();
                ParticleCreators.blackPowder(
                        level,
                        bullet.getX(),
                        bullet.getY(),
                        bullet.getZ(),
                        motion.x,
                        motion.y,
                        motion.z,
                        10,
                        0.25F,
                        0.5F,
                        10,
                        0.25F);
            }
        }
        level.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                ModSounds.GUN_SPAS_FIRE.get(),
                SoundSource.PLAYERS,
                1F,
                1F);
    }

    private static boolean carriesAttackDamage(ItemStack held) {
        boolean[] found = {false};
        for (EquipmentSlot slot : EquipmentSlot.VALUES) {
            held.forEachModifier(
                    slot,
                    (attribute, modifier) -> {
                        if (attribute.value() == Attributes.ATTACK_DAMAGE.value()) found[0] = true;
                    });
        }
        return found[0];
    }

    private static boolean consumeShell(Inventory inventory, BulletConfig config) {
        for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (config.matchesAmmo(stack)) {
                stack.shrink(1);
                if (stack.isEmpty()) inventory.setItem(slot, ItemStack.EMPTY);
                inventory.setChanged();
                return true;
            }
        }
        return false;
    }

    @Override
    protected void addSpecificTooltip(ItemStack stack, Consumer<Component> adder) {
        adder.accept(
                Component.translatable("desc.item.armorMod.twoKick.1")
                        .withStyle(ChatFormatting.ITALIC));
        adder.accept(
                Component.translatable("desc.item.armorMod.twoKick.2")
                        .withStyle(ChatFormatting.YELLOW));
    }

    @Override
    public void addDescription(List<Component> tooltip, ItemStack stack, ItemStack armor) {
        tooltip.add(
                Component.translatable("desc.item.armorMod.twoKick.installed", stack.getHoverName())
                        .withStyle(ChatFormatting.YELLOW));
    }
}
