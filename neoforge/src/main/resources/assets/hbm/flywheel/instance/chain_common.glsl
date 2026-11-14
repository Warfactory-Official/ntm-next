uint hbm_chainCode(uvec2 codes, int index) {
    return (codes[index >> 2] >> uint((index & 3) * 8)) & 0xFFu;
}

uint hbm_chainKind(uint code) {
    return (code >> 5u) & 3u;
}

int hbm_chainAxis(uint code) {
    return int((code >> 3u) & 3u);
}

bool hbm_chainClocked(uint modes, uint channel) {
    return ((modes >> channel) & 1u) != 0u;
}

bool hbm_chainSine(uint modes, uint channel) {
    return ((modes >> (channel + 8u)) & 1u) != 0u;
}

bool hbm_chainClamped(uint modes, uint channel) {
    return ((modes >> (channel + 16u)) & 1u) != 0u;
}




void hbm_chainValues(float previous[6], float current[6], uvec3 clock, float t, uint millis, out float values[6]) {
    uint elapsed = millis - clock.x;
    if (clock.y != 0u) elapsed %= clock.y;
    for (uint i = 0u; i < 6u; i++) {
        float from = previous[i];
        float value = hbm_chainClocked(clock.z, i) ? from + current[i] * float(elapsed)
                                                   : from + t * (current[i] - from);
        if (hbm_chainClamped(clock.z, i)) value = clamp(value, 0.0, 1.0);
        values[i] = hbm_chainSine(clock.z, i) ? sin(value) : value;
    }
}


vec3 hbm_chainRotate(int axis, float c, float s, vec3 v) {
    if (axis == 0) return vec3(v.x, c * v.y - s * v.z, s * v.y + c * v.z);
    if (axis == 1) return vec3(c * v.x + s * v.z, v.y, c * v.z - s * v.x);
    return vec3(c * v.x - s * v.y, s * v.x + c * v.y, v.z);
}

#if __VERSION__ >= 400
#define HBM_CHAIN_PRECISE precise
#else
#define HBM_CHAIN_PRECISE
#endif


vec3 hbm_chainApply(float values[6], vec4 steps[7], uvec2 codes, vec3 position, inout vec3 normal) {
    for (int k = 6; k >= 0; k--) {
        uint code = hbm_chainCode(codes, k);
        if (code == 0u) continue;
        uint kind = hbm_chainKind(code);
        if (kind == 3u) {
            uint channel = code & 7u;
            HBM_CHAIN_PRECISE float multiplied = values[channel] * steps[k].x;
            HBM_CHAIN_PRECISE float divided = multiplied / steps[k].y;
            values[channel] = divided;
            continue;
        }
        int axis = hbm_chainAxis(code);
        if (kind != 0u) {
            float value = values[code & 7u] * steps[k].w;
            if (kind == 1u) {
                float c = cos(value), s = sin(value);
                position = hbm_chainRotate(axis, c, s, position);
                normal = hbm_chainRotate(axis, c, s, normal);
            } else {
                position[axis] += value;
            }
        }
        position += steps[k].xyz;
    }
    return position;
}

#undef HBM_CHAIN_PRECISE
