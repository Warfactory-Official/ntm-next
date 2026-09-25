// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.render.anim;

import java.util.HashMap;
import java.util.Map.Entry;

public class BusAnimation {

    private final HashMap<String, BusAnimationSequence> animationBuses =
            new HashMap<String, BusAnimationSequence>();

    private int totalTime = 0;

    public BusAnimation addBus(String name, BusAnimationSequence bus) {

        animationBuses.put(name, bus);

        int duration = bus.getTotalTime();

        if (duration > totalTime) totalTime = duration;

        return this;
    }

    public void updateTime() {

        for (Entry<String, BusAnimationSequence> sequence : animationBuses.entrySet()) {

            int time = sequence.getValue().getTotalTime();

            if (time > totalTime) totalTime = time;
        }
    }

    public BusAnimationSequence getBus(String name) {
        return animationBuses.get(name);
    }

    public void setTimeMult(double mult) {
        for (Entry<String, BusAnimationSequence> sequence : animationBuses.entrySet()) {
            sequence.getValue().multiplyTime(mult);
        }
    }

    public double[] getTimedTransformation(String name, int millis) {

        if (this.animationBuses.containsKey(name))
            return animationBuses.get(name).getTransformation(millis);

        return null;
    }

    public int getDuration() {
        return totalTime;
    }
}
