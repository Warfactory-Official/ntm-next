// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.extprop;

import com.hbm.platform.NeoForgeEntityData;
import java.util.function.Supplier;
import net.neoforged.neoforge.attachment.AttachmentType;

public final class ModEntityData {

    public static Supplier<AttachmentType<HbmPlayerProps>> PLAYER_PROPS;
    public static Supplier<AttachmentType<HbmLivingProps>> LIVING_PROPS;

    private ModEntityData() {}

    public static void register() {
        PLAYER_PROPS =
                NeoForgeEntityData.ATTACHMENTS.register(
                        "player_props",
                        () -> {
                            AttachmentType.Builder<HbmPlayerProps> b =
                                    AttachmentType.builder(HbmPlayerProps::new)
                                            .serialize(HbmPlayerProps.MAP_CODEC);
                            if (true) b.copyOnDeath();
                            return b.build();
                        });
        LIVING_PROPS =
                NeoForgeEntityData.ATTACHMENTS.register(
                        "living_props",
                        () -> {
                            AttachmentType.Builder<HbmLivingProps> b2 =
                                    AttachmentType.builder(HbmLivingProps::new)
                                            .serialize(HbmLivingProps.MAP_CODEC);
                            if (false) b2.copyOnDeath();
                            return b2.build();
                        });
    }
}
