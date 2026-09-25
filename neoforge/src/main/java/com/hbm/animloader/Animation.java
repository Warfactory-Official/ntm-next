// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.animloader;

import java.util.HashMap;
import java.util.Map;

public final class Animation {

    public static final Animation EMPTY = createBlankAnimation();

    public int length;
    public int numKeyFrames;
    public Map<String, Transform[]> objectTransforms = new HashMap<>();

    private static Animation createBlankAnimation() {
        Animation anim = new Animation();
        anim.numKeyFrames = 0;
        anim.length = 0;
        anim.objectTransforms = new HashMap<>();
        return anim;
    }
}
