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

package com.hbm.handler.resistance;

import com.hbm.api.entity.DamageResistanceProvider.Protection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.jspecify.annotations.Nullable;

public final class DamageResistanceProfile {

    private final Map<String, Protection> exact;
    private final @Nullable Protection[] byCategory;
    private final @Nullable Protection fallback;
    private final String[] lineKeys;
    private final String[] lineValues;

    private DamageResistanceProfile(Builder builder) {
        exact = new HashMap<>(builder.exact);
        byCategory = new Protection[DamageCategory.values().length];
        builder.categories.forEach((category, entry) -> byCategory[category.ordinal()] = entry);
        fallback = builder.fallback;

        int lines = builder.categories.size() + builder.exact.size() + (fallback != null ? 1 : 0);
        lineKeys = new String[lines];
        lineValues = new String[lines];
        int line = 0;
        for (var entry : builder.categories.entrySet()) {
            line = line(line, entry.getKey().translationKey(), entry.getValue());
        }
        for (var entry : builder.exact.entrySet()) {
            line = line(line, "damage.exact." + entry.getKey(), entry.getValue());
        }
        if (fallback != null) line(line, "damage.other", fallback);
    }

    private int line(int at, String key, Protection entry) {
        lineKeys[at] = key;
        lineValues[at] = ": " + entry.threshold() + "/" + (int) (entry.resistance() * 100F) + "%";
        return at + 1;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Nullable Protection against(
            String messageId, @Nullable DamageCategory category, boolean bypassesArmor) {
        if (!exact.isEmpty()) {
            Protection hit = exact.get(messageId);
            if (hit != null) return hit;
        }
        if (category != null) {
            Protection hit = byCategory[category.ordinal()];
            if (hit != null) return hit;
        }
        return bypassesArmor ? null : fallback;
    }

    int lineCount() {
        return lineKeys.length;
    }

    String lineKey(int line) {
        return lineKeys[line];
    }

    String lineValue(int line) {
        return lineValues[line];
    }

    public static final class Builder {
        private final Map<DamageCategory, Protection> categories = new LinkedHashMap<>();
        private final Map<String, Protection> exact = new LinkedHashMap<>();
        private @Nullable Protection fallback;

        private Builder() {}

        public Builder category(DamageCategory category, float threshold, float resistance) {
            categories.put(category, entry(threshold, resistance));
            return this;
        }

        public Builder exact(String messageId, float threshold, float resistance) {
            exact.put(messageId.toLowerCase(Locale.ROOT), entry(threshold, resistance));
            return this;
        }

        public Builder fallback(float threshold, float resistance) {
            fallback = entry(threshold, resistance);
            return this;
        }

        public DamageResistanceProfile build() {
            return new DamageResistanceProfile(this);
        }

        private static Protection entry(float threshold, float resistance) {
            if (!(threshold >= 0F) || threshold == Float.POSITIVE_INFINITY) {
                throw new IllegalArgumentException(
                        "Damage threshold " + threshold + " is not a finite value >= 0");
            }
            if (!(resistance >= 0F && resistance <= 1F)) {
                throw new IllegalArgumentException(
                        "Damage resistance " + resistance + " is outside [0, 1]");
            }
            return new Protection(threshold, resistance);
        }
    }
}
