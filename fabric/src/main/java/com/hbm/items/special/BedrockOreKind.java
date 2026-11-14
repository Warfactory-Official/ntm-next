/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.items.special;

import com.hbm.items.special.BedrockOreNewItem.BedrockOreGrade;
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
