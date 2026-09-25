// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.extprop;

import com.hbm.platform.Services;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

public final class ModEntityData {

    public static AttachmentType<HbmPlayerProps> PLAYER_PROPS;
    public static AttachmentType<HbmLivingProps> LIVING_PROPS;

    private ModEntityData() {}

    public static void register() {
        PLAYER_PROPS =
                Services.ENTITY_DATA.register(
                        "player_props", HbmPlayerProps::new, HbmPlayerProps.MAP_CODEC, true);
        LIVING_PROPS =
                Services.ENTITY_DATA.register(
                        "living_props", HbmLivingProps::new, HbmLivingProps.MAP_CODEC, false);
    }
}
