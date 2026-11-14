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

package com.hbm.items.armor;

import com.hbm.util.RegistryUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

public final class HazmatMaskItem extends GasMaskItem {

    private final Variant variant;

    public HazmatMaskItem(Item.Properties properties, Suit suit, Variant variant) {
        super(properties, Type.M65, suit);
        this.variant = variant;
    }

    public Variant variant() {
        return variant;
    }

    public enum Variant {
        STANDARD(null),
        RED(RegistryUtil.id("textures/models/model_haz_red.png")),
        GREY(RegistryUtil.id("textures/models/model_haz_grey.png"));

        private final Identifier modelTexture;

        Variant(Identifier modelTexture) {
            this.modelTexture = modelTexture;
        }

        public Identifier modelTexture() {
            return modelTexture;
        }
    }
}
