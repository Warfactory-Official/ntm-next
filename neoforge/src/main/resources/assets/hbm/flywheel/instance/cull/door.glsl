#include "flywheel:util/matrix.glsl"


void flw_transformBoundingSphere(in FlwInstance i, inout vec3 center, inout float radius) {
    center = i.bounds.xyz;
    radius = i.bounds.w;
    transformBoundingSphere(i.pose, center, radius);
}
