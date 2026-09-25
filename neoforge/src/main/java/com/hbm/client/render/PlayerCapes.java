// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.lib.Library;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.ClientAsset;
import org.jspecify.annotations.Nullable;

public final class PlayerCapes {

    private static final UUID HBM = UUID.fromString("192af5d7-ed0f-48d8-bd89-9d41af8524f8");
    private static final UUID BLAZE = UUID.fromString("061bc566-ec74-4307-9614-ac3a70d2ef38");
    private static final ClientAsset.ResourceTexture HBM3 = cape("capehbm3");
    private static final ClientAsset.ResourceTexture HBM2 = cape("capehbm2");
    private static final ClientAsset.ResourceTexture BLAZE1 = cape("capeblaze");
    private static final ClientAsset.ResourceTexture BLAZE2 = cape("capeblaze2");
    private static final ClientAsset.ResourceTexture WIKI = cape("capewiki");
    private static final ClientAsset.ResourceTexture TEST = cape("capetest");
    private static final Set<UUID> CONTRIBUTORS =
            Set.of(
                    UUID.fromString("06ab7c03-55ce-43f8-9d3c-2850e3c652de"),
                    UUID.fromString("5bf069bc-5b46-4179-aafe-35c0a07dee8b"),
                    UUID.fromString("ccd9aa1c-26b9-4dde-8f37-b96f8d99de22"));
    private static final Map<UUID, ClientAsset.ResourceTexture> ASSIGNED =
            Map.ofEntries(
                    entry("41ebd03f-7a12-42f3-b037-0caa4d6f235b", "capedrillgon"),
                    entry("3af1c262-61c0-4b12-a4cb-424cc3a9c8c0", "capedafnik"),
                    entry("937c9804-e11f-4ad2-a5b1-42e62ac73077", "capeshield"),
                    entry("a41df45e-13d8-4677-9398-090d3882b74f", "capevertice_2"),
                    entry("e82684a7-30f1-44d2-ab37-41b342be1bbd", "capenostalgia2"),
                    entry("87c3960a-4332-46a0-a929-ef2a488d1cda", "capesam"),
                    entry("d7f29d9c-5103-4f6f-88e1-2632ff95973f", "capehoboy_mk3"),
                    entry("dc23a304-0f84-4e2d-b47d-84c8d3bfbcdb", "capemaster"),
                    entry("ac49720b-4a9a-4459-a26f-bee92160287a", "capemek"),
                    entry("03c20435-a229-489a-a1a1-671b803f7017", "capezippysqrl"),
                    entry("3a4a1944-5154-4e67-b80a-b6561e8630b7", "capeschrabbyalt"),
                    entry("5544aa30-b305-4362-b2c1-67349bb499d5", "capesweatyswiggs"),
                    entry("e4ab1199-1c22-4f82-a516-c3238bc2d0d1", "capedoctor17"),
                    entry("4d0477d7-58da-41a9-a945-e93df8601c5a", "capedoctor17"),
                    entry("37e5eb63-b9a2-4735-9007-1c77d703daa3", "capeleftnugget"),
                    entry("259785a0-20e9-4c63-9286-ac2f93ff528f", "caperightnugget"),
                    entry("609268ad-5b34-49c2-abba-a9d83229af03", "capetankish"),
                    entry("fc4cc2ee-12e8-4097-b26a-1c6cb1b96531", "capefrizzlefrazzle"),
                    entry("1121cb7a-8773-491f-8e2b-221290c93d81", "capevaer"),
                    entry("bbae7bfa-0eba-40ac-a0dd-f3b715e73e61", "capeadam"),
                    entry("0b399a4a-8545-45a1-be3d-ece70d7d48e9", "capealcater"),
                    entry("42ee978c-442a-4cd8-95b6-29e469b6df10", "capejame"));

    private PlayerCapes() {}

    private static ClientAsset.ResourceTexture cape(String basename) {
        return new ClientAsset.ResourceTexture(Library.id("models/capes/" + basename));
    }

    private static Map.Entry<UUID, ClientAsset.ResourceTexture> entry(
            String uuid, String basename) {
        return Map.entry(UUID.fromString(uuid), cape(basename));
    }

    public static ClientAsset.@Nullable ResourceTexture forPlayer(
            UUID uuid, String name, boolean balefire) {
        if (HBM.equals(uuid)) return balefire ? HBM2 : HBM3;
        if (BLAZE.equals(uuid)) return balefire ? BLAZE2 : BLAZE1;
        ClientAsset.ResourceTexture assigned = ASSIGNED.get(uuid);
        if (assigned != null) return assigned;
        if (CONTRIBUTORS.contains(uuid)) return WIKI;
        return name.startsWith("Player") ? TEST : null;
    }
}
