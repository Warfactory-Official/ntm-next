// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.extprop;

import com.hbm.lib.Library;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

public final class ModEntityData {

    public static AttachmentType<HbmPlayerProps> PLAYER_PROPS;
    public static AttachmentType<HbmLivingProps> LIVING_PROPS;

    private ModEntityData() {}

    public static void register() {
        PLAYER_PROPS =
                AttachmentRegistry.create(
                        Library.id("player_props"),
                        builder -> {
                            builder.initializer(HbmPlayerProps::new)
                                    .persistent(HbmPlayerProps.MAP_CODEC.codec());
                            if (true) builder.copyOnDeath();
                        });
        LIVING_PROPS =
                AttachmentRegistry.create(
                        Library.id("living_props"),
                        builder2 -> {
                            builder2.initializer(HbmLivingProps::new)
                                    .persistent(HbmLivingProps.MAP_CODEC.codec());
                            if (false) builder2.copyOnDeath();
                        });
    }
}
