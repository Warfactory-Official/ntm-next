// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LicenseRef-All-Rights-Reserved

#pragma once

#include "radsim/cpu_profile.hpp"
#include "radsim/internal.hpp"

#include <cassert>
#include <cmath>

namespace hbm::radsim::detail {

    inline uint32_t exchange_uni_column_scalar(double *__restrict__ ra, double *__restrict__ rb, int32_t off, int32_t n,
                                               double uniform_exchange, uint32_t lanes) noexcept {
        uint32_t changed = 0u;
        for (int32_t i = off; i < n; i++) {
            if (((lanes >> i) & 1u) == 0u)
                continue;
            if (ra[i] == rb[i])
                continue;
            const double avg = 0.5 * (ra[i] + rb[i]);
            const double delta = 0.5 * (ra[i] - rb[i]) * uniform_exchange;
            ra[i] = avg + delta;
            rb[i] = avg - delta;
            changed |= 1u << i;
        }
        return changed;
    }

    inline uint32_t exchange_uni_y_scalar(double *__restrict__ d, int32_t off, int32_t n, double uniform_exchange,
                                          uint32_t lanes) noexcept {
        uint32_t changed = 0u;
        for (int32_t sy = off; sy + 1 < n; sy += 2) {
            if (((lanes >> sy) & 3u) != 3u)
                continue;
            const double a = d[sy];
            const double b = d[sy + 1];
            if (a == b)
                continue;
            const double avg = 0.5 * (a + b);
            const double delta = 0.5 * (a - b) * uniform_exchange;
            d[sy] = avg + delta;
            d[sy + 1] = avg - delta;
            changed |= 3u << sy;
        }
        return changed;
    }

    template <bool kFuse>
    inline double exchange_uni_multi_body(double ra, const uint16_t *__restrict__ pocket,
                                          const double *__restrict__ uni_share, const double *__restrict__ pocket_share,
                                          double *__restrict__ density, size_t n,
                                          [[maybe_unused]] size_t density_count) noexcept {
        for (size_t i = 0; i < n; i++) {
            const size_t idx = pocket[i];

            assert(idx < density_count);
            const double rb = density[idx];
            const double gap = rb - ra;
            if constexpr (kFuse) {
                density[idx] = std::fma(-pocket_share[i], gap, rb);
                ra = std::fma(uni_share[i], gap, ra);
            } else {
                density[idx] = rb - pocket_share[i] * gap;
                ra = ra + uni_share[i] * gap;
            }
        }
        return ra;
    }

    inline double exchange_uni_multi_scalar(double ra, const uint16_t *__restrict__ pocket,
                                            const double *__restrict__ uni_share,
                                            const double *__restrict__ pocket_share, double *__restrict__ density,
                                            size_t n, size_t density_count) noexcept {
        return exchange_uni_multi_body<false>(ra, pocket, uni_share, pocket_share, density, n, density_count);
    }

    inline double exchange_uni_multi_soft_fma(double ra, const uint16_t *__restrict__ pocket,
                                              const double *__restrict__ uni_share,
                                              const double *__restrict__ pocket_share, double *__restrict__ density,
                                              size_t n, size_t density_count) noexcept {
        return exchange_uni_multi_body<true>(ra, pocket, uni_share, pocket_share, density, n, density_count);
    }

#if HBM_RADSIM_X86_SIMD

    uint32_t exchange_uni_column_avx2(double *__restrict__ ra, double *__restrict__ rb, int32_t n,
                                      double uniform_exchange, uint32_t lanes) noexcept;

    uint32_t exchange_uni_column_avx512(double *__restrict__ ra, double *__restrict__ rb, int32_t n,
                                        double uniform_exchange, uint32_t lanes) noexcept;

    uint32_t exchange_uni_y_avx2(double *__restrict__ d, int32_t off, int32_t n, double uniform_exchange,
                                 uint32_t lanes) noexcept;

    uint32_t exchange_uni_y_avx512(double *__restrict__ d, int32_t off, int32_t n, double uniform_exchange,
                                   uint32_t lanes) noexcept;

    uint32_t exchange_uni_column_avx(double *__restrict__ ra, double *__restrict__ rb, int32_t n,
                                     double uniform_exchange, uint32_t lanes) noexcept;

    uint32_t exchange_uni_y_avx(double *__restrict__ d, int32_t off, int32_t n, double uniform_exchange,
                                uint32_t lanes) noexcept;

    double exchange_uni_multi_fma(double ra, const uint16_t *__restrict__ pocket, const double *__restrict__ uni_share,
                                  const double *__restrict__ pocket_share, double *__restrict__ density, size_t n,
                                  size_t density_count) noexcept;

    inline bool exchange_uni_column_use_avx512() noexcept { return cpu_profile().widest_column_path_is_512(); }

    inline bool exchange_uni_column_use_avx2() noexcept { return cpu_profile().has_avx2 && cpu_profile().has_fma; }

    inline bool exchange_uni_column_use_avx() noexcept { return cpu_profile().has_avx; }
#endif

    inline double exchange_uni_multi_fused(double ra, const uint16_t *__restrict__ pocket,
                                           const double *__restrict__ uni_share,
                                           const double *__restrict__ pocket_share, double *__restrict__ density,
                                           size_t n, size_t density_count) noexcept {
#if HBM_RADSIM_X86_SIMD
        if (cpu_profile().has_fma) {
            return exchange_uni_multi_fma(ra, pocket, uni_share, pocket_share, density, n, density_count);
        }
#endif
        return exchange_uni_multi_soft_fma(ra, pocket, uni_share, pocket_share, density, n, density_count);
    }
}
