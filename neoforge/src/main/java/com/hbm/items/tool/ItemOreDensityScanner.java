// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.tool;

import com.hbm.inventory.fluid.FluidStackNTM;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.items.special.ItemBedrockOreNew.BedrockOreType;
import com.hbm.packet.toclient.OreDensityPayload;
import com.hbm.platform.Services;
import com.hbm.world.NtmWorldgenFields;
import com.hbm.world.feature.BedrockOreFeature;
import com.hbm.world.feature.BedrockOreField;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import org.jspecify.annotations.Nullable;

public class ItemOreDensityScanner extends Item {

    private static final int FIRST_ID = 777;
    private static final int LIFETIME = 4000;
    private static final int CADENCE = 5;

    public ItemOreDensityScanner(Properties props) {
        super(props);
    }

    public static String translateDensity(double density) {
        if (density <= 0.1D) return "item.hbm.ore_density_scanner.verypoor";
        if (density <= 0.35D) return "item.hbm.ore_density_scanner.poor";
        if (density <= 0.75D) return "item.hbm.ore_density_scanner.low";
        if (density >= 1.9D) return "item.hbm.ore_density_scanner.excellent";
        if (density >= 1.65D) return "item.hbm.ore_density_scanner.veryhigh";
        if (density >= 1.25D) return "item.hbm.ore_density_scanner.high";
        return "item.hbm.ore_density_scanner.moderate";
    }

    public static ChatFormatting getColor(double density) {
        if (density <= 0.1D) return ChatFormatting.DARK_RED;
        if (density <= 0.35D) return ChatFormatting.RED;
        if (density <= 0.75D) return ChatFormatting.GOLD;

        if (density > 2D) return ChatFormatting.LIGHT_PURPLE;
        if (density >= 1.9D) return ChatFormatting.AQUA;
        if (density >= 1.65D) return ChatFormatting.BLUE;
        if (density >= 1.25D) return ChatFormatting.GREEN;
        return ChatFormatting.YELLOW;
    }

    private static MutableComponent row(BedrockOreType type, double density) {
        return Component.translatable("item.hbm.bedrock_ore.type." + type.suffix)
                .append(Component.literal(": " + ((int) (density * 100) / 100D) + " ("))
                .append(
                        Component.translatable(translateDensity(density))
                                .withStyle(getColor(density)))
                .append(Component.literal(")"));
    }

    @Override
    public void inventoryTick(
            ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
        if (!(owner instanceof ServerPlayer player) || level.getGameTime() % CADENCE != 0) return;

        int x = (int) Math.floor(player.getX());
        int z = (int) Math.floor(player.getZ());
        BedrockOreField field = NtmWorldgenFields.get(level).bedrock();
        double richness = field.richness(x, z);
        double lightMetal = field.lightMetal(richness, x, z);
        double heavyMetal = field.heavyMetal(richness, x, z);
        double rareEarth = field.rareEarth(richness, x, z);
        double actinide = field.actinide(richness, x, z);
        double nonMetal = field.nonMetal(richness, x, z);
        double crystalline = field.crystalline(richness, x, z);
        double total =
                (lightMetal + heavyMetal + rareEarth + actinide + nonMetal + crystalline) / 6D;

        int tier = BedrockOreFeature.getTier(total);
        MutableComponent summary =
                Component.translatable("item.hbm.ore_density_scanner.tier", tier)
                        .withStyle(ChatFormatting.YELLOW);
        FluidStackNTM acid = BedrockOreFeature.getBoreFluid(tier);
        if (acid != null && acid.type() != Fluids.EMPTY) {
            summary.append(Component.literal(" - " + acid.amount() + "mB "))
                    .append(NTMFluidProperties.getDisplayName(acid.type()));
        }
        Services.NETWORK.sendTo(
                new OreDensityPayload(
                        row(BedrockOreType.LIGHT_METAL, lightMetal),
                        row(BedrockOreType.HEAVY_METAL, heavyMetal),
                        row(BedrockOreType.RARE_EARTH, rareEarth),
                        row(BedrockOreType.ACTINIDE, actinide),
                        row(BedrockOreType.NON_METAL, nonMetal),
                        row(BedrockOreType.CRYSTALLINE, crystalline),
                        summary,
                        FIRST_ID,
                        LIFETIME),
                player);
    }
}
