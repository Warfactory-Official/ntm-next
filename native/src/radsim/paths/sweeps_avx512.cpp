// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LicenseRef-All-Rights-Reserved

#include "radsim/sweeps_simd.hpp"

#if HBM_RADSIM_X86_SIMD
#include <immintrin.h>

#include "simd/avx512_target.h"

#if defined(__GNUC__) && !defined(__clang__)
#pragma GCC target("avx512f")
#else
#pragma clang attribute push(__attribute__((target(NTM_TARGET_AVX512F))), apply_to = function)
#endif

#include "radsim/sweeps_kernel.hpp"
#include "radsim/sweeps_traits.hpp"

namespace hbm::radsim::detail {

    struct Avx512Traits : ntm::simd::Avx512Core<ntm::simd::kTagRadsimAvx512> {

        static Vec swap_pairs(Vec v) noexcept { return _mm512_permute_pd(v, 0x55); }

        static uint32_t lane_bits(uint32_t lanes, int32_t i) noexcept { return (lanes >> i) & 0xFFu; }
        static Mask lane_keep(uint32_t bits) noexcept { return static_cast<Mask>(bits); }
        static Mask cmp_neq(Vec a, Vec b) noexcept { return _mm512_cmp_pd_mask(a, b, _CMP_NEQ_UQ); }
        static Mask mask_and(Mask a, Mask b) noexcept { return static_cast<Mask>(a & b); }
        static uint32_t mask_bits(Mask m) noexcept { return static_cast<uint32_t>(m); }
        static void store_sel(double *p, Mask m, Vec, Vec val) noexcept { _mm512_mask_storeu_pd(p, m, val); }
    };

    static_assert(SweepTraits<Avx512Traits>);

    uint32_t exchange_uni_column_avx512(double *__restrict__ ra, double *__restrict__ rb, int32_t n,
                                        double uniform_exchange, uint32_t lanes) noexcept {
        return exchange_uni_column_kernel<Avx512Traits>(ra, rb, n, uniform_exchange, lanes);
    }

    uint32_t exchange_uni_y_avx512(double *__restrict__ d, int32_t off, int32_t n, double uniform_exchange,
                                   uint32_t lanes) noexcept {
        return exchange_uni_y_kernel<Avx512Traits>(d, off, n, uniform_exchange, lanes);
    }
}

#if !defined(__GNUC__) || defined(__clang__)
#pragma clang attribute pop
#endif
#endif
