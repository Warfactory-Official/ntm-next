// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LicenseRef-All-Rights-Reserved

#pragma once

#include "radsim/sweeps_simd.hpp"
#include "radsim/sweeps_traits.hpp"

#if HBM_RADSIM_X86_SIMD

namespace hbm::radsim::detail {

    template <SweepTraits V>
    inline uint32_t exchange_uni_column_kernel(double *__restrict__ ra, double *__restrict__ rb, int32_t n,
                                               double uniform_exchange, uint32_t lanes) noexcept {
        const typename V::Vec half = V::set1(0.5);
        const typename V::Vec e = V::set1(uniform_exchange);
        uint32_t changed = 0u;
        int32_t i = 0;
        constexpr int32_t kW = static_cast<int32_t>(V::kWidth);
        for (; i + kW <= n; i += kW) {
            const uint32_t bits = V::lane_bits(lanes, i);
            if (bits == 0u)
                continue;
            const typename V::Vec a = V::loadu(ra + i);
            const typename V::Vec b = V::loadu(rb + i);

            const typename V::Mask ne = V::mask_and(V::cmp_neq(a, b), V::lane_keep(bits));
            const uint32_t moved = V::mask_bits(ne);
            if (moved == 0u)
                continue;

            const typename V::Vec avg = V::mul(V::add(a, b), half);
            const typename V::Vec delta = V::mul(V::mul(V::sub(a, b), half), e);

            V::store_sel(ra + i, ne, a, V::add(avg, delta));
            V::store_sel(rb + i, ne, b, V::sub(avg, delta));
            changed |= moved << i;
        }

        if (i < n) [[unlikely]]
            changed |= exchange_uni_column_scalar(ra, rb, i, n, uniform_exchange, lanes);
        return changed;
    }

    template <SweepTraits V>
    inline uint32_t exchange_uni_y_kernel(double *__restrict__ d, int32_t off, int32_t n, double uniform_exchange,
                                          uint32_t lanes) noexcept {
        const typename V::Vec half = V::set1(0.5);
        const typename V::Vec e = V::set1(uniform_exchange);
        uint32_t changed = 0u;
        int32_t i = off;
        constexpr int32_t kW = static_cast<int32_t>(V::kWidth);
        for (; i + kW <= n; i += kW) {
            const uint32_t bits = V::lane_bits(lanes, i);
            if (bits == 0u)
                continue;
            const typename V::Vec v = V::loadu(d + i);
            const typename V::Vec w = V::swap_pairs(v);
            const typename V::Mask ne = V::mask_and(V::cmp_neq(v, w), V::lane_keep(bits));
            const uint32_t moved = V::mask_bits(ne);
            if (moved == 0u)
                continue;

            const typename V::Vec avg = V::mul(V::add(v, w), half);
            const typename V::Vec delta = V::mul(V::mul(V::sub(v, w), half), e);
            V::store_sel(d + i, ne, v, V::add(avg, delta));
            changed |= moved << i;
        }
        if (i < n) [[unlikely]]
            changed |= exchange_uni_y_scalar(d, i, n, uniform_exchange, lanes);
        return changed;
    }
}

#endif
