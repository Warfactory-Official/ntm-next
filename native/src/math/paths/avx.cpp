// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LicenseRef-All-Rights-Reserved

#include "math/kernel.hpp"

#if defined(__x86_64__)

#if defined(__GNUC__) && !defined(__clang__)
#pragma GCC target("avx,fma")
#else
#pragma clang attribute push(__attribute__((target("avx,fma"))), apply_to = function)
#endif

#include "math/paths/avx.hpp"

namespace ntm::math {
    void simplex_accumulate_avx_entry(const uint32_t *perm, const double *gradX, const double *gradY, const double *xs,
                                      const double *ys, double scale, double weight, double *acc,
                                      uint32_t len) noexcept {
        simplex_accumulate_avx(perm, gradX, gradY, xs, ys, scale, weight, acc, len);
    }
}

#if !defined(__GNUC__) || defined(__clang__)
#pragma clang attribute pop
#endif
#endif
