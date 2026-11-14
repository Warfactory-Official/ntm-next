#ifndef HBM_ARMATURE_STYLE
#define HBM_ARMATURE_STYLE

void armatureApplyStyle(in FlwInstance instance) {
    flw_vertexColor = vec4(1.0);
    flw_vertexOverlay = ivec2(int(instance.style.x & 65535u), int(instance.style.x >> 16u));
    vec2 light = vec2(instance.style.y & 65535u, instance.style.y >> 16u) / 256.0;
    flw_vertexLight = max(flw_vertexLight, light);
}

#endif
