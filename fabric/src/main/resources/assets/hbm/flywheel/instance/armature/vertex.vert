#include "hbm:instance/armature/local_pose.glsl"
#include "hbm:instance/armature/style.glsl"

void flw_instanceVertex(in FlwInstance instance) {
    uint phase = instance.style.z & 3u;
    vec3 posedNormal;
    if (phase < 2u) {
        mat3 endpointNormal = mat3(instance.postRows[0].xyz,
            instance.postRows[1].xyz, instance.postRows[2].xyz);
        vec3 normalModel = endpointNormal * flw_vertexNormal;
        mat3 rootNormal = mat3(instance.rootPose);
        if ((instance.style.z & 16u) == 0u) {
            rootNormal = transpose(inverse(rootNormal));
        }
        posedNormal = rootNormal * normalModel;
        if ((instance.style.z & 8u) != 0u) {
            flw_vertexPos = instance.rootPose * vec4(0.0, 0.0, 0.0, 1.0);
        } else {
            mat4 endpointPose = mat4(instance.poseKeys[0], instance.poseKeys[1],
                instance.poseKeys[2], instance.poseKeys[3]);
            vec4 endpointPos = endpointPose * flw_vertexPos;
            flw_vertexPos = instance.rootPose * endpointPos;
        }
    } else {
        mat4 pose = armatureWorldPose(instance);
        if ((instance.style.z & 8u) != 0u) {
            flw_vertexPos = instance.rootPose * vec4(0.0, 0.0, 0.0, 1.0);
        } else {
            flw_vertexPos = pose * flw_vertexPos;
        }
        posedNormal = transpose(inverse(mat3(pose))) * flw_vertexNormal;
    }
    flw_vertexNormal = normalize(posedNormal);
    armatureApplyStyle(instance);
}
