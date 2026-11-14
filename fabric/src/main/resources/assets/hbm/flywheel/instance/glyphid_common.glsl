#ifndef HBM_GLYPHID_COMMON
#define HBM_GLYPHID_COMMON

void hbm_glyphidTranslate(inout mat4 pose, vec3 offset) {
    pose[3] += pose[0] * offset.x + pose[1] * offset.y + pose[2] * offset.z;
}

void hbm_glyphidRotateX(inout mat4 pose, float angle) {
    float sine = sin(angle);
    float cosine = cos(angle);
    vec4 y = pose[1];
    vec4 z = pose[2];
    pose[1] = y * cosine + z * sine;
    pose[2] = z * cosine - y * sine;
}

void hbm_glyphidRotateY(inout mat4 pose, float angle) {
    float sine = sin(angle);
    float cosine = cos(angle);
    vec4 x = pose[0];
    vec4 z = pose[2];
    pose[0] = x * cosine - z * sine;
    pose[2] = x * sine + z * cosine;
}

void hbm_glyphidRotateZ(inout mat4 pose, float angle) {
    float sine = sin(angle);
    float cosine = cos(angle);
    vec4 x = pose[0];
    vec4 y = pose[1];
    pose[0] = x * cosine + y * sine;
    pose[1] = y * cosine - x * sine;
}

mat4 hbm_glyphidArm(float side, int depth, vec3 joints) {
    mat4 pose = mat4(1.0);
    hbm_glyphidTranslate(pose, vec3(side * 0.25, 0.625, 0.0625));
    hbm_glyphidRotateY(pose, side * 0.1745329201221466);
    hbm_glyphidRotateX(pose, joints.x);
    hbm_glyphidTranslate(pose, vec3(-side * 0.25, -0.625, -0.0625));
    if (depth >= 2) {
        hbm_glyphidTranslate(pose, vec3(side * 0.25, 0.625, 0.4375));
        hbm_glyphidRotateX(pose, joints.y);
        hbm_glyphidTranslate(pose, vec3(-side * 0.25, -0.625, -0.4375));
    }
    if (depth >= 3) {
        hbm_glyphidTranslate(pose, vec3(side * 0.25, 0.625, 0.9375));
        hbm_glyphidRotateX(pose, joints.z);
        hbm_glyphidTranslate(pose, vec3(-side * 0.25, -0.625, -0.9375));
    }
    return pose;
}

mat4 hbm_glyphidJaw(float side, vec3 joints) {
    mat4 pose = mat4(1.0);
    hbm_glyphidTranslate(pose, vec3(0.0, 0.5, 0.25));
    hbm_glyphidRotateZ(pose, joints.x);
    hbm_glyphidTranslate(pose, vec3(0.0, -0.5, -0.25));
    hbm_glyphidTranslate(pose, vec3(0.0, 0.5, 0.25));
    if (side == 0.0) {
        hbm_glyphidRotateX(pose, -joints.y);
    } else {
        hbm_glyphidRotateY(pose, side * joints.y);
        hbm_glyphidRotateX(pose, joints.y);
    }
    hbm_glyphidTranslate(pose, vec3(0.0, -0.5, -0.25));
    return pose;
}

mat4 hbm_glyphidUpperLeg(vec3 joints) {
    mat4 pose = mat4(1.0);
    hbm_glyphidTranslate(pose, vec3(0.0, 0.25, 0.0));
    hbm_glyphidRotateY(pose, joints.x);
    hbm_glyphidRotateZ(pose, joints.y);
    hbm_glyphidTranslate(pose, vec3(0.0, -0.25, 0.0));
    return pose;
}

mat4 hbm_glyphidLowerLeg(float pivotX, vec3 joints) {
    mat4 pose = hbm_glyphidUpperLeg(joints);
    hbm_glyphidTranslate(pose, vec3(pivotX, 0.25, 0.0));
    hbm_glyphidRotateZ(pose, joints.z);
    hbm_glyphidTranslate(pose, vec3(-pivotX, -0.25, 0.0));
    return pose;
}

mat4 hbm_glyphidPose(int part, vec3 joints) {
    switch (part) {
        case 4:
            return hbm_glyphidArm(1.0, 1, joints);
        case 5:
            return hbm_glyphidArm(1.0, 2, joints);
        case 6:
        case 7:
            return hbm_glyphidArm(1.0, 3, joints);
        case 8:
            return hbm_glyphidArm(-1.0, 1, joints);
        case 9:
            return hbm_glyphidArm(-1.0, 2, joints);
        case 10:
        case 11:
            return hbm_glyphidArm(-1.0, 3, joints);
        case 12:
            return hbm_glyphidJaw(0.0, joints);
        case 13:
            return hbm_glyphidJaw(1.0, joints);
        case 14:
            return hbm_glyphidJaw(-1.0, joints);
        case 15:
        case 17:
        case 19:
        case 21:
        case 23:
        case 25:
            return hbm_glyphidUpperLeg(joints);
        case 16:
        case 20:
        case 24:
            return hbm_glyphidLowerLeg(0.5625, joints);
        case 18:
        case 22:
        case 26:
            return hbm_glyphidLowerLeg(-0.5625, joints);
        case 27: {
            mat4 pose = mat4(1.0);
            hbm_glyphidTranslate(pose, vec3(0.0, 1.0, 0.0));
            hbm_glyphidRotateX(pose, 1.5707963705062866);
            return pose;
        }
    }
    return mat4(1.0);
}

#endif
