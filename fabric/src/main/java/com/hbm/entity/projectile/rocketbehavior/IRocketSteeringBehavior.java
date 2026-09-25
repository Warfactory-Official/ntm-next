// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.projectile.rocketbehavior;

import com.hbm.entity.projectile.EntityArtilleryRocket;

public interface IRocketSteeringBehavior {

    void adjustCourse(EntityArtilleryRocket rocket, double speed, double turnSpeed);
}
