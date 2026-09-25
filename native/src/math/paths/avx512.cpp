// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LicenseRef-All-Rights-Reserved

#include "math/kernel.hpp"

#if defined(__x86_64__)

#include "simd/avx512_target.h"

#if defined(__GNUC__) && !defined(__clang__)
#pragma GCC target("avx512f")
#else
#pragma clang attribute push(__attribute__((target(NTM_TARGET_AVX512F))), apply_to = function)
#endif

#include "math/paths/avx512.hpp"

namespace ntm::math {
    void simplex_accumulate_avx512_entry(const uint32_t *perm, const double *gradX, const double *gradY,
                                         const double *xs, const double *ys, double scale, double weight, double *acc,
                                         uint32_t len) noexcept {
        simplex_accumulate_avx512(perm, gradX, gradY, xs, ys, scale, weight, acc, len);
    }
}

#if !defined(__GNUC__) || defined(__clang__)
#pragma clang attribute pop
#endif
#endif
