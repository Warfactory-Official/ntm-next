// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.handler;

import com.hbm.entity.mob.EntityCyberCrab;
import com.hbm.interfaces.injected.ArmorModWearer;
import com.hbm.inventory.material.Mats;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModItems;
import com.hbm.items.armor.ArmorMods;
import com.hbm.items.armor.ItemArmorMod;
import com.hbm.items.armor.ItemModCloud;
import com.hbm.items.armor.ItemModObsidian;
import com.hbm.items.armor.ItemModRevive;
import com.hbm.lib.Library;
import com.hbm.util.KeyHints;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.spider.CaveSpider;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.gamerules.GameRules;

public final class ArmorModHandler {
    public static final int HELMET_ONLY = 0;
    public static final int PLATE_ONLY = 1;
    public static final int LEGS_ONLY = 2;
    public static final int BOOTS_ONLY = 3;
    public static final int SERVOS = 4;
    public static final int CLADDING = 5;
    public static final int KEVLAR = 6;
    public static final int EXTRA = 7;
    public static final int BATTERY = 8;
    public static final int MOD_SLOTS = 9;

    private static final Identifier[] PIECE_MODIFIERS = {
        Library.id("armor_mod_feet"),
        Library.id("armor_mod_legs"),
        Library.id("armor_mod_chest"),
        Library.id("armor_mod_head")
    };

    public static final List<Holder<Attribute>> MODIFIABLE =
            List.of(
                    Attributes.MAX_HEALTH,
                    Attributes.MOVEMENT_SPEED,
                    Attributes.ATTACK_DAMAGE,
                    Attributes.KNOCKBACK_RESISTANCE);

    private static final Reference2ObjectOpenHashMap<Item, ForeignMod> FOREIGN_MODS =
            new Reference2ObjectOpenHashMap<>();

    private record ForeignMod(int sockets, EquipmentSlot piece) {}

    private ArmorModHandler() {}

    public static void registerForeignMod(Item item, int sockets, EquipmentSlot piece) {
        FOREIGN_MODS.put(item, new ForeignMod(sockets, piece));
    }

    public static boolean isArmor(ItemStack stack) {
        if (stack.isEmpty()
                || stack.getItem() instanceof ItemArmorMod
                || EquipmentProtections.isCladding(stack)
                || FOREIGN_MODS.containsKey(stack.getItem())) return false;
        var equippable = stack.get(DataComponents.EQUIPPABLE);
        return equippable != null
                && switch (equippable.slot()) {
                    case HEAD, CHEST, LEGS, FEET -> true;
                    default -> false;
                };
    }

    public static int modSlot(ItemStack stack) {
        if (EquipmentProtections.isCladding(stack)) return CLADDING;
        if (stack.getItem() instanceof ItemArmorMod mod && mod.type != CLADDING) return mod.type;
        return -1;
    }

    public static boolean fits(ItemStack mod, int socket) {
        ForeignMod foreign = FOREIGN_MODS.get(mod.getItem());
        return foreign == null ? modSlot(mod) == socket : (foreign.sockets & 1 << socket) != 0;
    }

    public static boolean isApplicable(ItemStack armor, ItemStack mod) {
        if (!isArmor(armor)) return false;
        ForeignMod foreign = FOREIGN_MODS.get(mod.getItem());
        if (foreign != null) return armor.get(DataComponents.EQUIPPABLE).slot() == foreign.piece;
        if (EquipmentProtections.isCladding(mod))
            return EquipmentProtections.claddingAllows(
                    mod, armor.get(DataComponents.EQUIPPABLE).slot());
        if (!(mod.getItem() instanceof ItemArmorMod armorMod) || armorMod.type == CLADDING)
            return false;
        return switch (armor.get(DataComponents.EQUIPPABLE).slot()) {
            case HEAD -> armorMod.helmet;
            case CHEST -> armorMod.chestplate;
            case LEGS -> armorMod.leggings;
            case FEET -> armorMod.boots;
            default -> false;
        };
    }

    public static ArmorMods mods(ItemInstance armor) {
        return armor.getOrDefault(ModDataComponents.ARMOR_MODS.get(), ArmorMods.EMPTY);
    }

    public static boolean hasMods(ItemInstance armor) {
        return mods(armor).occupied() != 0;
    }

    public static Item modItem(ItemStack armor, int slot) {
        return mods(armor).item(slot);
    }

    public static void applyMod(ItemStack armor, ItemStack mod) {
        applyMod(armor, mod, modSlot(mod));
    }

    public static void applyMod(ItemStack armor, ItemStack mod, int slot) {
        assert slot >= 0;
        ArmorMods current = mods(armor);
        ArmorMods next = current.with(slot, mod);
        if (next != current) armor.set(ModDataComponents.ARMOR_MODS.get(), next);
    }

    public static void removeMod(ItemStack armor, int slot) {
        ArmorMods current = mods(armor);
        ArmorMods next = current.with(slot, ItemStack.EMPTY);
        if (next == current) return;
        if (next == ArmorMods.EMPTY) armor.remove(ModDataComponents.ARMOR_MODS.get());
        else armor.set(ModDataComponents.ARMOR_MODS.get(), next);
    }

    public static void clearMods(ItemStack armor) {
        armor.remove(ModDataComponents.ARMOR_MODS.get());
    }

    public static ItemStack[] pryMods(ItemStack armor) {
        ArmorMods mods = mods(armor);
        ItemStack[] copies = new ItemStack[MOD_SLOTS];
        for (int i = 0; i < copies.length; i++) copies[i] = mods.copy(i);
        return copies;
    }

    public static ItemStack pryMod(ItemStack armor, int slot) {
        return mods(armor).copy(slot);
    }

    public static void addInstalledModTooltip(
            ItemStack armor, List<Component> tooltip, boolean expanded) {
        if (!isArmor(armor) || !hasMods(armor)) return;
        if (!expanded) {
            tooltip.add(
                    Component.translatable(
                                    "tooltip.hbm.armor.mods_hint",
                                    KeyHints.key(
                                                    "key.hbm.tooltipDetails",
                                                    "key.keyboard.left.shift")
                                            .copy()
                                            .withStyle(
                                                    ChatFormatting.YELLOW, ChatFormatting.ITALIC))
                            .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
            return;
        }
        tooltip.add(
                Component.translatable("desc.item.armorMod.mods").withStyle(ChatFormatting.YELLOW));
        ItemStack[] mods = pryMods(armor);
        for (int i = 0; i < EXTRA + 1; i++) {
            if (EquipmentProtections.claddingTooltip(mods[i])) {
                tooltip.add(
                        Component.translatable(
                                        "desc.item.armorMod.cladding.installed",
                                        mods[i].getHoverName(),
                                        EquipmentProtections.claddingResistance(mods[i]))
                                .withStyle(ChatFormatting.YELLOW));
            } else if (mods[i].getItem() instanceof ItemArmorMod mod)
                mod.addDescription(tooltip, mods[i], armor);
            else if (FOREIGN_MODS.containsKey(mods[i].getItem()))
                tooltip.add(mods[i].getHoverName());
        }
    }

    public static float modifyDamage(LivingEntity entity, DamageSource source, float amount) {
        for (EquipmentSlot armorSlot : ArmorUtil.ARMOR_SLOTS) {
            ItemStack armor = entity.getItemBySlot(armorSlot);
            if (!isArmor(armor)) continue;
            for (int socket = 0; socket < MOD_SLOTS; socket++) {
                ArmorMods before = mods(armor);
                if (before.item(socket) instanceof ItemArmorMod armorMod) {
                    amount = armorMod.modDamage(entity, source, amount, armor);
                    if (mods(armor) != before) equipmentChanged(entity);
                }
            }
        }
        return amount;
    }

    public static boolean tryRevive(LivingEntity entity) {
        for (EquipmentSlot armorSlot : ArmorUtil.ARMOR_SLOTS) {
            ItemStack armor = entity.getItemBySlot(armorSlot);
            if (!isArmor(armor)) continue;
            if (!(modItem(armor, EXTRA) instanceof ItemModRevive)) continue;
            ItemStack mod = pryMod(armor, EXTRA);

            mod.setDamageValue(mod.getDamageValue() + 1);
            if (mod.getDamageValue() >= mod.getMaxDamage()) removeMod(armor, EXTRA);
            else applyMod(armor, mod);
            equipmentChanged(entity);
            entity.setHealth(entity.getMaxHealth());
            entity.addEffect(
                    new MobEffectInstance(
                            MobEffects.RESISTANCE, 3 * SharedConstants.TICKS_PER_SECOND, 99));
            return true;
        }
        return false;
    }

    public static void clientTick(LivingEntity entity) {
        ((ArmorModWearer) entity).hbm$tickClientArmorMods();
    }

    public static void onItemTossed(ItemEntity entity) {
        ItemStack stack = entity.getItem();
        if (isArmor(stack) && modItem(stack, CLADDING) instanceof ItemModObsidian)
            entity.setInvulnerable(true);
    }

    public static void onKilledByPlayer(LivingEntity killed) {
        if (!(killed.level() instanceof ServerLevel level)
                || !level.getGameRules().get(GameRules.MOB_DROPS)) return;
        RandomSource random = killed.getRandom();
        if (killed instanceof Spider && random.nextInt(500) == 0)
            drop(killed, level, ModItems.SPIDER_MILK.get());
        if (killed instanceof CaveSpider && random.nextInt(100) == 0)
            drop(killed, level, ModItems.SERUM.get());
        if (killed instanceof Animal && random.nextInt(500) == 0)
            drop(killed, level, ModItems.BANDAID.get());
        if (killed instanceof Enemy) {
            if (random.nextInt(1000) == 0) drop(killed, level, ModItems.HEART_PIECE.get());
            if (random.nextInt(250) == 0) drop(killed, level, ModItems.KEY_RED_CRACKED.get());
            if (random.nextInt(250) == 0) drop(killed, level, ModItems.LAUNCH_CODE_PIECE.get());
        }
        if (killed instanceof EntityCyberCrab && random.nextInt(500) == 0)
            drop(killed, level, ModItems.WD40.get());
        if (killed instanceof Zombie) {
            if (random.nextInt(200) == 0) drop(killed, level, Items.COPPER_INGOT);
            if (random.nextInt(200) == 0) drop(killed, level, ModItems.ingot(Mats.MAT_ALUMINIUM));
            if (random.nextInt(200) == 0) drop(killed, level, ModItems.ingot(Mats.MAT_TITANIUM));
        }
    }

    public static void onItemSmelted(Player player, ItemStack smelted) {
        if (player.level().isClientSide()) return;
        if (smelted.is(Items.IRON_INGOT)) reward(player, ModItems.LODESTONE.get());
        if (smelted.is(ModItems.ingot(Mats.MAT_URANIUM)))
            reward(player, ModItems.QUARTZ_PLUTONIUM.get());
    }

    private static void reward(Player player, Item prize) {
        if (player.getRandom().nextInt(64) != 0) return;
        ItemStack stack = new ItemStack(prize);
        player.getInventory().placeItemBackInInventory(stack);
    }

    public static void equipmentChanged(LivingEntity entity) {
        ((ArmorModWearer) entity).hbm$reconcileArmorMods();
    }

    public static void tick(LivingEntity entity) {
        ((ArmorModWearer) entity).hbm$tickArmorMods();
    }

    public static boolean reconcileAttributes(
            LivingEntity entity,
            ArmorMods previousMods,
            ArmorMods mods,
            EquipmentSlot declaredSlot,
            int piece) {
        AttributeModifier[] next = new AttributeModifier[MODIFIABLE.size() + 1];
        for (int socket = 0; socket < MOD_SLOTS; socket++) {
            if (!(mods.item(socket) instanceof ItemArmorMod mod) || !mod.hasAttributes()) continue;
            mod.collectModifiers(
                    declaredSlot,
                    PIECE_MODIFIERS[piece],
                    (attribute, modifier) -> {
                        int index = MODIFIABLE.indexOf(attribute);
                        assert index >= 0;
                        if (modifier.id().equals(ItemModCloud.SPEED_ID)) {
                            assert attribute.equals(Attributes.MOVEMENT_SPEED);
                            index = MODIFIABLE.size();
                        } else {
                            assert modifier.id().equals(PIECE_MODIFIERS[piece]);
                        }
                        next[index] = modifier;
                    });
        }
        boolean healthChanged = false;
        for (int i = 0; i < next.length; i++) {
            boolean cloud = i == MODIFIABLE.size();
            if (cloud
                    && next[i] == null
                    && !(previousMods.item(PLATE_ONLY) instanceof ItemModCloud)) continue;
            AttributeInstance instance =
                    entity.getAttribute(cloud ? Attributes.MOVEMENT_SPEED : MODIFIABLE.get(i));
            if (instance == null) continue;
            AttributeModifier previous =
                    instance.getModifier(cloud ? ItemModCloud.SPEED_ID : PIECE_MODIFIERS[piece]);
            if (java.util.Objects.equals(previous, next[i])) continue;
            if (previous != null) instance.removeModifier(previous);
            if (next[i] != null) instance.addTransientModifier(next[i]);
            if (i == 0) healthChanged = true;
        }
        return healthChanged;
    }

    private static void drop(LivingEntity entity, ServerLevel level, Item item) {
        entity.spawnAtLocation(level, new ItemStack(item));
    }
}
