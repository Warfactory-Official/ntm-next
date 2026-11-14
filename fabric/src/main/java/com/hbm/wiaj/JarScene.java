/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.wiaj;

import com.hbm.wiaj.actions.SceneAction;
import java.util.ArrayList;
import java.util.List;

public final class JarScene {
    public final List<SceneAction> actions = new ArrayList<>();
    public final JarScript script;
    public SceneAction currentAction;
    private int cursor;
    private int startedAt;

    public JarScene(JarScript script) {
        this.script = script;
    }

    public JarScene add(SceneAction action) {
        actions.add(action);
        if (currentAction == null) currentAction = action;
        return this;
    }

    public void tick() {
        while (currentAction != null) {
            currentAction.perform(script.world, this);
            if (startedAt + currentAction.duration() > script.ticksElapsed) return;
            startedAt = script.ticksElapsed;
            cursor++;
            currentAction = cursor < actions.size() ? actions.get(cursor) : null;
        }
    }

    public void reset() {
        cursor = 0;
        currentAction = actions.getFirst();
        startedAt = script.ticksElapsed;
    }
}
