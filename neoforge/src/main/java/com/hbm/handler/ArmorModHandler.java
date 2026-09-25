// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.handler;

import com.hbm.extprop.HbmLivingProps;
import com.hbm.hazard.HazardSystem;
import com.hbm.inventory.material.Mats;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModItems;
import com.hbm.items.armor.ItemArmorMod;
import com.hbm.items.armor.ItemModDefuser;
import com.hbm.items.armor.ItemModObsidian;
import com.hbm.items.armor.ItemModRevive;
import com.hbm.lib.Library;
import com.hbm.platform.Services;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.spider.CaveSpider;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
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

    public static final String MOD_COMPOUND_KEY = "ntm_armor_mods";
    public static final String MOD_SLOT_KEY = "mod_slot_";

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

    private ArmorModHandler() {}

    public static void init() {
        Services.SERVER.onServerTickPost(ArmorModHandler::onServerTick);
    }

    public static boolean isArmor(ItemStack stack) {

        if (stack.isEmpty() || stack.getItem() instanceof ItemArmorMod) return false;
        var equippable = stack.get(DataComponents.EQUIPPABLE);
        return equippable != null
                && switch (equippable.slot()) {
                    case HEAD, CHEST, LEGS, FEET -> true;
                    default -> false;
                };
    }

    public static boolean isApplicable(ItemStack armor, ItemStack mod) {
        if (!isArmor(armor) || !(mod.getItem() instanceof ItemArmorMod armorMod)) return false;
        return switch (armor.get(DataComponents.EQUIPPABLE).slot()) {
            case HEAD -> armorMod.helmet;
            case CHEST -> armorMod.chestplate;
            case LEGS -> armorMod.leggings;
            case FEET -> armorMod.boots;
            default -> false;
        };
    }

    public static boolean hasMods(ItemStack armor) {
        CustomData data = armor.get(ModDataComponents.PERSISTENT_DATA.get());
        return data != null && data.copyTag().contains(MOD_COMPOUND_KEY);
    }

    public static void applyMod(ItemStack armor, ItemStack mod) {
        ItemArmorMod armorMod = (ItemArmorMod) mod.getItem();
        Tag encoded =
                ItemStack.CODEC.encodeStart(NbtOps.INSTANCE, mod.copyWithCount(1)).getOrThrow();
        CustomData.update(
                ModDataComponents.PERSISTENT_DATA.get(),
                armor,
                root -> {
                    CompoundTag mods = root.getCompoundOrEmpty(MOD_COMPOUND_KEY);
                    mods.put(MOD_SLOT_KEY + armorMod.type, encoded);
                    root.put(MOD_COMPOUND_KEY, mods);
                });
    }

    public static void removeMod(ItemStack armor, int slot) {
        CustomData data = armor.get(ModDataComponents.PERSISTENT_DATA.get());
        if (data == null) return;
        CustomData.update(
                ModDataComponents.PERSISTENT_DATA.get(),
                armor,
                root -> {
                    CompoundTag mods = root.getCompoundOrEmpty(MOD_COMPOUND_KEY);
                    mods.remove(MOD_SLOT_KEY + slot);
                    if (mods.isEmpty()) root.remove(MOD_COMPOUND_KEY);
                    else root.put(MOD_COMPOUND_KEY, mods);
                });
    }

    public static void clearMods(ItemStack armor) {
        if (armor.get(ModDataComponents.PERSISTENT_DATA.get()) != null) {
            CustomData.update(
                    ModDataComponents.PERSISTENT_DATA.get(),
                    armor,
                    root -> root.remove(MOD_COMPOUND_KEY));
        }
    }

    public static ItemStack[] pryMods(ItemStack armor) {
        ItemStack[] slots = new ItemStack[MOD_SLOTS];
        Arrays.fill(slots, ItemStack.EMPTY);
        CustomData data = armor.get(ModDataComponents.PERSISTENT_DATA.get());
        if (data == null) return slots;

        CompoundTag root = data.copyTag();
        CompoundTag mods = root.getCompoundOrEmpty(MOD_COMPOUND_KEY);
        boolean dirty = false;
        for (int i = 0; i < MOD_SLOTS; i++) {
            String key = MOD_SLOT_KEY + i;
            Tag encoded = mods.get(key);
            if (encoded == null) continue;
            ItemStack mod =
                    ItemStack.CODEC
                            .parse(NbtOps.INSTANCE, encoded)
                            .result()
                            .filter(stack -> stack.getItem() instanceof ItemArmorMod)
                            .orElse(ItemStack.EMPTY);
            if (mod.isEmpty()) {
                mods.remove(key);
                dirty = true;
            } else {
                slots[i] = mod;
            }
        }
        if (dirty) {
            if (mods.isEmpty()) root.remove(MOD_COMPOUND_KEY);
            else root.put(MOD_COMPOUND_KEY, mods);
            CustomData.set(ModDataComponents.PERSISTENT_DATA.get(), armor, root);
        }
        return slots;
    }

    public static void setFlag(ItemStack armor, String key) {
        CustomData.update(
                ModDataComponents.PERSISTENT_DATA.get(), armor, root -> root.putBoolean(key, true));
    }

    public static boolean hasFlag(ItemStack armor, String key) {
        CustomData data = armor.get(ModDataComponents.PERSISTENT_DATA.get());
        return data != null && data.copyTag().getBooleanOr(key, false);
    }

    public static void clearFlag(ItemStack armor, String key) {
        if (armor.get(ModDataComponents.PERSISTENT_DATA.get()) != null)
            CustomData.update(
                    ModDataComponents.PERSISTENT_DATA.get(), armor, root -> root.remove(key));
    }

    public static ItemStack pryMod(ItemStack armor, int slot) {
        return pryMods(armor)[slot];
    }

    public static void addInstalledModTooltip(
            ItemStack armor, List<Component> tooltip, boolean expanded) {
        if (!isArmor(armor) || !hasMods(armor)) return;
        if (!expanded) {

            tooltip.add(
                    Component.translatable(
                                    "desc.misc.lshift",
                                    Component.translatable("desc.item.armorMod.display"))
                            .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
            return;
        }
        tooltip.add(
                Component.translatable("desc.item.armorMod.mods").withStyle(ChatFormatting.YELLOW));
        ItemStack[] mods = pryMods(armor);
        for (int i = 0; i < EXTRA + 1; i++) {
            if (mods[i].getItem() instanceof ItemArmorMod mod)
                mod.addDescription(tooltip, mods[i], armor);
        }
    }

    public static float modifyDamage(LivingEntity entity, DamageSource source, float amount) {
        for (EquipmentSlot armorSlot : ArmorUtil.ARMOR_SLOTS) {
            ItemStack armor = entity.getItemBySlot(armorSlot);
            if (!isArmor(armor)) continue;
            for (ItemStack mod : pryMods(armor)) {
                if (mod.getItem() instanceof ItemArmorMod armorMod) {
                    amount = armorMod.modDamage(entity, source, amount, armor);
                }
            }
        }
        return amount;
    }

    public static boolean tryRevive(LivingEntity entity) {
        for (EquipmentSlot armorSlot : ArmorUtil.ARMOR_SLOTS) {
            ItemStack armor = entity.getItemBySlot(armorSlot);
            if (!isArmor(armor)) continue;
            ItemStack mod = pryMod(armor, EXTRA);
            if (!(mod.getItem() instanceof ItemModRevive)) continue;

            mod.setDamageValue(mod.getDamageValue() + 1);
            if (mod.getDamageValue() >= mod.getMaxDamage()) removeMod(armor, EXTRA);
            else applyMod(armor, mod);
            entity.setHealth(entity.getMaxHealth());
            entity.addEffect(
                    new MobEffectInstance(
                            MobEffects.RESISTANCE, 3 * SharedConstants.TICKS_PER_SECOND, 99));
            return true;
        }
        return false;
    }

    public static void clientTick(LivingEntity entity) {
        for (EquipmentSlot armorSlot : ArmorUtil.ARMOR_SLOTS) {
            ItemStack armor = entity.getItemBySlot(armorSlot);
            if (!isArmor(armor)) continue;
            for (ItemStack mod : pryMods(armor)) {
                if (mod.getItem() instanceof ItemArmorMod armorMod)
                    armorMod.modUpdateClient(entity, armor);
            }
        }
    }

    public static void onItemTossed(ItemEntity entity) {
        ItemStack stack = entity.getItem();
        if (isArmor(stack) && pryMod(stack, CLADDING).getItem() instanceof ItemModObsidian)
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
        if (killed instanceof Enemy && random.nextInt(1000) == 0)
            drop(killed, level, ModItems.HEART_PIECE.get());
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

    private static void onServerTick(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            List<LivingEntity> living = new ArrayList<>();
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof LivingEntity target) living.add(target);
            }
            for (LivingEntity entity : living) {
                if (!entity.isRemoved()) update(entity);
            }
        }
    }

    public static void update(LivingEntity entity) {
        if (entity instanceof Creeper creeper && HbmLivingProps.isDefused(creeper)) {
            ItemModDefuser.castrateCreeper(creeper, null, false);
        }
        for (Holder<Attribute> attribute : MODIFIABLE) {
            AttributeInstance instance = entity.getAttribute(attribute);
            if (instance == null) continue;
            for (Identifier id : PIECE_MODIFIERS) instance.removeModifier(id);
        }
        for (int i = 0; i < ArmorUtil.ARMOR_SLOTS.length; i++) {
            ItemStack armor = entity.getItemBySlot(ArmorUtil.ARMOR_SLOTS[i]);
            if (!isArmor(armor)) continue;
            Identifier id = PIECE_MODIFIERS[i];
            for (ItemStack mod : pryMods(armor)) {
                if (!(mod.getItem() instanceof ItemArmorMod armorMod)) continue;
                armorMod.modUpdate(entity, armor);
                HazardSystem.applyHazards(mod, entity);
                armorMod.collectModifiers(
                        armor,
                        id,
                        (attribute, modifier) -> {
                            assert MODIFIABLE.contains(attribute);
                            AttributeInstance instance = entity.getAttribute(attribute);

                            if (instance != null) instance.addOrUpdateTransientModifier(modifier);
                        });
            }
        }
        if (entity.getHealth() > entity.getMaxHealth()) entity.setHealth(entity.getMaxHealth());
    }

    private static void drop(LivingEntity entity, ServerLevel level, Item item) {
        entity.spawnAtLocation(level, new ItemStack(item));
    }
}
