#include "flywheel:util/matrix.glsl"
#include "hbm:instance/chain_common.glsl"



void flw_transformBoundingSphere(in FlwInstance i, inout vec3 center, inout float radius) {
    for (int k = 6; k >= 0; k--) {
        uint code = hbm_chainCode(i.codes, k);
        if (code == 0u) continue;
        uint kind = hbm_chainKind(code);
        if (kind == 3u) continue;
        int axis = hbm_chainAxis(code);
        if (kind == 1u) {
            vec3 onAxis = vec3(0.0);
            onAxis[axis] = center[axis];
            radius += length(center - onAxis);
            center = onAxis;
        } else if (kind == 2u) {
            uint channel = code & 7u;
            float scale = i.steps[k].w;
            if (hbm_chainSine(i.clock.z, channel)) {
                radius += abs(scale);
            } else {
                float from = i.previous[channel];
                float to = hbm_chainClocked(i.clock.z, channel) ? from + i.current[channel] * float(i.clock.y)
                                                                 : i.current[channel];
                if (hbm_chainClamped(i.clock.z, channel)) {
                    from = clamp(from, 0.0, 1.0);
                    to = clamp(to, 0.0, 1.0);
                }
                center[axis] += (from + to) * 0.5 * scale;
                radius += abs((to - from) * scale) * 0.5;
            }
        }
        center += i.steps[k].xyz;
    }
    transformBoundingSphere(i.pose, center, radius);
}
