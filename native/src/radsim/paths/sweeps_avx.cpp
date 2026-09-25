// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LicenseRef-All-Rights-Reserved

#include "radsim/sweeps_simd.hpp"

#if HBM_RADSIM_X86_SIMD
#include <immintrin.h>

#if defined(__GNUC__) && !defined(__clang__)
#pragma GCC target("avx")
#else
#pragma clang attribute push(__attribute__((target("avx"))), apply_to = function)
#endif

#include "radsim/sweeps_kernel.hpp"
#include "radsim/sweeps_traits.hpp"

namespace hbm::radsim::detail {

    struct LaneMaskTable {
        alignas(32) uint64_t v[16][4];
    };

    constexpr LaneMaskTable make_lane_masks() {
        LaneMaskTable table{};
        for (int mask = 0; mask < 16; mask++) {
            for (int lane = 0; lane < 4; lane++) {
                table.v[mask][lane] = ((mask >> lane) & 1) ? ~static_cast<uint64_t>(0) : static_cast<uint64_t>(0);
            }
        }
        return table;
    }

    constexpr LaneMaskTable kLaneMasks = make_lane_masks();

    struct AvxTraits : Avx256Base<ntm::simd::kTagRadsimAvx> {
        static Mask lane_keep(uint32_t bits) noexcept {
            return _mm256_load_pd(reinterpret_cast<const double *>(kLaneMasks.v[bits & 0xFu]));
        }
    };

    static_assert(SweepTraits<AvxTraits>);

    uint32_t exchange_uni_column_avx(double *__restrict__ ra, double *__restrict__ rb, int32_t n,
                                     double uniform_exchange, uint32_t lanes) noexcept {
        return exchange_uni_column_kernel<AvxTraits>(ra, rb, n, uniform_exchange, lanes);
    }

    uint32_t exchange_uni_y_avx(double *__restrict__ d, int32_t off, int32_t n, double uniform_exchange,
                                uint32_t lanes) noexcept {
        return exchange_uni_y_kernel<AvxTraits>(d, off, n, uniform_exchange, lanes);
    }
}

#if !defined(__GNUC__) || defined(__clang__)
#pragma clang attribute pop
#endif
#endif
