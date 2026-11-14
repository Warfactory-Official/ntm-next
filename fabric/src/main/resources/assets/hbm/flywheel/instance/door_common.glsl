
#define HBM_DOOR_FIRE 0u
#define HBM_DOOR_SECURE 1u
#define HBM_DOOR_CARGO 2u
#define HBM_DOOR_WATER 3u
#define HBM_DOOR_VAULT 4u
#define HBM_DOOR_QE_SLIDING 5u
#define HBM_DOOR_SLIDING_SEAL 6u
#define HBM_DOOR_QE_CONTAINMENT 7u
#define HBM_DOOR_ROUND_AIRLOCK 8u
#define HBM_DOOR_LARGE_VEHICLE 9u
#define HBM_DOOR_SLIDE 10u
#define HBM_DOOR_SILO 11u


#define HBM_HALF_PI 1.5707964
#define HBM_DEG_TO_RAD 0.017453292
#define HBM_PI 3.141592653589793


float hbm_doorTrack(uvec4 ends, vec4 values, uint count, uint shapes, float rest, int millis) {
    float from = 0.0;
    int start = 0;
    for (uint k = 0u; k < count; k++) {
        int end = int(ends[k]);
        float to = values[k];
        if (end > millis) {
            uint shape = (shapes >> (k * 4u)) & 15u;
            float x = float(millis - start), d = float(end - start);
            if (shape == 0u) return to;
            if (shape == 1u) return from;
            if (shape == 2u) return (to - from) * (x / d) + from;
            if (shape == 3u) return (to - from) * (-sin((x / d * HBM_PI + HBM_PI) / 2.0) + 1.0) + from;
            if (shape == 4u) return (to - from) * sin(x / d * HBM_PI / 2.0) + from;
            return (to - from) * ((-cos(x / d * HBM_PI) + 1.0) / 2.0) + from;
        }
        from = to;
        start = end;
    }
    return count == 0u ? rest : from;
}



void hbm_doorChannels(uvec4 timing, uvec2 shapes, vec2 rest, uvec4 ends[2], vec4 values[2], int millis, float t,
                      out float first, out float second, out vec3 origin) {
    if ((timing.z & 0xFFu) == HBM_DOOR_SILO) {
        first = clamp(values[0].x + values[0].y * t, 0.0, values[0].z);
        second = 0.0;
        origin = values[1].xyz;
    } else {
        first = hbm_doorTrack(ends[0], values[0], timing.w & 0xFFu, shapes.x, rest.x, millis);
        second = hbm_doorTrack(ends[1], values[1], timing.w >> 8u, shapes.y, rest.y, millis);
        origin = vec3(0.0);
    }
}


mat4 hbm_translate(mat4 m, vec3 t) {
    m[3] = m * vec4(t, 1.0);
    return m;
}

mat4 hbm_rotateX(mat4 m, float a) {
    float c = cos(a), s = sin(a);
    return m * mat4(1.0, 0.0, 0.0, 0.0, 0.0, c, s, 0.0, 0.0, -s, c, 0.0, 0.0, 0.0, 0.0, 1.0);
}

mat4 hbm_rotateY(mat4 m, float a) {
    float c = cos(a), s = sin(a);
    return m * mat4(c, 0.0, -s, 0.0, 0.0, 1.0, 0.0, 0.0, s, 0.0, c, 0.0, 0.0, 0.0, 0.0, 1.0);
}

mat4 hbm_rotateZ(mat4 m, float a) {
    float c = cos(a), s = sin(a);
    return m * mat4(c, s, 0.0, 0.0, -s, c, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0);
}


float hbm_doorSmoothstep(float t) {
    t = clamp(t, 0.0, 1.0);
    return t * t * (3.0 - 2.0 * t);
}

float hbm_doorNormTime(float time, float min, float max) {
    return clamp((time - min) / (max - min), 0.0, 1.0);
}

mat4 hbm_doorWaterLeaf(float first) {
    mat4 m = hbm_translate(mat4(1.0), vec3(0.375, 0.0, 0.0));
    m = hbm_rotateY(m, HBM_HALF_PI);
    m = hbm_translate(m, vec3(-1.1875, 0.0, 0.0));
    m = hbm_rotateY(m, (-first * 120.0) * HBM_DEG_TO_RAD);
    return hbm_translate(m, vec3(1.1875, 0.0, 0.0));
}

mat4 hbm_doorWaterWheel(float first, float second, float y) {
    mat4 m = hbm_translate(hbm_doorWaterLeaf(first), vec3(0.40625, y, 0.0));
    m = hbm_rotateZ(m, (second * 360.0) * HBM_DEG_TO_RAD);
    return hbm_translate(m, vec3(-0.40625, -y, 0.0));
}





void hbm_doorPose(uint kind, uint part, float first, float second, vec3 origin, out mat4 pose, out vec3 slide,
                  out vec4 plane) {
    pose = mat4(1.0);
    slide = vec3(0.0);
    plane = vec4(0.0);
    if (kind == HBM_DOOR_FIRE) {
        pose = hbm_translate(hbm_rotateY(pose, HBM_HALF_PI), vec3(-0.5, clamp(first * 2.75, 0.0, 2.75), 0.0));
    } else if (kind == HBM_DOOR_SECURE) {
        pose = hbm_translate(pose, vec3(0.0, 1.0 + clamp(first * 3.5, 0.0, 3.5), 0.0));
    } else if (kind == HBM_DOOR_CARGO) {
        pose[3].y = part == 0u ? clamp(first, 0.0, 1.0) : clamp(second, 0.0, 1.0) * 2.0;
    } else if (kind == HBM_DOOR_WATER) {
        if (part == 0u) {
            pose = hbm_doorWaterLeaf(first);
        } else if (part == 1u) {
            pose = hbm_translate(hbm_doorWaterLeaf(first), vec3(-0.4 * second, 0.0, 0.0));
        } else {
            pose = hbm_doorWaterWheel(first, second, part == 2u ? 2.28125 : 0.71875);
        }
    } else if (kind == HBM_DOOR_VAULT) {
        pose = hbm_translate(pose, vec3(-first, 0.0, 0.0));
        pose = hbm_translate(pose, vec3(0.0, 0.0, second * 5.0));
        pose = hbm_translate(pose, vec3(0.0, 2.5, 0.0));
        pose = hbm_rotateX(pose, (360.0 * (second * 5.0) / (4.25 * HBM_PI)) * HBM_DEG_TO_RAD);
        pose = hbm_translate(pose, vec3(0.0, -2.5, 0.0));
    } else if (kind == HBM_DOOR_QE_SLIDING) {
        float travel = clamp(first * 0.95, 0.0, 0.95);
        pose[3].xyz = vec3(0.53125, 0.001, part == 0u ? 0.5 + travel : 0.5 - travel);
    } else if (kind == HBM_DOOR_SLIDING_SEAL) {
        pose[3].x = 0.5;
        slide = vec3(0.0, 0.0, hbm_doorSmoothstep(clamp(first, 0.0, 1.0)) * 0.9);
        plane = vec4(0.0, 0.0, 1.0, 0.5001);
    } else if (kind == HBM_DOOR_QE_CONTAINMENT) {
        pose[3].x = 0.25;
        slide = vec3(0.0, clamp(first * 2.25, 0.0, 2.25), 0.0);
        plane = vec4(0.0, 1.0, 0.0, 3.0);
    } else if (kind == HBM_DOOR_ROUND_AIRLOCK) {
        float travel = clamp(first * 1.5, 0.0, 1.5);
        pose[3].z = 0.5;
        slide = vec3(0.0, 0.0, part == 0u ? travel : -travel);
        plane = vec4(0.0, 0.0, 1.0, 1.999);
    } else if (kind == HBM_DOOR_LARGE_VEHICLE) {
        float travel = clamp(first * 3.0, 0.0, 3.0);
        pose = hbm_rotateY(pose, HBM_HALF_PI);
        slide = vec3(part == 0u ? -travel : travel, 0.0, 0.0);
        plane = vec4(1.0, 0.0, 0.0, 3.4375);
    } else if (kind == HBM_DOOR_SLIDE) {
        float travel = clamp(first * 2.125, 0.0, 2.125);
        if (part >= 2u) travel = -travel;
        if ((part & 1u) == 0u) {
            slide = vec3(0.0, 0.0, travel);
            plane = vec4(0.0, 0.0, 1.0, 2.5);
        } else {
            float swing = radians(90.0 + second * 90.0);
            float side = travel < 0.0 ? -1.0 : 1.0;
            pose = hbm_translate(pose, vec3(0.0, 0.0, travel));
            pose = hbm_translate(pose, vec3(0.0, 1.8125, 0.0));
            pose = hbm_rotateX(pose, swing);
            slide = vec3(0.0, -1.8125, 0.0);
            plane = vec4(0.0, side * sin(swing), side * cos(swing), 2.5 - abs(travel));
        }
    } else {
        pose = hbm_translate(pose, origin);
        float rotation = hbm_doorSmoothstep(hbm_doorNormTime(first, 20.0, 100.0)) * -240.0;
        if (rotation != 0.0) pose = hbm_rotateX(pose, rotation * HBM_DEG_TO_RAD);
        float lift = 0.25 * hbm_doorSmoothstep(hbm_doorNormTime(first, 0.0, 10.0));
        pose = hbm_translate(pose, -origin + vec3(0.0, lift, 0.0));
    }
}
