// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.special;

import com.hbm.items.special.ItemBedrockOreNew.BedrockOreGrade;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

public interface BedrockOreKind {
    String id();

    String suffix();

    default Component typeName() {
        return Component.translatable("item.hbm.bedrock_ore.type." + suffix());
    }

    int light();

    int dark();

    Identifier channel();

    default @Nullable Component nameSuffix() {
        return null;
    }

    default void appendHoverText(BedrockOreGrade grade, Consumer<Component> adder) {}
}
