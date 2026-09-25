// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LicenseRef-All-Rights-Reserved

#pragma once

#include "radsim/sweeps_simd.hpp"
#include "simd/traits.hpp"

#if HBM_RADSIM_X86_SIMD
#include <immintrin.h>

#include <concepts>

namespace hbm::radsim::detail {

    template <class V>
    concept SweepTraits = requires(typename V::Vec v, typename V::Mask m, const double *src, double *dst, uint32_t bits,
                                   int32_t lane, double x) {
        typename V::Vec;
        typename V::Mask;
        { V::kWidth } -> std::convertible_to<int32_t>;

        { V::set1(x) } -> std::same_as<typename V::Vec>;
        { V::loadu(src) } -> std::same_as<typename V::Vec>;
        { V::add(v, v) } -> std::same_as<typename V::Vec>;
        { V::sub(v, v) } -> std::same_as<typename V::Vec>;
        { V::mul(v, v) } -> std::same_as<typename V::Vec>;
        { V::swap_pairs(v) } -> std::same_as<typename V::Vec>;

        { V::lane_bits(bits, lane) } -> std::same_as<uint32_t>;
        { V::lane_keep(bits) } -> std::same_as<typename V::Mask>;
        { V::cmp_neq(v, v) } -> std::same_as<typename V::Mask>;
        { V::mask_and(m, m) } -> std::same_as<typename V::Mask>;
        { V::mask_bits(m) } -> std::same_as<uint32_t>;
        { V::store_sel(dst, m, v, v) } -> std::same_as<void>;
    };

    template <int kArmTag> struct Avx256Base : ntm::simd::Avx256Core<kArmTag> {
        using Vec = typename ntm::simd::Avx256Core<kArmTag>::Vec;
        using Mask = typename ntm::simd::Avx256Core<kArmTag>::Mask;

        static Vec swap_pairs(Vec v) noexcept { return _mm256_permute_pd(v, 0x5); }

        static uint32_t lane_bits(uint32_t lanes, int32_t i) noexcept { return (lanes >> i) & 0xFu; }
        static Mask cmp_neq(Vec a, Vec b) noexcept { return _mm256_cmp_pd(a, b, _CMP_NEQ_UQ); }
        static Mask mask_and(Mask a, Mask b) noexcept { return _mm256_and_pd(a, b); }
        static uint32_t mask_bits(Mask m) noexcept { return static_cast<uint32_t>(_mm256_movemask_pd(m)); }
        static void store_sel(double *p, Mask m, Vec orig, Vec val) noexcept {
            _mm256_storeu_pd(p, _mm256_blendv_pd(orig, val, m));
        }
    };
}

#endif
