// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LicenseRef-All-Rights-Reserved

#include "radsim/sweeps_simd.hpp"

#include <cmath>

#if HBM_RADSIM_X86_SIMD
#include <immintrin.h>

#if defined(__GNUC__) && !defined(__clang__)
#pragma GCC target("avx2,fma")
#else
#pragma clang attribute push(__attribute__((target("avx2,fma"))), apply_to = function)
#endif

#include "radsim/sweeps_kernel.hpp"
#include "radsim/sweeps_traits.hpp"

namespace hbm::radsim::detail {

    struct Avx2Traits : Avx256Base<ntm::simd::kTagRadsimAvx2> {
        static Mask lane_keep(uint32_t bits) noexcept {
            const __m256i lane_bit = _mm256_setr_epi64x(1, 2, 4, 8);
            return _mm256_castsi256_pd(_mm256_cmpeq_epi64(
                    _mm256_and_si256(_mm256_set1_epi64x(static_cast<long long>(bits)), lane_bit), lane_bit));
        }
    };

    static_assert(SweepTraits<Avx2Traits>);

    uint32_t exchange_uni_column_avx2(double *__restrict__ ra, double *__restrict__ rb, int32_t n,
                                      double uniform_exchange, uint32_t lanes) noexcept {
        return exchange_uni_column_kernel<Avx2Traits>(ra, rb, n, uniform_exchange, lanes);
    }

    uint32_t exchange_uni_y_avx2(double *__restrict__ d, int32_t off, int32_t n, double uniform_exchange,
                                 uint32_t lanes) noexcept {
        return exchange_uni_y_kernel<Avx2Traits>(d, off, n, uniform_exchange, lanes);
    }
}

#if !defined(__GNUC__) || defined(__clang__)
#pragma clang attribute pop
#endif
#endif
