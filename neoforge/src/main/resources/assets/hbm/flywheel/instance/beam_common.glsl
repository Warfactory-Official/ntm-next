uint hbm_beamHash(uint value) {
    value ^= value >> 16u;
    value *= 0x7feb352du;
    value ^= value >> 15u;
    value *= 0x846ca68bu;
    return value ^ (value >> 16u);
}

float hbm_beamUniform(uint value) {
    return float(hbm_beamHash(value) & 0x00ffffffu) * (1.0 / 16777216.0);
}

vec3 hbm_beamJoint(int index, float length, float size, int count, int phase, int wave) {
    float angle;
    if (wave == 1) {
        angle = radians(float(phase)) + radians(45.0) * float(index);
    } else {
        uint seed = uint(phase) * 0x9e3779b9u ^ uint(index) * 0x85ebca6bu;
        angle = 6.28318530718 * (hbm_beamUniform(seed) + hbm_beamUniform(seed ^ 0xc2b2ae35u));
    }
    return vec3(size * cos(angle), length * float(index) / float(count), -size * sin(angle));
}

vec3 hbm_beamColor(vec3 outerColor, vec3 innerColor, float fraction) {
    return floor(mix(outerColor, innerColor, fraction) * 255.0) / 255.0;
}
