// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LicenseRef-All-Rights-Reserved

#include "radsim/cpu_profile.hpp"

#include <cstddef>

#if defined(__x86_64__) || defined(__i386__)
#define HBM_RADSIM_CPU_PROFILE_X86 1
#else
#define HBM_RADSIM_CPU_PROFILE_X86 0
#endif

namespace hbm::radsim::detail {
    namespace {
        using Vendor = CpuProfile::Vendor;
        using Uarch = CpuProfile::Uarch;
        using Features = CpuProfile::Features;

        Uarch classify_intel_family6(uint32_t model, const Features &features) noexcept {
            switch (model) {
            case 0x1c:
            case 0x26:
                return Uarch::Bonnell;
            case 0x37:
            case 0x4a:
            case 0x4d:
            case 0x5d:
            case 0x4c:
            case 0x5a:
            case 0x75:
                return Uarch::Silvermont;
            case 0x5c:
            case 0x5f:
                return Uarch::Goldmont;
            case 0x7a:
                return Uarch::GoldmontPlus;
            case 0x86:
            case 0x96:
            case 0x9c:
                return Uarch::Tremont;
            case 0x0f:
            case 0x17:
            case 0x1d:
                return Uarch::Core2;
            case 0x1a:
            case 0x1e:
            case 0x1f:
            case 0x2e:
                return Uarch::Nehalem;
            case 0x25:
            case 0x2c:
            case 0x2f:
                return Uarch::Westmere;
            case 0x2a:
            case 0x2d:
                return Uarch::SandyBridge;
            case 0x3a:
            case 0x3e:
                return Uarch::IvyBridge;
            case 0x3c:
            case 0x3f:
            case 0x45:
            case 0x46:
                return Uarch::Haswell;
            case 0x3d:
            case 0x47:
            case 0x4f:
            case 0x56:
                return Uarch::Broadwell;
            case 0x4e:
            case 0x5e:
            case 0x8e:
            case 0x9e:
            case 0xa5:
            case 0xa6:
                return Uarch::Skylake;

            case 0x55:
                if (features.avx512bf16)
                    return Uarch::CooperLake;
                if (features.avx512vnni)
                    return Uarch::CascadeLake;
                return Uarch::SkylakeAvx512;
            case 0x66:
                return Uarch::CannonLake;
            case 0x7d:
            case 0x7e:
            case 0x9d:
                return Uarch::IcelakeClient;
            case 0x6a:
            case 0x6c:
                return Uarch::IcelakeServer;
            case 0xa7:
                return Uarch::RocketLake;
            case 0x8c:
            case 0x8d:
                return Uarch::TigerLake;
            case 0xbe:
            case 0x97:
            case 0x9a:
            case 0xb7:
            case 0xba:
            case 0xbf:
            case 0xaa:
            case 0xac:
                return Uarch::AlderLake;
            case 0x8f:
            case 0xcf:
                return Uarch::SapphireRapids;
            case 0xaf:
                return Uarch::SierraForest;
            case 0xad:
                return Uarch::GraniteRapids;
            case 0xae:
                return Uarch::GraniteRapidsD;
            case 0xb6:
                return Uarch::GrandRidge;
            case 0xb5:
            case 0xc5:
                return Uarch::ArrowLake;
            case 0xc6:
            case 0xbd:
                return Uarch::ArrowLakeS;
            case 0xdd:
                return Uarch::ClearwaterForest;
            case 0xcc:
            case 0xd5:
                return Uarch::PantherLake;
            default:
                return Uarch::Unknown;
            }
        }

        Uarch classify_amd(uint32_t family, uint32_t model, const Features &features) noexcept {
            switch (family) {
            case 0x10u:
                return Uarch::AmdFam10h;
            case 0x14u:
                return Uarch::Btver1;
            case 0x15u:

                if (model == 0x2u)
                    return Uarch::Bdver2;
                if (model <= 0x0fu)
                    return Uarch::Bdver1;
                if (model <= 0x2fu)
                    return Uarch::Bdver2;
                if (model <= 0x4fu)
                    return Uarch::Bdver3;
                if (model <= 0x7fu)
                    return Uarch::Bdver4;
                return Uarch::Unknown;
            case 0x16u:
                return Uarch::Btver2;
            case 0x17u:
                if (model <= 0x2fu)
                    return Uarch::Znver1;
                return Uarch::Znver2;
            case 0x19u:
                if (model <= 0x0fu)
                    return Uarch::Znver3;
                if (model <= 0x1fu)
                    return Uarch::Znver4;
                if (model <= 0x5fu)
                    return Uarch::Znver3;
                if (model <= 0xafu)
                    return Uarch::Znver4;
                if (features.avx512f)
                    return Uarch::Znver4;
                if (features.vaes)
                    return Uarch::Znver3;
                return Uarch::Unknown;
            case 0x1au:

                if (model >= 0x20u && model <= 0x3fu)
                    return Uarch::Znver5Mobile;
                if (model <= 0x4fu)
                    return Uarch::Znver5;
                if (model <= 0x5fu)
                    return Uarch::Znver6;
                if (model <= 0x77u)
                    return Uarch::Znver5;
                if (model < 0x80u)
                    return Uarch::Unknown;
                if (model <= 0xcfu)
                    return Uarch::Znver6;
                if (model <= 0xd7u)
                    return Uarch::Znver5;
                if (model <= 0xe7u)
                    return Uarch::Znver6;
                return Uarch::Unknown;
            default:
                return Uarch::Unknown;
            }
        }

        struct SelfCheckRow {
            Vendor vendor;
            uint32_t family;
            uint32_t model;
            Features features;
            Uarch uarch;
            bool prefer_512;
        };

        constexpr Features kNoFeatures{};
        constexpr Features kVnni{false, true, false, false};
        constexpr Features kBf16{false, true, true, false};
        constexpr Features kVaes{false, false, false, true};
        constexpr Features kAvx512{true, false, false, false};

        constexpr SelfCheckRow kSelfCheck[] = {

                {Vendor::Intel, 0x13u, 0x01u, kNoFeatures, Uarch::DiamondRapids, false},
                {Vendor::Intel, 0x12u, 0x01u, kNoFeatures, Uarch::NovaLake, false},
                {Vendor::Intel, 0x12u, 0x03u, kNoFeatures, Uarch::NovaLake, false},
                {Vendor::Intel, 0x13u, 0x02u, kNoFeatures, Uarch::Unknown, false},

                {Vendor::Intel, 0x06u, 0x55u, kBf16, Uarch::CooperLake, false},
                {Vendor::Intel, 0x06u, 0x55u, kVnni, Uarch::CascadeLake, false},
                {Vendor::Intel, 0x06u, 0x55u, kNoFeatures, Uarch::SkylakeAvx512, false},

                {Vendor::Intel, 0x06u, 0xadu, kNoFeatures, Uarch::GraniteRapids, false},
                {Vendor::Intel, 0x06u, 0xaeu, kNoFeatures, Uarch::GraniteRapidsD, false},
                {Vendor::Intel, 0x06u, 0xafu, kNoFeatures, Uarch::SierraForest, false},
                {Vendor::Intel, 0x06u, 0xaau, kNoFeatures, Uarch::AlderLake, false},
                {Vendor::Intel, 0x06u, 0x8fu, kNoFeatures, Uarch::SapphireRapids, false},
                {Vendor::Intel, 0x06u, 0xcfu, kNoFeatures, Uarch::SapphireRapids, false},

                {Vendor::Intel, 0x06u, 0xddu, kNoFeatures, Uarch::ClearwaterForest, false},

                {Vendor::Amd, 0x1au, 0x1fu, kNoFeatures, Uarch::Znver5, true},
                {Vendor::Amd, 0x1au, 0x20u, kNoFeatures, Uarch::Znver5Mobile, false},
                {Vendor::Amd, 0x1au, 0x3fu, kNoFeatures, Uarch::Znver5Mobile, false},
                {Vendor::Amd, 0x1au, 0x40u, kNoFeatures, Uarch::Znver5, true},
                {Vendor::Amd, 0x1au, 0x44u, kNoFeatures, Uarch::Znver5, true},

                {Vendor::Amd, 0x1au, 0x4fu, kNoFeatures, Uarch::Znver5, true},
                {Vendor::Amd, 0x1au, 0x50u, kNoFeatures, Uarch::Znver6, true},
                {Vendor::Amd, 0x1au, 0x5fu, kNoFeatures, Uarch::Znver6, true},
                {Vendor::Amd, 0x1au, 0x60u, kNoFeatures, Uarch::Znver5, true},
                {Vendor::Amd, 0x1au, 0x77u, kNoFeatures, Uarch::Znver5, true},
                {Vendor::Amd, 0x1au, 0x78u, kNoFeatures, Uarch::Unknown, true},
                {Vendor::Amd, 0x1au, 0x80u, kNoFeatures, Uarch::Znver6, true},
                {Vendor::Amd, 0x1au, 0xcfu, kNoFeatures, Uarch::Znver6, true},
                {Vendor::Amd, 0x1au, 0xd0u, kNoFeatures, Uarch::Znver5, true},
                {Vendor::Amd, 0x1au, 0xd7u, kNoFeatures, Uarch::Znver5, true},
                {Vendor::Amd, 0x1au, 0xd8u, kNoFeatures, Uarch::Znver6, true},
                {Vendor::Amd, 0x1au, 0xe7u, kNoFeatures, Uarch::Znver6, true},

                {Vendor::Amd, 0x19u, 0x0fu, kNoFeatures, Uarch::Znver3, false},
                {Vendor::Amd, 0x19u, 0x10u, kNoFeatures, Uarch::Znver4, false},
                {Vendor::Amd, 0x19u, 0x1fu, kNoFeatures, Uarch::Znver4, false},
                {Vendor::Amd, 0x19u, 0x21u, kNoFeatures, Uarch::Znver3, false},
                {Vendor::Amd, 0x19u, 0x61u, kNoFeatures, Uarch::Znver4, false},
                {Vendor::Amd, 0x19u, 0xb0u, kAvx512, Uarch::Znver4, false},
                {Vendor::Amd, 0x19u, 0xb0u, kVaes, Uarch::Znver3, false},
                {Vendor::Amd, 0x19u, 0xb0u, kNoFeatures, Uarch::Unknown, false},

                {Vendor::Amd, 0x17u, 0x2fu, kNoFeatures, Uarch::Znver1, false},
                {Vendor::Amd, 0x17u, 0x30u, kNoFeatures, Uarch::Znver2, false},
                {Vendor::Amd, 0x15u, 0x02u, kNoFeatures, Uarch::Bdver2, false},
                {Vendor::Amd, 0x15u, 0x01u, kNoFeatures, Uarch::Bdver1, false},
                {Vendor::Amd, 0x15u, 0x30u, kNoFeatures, Uarch::Bdver3, false},
                {Vendor::Amd, 0x16u, 0x00u, kNoFeatures, Uarch::Btver2, false},

                {Vendor::Amd, 0x1bu, 0x00u, kNoFeatures, Uarch::Unknown, true},
                {Vendor::Amd, 0x19u, 0x00u, kNoFeatures, Uarch::Znver3, false},
                {Vendor::Unknown, 0x1au, 0x44u, kNoFeatures, Uarch::Unknown, false},
        };

#if HBM_RADSIM_CPU_PROFILE_X86
        void cpuid_ex(uint32_t regs[4], uint32_t leaf, uint32_t sub) noexcept {
            __asm__ __volatile__("cpuid"
                                 : "=a"(regs[0]), "=b"(regs[1]), "=c"(regs[2]), "=d"(regs[3])
                                 : "0"(leaf), "2"(sub));
        }

        uint64_t xcr0() noexcept {
            uint32_t lo, hi;
            __asm__ __volatile__("xgetbv" : "=a"(lo), "=d"(hi) : "c"(0u));
            return (static_cast<uint64_t>(hi) << 32u) | lo;
        }
#endif

        CpuProfile resolve() noexcept {
            CpuProfile profile;
#if HBM_RADSIM_CPU_PROFILE_X86
            Features features;
            uint32_t leaf0[4];
            cpuid_ex(leaf0, 0u, 0u);
            const uint32_t max_leaf = leaf0[0];
            if (leaf0[1] == 0x756E6547u && leaf0[3] == 0x49656E69u && leaf0[2] == 0x6C65746Eu) {
                profile.vendor = Vendor::Intel;
            } else if (leaf0[1] == 0x68747541u && leaf0[3] == 0x69746E65u && leaf0[2] == 0x444D4163u) {
                profile.vendor = Vendor::Amd;
            }

            if (max_leaf >= 1u) {
                uint32_t leaf1[4];
                cpuid_ex(leaf1, 1u, 0u);
                const uint32_t base_family = (leaf1[0] >> 8u) & 0xFu;
                const uint32_t base_model = (leaf1[0] >> 4u) & 0xFu;

                profile.family = base_family + ((base_family == 0xFu) ? ((leaf1[0] >> 20u) & 0xFFu) : 0u);
                profile.model =
                        base_model +
                        (((base_family == 0xFu) || (base_family == 0x6u)) ? (((leaf1[0] >> 16u) & 0xFu) << 4u) : 0u);

                const bool osxsave = (leaf1[2] & (1u << 27u)) != 0u;
                const uint64_t xcr = osxsave ? xcr0() : 0u;
                const bool os_ymm = (xcr & 0x6u) == 0x6u;
                const bool os_zmm = os_ymm && (xcr & 0xE0u) == 0xE0u;
                profile.has_avx = os_ymm && (leaf1[2] & (1u << 28u)) != 0u;
                profile.has_fma = os_ymm && (leaf1[2] & (1u << 12u)) != 0u;
                profile.os_avx_state = os_ymm;
                profile.os_avx512_state = os_zmm;
            }

            if (max_leaf >= 7u) {
                uint32_t leaf7[4];
                cpuid_ex(leaf7, 7u, 0u);
                profile.has_avx2 = profile.os_avx_state && (leaf7[1] & (1u << 5u)) != 0u;
                profile.has_avx512f = profile.os_avx512_state && (leaf7[1] & (1u << 16u)) != 0u;
                features.avx512f = profile.has_avx512f;
                features.avx512vnni = (leaf7[2] & (1u << 11u)) != 0u;
                features.vaes = (leaf7[2] & (1u << 9u)) != 0u;

                if (leaf7[0] >= 1u) {
                    uint32_t leaf7_1[4];
                    cpuid_ex(leaf7_1, 7u, 1u);
                    features.avx512bf16 = (leaf7_1[0] & (1u << 5u)) != 0u;
                }
            }

            profile.uarch = classify(profile.vendor, profile.family, profile.model, features);
            profile.prefer_512_columns = prefers_512_columns(profile.vendor, profile.family, profile.model);
#endif
            return profile;
        }
    }

    CpuProfile::Uarch classify(const Vendor vendor, const uint32_t family, const uint32_t model,
                               const Features &features) noexcept {
        switch (vendor) {
        case Vendor::Intel:

            if (family == 0x6u)
                return classify_intel_family6(model, features);
            if (family == 0x12u)
                return (model == 0x1u || model == 0x3u) ? Uarch::NovaLake : Uarch::Unknown;
            if (family == 0x13u)
                return (model == 0x1u) ? Uarch::DiamondRapids : Uarch::Unknown;
            return Uarch::Unknown;
        case Vendor::Amd:
            return classify_amd(family, model, features);
        case Vendor::Unknown:
        default:
            return Uarch::Unknown;
        }
    }

    bool prefers_512_columns(const Vendor vendor, const uint32_t family, const uint32_t model) noexcept {
        if (vendor != Vendor::Amd)
            return false;
        if (family < 0x1Au)
            return false;
        if (family == 0x1Au && model >= 0x20u && model <= 0x3Fu)
            return false;
        return true;
    }

    uint32_t tuning_table_self_check_failures() noexcept {
        uint32_t failures = 0u;
        for (size_t i = 0; i < sizeof(kSelfCheck) / sizeof(kSelfCheck[0]); i++) {
            const SelfCheckRow &row = kSelfCheck[i];
            if (classify(row.vendor, row.family, row.model, row.features) != row.uarch)
                failures++;
            if (prefers_512_columns(row.vendor, row.family, row.model) != row.prefer_512)
                failures++;
        }
        return failures;
    }

    const CpuProfile &cpu_profile() noexcept {
        static const CpuProfile profile = resolve();
        return profile;
    }

    int32_t cpu_profile_code() noexcept {
        const CpuProfile &p = cpu_profile();
        uint32_t failures = tuning_table_self_check_failures();
        if (failures > 31u)
            failures = 31u;
        return static_cast<int32_t>((static_cast<uint32_t>(p.vendor) & 0x3u) |
                                    ((static_cast<uint32_t>(p.uarch) & 0x3Fu) << 2u) | (p.has_avx2 ? (1u << 8u) : 0u) |
                                    (p.has_avx512f ? (1u << 9u) : 0u) | (p.prefer_512_columns ? (1u << 10u) : 0u) |
                                    (failures << 11u) | ((p.family & 0xFFu) << 16u) | ((p.model & 0xFFu) << 24u));
    }
}
