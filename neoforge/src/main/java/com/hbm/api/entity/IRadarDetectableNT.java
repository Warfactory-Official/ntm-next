// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.api.entity;

public interface IRadarDetectableNT {

    int TIER0 = 0;
    int TIER1 = 1;
    int TIER2 = 2;
    int TIER3 = 3;
    int TIER4 = 4;
    int TIER10 = 5;
    int TIER10_15 = 6;
    int TIER15 = 7;
    int TIER15_20 = 8;
    int TIER20 = 9;
    int TIER_AB = 10;
    int PLAYER = 11;
    int ARTY = 12;

    int SPECIAL = 13;

    String getRadarName();

    int getBlipLevel();

    boolean canBeSeenBy(Object radar);

    boolean paramsApplicable(RadarScanParams params);

    boolean suppliesRedstone(RadarScanParams params);

    class RadarScanParams {
        public boolean scanMissiles = true;
        public boolean scanShells = true;
        public boolean scanPlayers = true;
        public boolean smartMode = true;

        public RadarScanParams(boolean m, boolean s, boolean p, boolean smart) {
            this.scanMissiles = m;
            this.scanShells = s;
            this.scanPlayers = p;
            this.smartMode = smart;
        }
    }
}
