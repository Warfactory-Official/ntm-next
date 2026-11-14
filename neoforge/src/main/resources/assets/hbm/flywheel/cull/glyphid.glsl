#include "hbm:instance/glyphid_common.glsl"
#include "flywheel:util/matrix.glsl"

void flw_transformBoundingSphere(in FlwInstance instance, inout vec3 center, inout float radius) {
    center = (hbm_glyphidPose(instance.part, instance.joints) * vec4(center, 1.0)).xyz;
    transformBoundingSphere(instance.pose, center, radius);
}
