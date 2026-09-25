// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LicenseRef-All-Rights-Reserved

#pragma once

#if defined(__clang__) && __clang_major__ < 22
#define NTM_TARGET_AVX512F "avx512f,evex512"
#else
#define NTM_TARGET_AVX512F "avx512f"
#endif
