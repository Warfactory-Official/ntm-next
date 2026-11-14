#ifndef NTM_RIBBON_LAYOUT
#define NTM_RIBBON_LAYOUT(slot)
#endif

NTM_RIBBON_LAYOUT(0) noperspective in vec2 hbmRibbonCoord;
NTM_RIBBON_LAYOUT(1) flat in float hbmRibbonLength;
NTM_RIBBON_LAYOUT(2) flat in vec2 hbmRibbonRadii;
NTM_RIBBON_LAYOUT(3) flat in vec2 hbmRibbonInvW;
NTM_RIBBON_LAYOUT(4) flat in vec4 hbmRibbonHeadColor;
NTM_RIBBON_LAYOUT(5) flat in vec4 hbmRibbonTailColor;
NTM_RIBBON_LAYOUT(6) flat in vec4 hbmRibbonLight;
NTM_RIBBON_LAYOUT(7) flat in vec4 hbmRibbonFog;

vec4 hbmRibbonSurface(out float perspectiveT) {
    float along = clamp(hbmRibbonCoord.x, 0.0, hbmRibbonLength);
    float screenT = hbmRibbonLength > 0.0 ? along / hbmRibbonLength : 0.0;
    float radius = mix(hbmRibbonRadii.x, hbmRibbonRadii.y, screenT);
    float distanceToEdge = length(vec2(hbmRibbonCoord.x - along, hbmRibbonCoord.y)) - radius;
    float coverage = clamp(0.5 - distanceToEdge / max(fwidth(distanceToEdge), 1.0), 0.0, 1.0);
    if (coverage == 0.0) discard;

    perspectiveT = screenT * hbmRibbonInvW.y / mix(hbmRibbonInvW.x, hbmRibbonInvW.y, screenT);
    vec4 color = mix(hbmRibbonHeadColor, hbmRibbonTailColor, perspectiveT);
    color.a *= coverage;
    return color;
}
