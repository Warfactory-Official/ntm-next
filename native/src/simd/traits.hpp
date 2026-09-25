// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LicenseRef-All-Rights-Reserved

#pragma once

#include <cstddef>
#include <cstdint>

#if defined(__x86_64__)
#include <immintrin.h>
#elif defined(__aarch64__)
#include <arm_neon.h>
#endif

namespace ntm::simd {

#if defined(__x86_64__)

    enum : int {
        kTagMathAvx4 = 0,
        kTagMathAvx512x8 = 1,
        kTagRadsimAvx = 2,
        kTagRadsimAvx2 = 3,
        kTagRadsimAvx512 = 4,
    };

    template <int kTag> struct Avx256Core {
        using Vec = __m256d;
        using Mask = __m256d;
        static constexpr uint32_t kWidth = 4u;

        static Vec set1(double x) noexcept { return _mm256_set1_pd(x); }
        static Vec loadu(const double *p) noexcept { return _mm256_loadu_pd(p); }
        static Vec add(Vec a, Vec b) noexcept { return _mm256_add_pd(a, b); }
        static Vec sub(Vec a, Vec b) noexcept { return _mm256_sub_pd(a, b); }
        static Vec mul(Vec a, Vec b) noexcept { return _mm256_mul_pd(a, b); }
    };

    template <int kTag> struct Avx512Core {
        using Vec = __m512d;
        using Mask = __mmask8;
        static constexpr uint32_t kWidth = 8u;

        static Vec set1(double x) noexcept { return _mm512_set1_pd(x); }
        static Vec loadu(const double *p) noexcept { return _mm512_loadu_pd(p); }
        static Vec add(Vec a, Vec b) noexcept { return _mm512_add_pd(a, b); }
        static Vec sub(Vec a, Vec b) noexcept { return _mm512_sub_pd(a, b); }
        static Vec mul(Vec a, Vec b) noexcept { return _mm512_mul_pd(a, b); }
    };

    template <bool kFuse> struct Avx4 : Avx256Core<kTagMathAvx4> {
        using IVec = __m128i;

        static constexpr size_t kAlign = 32u;
        static constexpr size_t kIAlign = 16u;

        static Vec zero() noexcept { return _mm256_setzero_pd(); }
        static Vec load(const double *p) noexcept { return _mm256_load_pd(p); }
        static void store(double *p, Vec v) noexcept { _mm256_store_pd(p, v); }
        static void storeu(double *p, Vec v) noexcept { _mm256_storeu_pd(p, v); }
        static Vec fma(Vec a, Vec b, Vec c) noexcept {
            if constexpr (kFuse)
                return _mm256_fmadd_pd(a, b, c);
            else
                return _mm256_add_pd(_mm256_mul_pd(a, b), c);
        }
        static Vec fnma(Vec a, Vec b, Vec c) noexcept {
            if constexpr (kFuse)
                return _mm256_fnmadd_pd(a, b, c);
            else
                return _mm256_sub_pd(c, _mm256_mul_pd(a, b));
        }
        static Mask cmp_gt(Vec a, Vec b) noexcept { return _mm256_cmp_pd(a, b, _CMP_GT_OQ); }
        static Vec select(Mask m, Vec t, Vec f) noexcept { return _mm256_blendv_pd(f, t, m); }

        static Vec max(Vec a, Vec b) noexcept { return _mm256_max_pd(a, b); }

        static Vec floor(Vec v) noexcept { return _mm256_floor_pd(v); }
        static IVec to_i32(Vec v) noexcept { return _mm256_cvttpd_epi32(v); }
        static Vec from_i32(IVec v) noexcept { return _mm256_cvtepi32_pd(v); }
        static IVec i_set1(int32_t x) noexcept { return _mm_set1_epi32(x); }
        static IVec i_add(IVec a, IVec b) noexcept { return _mm_add_epi32(a, b); }
        static IVec i_and(IVec a, IVec b) noexcept { return _mm_and_si128(a, b); }
        static void i_store(int32_t *p, IVec v) noexcept { _mm_store_si128(reinterpret_cast<__m128i *>(p), v); }
    };

    template <bool kFuse> struct Avx512x8 : Avx512Core<kTagMathAvx512x8> {
        using IVec = __m256i;

        static constexpr size_t kAlign = 64u;
        static constexpr size_t kIAlign = 32u;

        static Vec zero() noexcept { return _mm512_setzero_pd(); }
        static Vec load(const double *p) noexcept { return _mm512_load_pd(p); }
        static void store(double *p, Vec v) noexcept { _mm512_store_pd(p, v); }
        static void storeu(double *p, Vec v) noexcept { _mm512_storeu_pd(p, v); }
        static Vec fma(Vec a, Vec b, Vec c) noexcept {
            if constexpr (kFuse)
                return _mm512_fmadd_pd(a, b, c);
            else
                return _mm512_add_pd(_mm512_mul_pd(a, b), c);
        }
        static Vec fnma(Vec a, Vec b, Vec c) noexcept {
            if constexpr (kFuse)
                return _mm512_fnmadd_pd(a, b, c);
            else
                return _mm512_sub_pd(c, _mm512_mul_pd(a, b));
        }
        static Mask cmp_gt(Vec a, Vec b) noexcept { return _mm512_cmp_pd_mask(a, b, _CMP_GT_OQ); }
        static Vec select(Mask m, Vec t, Vec f) noexcept { return _mm512_mask_blend_pd(m, f, t); }
        static Vec max(Vec a, Vec b) noexcept { return _mm512_max_pd(a, b); }

        static Vec floor(Vec v) noexcept { return _mm512_roundscale_pd(v, 0x09); }
        static IVec to_i32(Vec v) noexcept { return _mm512_cvttpd_epi32(v); }
        static Vec from_i32(IVec v) noexcept { return _mm512_cvtepi32_pd(v); }
        static IVec i_set1(int32_t x) noexcept { return _mm256_set1_epi32(x); }
        static IVec i_add(IVec a, IVec b) noexcept { return _mm256_add_epi32(a, b); }
        static IVec i_and(IVec a, IVec b) noexcept { return _mm256_and_si256(a, b); }
        static void i_store(int32_t *p, IVec v) noexcept { _mm256_store_si256(reinterpret_cast<__m256i *>(p), v); }
    };

#elif defined(__aarch64__)

    template <bool kFuse> struct Neon2 {
        using Vec = float64x2_t;
        using IVec = int32x2_t;
        using Mask = uint64x2_t;
        static constexpr uint32_t kWidth = 2u;
        static constexpr size_t kAlign = 16u;
        static constexpr size_t kIAlign = 8u;

        static Vec set1(double x) noexcept { return vdupq_n_f64(x); }
        static Vec zero() noexcept { return vdupq_n_f64(0.0); }

        static Vec load(const double *p) noexcept { return vld1q_f64(p); }
        static Vec loadu(const double *p) noexcept { return vld1q_f64(p); }
        static void store(double *p, Vec v) noexcept { vst1q_f64(p, v); }
        static void storeu(double *p, Vec v) noexcept { vst1q_f64(p, v); }
        static Vec add(Vec a, Vec b) noexcept { return vaddq_f64(a, b); }
        static Vec sub(Vec a, Vec b) noexcept { return vsubq_f64(a, b); }
        static Vec mul(Vec a, Vec b) noexcept { return vmulq_f64(a, b); }
        static Vec fma(Vec a, Vec b, Vec c) noexcept {
            if constexpr (kFuse)
                return vfmaq_f64(c, a, b);
            else
                return vaddq_f64(vmulq_f64(a, b), c);
        }
        static Vec fnma(Vec a, Vec b, Vec c) noexcept {
            if constexpr (kFuse)
                return vfmsq_f64(c, a, b);
            else
                return vsubq_f64(c, vmulq_f64(a, b));
        }
        static Mask cmp_gt(Vec a, Vec b) noexcept { return vcgtq_f64(a, b); }
        static Vec select(Mask m, Vec t, Vec f) noexcept { return vbslq_f64(m, t, f); }
        static Vec max(Vec a, Vec b) noexcept { return vmaxq_f64(a, b); }

        static Vec floor(Vec v) noexcept { return vrndmq_f64(v); }
        static IVec to_i32(Vec v) noexcept { return vmovn_s64(vcvtq_s64_f64(v)); }
        static Vec from_i32(IVec v) noexcept { return vcvtq_f64_s64(vmovl_s32(v)); }
        static IVec i_set1(int32_t x) noexcept { return vdup_n_s32(x); }
        static IVec i_add(IVec a, IVec b) noexcept { return vadd_s32(a, b); }
        static IVec i_and(IVec a, IVec b) noexcept { return vand_s32(a, b); }
        static void i_store(int32_t *p, IVec v) noexcept { vst1_s32(p, v); }
    };

#endif
}
