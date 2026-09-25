#include "flywheel:util/matrix.glsl"

void flw_transformBoundingSphere(in FlwInstance i, inout vec3 center, inout float radius) {
    center = vec3(0.0, i.length * 0.5, 0.0);
    radius = i.length * 0.5 + abs(i.size) + abs(i.thickness) * 1.415 + 1.0;
    transformBoundingSphere(i.pose, center, radius);
}
