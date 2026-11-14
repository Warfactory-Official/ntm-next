#include "hbm:instance/glyphid_common.glsl"

void flw_instanceVertex(in FlwInstance instance) {
    mat4 localPose = hbm_glyphidPose(instance.part, instance.joints);
    flw_vertexPos = instance.pose * (localPose * flw_vertexPos);
    flw_vertexNormal = transpose(inverse(mat3(instance.pose))) * (mat3(localPose) * flw_vertexNormal);
    flw_vertexColor *= instance.color;
    flw_vertexOverlay = instance.overlay;
    flw_vertexLight = max(vec2(instance.light) / 256.0, flw_vertexLight);
}
