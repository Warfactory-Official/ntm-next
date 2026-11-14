#ifndef HBM_ARMATURE_LOCAL_POSE
#define HBM_ARMATURE_LOCAL_POSE

vec4 armatureBlendOrientation(vec4 first, vec4 second, float cosine,
        float angle, float reciprocalSine, float fraction) {
    if (cosine > 0.9999999) {
        return normalize(mix(first, second, fraction));
    }
    float firstWeight = sin((1.0 - fraction) * angle) * reciprocalSine;
    float secondWeight = sin(fraction * angle) * reciprocalSine;
    return normalize(first * firstWeight + second * secondWeight);
}

mat4 armatureComposeLocal(vec3 translation, vec3 scale, vec4 rotation) {
    vec3 doubled = rotation.xyz * 2.0;
    float xx = rotation.x * doubled.x;
    float yy = rotation.y * doubled.y;
    float zz = rotation.z * doubled.z;
    float xy = rotation.x * doubled.y;
    float xz = rotation.x * doubled.z;
    float yz = rotation.y * doubled.z;
    float wx = rotation.w * doubled.x;
    float wy = rotation.w * doubled.y;
    float wz = rotation.w * doubled.z;
    return mat4(
        vec4((1.0 - yy - zz) * scale.x, (xy + wz) * scale.x,
            (xz - wy) * scale.x, 0.0),
        vec4((xy - wz) * scale.y, (1.0 - xx - zz) * scale.y,
            (yz + wx) * scale.y, 0.0),
        vec4((xz + wy) * scale.z, (yz - wx) * scale.z,
            (1.0 - xx - yy) * scale.z, 0.0),
        vec4(translation, 1.0));
}

uint armatureDirectedMillis(in FlwInstance instance) {
    uint phase = instance.style.z & 3u;
    uint duration = instance.clock.z;
    if (phase == 0u) {
        return 0u;
    }
    if (phase == 1u) {
        return duration;
    }
    uint logicalNow = (instance.style.z & 4u) != 0u
        ? instance.clock.x
        : flw_gameMillis + instance.clock.x;
    int relativeMillis = int(logicalNow - instance.clock.y);
    uint elapsed = uint(clamp(relativeMillis, 0, int(duration)));
    return phase == 3u ? duration - elapsed : elapsed;
}

float armatureSampleFraction(in FlwInstance instance) {
#if __VERSION__ >= 400
    precise float normalizedTime;
    precise float framePosition;
#else
    float normalizedTime;
    float framePosition;
#endif
    normalizedTime = float(armatureDirectedMillis(instance)) / float(instance.clock.z);
    framePosition = normalizedTime * float(instance.clock.w);
    return framePosition - float(instance.style.w);
}

mat4 armaturePostPose(in FlwInstance instance) {
    vec4 first = instance.postRows[0];
    vec4 second = instance.postRows[1];
    vec4 third = instance.postRows[2];
    return mat4(
        vec4(first.x, second.x, third.x, 0.0),
        vec4(first.y, second.y, third.y, 0.0),
        vec4(first.z, second.z, third.z, 0.0),
        vec4(first.w, second.w, third.w, 1.0));
}

mat4 armatureWorldPose(in FlwInstance instance) {
    float fraction = armatureSampleFraction(instance);
    vec4 first = instance.poseKeys[0];
    vec4 second = instance.poseKeys[1];
    vec4 third = instance.poseKeys[2];
    vec3 translation = mix(first.xyz, vec3(first.w, second.x, second.y), fraction);
    vec3 scale = mix(vec3(second.z, second.w, third.x), third.yzw, fraction);
    vec4 curve = instance.orientationCurve;
    vec4 rotation = armatureBlendOrientation(instance.poseKeys[3], instance.poseKeys[4],
        curve.x, curve.y, curve.z, fraction);
    return instance.rootPose * armatureComposeLocal(translation, scale, rotation)
        * armaturePostPose(instance);
}

#endif