// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LicenseRef-All-Rights-Reserved

#pragma once

#include <cstdint>

namespace hbm::radsim::detail {

    struct CpuProfile {
        enum class Vendor : uint8_t { Unknown = 0, Intel = 1, Amd = 2 };

        enum class Uarch : uint8_t {
            Unknown = 0,

            Bonnell = 1,
            Silvermont = 2,
            Goldmont = 3,
            GoldmontPlus = 4,
            Tremont = 5,
            Core2 = 6,
            Nehalem = 7,
            Westmere = 8,
            SandyBridge = 9,
            IvyBridge = 10,
            Haswell = 11,
            Broadwell = 12,
            Skylake = 13,
            SkylakeAvx512 = 14,
            CascadeLake = 15,
            CooperLake = 16,
            CannonLake = 17,
            IcelakeClient = 18,
            IcelakeServer = 19,
            RocketLake = 20,
            TigerLake = 21,
            AlderLake = 22,
            SapphireRapids = 23,
            SierraForest = 24,
            GraniteRapids = 25,
            GraniteRapidsD = 26,
            GrandRidge = 27,
            ArrowLake = 28,
            ArrowLakeS = 29,
            ClearwaterForest = 30,
            PantherLake = 31,
            NovaLake = 32,
            DiamondRapids = 33,

            AmdFam10h = 40,
            Btver1 = 41,
            Bdver1 = 42,
            Bdver2 = 43,
            Bdver3 = 44,
            Bdver4 = 45,
            Btver2 = 46,
            Znver1 = 47,
            Znver2 = 48,
            Znver3 = 49,
            Znver4 = 50,
            Znver5 = 51,

            Znver5Mobile = 52,
            Znver6 = 53,
        };

        struct Features {
            bool avx512f = false;
            bool avx512vnni = false;
            bool avx512bf16 = false;
            bool vaes = false;
        };

        Vendor vendor = Vendor::Unknown;

        bool os_avx_state = false;
        bool os_avx512_state = false;
        Uarch uarch = Uarch::Unknown;
        uint32_t family = 0u;
        uint32_t model = 0u;

        bool has_avx = false;
        bool has_avx2 = false;
        bool has_avx512f = false;

        bool has_fma = false;

        bool prefer_512_columns = false;

        [[nodiscard]] bool widest_column_path_is_512() const noexcept { return has_avx512f && prefer_512_columns; }
    };

    CpuProfile::Uarch classify(CpuProfile::Vendor vendor, uint32_t family, uint32_t model,
                               const CpuProfile::Features &features) noexcept;

    bool prefers_512_columns(CpuProfile::Vendor vendor, uint32_t family, uint32_t model) noexcept;

    uint32_t tuning_table_self_check_failures() noexcept;

    const CpuProfile &cpu_profile() noexcept;

    int32_t cpu_profile_code() noexcept;
}
