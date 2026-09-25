// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.interfaces.injected;

public interface ISectionGeometryNormal {
    boolean hbm$hasSectionNormal();

    float hbm$sectionNormalX();

    float hbm$sectionNormalY();

    float hbm$sectionNormalZ();

    void hbm$sectionNormal(float x, float y, float z);

    void hbm$clearSectionNormal();
}
