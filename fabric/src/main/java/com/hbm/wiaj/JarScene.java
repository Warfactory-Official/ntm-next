// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.wiaj;

import com.hbm.wiaj.actions.IJarAction;
import java.util.ArrayList;
import java.util.List;

public final class JarScene {
    public final List<IJarAction> actions = new ArrayList<>();
    public final JarScript script;
    public int actionNumber;
    public IJarAction currentAction;
    public int currentActionStart;

    public JarScene(JarScript script) {
        this.script = script;
    }

    public JarScene add(IJarAction action) {
        if (currentAction == null) currentAction = action;
        actions.add(action);
        return this;
    }

    public void tick() {
        if (currentAction == null) return;
        currentAction.act(script.world, this);
        if (currentActionStart + currentAction.getDuration() <= script.ticksElapsed) {
            actionNumber++;
            currentActionStart = script.ticksElapsed;
            if (actionNumber < actions.size()) {
                currentAction = actions.get(actionNumber);
                tick();
            } else {
                currentAction = null;
            }
        }
    }

    public void reset() {
        currentAction = actions.getFirst();
        actionNumber = 0;
        currentActionStart = script.ticksElapsed;
    }
}
