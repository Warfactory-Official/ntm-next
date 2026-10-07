// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.special;

import com.hbm.items.ModItems;
import com.hbm.items.special.ItemBedrockOreNew.BedrockOreType;
import com.hbm.lib.Library;
import com.hbm.registration.OpenRegistry;
import com.hbm.registration.OverridePoint;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public record BedrockOreSet(String id, List<BedrockOreKind> kinds, @Nullable Component origin) {

    public static final BedrockOreSet NTM =
            new BedrockOreSet("hbm", List.of(BedrockOreType.VALUES), null);

    public static final OpenRegistry<BedrockOreSet> SETS =
            new OpenRegistry<>("bedrock ore set", BedrockOreSet::id);

    public static final OverridePoint<Function<Level, BedrockOreSet>> OF =
            OverridePoint.of(Library.id("bedrock_ore_set"), level -> NTM);

    public static final OverridePoint<Supplier<List<ItemStack>>> SAMPLES =
            OverridePoint.of(
                    Library.id("bedrock_ore_samples"),
                    () -> List.of(new ItemStack(ModItems.BEDROCK_ORE_BASE)));

    static {
        SETS.add(NTM);
    }

    public BedrockOreSet {
        kinds = List.copyOf(kinds);
    }

    public int indexOf(BedrockOreKind kind) {
        return kinds.indexOf(kind);
    }
}
