// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine.rbmk;

import com.hbm.handler.neutron.NeutronStream;

public interface IRBMKFluxReceiver {

    void receiveFlux(NeutronStream stream);

    enum NType {
        SLOW("trait.rbmk.neutron.slow"),
        FAST("trait.rbmk.neutron.fast"),
        ANY("trait.rbmk.neutron.any");

        public final String unlocalized;

        NType(String unlocalized) {
            this.unlocalized = unlocalized;
        }
    }
}
