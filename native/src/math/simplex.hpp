// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LicenseRef-All-Rights-Reserved

#pragma once

#include "math/kernel.hpp"
#include "simd/traits.hpp"

#if defined(__x86_64__) || defined(__aarch64__)

namespace ntm::math {

    [[gnu::always_inline]] inline void gather_pair(double *__restrict dst, const double *__restrict table,
                                                   const uint32_t k0, const uint32_t k1) noexcept {
#if defined(__x86_64__)
        _mm_store_pd(dst, _mm_loadh_pd(_mm_load_sd(table + k0), table + k1));
#else
        dst[0] = table[k0];
        dst[1] = table[k1];
#endif
    }

    template <class V>
    inline void simplex_accumulate_simd(const uint32_t *__restrict perm, const double *__restrict gradX,
                                        const double *__restrict gradY, const double *__restrict xs,
                                        const double *__restrict ys, const double scale, const double weight,
                                        double *__restrict acc, const uint32_t len) noexcept {
        using Vec = typename V::Vec;
        using IVec = typename V::IVec;

        alignas(V::kAlign) __attribute__((uninitialized)) double dsc[12][kTile];
        alignas(V::kIAlign) __attribute__((uninitialized)) int32_t isc[3][kTile];
        double (&x0)[kTile] = dsc[0];
        double (&y0)[kTile] = dsc[1];
        double (&x1)[kTile] = dsc[2];
        double (&y1)[kTile] = dsc[3];
        double (&x2)[kTile] = dsc[4];
        double (&y2)[kTile] = dsc[5];
        double (&gx0)[kTile] = dsc[6];
        double (&gy0)[kTile] = dsc[7];
        double (&gx1)[kTile] = dsc[8];
        double (&gy1)[kTile] = dsc[9];
        double (&gx2)[kTile] = dsc[10];
        double (&gy2)[kTile] = dsc[11];
        int32_t (&ii)[kTile] = isc[0];
        int32_t (&jj)[kTile] = isc[1];
        int32_t (&i1)[kTile] = isc[2];

        const Vec v_scale = V::set1(scale);
        const Vec v_skew = V::set1(kSkew2D);
        const Vec v_unskew = V::set1(kUnskew2D);
        const Vec v_one = V::set1(1.0);
        const Vec v_half = V::set1(0.5);
        const Vec v_zero = V::zero();
        const Vec v_2unskew = V::set1(2.0 * kUnskew2D);
        const Vec v_70 = V::set1(70.0);
        const Vec v_weight = V::set1(weight);
        const IVec v_ff = V::i_set1(0xFF);

        for (uint32_t base = 0; base < len; base += kTile) {
            const uint32_t n = (len - base < kTile) ? (len - base) : kTile;
            uint32_t k = 0;

            for (; k + V::kWidth <= n; k += V::kWidth) {
                const Vec xin = V::mul(V::loadu(xs + base + k), v_scale);
                const Vec yin = V::mul(V::loadu(ys + base + k), v_scale);
                const Vec skew = V::add(xin, yin);

                const Vec fi = V::floor(V::fma(skew, v_skew, xin));
                const Vec fj = V::floor(V::fma(skew, v_skew, yin));
                const IVec i32 = V::to_i32(fi);
                const IVec j32 = V::to_i32(fj);

                const Vec ij = V::from_i32(V::i_add(i32, j32));
                const Vec px = V::sub(xin, V::fnma(ij, v_unskew, fi));
                const Vec py = V::sub(yin, V::fnma(ij, v_unskew, fj));

                const Vec ad = V::select(V::cmp_gt(px, py), v_one, v_zero);

                V::store(x0 + k, px);
                V::store(y0 + k, py);
                V::store(x1 + k, V::add(V::sub(px, ad), v_unskew));
                V::store(y1 + k, V::add(V::sub(py, V::sub(v_one, ad)), v_unskew));
                V::store(x2 + k, V::add(V::sub(px, v_one), v_2unskew));
                V::store(y2 + k, V::add(V::sub(py, v_one), v_2unskew));
                V::i_store(ii + k, V::i_and(i32, v_ff));
                V::i_store(jj + k, V::i_and(j32, v_ff));
                V::i_store(i1 + k, V::to_i32(ad));
            }
            for (; k < n; k++) {
                const Lattice l = lattice(xs[base + k], ys[base + k], scale);
                x0[k] = l.x0;
                y0[k] = l.y0;
                x1[k] = l.x1;
                y1[k] = l.y1;
                x2[k] = l.x2;
                y2[k] = l.y2;
                ii[k] = l.ii;
                jj[k] = l.jj;
                i1[k] = l.a;
            }

            uint32_t q = 0;
            for (; q + 2u <= n; q += 2u) {
                uint32_t kk[3][2];
                for (int e = 0; e < 2; e++) {
                    lookup_keys(perm, static_cast<uint32_t>(ii[q + e]), static_cast<uint32_t>(jj[q + e]),
                                static_cast<uint32_t>(i1[q + e]), kk[0][e], kk[1][e], kk[2][e]);
                }
                double *const dx[3] = {gx0 + q, gx1 + q, gx2 + q};
                double *const dy[3] = {gy0 + q, gy1 + q, gy2 + q};
                for (int c = 0; c < 3; c++) {
                    gather_pair(dx[c], gradX, kk[c][0], kk[c][1]);
                    gather_pair(dy[c], gradY, kk[c][0], kk[c][1]);
                }
            }
            for (; q < n; q++) {
                uint32_t k0, k1, k2;
                lookup_keys(perm, static_cast<uint32_t>(ii[q]), static_cast<uint32_t>(jj[q]),
                            static_cast<uint32_t>(i1[q]), k0, k1, k2);
                gx0[q] = gradX[k0];
                gy0[q] = gradY[k0];
                gx1[q] = gradX[k1];
                gy1[q] = gradY[k1];
                gx2[q] = gradX[k2];
                gy2[q] = gradY[k2];
            }

            k = 0;
            for (; k + V::kWidth <= n; k += V::kWidth) {
                Vec c = v_zero;
                const double *cx[3] = {x0 + k, x1 + k, x2 + k};
                const double *cy[3] = {y0 + k, y1 + k, y2 + k};
                const double *cgx[3] = {gx0 + k, gx1 + k, gx2 + k};
                const double *cgy[3] = {gy0 + k, gy1 + k, gy2 + k};
                for (int corner_index = 0; corner_index < 3; corner_index++) {
                    const Vec x = V::load(cx[corner_index]);
                    const Vec y = V::load(cy[corner_index]);
                    const Vec gx = V::load(cgx[corner_index]);
                    const Vec gy = V::load(cgy[corner_index]);
                    const Vec t0 = V::max(V::fnma(y, y, V::fnma(x, x, v_half)), v_zero);
                    const Vec squared = V::mul(t0, t0);
                    const Vec dot = V::fma(gy, y, V::mul(gx, x));
                    const Vec weight = V::mul(squared, squared);

                    c = corner_index == 0 ? V::mul(weight, dot) : V::fma(weight, dot, c);
                }
                V::storeu(acc + base + k, V::fma(V::mul(c, v_70), v_weight, V::loadu(acc + base + k)));
            }
            for (; k < n; k++) {
                const double c = three_corners(corner_parts(gx0[k], gy0[k], x0[k], y0[k]),
                                               corner_parts(gx1[k], gy1[k], x1[k], y1[k]),
                                               corner_parts(gx2[k], gy2[k], x2[k], y2[k]));
                acc[base + k] = fuse(c * 70.0, weight, acc[base + k]);
            }
        }
    }
}

#endif
