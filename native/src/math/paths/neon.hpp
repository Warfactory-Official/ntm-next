// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LicenseRef-All-Rights-Reserved

#pragma once

#include "math/simplex.hpp"

#if defined(__aarch64__)

namespace ntm::math {

    using SimplexNeon = ntm::simd::Neon2<true>;

    inline void simplex_accumulate_neon(const uint32_t *__restrict perm, const double *__restrict gradX,
                                        const double *__restrict gradY, const double *__restrict xs,
                                        const double *__restrict ys, const double scale, const double weight,
                                        double *__restrict acc, const uint32_t len) noexcept {
        simplex_accumulate_simd<SimplexNeon>(perm, gradX, gradY, xs, ys, scale, weight, acc, len);
    }
}

#endif
