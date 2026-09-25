// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.hazard;

import com.tiviacz.travelersbackpack.TravelersBackpack;
import com.tiviacz.travelersbackpack.attachment.AttachmentUtils;
import net.minecraft.world.entity.player.Player;

public final class TravelersEquipmentHazards {
    private TravelersEquipmentHazards() {}

    public static float apply(Player player) {
        return TravelersBackpack.enableIntegration()
                ? 0F
                : HazardSystem.applyExternalEquipment(
                        AttachmentUtils.getWearingBackpack(player), player);
    }
}
