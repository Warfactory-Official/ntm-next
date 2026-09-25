// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LicenseRef-All-Rights-Reserved

#pragma once

#include "radsim/world_state.hpp"

#include <cstddef>
#include <memory>
#include <vector>

namespace hbm::radsim::detail {
    struct WorldRegistry {

        std::vector<std::unique_ptr<RadWorld>> worlds_by_handle{1u};
        std::vector<size_t> free_handles;
    };

    inline WorldRegistry &world_registry() {
        static WorldRegistry registry{};
        return registry;
    }

    inline size_t next_handle_non_zero() {
        WorldRegistry &registry = world_registry();
        if (!registry.free_handles.empty()) {
            const size_t handle = registry.free_handles.back();
            registry.free_handles.pop_back();
            return handle;
        }
        registry.worlds_by_handle.push_back(nullptr);
        return registry.worlds_by_handle.size() - 1u;
    }

    inline size_t install_world(std::unique_ptr<RadWorld> world) {
        if (!world)
            return 0u;
        WorldRegistry &registry = world_registry();
        const size_t handle = next_handle_non_zero();
        if (handle >= registry.worlds_by_handle.size()) {
            registry.worlds_by_handle.resize(handle + 1u);
        }
        registry.worlds_by_handle[handle] = std::move(world);
        return handle;
    }

    inline void destroy_world(size_t handle) {
        if (handle == 0u)
            return;
        WorldRegistry &registry = world_registry();
        if (handle >= registry.worlds_by_handle.size())
            return;
        auto &world = registry.worlds_by_handle[handle];
        if (!world)
            return;
        world.reset();
        registry.free_handles.push_back(handle);
    }

    inline RadWorld *retain_world(size_t handle) {
        if (handle == 0u)
            return nullptr;
        WorldRegistry &registry = world_registry();
        if (handle >= registry.worlds_by_handle.size())
            return nullptr;
        return registry.worlds_by_handle[handle].get();
    }
}
