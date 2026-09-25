// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.model;

import com.hbm.render.loader.GroupObject;
import com.hbm.render.loader.HFRWavefrontObject;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

public final class Meshes {

    private Meshes() {}

    public static HFRWavefrontObject load(Identifier id) {
        return new HFRWavefrontObject(Minecraft.getInstance().getResourceManager(), id);
    }

    public static HFRWavefrontObject faceNormals(Identifier id) {
        return load(id).noSmooth();
    }

    public static HFRWavefrontObject flatShaded(HFRWavefrontObject mesh) {
        List<GroupObject> groups = new ArrayList<>(mesh.groups.length);
        for (GroupObject group : mesh.groups) groups.add(group.flatShaded());
        return new HFRWavefrontObject(mesh.source, groups);
    }
}
