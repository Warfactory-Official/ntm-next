// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine.fusion;

public interface IFusionPowerReceiver {

    boolean receivesFusionPower();

    void receiveFusionPower(long fusionPower, double neutronPower, float r, float g, float b);
}
