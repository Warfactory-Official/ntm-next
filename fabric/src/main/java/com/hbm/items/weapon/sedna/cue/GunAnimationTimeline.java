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

package com.hbm.items.weapon.sedna.cue;

import com.hbm.items.weapon.sedna.GunBaseNTItem.LambdaContext;
import com.hbm.items.weapon.sedna.GunBaseNTItem;
import com.hbm.render.anim.AnimationEnums.GunAnimation;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public final class GunAnimationTimeline implements BiConsumer<ItemStack, LambdaContext> {

    private static final GunAnimationAction[] NONE = {};

    private final Consumer<Score> score;
    private @Nullable Compiled compiled;

    private GunAnimationTimeline(Consumer<Score> score) {
        this.score = score;
    }

    public static GunAnimationTimeline of(Consumer<Score> score) {
        return new GunAnimationTimeline(score);
    }

    public GunAnimationTimeline with(Consumer<Score> changes) {
        return new GunAnimationTimeline(score.andThen(changes));
    }

    @Override
    public void accept(ItemStack stack, LambdaContext ctx) {
        var table = compiled;
        if (table == null) {
            var built = new Score();
            score.accept(built);
            compiled = table = built.compile();
        }
        table.play(stack, ctx);
    }

    public static final class Score {
        private @Nullable GunAnimationAction client;
        private final List<GunAnimationAction> perCall = new ArrayList<>();
        private final Map<GunAnimation, List<Span>> spans = new EnumMap<>(GunAnimation.class);
        private GunAnimation[] current = {};

        private Score() {}

        public Score client(GunAnimationAction hook) {
            client = hook;
            return this;
        }

        public Score always(GunAnimationAction cue) {
            perCall.add(cue);
            return this;
        }

        public Score on(GunAnimation... animations) {
            current = animations;
            return this;
        }

        public Score replace(GunAnimation animation) {
            spans.remove(animation);
            return on(animation);
        }

        public Score at(int tick, GunAnimationAction... cues) {
            for (var cue : cues) add(new Span(tick, tick + 1, 1, cue));
            return this;
        }

        public Score at(GunAnimationAction cue, int... ticks) {
            for (int tick : ticks) add(new Span(tick, tick + 1, 1, cue));
            return this;
        }

        public Score during(int from, int until, GunAnimationAction cue) {
            return add(new Span(from, until, 1, cue));
        }

        public Score every(int period, int until, GunAnimationAction cue) {
            return add(new Span(0, until, period, cue));
        }

        public Score from(int from, GunAnimationAction cue) {
            return add(new Span(from, Integer.MAX_VALUE, 1, cue));
        }

        private Score add(Span span) {
            for (var animation : current)
                spans.computeIfAbsent(animation, _ -> new ArrayList<>())
                        .add(span);
            return this;
        }

        private Compiled compile() {
            var lanes = new Lane[GunAnimation.VALUES.length];
            for (var entry : spans.entrySet())
                lanes[entry.getKey().ordinal()] = lane(entry.getValue());
            return new Compiled(client, perCall.toArray(NONE), lanes);
        }

        private static Lane lane(List<Span> spans) {
            int length = 0;
            for (var span : spans) length = Math.max(length, span.open() ? span.from : span.until);
            var ticks = new GunAnimationAction[length][];
            for (int tick = 0; tick < length; tick++) {
                var due = new ArrayList<GunAnimationAction>();
                for (var span : spans) if (span.covers(tick)) due.add(span.cue);
                ticks[tick] = due.isEmpty() ? NONE : due.toArray(NONE);
            }
            var beyond = new ArrayList<GunAnimationAction>();
            for (var span : spans) if (span.open()) beyond.add(span.cue);
            return new Lane(ticks, beyond.toArray(NONE));
        }
    }

    private record Span(int from, int until, int period, GunAnimationAction cue) {
        boolean open() {
            return until == Integer.MAX_VALUE;
        }

        boolean covers(int tick) {
            return tick >= from && tick < until && (tick - from) % period == 0;
        }
    }

    private record Lane(GunAnimationAction[][] ticks, GunAnimationAction[] beyond) {
        GunAnimationAction[] at(int tick) {
            return tick < ticks.length ? ticks[tick] : beyond;
        }
    }

    private record Compiled(
            @Nullable GunAnimationAction client, GunAnimationAction[] perCall, Lane[] lanes) {
        void play(ItemStack stack, LambdaContext ctx) {
            if (ctx.entity().level().isClientSide()) {
                if (client != null) client.play(stack, ctx);
                return;
            }
            int config = ctx.configIndex();
            var lane = lanes[GunBaseNTItem.getLastAnim(stack, config).ordinal()];
            int tick = GunBaseNTItem.getAnimTimer(stack, config);
            for (var cue : perCall) cue.play(stack, ctx);
            if (lane != null) for (var cue : lane.at(tick)) cue.play(stack, ctx);
        }
    }
}
