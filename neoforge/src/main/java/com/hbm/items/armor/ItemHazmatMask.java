// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.armor;

import com.hbm.hazard.HazardClass;
import com.hbm.lib.Library;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

public final class ItemHazmatMask extends ItemGasMask {

    private final Variant variant;

    public ItemHazmatMask(Item.Properties properties, Suit suit, Variant variant) {
        super(properties, Type.M65, suit);
        this.variant = variant;
    }

    public Variant variant() {
        return variant;
    }

    @Override
    public HazardClass[] filterBlacklist() {
        return new HazardClass[0];
    }

    public enum Variant {
        STANDARD(null),
        RED(Library.id("textures/models/model_haz_red.png")),
        GREY(Library.id("textures/models/model_haz_grey.png"));

        private final Identifier modelTexture;

        Variant(Identifier modelTexture) {
            this.modelTexture = modelTexture;
        }

        public Identifier modelTexture() {
            return modelTexture;
        }
    }
}
