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

package com.hbm.config;

public final class RenderConfig {

    public static volatile boolean skyboxes = true;
    public static volatile boolean coolingTowerParticles = true;
    public static volatile boolean cableHang = true;
    public static volatile boolean simpleRebar = false;

    private RenderConfig() {}

    public static void loadFrom(ConfigStore c) {
        skyboxes = c.get(ConfigSchema.ENABLE_SKYBOXES);
        coolingTowerParticles = c.get(ConfigSchema.COOLING_TOWER_PARTICLES);
        cableHang = c.get(ConfigSchema.RENDER_CABLE_HANG);
        simpleRebar = c.get(ConfigSchema.RENDER_REBAR_SIMPLE);
    }
}
