#version 330
#extension GL_ARB_separate_shader_objects : require

#include <minecraft:dynamictransforms.glsl>
#include <minecraft:oit.glsl>
#define NTM_RIBBON_LAYOUT(slot) layout(location = slot)
#include <hbm:tracer_ribbon_fragment.glsl>

#ifndef OIT_ALPHA_ONLY
layout(location = 0) out vec4 fragColor;
#endif

void main() {
    float perspectiveT;
    vec4 color = hbmRibbonSurface(perspectiveT) * hbmRibbonLight * ColorModulator;
#ifdef OIT_ALPHA_ONLY
    executeAlphaOnlyPhase(gl_FragCoord.z, color.a);
#else
#ifdef OIT_ACCUMULATE
    color = sampleColorForAccumulation(color);
#else
    color.rgb *= color.a;
#endif
    fragColor = color;
#endif
}
