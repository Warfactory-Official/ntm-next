#ifndef HBM_LPW2_MOTION
#define HBM_LPW2_MOTION

float lpw2Harmonic(float phase, vec4 law1) {
    return sin(phase) + sin(law1.x * phase) + sin(law1.y * phase);
}

float lpw2Rocking(float phase, float offset, vec4 law0, vec4 law1) {
    return (sin(phase + offset) + sin(law1.z * phase)) / law0.z;
}

float lpw2SoftPeak(float phase, float halfPi) {
    return sin(halfPi * cos(phase));
}

float lpw2Motion(int signal, vec4 phase0, vec3 phase1, vec4 law0, vec4 law1) {
    switch (signal) {
        case 1:
            return (lpw2Harmonic(phase0.x, law1) + law0.y) * law1.w;
        case 2:
            return lpw2Rocking(phase0.y, law0.x * 2.0, law0, law1);
        case 3:
            return lpw2Rocking(phase0.y, 0.0, law0, law1);
        case 4:
            return lpw2Harmonic(phase0.y, law1) * law1.w;
        case 5:
            return lpw2SoftPeak(phase0.z, law0.x);
        case 6:
            return (lpw2SoftPeak(phase0.w, law0.x) + phase0.w * law1.w - 1.0) / law0.w;
        case 7:
            return phase1.y;
        case 8:
            return lpw2Rocking(phase1.x, law0.x * 2.0, law0, law1);
        case 9:
            return lpw2Rocking(phase1.x, 0.0, law0, law1);
        default:
            return 0.0;
    }
}

vec3 lpw2RotateX(vec3 value, float angle) {
    float sine = sin(angle);
    float cosine = cos(angle);
    return vec3(value.x, cosine * value.y - sine * value.z, sine * value.y + cosine * value.z);
}

vec3 lpw2RotateY(vec3 value, float angle) {
    float sine = sin(angle);
    float cosine = cos(angle);
    return vec3(cosine * value.x + sine * value.z, value.y, -sine * value.x + cosine * value.z);
}

vec3 lpw2RotateZ(vec3 value, float angle) {
    float sine = sin(angle);
    float cosine = cos(angle);
    return vec3(cosine * value.x - sine * value.y, sine * value.x + cosine * value.y, value.z);
}

float lpw2Transform(vec4 base, vec4 phase0, vec3 phase1, vec4 meta, vec4 data0, vec4 data1,
                    vec4 law0, vec4 law1, inout vec3 position, inout vec3 normal) {
    int kind = int(meta.x);
    float first = lpw2Motion(int(meta.y), phase0, phase1, law0, law1);
    float second = lpw2Motion(int(meta.z), phase0, phase1, law0, law1);
    vec4 a = data0;
    vec4 b = data1;
    float radiusScale = 1.0;
    float radiansPerDegree = law0.x / 90.0;

    if (kind == 0) {
        position += vec3(a.x * first, a.y * first, a.z * second);
    } else if (kind == 1) {
        vec3 pivot = vec3(0.0, a.z, a.w);
        float angle = a.y * first * radiansPerDegree;
        position = pivot + lpw2RotateZ(position - pivot, angle);
        normal = lpw2RotateZ(normal, angle);
        position.z += a.x * lpw2Motion(1, phase0, phase1, law0, law1);
    } else if (kind == 2) {
        position.z += a.y * lpw2Motion(5, phase0, phase1, law0, law1) + a.z;
        position.z += a.x * lpw2Motion(1, phase0, phase1, law0, law1);
    } else if (kind == 3) {
        vec3 pivot = vec3(0.0, a.y, a.z);
        float horizontal = b.x * lpw2Motion(2, phase0, phase1, law0, law1) * radiansPerDegree;
        float vertical = a.w * lpw2Motion(3, phase0, phase1, law0, law1) * radiansPerDegree;
        position = pivot + lpw2RotateY(lpw2RotateX(position - pivot, horizontal), vertical);
        normal = lpw2RotateY(lpw2RotateX(normal, horizontal), vertical);
        position.z += a.x * lpw2Motion(1, phase0, phase1, law0, law1);
    } else if (kind == 4) {
        vec3 hinge = vec3(0.0, b.z, b.w);
        vec3 orientation = vec3(0.0, b.y, 0.0);
        float hingeAngle = (a.w + b.x * second) * radiansPerDegree;
        float orientationAngle = a.z * radiansPerDegree;
        position = hinge + lpw2RotateX(position - hinge, hingeAngle);
        position = orientation + lpw2RotateZ(position - orientation, orientationAngle);
        normal = lpw2RotateZ(lpw2RotateX(normal, hingeAngle), orientationAngle);
        position += vec3(a.x * first, a.y * first, 0.0);
    } else if (kind == 5) {
        int axis = int(a.w);
        float scale = (a.y + a.z * first) / a.y;
        position[axis] = a.x + (position[axis] - a.x) * scale;
        normal[axis] /= scale;
        radiusScale = max(1.0, scale);
    } else if (kind == 6) {
        vec3 pivot = a.yzw;
        float angle = a.x * first * radiansPerDegree;
        position = pivot + lpw2RotateY(position - pivot, angle);
        normal = lpw2RotateY(normal, angle);
    }

    position = lpw2RotateY(position, base.w) + base.xyz;
    normal = lpw2RotateY(normal, base.w);
    return radiusScale;
}

#endif
