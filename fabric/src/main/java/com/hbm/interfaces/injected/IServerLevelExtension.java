// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.interfaces.injected;

import com.hbm.capability.ResolvedCapCache;
import com.hbm.uninos.graph.EndpointRegistry;
import com.hbm.uninos.graph.LevelNodeGraph;
import com.hbm.world.NtmWorldgenFields;
import java.util.List;

public interface IServerLevelExtension {

    EndpointRegistry hbm$endpoints();

    List<LevelNodeGraph<?>> hbm$graphs();

    ResolvedCapCache hbm$resolvedCaps();

    NtmWorldgenFields hbm$worldgenFields();
}
