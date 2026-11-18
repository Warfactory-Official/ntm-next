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

package com.hbm.util.datafix;

import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import java.util.stream.Stream;

final class Release14Fix extends DataFix {
    Release14Fix(Schema schema) {
        super(schema, false);
    }

    @Override
    protected TypeRewriteRule makeRule() {
        var schema = getOutputSchema();
        return TypeRewriteRule.seq(
                Stream.of(
                                new MultiblockCellLayoutFix(schema),
                                new BlockStatePropertyFix(schema),
                                new FallingMultiblockCellFix(schema),
                                new ProjectileStateFix(schema),
                                new GrenadeTimerFix(schema),
                                new BulletDamageFix(schema),
                                new AntiBallisticVelocityFix(schema),
                                new BlastDoorLatchFix(schema),
                                new ExplosionClockFix(schema),
                                new StructureCellLayoutFix(schema),
                                new UnpinnedLockFix(schema))
                        .map(DataFix::getRule)
                        .toList());
    }
}
