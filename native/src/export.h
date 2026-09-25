// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LicenseRef-All-Rights-Reserved

#pragma once

#define NTM_NATIVE_ABI_VERSION 33

#if defined(_WIN32)
#define NTM_NATIVE_EXPORT __declspec(dllexport)
#else
#define NTM_NATIVE_EXPORT __attribute__((visibility("default")))
#endif
