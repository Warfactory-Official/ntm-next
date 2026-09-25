// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.platform;

import com.hbm.NuclearTech;
import com.hbm.config.HbmConfig;
import com.hbm.platform.services.*;
import com.hbm.registration.IRegistrar;
import java.util.ServiceLoader;

public final class Services {
    public static final IPlatformHelper PLATFORM = load(IPlatformHelper.class);
    public static final IServerAccessor SERVER = load(IServerAccessor.class);
    public static final IRegistrar REGISTRAR = load(IRegistrar.class);
    public static final INetworkService NETWORK = load(INetworkService.class);
    public static final ITrustedLookupProvider TRUSTED_LOOKUP = load(ITrustedLookupProvider.class);
    public static final HbmConfig CONFIG = load(HbmConfig.class);
    public static final ICapabilityService CAPS = load(ICapabilityService.class);
    public static final IEntityDataService ENTITY_DATA = load(IEntityDataService.class);
    public static final IBiomeModifierService BIOME_MODIFIER = load(IBiomeModifierService.class);

    private Services() {}

    public static <T> T load(Class<T> clazz) {
        final T loaded =
                ServiceLoader.load(clazz)
                        .findFirst()
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "No service implementation found for "
                                                        + clazz.getName()));
        NuclearTech.LOGGER.debug("Loaded {} for service {}", loaded, clazz.getName());
        return loaded;
    }
}
