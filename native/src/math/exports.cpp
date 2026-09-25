// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LicenseRef-All-Rights-Reserved

#include "export.h"
#include "math/kernel.hpp"
#include "radsim/cpu_profile.hpp"

#include <algorithm>

namespace {
    using hbm::radsim::detail::cpu_profile;

    using Kernel = void (*)(const uint32_t *, const double *, const double *, const double *, const double *, double,
                            double, double *, uint32_t);

    Kernel resolve() noexcept {
#if defined(__aarch64__)

        return &ntm::math::simplex_accumulate_neon_entry;
#elif defined(__x86_64__)
        const auto &cpu = cpu_profile();
        const bool can_avx = cpu.has_avx && cpu.has_fma;
        const bool can_512 = cpu.has_avx512f;

        if (can_512)
            return &ntm::math::simplex_accumulate_avx512_entry;
        if (can_avx)
            return &ntm::math::simplex_accumulate_avx_entry;
#endif
        return &ntm::math::simplex_accumulate;
    }

    const Kernel kDispatch = resolve();

    void fractal(const uint32_t *perm, const double *gradX, const double *gradY, const double *xs, const double *ys,
                 double *out, const uint32_t octaves, const uint32_t len) {
        std::fill_n(out, len, 0.0);
        double scale = 1.0;
        for (uint32_t octave = 0; octave < octaves; ++octave) {
            const uint32_t offset = octave * 256u;
            kDispatch(perm + offset, gradX + offset, gradY + offset, xs, ys, scale, 1.0 / scale, out, len);
            scale *= 0.5;
        }
    }
}

extern "C" NTM_NATIVE_EXPORT void ntm_natives_simplex_accumulate(const uint32_t *perm, const double *gradX,
                                                                 const double *gradY, const double *xs,
                                                                 const double *ys, double scale, double weight,
                                                                 double *acc, uint32_t len) {
    kDispatch(perm, gradX, gradY, xs, ys, scale, weight, acc, len);
}

extern "C" NTM_NATIVE_EXPORT void ntm_natives_simplex_fractal(const uint32_t *perm, const double *gradX,
                                                              const double *gradY, const double *xs, const double *ys,
                                                              double *out, uint32_t octaves, uint32_t start,
                                                              uint32_t len) {
    fractal(perm, gradX, gradY, xs + start, ys + start, out + start, octaves, len);
}

extern "C" NTM_NATIVE_EXPORT void ntm_natives_simplex_fractal_grid(const uint32_t *perm, const double *gradX,
                                                                   const double *gradY, double *out, uint32_t octaves,
                                                                   int32_t firstX, int32_t firstZ, uint32_t sizeZ,
                                                                   double scaleX, double scaleZ, uint32_t start,
                                                                   uint32_t len) {
    alignas(64) __attribute__((uninitialized)) double xs[ntm::math::kTile];
    alignas(64) __attribute__((uninitialized)) double zs[ntm::math::kTile];
    for (uint32_t base = 0; base < len; base += ntm::math::kTile) {
        const uint32_t count = std::min(len - base, ntm::math::kTile);
        for (uint32_t i = 0; i < count; ++i) {
            const uint32_t index = start + base + i;

            const int32_t x = static_cast<int32_t>(static_cast<uint32_t>(firstX) + index / sizeZ);
            const int32_t z = static_cast<int32_t>(static_cast<uint32_t>(firstZ) + index % sizeZ);
            xs[i] = static_cast<double>(x) * scaleX;
            zs[i] = static_cast<double>(z) * scaleZ;
        }
        fractal(perm, gradX, gradY, xs, zs, out + start + base, octaves, count);
    }
}

extern "C" NTM_NATIVE_EXPORT int32_t ntm_natives_active_path() {
#if defined(__aarch64__)
    if (kDispatch == &ntm::math::simplex_accumulate_neon_entry)
        return 1;
#elif defined(__x86_64__)
    if (kDispatch == &ntm::math::simplex_accumulate_avx512_entry)
        return 2;
    if (kDispatch == &ntm::math::simplex_accumulate_avx_entry)
        return 1;
#endif
    return 0;
}

extern "C" NTM_NATIVE_EXPORT int32_t ntm_natives_abi_version() { return NTM_NATIVE_ABI_VERSION; }
