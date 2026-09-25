// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.weapon;

import com.hbm.entity.missile.EntityMissileCustom;
import com.hbm.items.special.ItemLootCrate;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import org.jspecify.annotations.Nullable;

public class ItemCustomMissilePart extends Item {

    public PartType type;
    public PartSize top;
    public PartSize bottom;
    public Rarity rarity;
    public float health;

    public float height;
    public @Nullable Identifier mesh;
    public @Nullable Identifier skin;

    private String title;
    private String author;
    private String witty;

    public Object[] attributes;

    public ItemCustomMissilePart(Properties properties) {
        super(properties);
    }

    public enum PartType {
        CHIP,
        WARHEAD,
        FUSELAGE,
        FINS,
        THRUSTER
    }

    public enum PartSize {
        ANY,

        NONE,
        SIZE_10,
        SIZE_15,
        SIZE_20;

        public Component getDisplay() {
            switch (this) {
                case ANY:
                    return Component.translatable("item.hbm.missile.part.size.any");
                case SIZE_10:
                    return Component.literal("1.0m");
                case SIZE_15:
                    return Component.literal("1.5m");
                case SIZE_20:
                    return Component.literal("2.0m");
                default:
                    return Component.translatable("item.hbm.missile.part.size.none");
            }
        }
    }

    public enum WarheadType {
        HE,
        INC,
        BUSTER,
        CLUSTER,
        NUCLEAR,
        TX,
        N2,
        BALEFIRE,
        SCHRAB,
        TAINT,
        CLOUD,
        TURBINE,

        CUSTOM0,
        CUSTOM1,
        CUSTOM2,
        CUSTOM3,
        CUSTOM4,
        CUSTOM5,
        CUSTOM6,
        CUSTOM7,
        CUSTOM8,
        CUSTOM9;

        public Consumer<EntityMissileCustom> impactCustom = null;

        public Consumer<EntityMissileCustom> updateCustom = null;

        public String labelCustom = null;

        public Component getDisplay() {

            if (this.labelCustom != null) return Component.literal(this.labelCustom);

            switch (this) {
                case HE:
                    return key("he", ChatFormatting.YELLOW);
                case INC:
                    return key("incendiary", ChatFormatting.GOLD);
                case CLUSTER:
                    return key("cluster", ChatFormatting.GRAY);
                case BUSTER:
                    return key("bunker_buster", ChatFormatting.WHITE);
                case NUCLEAR:
                    return key("nuclear", ChatFormatting.DARK_GREEN);
                case TX:
                    return key("thermonuclear", ChatFormatting.DARK_PURPLE);
                case N2:
                    return key("n2", ChatFormatting.RED);
                case BALEFIRE:
                    return key("balefire", ChatFormatting.GREEN);
                case SCHRAB:
                    return key("schrabidium", ChatFormatting.AQUA);
                case TAINT:
                    return key("taint", ChatFormatting.DARK_PURPLE);
                case CLOUD:
                    return key("cloud", ChatFormatting.LIGHT_PURPLE);

                case TURBINE:
                    return key(
                            "turbine",
                            System.currentTimeMillis() % 1000 < 500
                                    ? ChatFormatting.RED
                                    : ChatFormatting.LIGHT_PURPLE);
                default:
                    return Component.translatable("general.na").withStyle(ChatFormatting.BOLD);
            }
        }

        private static Component key(String name, ChatFormatting color) {
            return Component.translatable("item.hbm.warhead.desc." + name).withStyle(color);
        }
    }

    public enum FuelType {
        KEROSENE("kerosene_peroxide", ChatFormatting.LIGHT_PURPLE),
        SOLID("solid", ChatFormatting.GOLD),
        HYDROGEN("hydrogen", ChatFormatting.DARK_AQUA),
        XENON("xenon", ChatFormatting.DARK_PURPLE),
        BALEFIRE("balefire", ChatFormatting.GREEN);

        private final String key;
        private final ChatFormatting color;

        FuelType(String key, ChatFormatting color) {
            this.key = key;
            this.color = color;
        }

        public Component getDisplay() {
            return Component.translatable("item.hbm.missile.fuel." + key).withStyle(color);
        }
    }

    public enum Rarity {
        COMMON("common", ChatFormatting.GRAY),
        UNCOMMON("uncommon", ChatFormatting.YELLOW),
        RARE("rare", ChatFormatting.AQUA),
        EPIC("epic", ChatFormatting.LIGHT_PURPLE),
        LEGENDARY("legendary", ChatFormatting.DARK_GREEN),
        STRANGE("strange", ChatFormatting.DARK_AQUA);

        private final String key;
        private final ChatFormatting color;

        Rarity(String key, ChatFormatting color) {
            this.key = key;
            this.color = color;
        }

        public Component getDisplay() {
            return Component.translatable("item.hbm.missile.part.rarity." + key).withStyle(color);
        }
    }

    public ItemCustomMissilePart makeChip(float inaccuracy) {
        this.type = PartType.CHIP;
        this.top = PartSize.ANY;
        this.bottom = PartSize.ANY;
        this.attributes = new Object[] {inaccuracy};
        return this;
    }

    public ItemCustomMissilePart makeWarhead(
            WarheadType type, float punch, float weight, PartSize size) {
        this.type = PartType.WARHEAD;
        this.top = PartSize.NONE;
        this.bottom = size;
        this.attributes = new Object[] {type, punch, weight};
        return this;
    }

    public ItemCustomMissilePart makeFuselage(
            FuelType type, float fuel, PartSize top, PartSize bottom) {
        this.type = PartType.FUSELAGE;
        this.top = top;
        this.bottom = bottom;
        this.attributes = new Object[] {type, fuel};
        return this;
    }

    public ItemCustomMissilePart makeStability(float inaccuracy, PartSize size) {
        this.type = PartType.FINS;
        this.top = size;
        this.bottom = size;
        this.attributes = new Object[] {inaccuracy};
        return this;
    }

    public ItemCustomMissilePart makeThruster(
            FuelType type, float consumption, float lift, PartSize size) {
        this.type = PartType.THRUSTER;
        this.top = size;
        this.bottom = PartSize.NONE;
        this.attributes = new Object[] {type, consumption, lift};
        return this;
    }

    public ItemCustomMissilePart setAssembly(float height, Identifier mesh, Identifier skin) {
        this.height = height;
        this.mesh = mesh;
        this.skin = skin;
        return this;
    }

    public ItemCustomMissilePart setAuthor(String author) {
        this.author = author;
        return this;
    }

    public ItemCustomMissilePart setTitle(String title) {
        this.title = title;
        return this;
    }

    public ItemCustomMissilePart setWittyText(String witty) {
        this.witty = witty;
        return this;
    }

    public ItemCustomMissilePart setHealth(float health) {
        this.health = health;
        return this;
    }

    public ItemCustomMissilePart setRarity(Rarity rarity) {
        this.rarity = rarity;

        if (this.type == PartType.FUSELAGE) {
            if (this.top == PartSize.SIZE_10) ItemLootCrate.LIST_10.add(this);
            if (this.top == PartSize.SIZE_15) ItemLootCrate.LIST_15.add(this);
        } else {
            ItemLootCrate.LIST_MISC.add(this);
        }

        return this;
    }

    public WarheadType warheadType() {
        return this.type == PartType.WARHEAD ? (WarheadType) this.attributes[0] : null;
    }

    public float warheadStrength() {
        return this.type == PartType.WARHEAD ? (Float) this.attributes[1] : 0F;
    }

    public FuelType fuelType() {
        return this.type == PartType.FUSELAGE || this.type == PartType.THRUSTER
                ? (FuelType) this.attributes[0]
                : null;
    }

    public float fuelAmount() {
        return this.type == PartType.FUSELAGE ? (Float) this.attributes[1] : 0F;
    }

    public float consumption() {
        return this.type == PartType.THRUSTER ? (Float) this.attributes[1] : 0F;
    }

    public float inaccuracy() {
        return this.type == PartType.CHIP || this.type == PartType.FINS
                ? (Float) this.attributes[0]
                : 0F;
    }

    public static ItemCustomMissilePart part(ItemStack stack) {
        return stack != null && stack.getItem() instanceof ItemCustomMissilePart part ? part : null;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {

        if (this.title != null) {
            adder.accept(
                    Component.literal("\"" + this.title + "\"")
                            .withStyle(ChatFormatting.DARK_PURPLE));
        }

        if (this.type != null && this.attributes != null) {
            switch (this.type) {
                case CHIP:
                    adder.accept(stat("inaccuracy", Component.literal(inaccuracy() * 100 + "%")));
                    break;
                case WARHEAD:
                    adder.accept(stat("size", this.bottom.getDisplay()));
                    adder.accept(stat("type", warheadType().getDisplay()));
                    adder.accept(
                            stat("strength", Component.literal(String.valueOf(warheadStrength()))));
                    adder.accept(stat("weight", Component.literal(this.attributes[2] + "t")));
                    break;
                case FUSELAGE:
                    adder.accept(stat("topSize", this.top.getDisplay()));
                    adder.accept(stat("bottomSize", this.bottom.getDisplay()));
                    adder.accept(stat("fuelType", fuelType().getDisplay()));
                    adder.accept(stat("fuelAmount", Component.literal(fuelAmount() + "l")));
                    break;
                case FINS:
                    adder.accept(stat("size", this.top.getDisplay()));
                    adder.accept(stat("inaccuracy", Component.literal(inaccuracy() * 100 + "%")));
                    break;
                case THRUSTER:
                    adder.accept(stat("size", this.top.getDisplay()));
                    adder.accept(stat("fuelType", fuelType().getDisplay()));
                    adder.accept(
                            stat("fuelConsumption", Component.literal(consumption() + "l/tick")));
                    adder.accept(stat("maxPayload", Component.literal(this.attributes[2] + "t")));
                    break;
            }
        }

        if (this.type != PartType.CHIP) {
            adder.accept(stat("health", Component.literal(this.health + "HP")));
        }
        if (this.rarity != null) {
            adder.accept(stat("rarity", this.rarity.getDisplay()));
        }
        if (this.author != null) {
            adder.accept(
                    Component.translatable("item.hbm.missile.part.by")
                            .append(" " + this.author)
                            .withStyle(ChatFormatting.WHITE));
        }
        if (this.witty != null) {
            adder.accept(
                    Component.literal("\"" + this.witty + "\"")
                            .withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
        }
    }

    private static Component stat(String key, Component value) {
        return Component.translatable("item.hbm.missile.part." + key)
                .withStyle(ChatFormatting.BOLD)
                .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                .append(value);
    }
}
