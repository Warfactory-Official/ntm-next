// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LicenseRef-All-Rights-Reserved

#include "math/kernel.hpp"

#if defined(__aarch64__)

#include "math/paths/neon.hpp"

namespace ntm::math {
    void simplex_accumulate_neon_entry(const uint32_t *perm, const double *gradX, const double *gradY, const double *xs,
                                       const double *ys, double scale, double weight, double *acc,
                                       uint32_t len) noexcept {
        simplex_accumulate_neon(perm, gradX, gradY, xs, ys, scale, weight, acc, len);
    }
}

#endif
