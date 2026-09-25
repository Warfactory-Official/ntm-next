#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <hbm:tracer_ribbon_fragment.glsl>

out vec4 fragColor;

void main() {
    float perspectiveT;
    vec4 color = hbmRibbonSurface(perspectiveT) * hbmRibbonLight * ColorModulator;
    color.rgb *= color.a;
    fragColor = color;
}
