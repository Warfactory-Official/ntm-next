// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LicenseRef-All-Rights-Reserved

#pragma once

#include "math/simplex.hpp"

#if defined(__x86_64__)

namespace ntm::math {

    using SimplexAvx = ntm::simd::Avx4<true>;

    inline void simplex_accumulate_avx(const uint32_t *__restrict perm, const double *__restrict gradX,
                                       const double *__restrict gradY, const double *__restrict xs,
                                       const double *__restrict ys, const double scale, const double weight,
                                       double *__restrict acc, const uint32_t len) noexcept {
        simplex_accumulate_simd<SimplexAvx>(perm, gradX, gradY, xs, ys, scale, weight, acc, len);
    }
}

#endif
