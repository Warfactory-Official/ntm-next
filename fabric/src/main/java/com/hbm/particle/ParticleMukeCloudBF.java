// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.particle;

import com.hbm.lib.Library;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.Identifier;

public class ParticleMukeCloudBF extends ParticleMukeCloud {

    private static final Identifier TEXTURE = Library.id("textures/particle/explosion_bf.png");

    public ParticleMukeCloudBF(
            ClientLevel level, double x, double y, double z, double mx, double my, double mz) {
        super(level, x, y, z, mx, my, mz);
    }

    @Override
    public Identifier texture() {
        return TEXTURE;
    }
}
