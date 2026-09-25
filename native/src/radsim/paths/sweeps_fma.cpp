// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LicenseRef-All-Rights-Reserved

#include "radsim/sweeps_simd.hpp"

#if HBM_RADSIM_X86_SIMD

#if defined(__GNUC__) && !defined(__clang__)
#pragma GCC target("fma")
#else
#pragma clang attribute push(__attribute__((target("fma"))), apply_to = function)
#endif

namespace hbm::radsim::detail {
    double exchange_uni_multi_fma(double ra, const uint16_t *__restrict__ pocket, const double *__restrict__ uni_share,
                                  const double *__restrict__ pocket_share, double *__restrict__ density, size_t n,
                                  size_t density_count) noexcept {
        return exchange_uni_multi_body<true>(ra, pocket, uni_share, pocket_share, density, n, density_count);
    }
}

#if !defined(__GNUC__) || defined(__clang__)
#pragma clang attribute pop
#endif
#endif
