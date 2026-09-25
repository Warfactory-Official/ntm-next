#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:fog.glsl>
#moj_import <hbm:tracer_ribbon_fragment.glsl>

out vec4 fragColor;

void main() {
    float perspectiveT;
    vec4 color = hbmRibbonSurface(perspectiveT) * hbmRibbonLight * ColorModulator;
    color = apply_fog(color, mix(hbmRibbonFog.x, hbmRibbonFog.y, perspectiveT),
            mix(hbmRibbonFog.z, hbmRibbonFog.w, perspectiveT), FogEnvironmentalStart,
            FogEnvironmentalEnd, FogRenderDistanceStart, FogRenderDistanceEnd, FogColor);
    color.rgb *= color.a;
    fragColor = color;
}
