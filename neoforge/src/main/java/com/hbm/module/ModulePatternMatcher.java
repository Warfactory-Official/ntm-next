// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.module;

import com.hbm.inventory.material.MaterialShapes;
import com.hbm.items.ModItems;
import com.hbm.items.special.ItemBedrockOreNew;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncSource;
import com.hbm.registration.ItemFamily;
import com.hbm.registration.ItemStates;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public final class ModulePatternMatcher implements SyncSource {

    @SyncField private final String[] modes;

    private ModulePatternMatcher(String[] modes) {
        this.modes = modes;
    }

    public String mode(int index) {
        return modes[index];
    }

    public void mode(int index, String mode) {
        modes[index] = mode;
    }

    public static final String MODE_EXACT = "exact";
    public static final String MODE_WILDCARD = "wildcard";
    public static final String MODE_BEDROCK = "bedrock";

    private static final String[] SMART_SHAPE_PATHS = {
        MaterialShapes.INGOT.name() + "/",
        MaterialShapes.BLOCK.name() + "/",
        MaterialShapes.DUST.name() + "/",
        MaterialShapes.NUGGET.name() + "/",
        MaterialShapes.PLATE.name() + "/",
    };

    public ModulePatternMatcher(int count) {
        this(new String[count]);
    }

    public static boolean sameItemAndState(ItemStack a, ItemStack b) {
        return a.is(b.getItem()) && ItemStates.sameState(a, b);
    }

    public static Component getLabel(String mode) {
        String text =
                switch (mode) {
                    case MODE_EXACT -> "Item and variant match";
                    case MODE_WILDCARD -> "Item matches";
                    case MODE_BEDROCK -> "Item and bedrock grade match";
                    default -> "Item tag matches: " + mode;
                };
        return Component.literal(text).withStyle(ChatFormatting.YELLOW);
    }

    private static String path(String tag) {
        int colon = tag.indexOf(':');
        return colon < 0 ? tag : tag.substring(colon + 1);
    }

    private static List<String> tags(ItemStack stack) {
        return BuiltInRegistries.ITEM
                .wrapAsHolder(stack.getItem())
                .tags()
                .map(tag -> tag.location().toString())
                .toList();
    }

    private static boolean matchesTag(ItemStack stack, String mode) {
        @Nullable Identifier id = Identifier.tryParse(mode);
        return id != null && stack.is(TagKey.create(Registries.ITEM, id));
    }

    public void initPatternSmart(Level level, ItemStack stack, int index) {
        if (level.isClientSide()) return;
        if (stack.isEmpty()) {
            modes[index] = null;
            return;
        }

        List<String> tags = tags(stack);
        for (String prefix : SMART_SHAPE_PATHS) {
            for (String tag : tags) {
                if (path(tag).startsWith(prefix)) {
                    modes[index] = tag;
                    return;
                }
            }
        }

        initPatternStandard(level, stack, index);
    }

    public void initPatternStandard(Level level, ItemStack stack, int index) {
        if (level.isClientSide()) return;
        if (stack.isEmpty()) {
            modes[index] = null;
        } else if (stack.is(ModItems.BEDROCK_ORE.get())) {
            modes[index] = MODE_BEDROCK;
        } else if (!ItemStates.of(stack.getItem()).isEmpty()
                || ItemFamily.of(stack.getItem()) != null) {
            modes[index] = MODE_EXACT;
        } else {
            modes[index] = MODE_WILDCARD;
        }
    }

    public void nextMode(Level level, ItemStack pattern, int index) {
        if (level.isClientSide()) return;
        if (pattern.isEmpty()) {
            modes[index] = null;
            return;
        }

        String mode = modes[index];
        if (mode == null) {
            modes[index] = MODE_EXACT;
        } else if (MODE_EXACT.equals(mode)) {
            modes[index] = pattern.is(ModItems.BEDROCK_ORE.get()) ? MODE_BEDROCK : MODE_WILDCARD;
        } else if (MODE_BEDROCK.equals(mode)) {
            modes[index] = MODE_WILDCARD;
        } else if (MODE_WILDCARD.equals(mode)) {
            List<String> tags = tags(pattern);
            modes[index] = tags.isEmpty() ? MODE_EXACT : tags.getFirst();
        } else {
            List<String> tags = tags(pattern);
            int current = tags.indexOf(mode);
            modes[index] =
                    current >= 0 && current + 1 < tags.size() ? tags.get(current + 1) : MODE_EXACT;
        }
    }

    public boolean isValidForFilter(ItemStack filter, int index, ItemStack input) {
        String mode = modes[index];
        if (mode == null) modes[index] = mode = MODE_EXACT;
        return matches(mode, filter, input);
    }

    public static boolean matches(String mode, ItemStack filter, ItemStack input) {
        return switch (mode) {
            case MODE_EXACT -> sameItemAndState(input, filter);

            case MODE_WILDCARD ->
                    input.is(filter.getItem())
                            || ItemFamily.of(filter.getItem()) != null
                                    && ItemFamily.of(filter.getItem())
                                            == ItemFamily.of(input.getItem());
            case MODE_BEDROCK ->
                    input.is(filter.getItem())
                            && input.is(ModItems.BEDROCK_ORE.get())
                            && ItemBedrockOreNew.ore(input).grade()
                                    == ItemBedrockOreNew.ore(filter).grade();
            default -> matchesTag(input, mode);
        };
    }

    public void load(ValueInput input) {
        for (int i = 0; i < modes.length; i++) modes[i] = input.getString("mode" + i).orElse(null);
    }

    public void save(ValueOutput output) {
        for (int i = 0; i < modes.length; i++) {
            if (modes[i] != null) output.putString("mode" + i, modes[i]);
        }
    }

    public void serialize(ByteBuf buffer) {
        for (String mode : modes)
            ByteBufCodecs.STRING_UTF8.encode(buffer, mode == null ? "" : mode);
    }

    public void deserialize(ByteBuf buffer) {
        for (int i = 0; i < modes.length; i++) {
            String mode = ByteBufCodecs.STRING_UTF8.decode(buffer);
            modes[i] = mode.isEmpty() ? null : mode;
        }
    }
}
