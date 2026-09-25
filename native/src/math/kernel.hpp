// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LicenseRef-All-Rights-Reserved

#pragma once

#include <cmath>
#include <cstdint>

namespace ntm::math {

    inline constexpr double kSkew2D = 0.3660254037844386;

    inline constexpr double kUnskew2D = 0.21132486540518713;

    inline constexpr uint32_t kTile = 24u;

    [[gnu::always_inline]] inline double fuse(const double a, const double b, const double c) noexcept {
        return std::fma(a, b, c);
    }

    [[gnu::always_inline]] inline double nfuse(const double a, const double b, const double c) noexcept {
        return std::fma(-a, b, c);
    }

    [[gnu::always_inline]] inline int32_t floor_i32(const double x) noexcept {
        const auto i = static_cast<int32_t>(x);
        return x < static_cast<double>(i) ? i - 1 : i;
    }

    struct CornerParts {
        double weight;
        double dot;
    };

    [[gnu::always_inline]] inline CornerParts corner_parts(const double gx, const double gy, const double x,
                                                           const double y) noexcept {

        const double t = nfuse(y, y, nfuse(x, x, 0.5));

        const double t0 = t > 0.0 ? t : 0.0;
        const double squared = t0 * t0;

        return {squared * squared, fuse(gy, y, gx * x)};
    }

    [[gnu::always_inline]] inline double three_corners(const CornerParts p0, const CornerParts p1,
                                                       const CornerParts p2) noexcept {
        double c = p0.weight * p0.dot;
        c = fuse(p1.weight, p1.dot, c);
        return fuse(p2.weight, p2.dot, c);
    }

    struct Lattice {
        double x0, y0, x1, y1, x2, y2;
        int32_t ii, jj, a;
    };

    [[gnu::always_inline]] inline Lattice lattice(const double x, const double y, const double scale) noexcept {
        const double xin = x * scale;
        const double yin = y * scale;

        const double skew = xin + yin;
        const int32_t i = floor_i32(fuse(skew, kSkew2D, xin));
        const int32_t j = floor_i32(fuse(skew, kSkew2D, yin));

        const double ij =
                static_cast<double>(static_cast<int32_t>(static_cast<uint32_t>(i) + static_cast<uint32_t>(j)));

        const double px = xin - nfuse(ij, kUnskew2D, static_cast<double>(i));
        const double py = yin - nfuse(ij, kUnskew2D, static_cast<double>(j));

        const int32_t a = px > py ? 1 : 0;
        return {px,
                py,
                px - static_cast<double>(a) + kUnskew2D,
                py - static_cast<double>(1 - a) + kUnskew2D,
                px - 1.0 + 2.0 * kUnskew2D,
                py - 1.0 + 2.0 * kUnskew2D,
                static_cast<int32_t>(static_cast<uint32_t>(i) & 0xFFu),
                static_cast<int32_t>(static_cast<uint32_t>(j) & 0xFFu),
                a};
    }

    [[gnu::always_inline]] inline void lookup_keys(const uint32_t *__restrict perm, const uint32_t a, const uint32_t b,
                                                   const uint32_t ia, uint32_t &k0, uint32_t &k1,
                                                   uint32_t &k2) noexcept {
        const uint32_t pb = perm[b];
        const uint32_t pb1 = perm[(b + 1u) & 0xFFu];
        k0 = (a + pb) & 0xFFu;
        k1 = (a + ia + (ia ? pb : pb1)) & 0xFFu;
        k2 = (a + 1u + pb1) & 0xFFu;
    }

    inline void simplex_accumulate(const uint32_t *__restrict perm, const double *__restrict gradX,
                                   const double *__restrict gradY, const double *__restrict xs,
                                   const double *__restrict ys, const double scale, const double weight,
                                   double *__restrict acc, const uint32_t len) noexcept {

        __attribute__((uninitialized)) double d_scratch[12][kTile];
        __attribute__((uninitialized)) int32_t i_scratch[4][kTile];
        double (&x0)[kTile] = d_scratch[0];
        double (&y0)[kTile] = d_scratch[1];
        double (&x1)[kTile] = d_scratch[2];
        double (&y1)[kTile] = d_scratch[3];
        double (&x2)[kTile] = d_scratch[4];
        double (&y2)[kTile] = d_scratch[5];
        double (&gx0)[kTile] = d_scratch[6];
        double (&gy0)[kTile] = d_scratch[7];
        double (&gx1)[kTile] = d_scratch[8];
        double (&gy1)[kTile] = d_scratch[9];
        double (&gx2)[kTile] = d_scratch[10];
        double (&gy2)[kTile] = d_scratch[11];
        int32_t (&ii)[kTile] = i_scratch[0];
        int32_t (&jj)[kTile] = i_scratch[1];
        int32_t (&i1)[kTile] = i_scratch[2];
        int32_t (&j1)[kTile] = i_scratch[3];

        for (uint32_t base = 0; base < len; base += kTile) {
            const uint32_t n = (len - base < kTile) ? (len - base) : kTile;

            for (uint32_t k = 0; k < n; k++) {
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
                j1[k] = 1 - l.a;
            }

            for (uint32_t k = 0; k < n; k++) {
                const auto a = static_cast<uint32_t>(ii[k]);
                const auto b = static_cast<uint32_t>(jj[k]);
                const uint32_t k0 = (a + perm[b]) & 0xFFu;
                const uint32_t k1 =
                        (a + static_cast<uint32_t>(i1[k]) + perm[(b + static_cast<uint32_t>(j1[k])) & 0xFFu]) & 0xFFu;
                const uint32_t k2 = (a + 1u + perm[(b + 1u) & 0xFFu]) & 0xFFu;
                gx0[k] = gradX[k0];
                gy0[k] = gradY[k0];
                gx1[k] = gradX[k1];
                gy1[k] = gradY[k1];
                gx2[k] = gradX[k2];
                gy2[k] = gradY[k2];
            }

            for (uint32_t k = 0; k < n; k++) {
                const double c = three_corners(corner_parts(gx0[k], gy0[k], x0[k], y0[k]),
                                               corner_parts(gx1[k], gy1[k], x1[k], y1[k]),
                                               corner_parts(gx2[k], gy2[k], x2[k], y2[k]));

                acc[base + k] = fuse(c * 70.0, weight, acc[base + k]);
            }
        }
    }

    void simplex_accumulate_avx_entry(const uint32_t *perm, const double *gradX, const double *gradY, const double *xs,
                                      const double *ys, double scale, double weight, double *acc,
                                      uint32_t len) noexcept;

    void simplex_accumulate_neon_entry(const uint32_t *perm, const double *gradX, const double *gradY, const double *xs,
                                       const double *ys, double scale, double weight, double *acc,
                                       uint32_t len) noexcept;

    void simplex_accumulate_avx512_entry(const uint32_t *perm, const double *gradX, const double *gradY,
                                         const double *xs, const double *ys, double scale, double weight, double *acc,
                                         uint32_t len) noexcept;
}
