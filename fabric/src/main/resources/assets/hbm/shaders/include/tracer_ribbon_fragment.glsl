noperspective in vec2 hbmRibbonCoord;
flat in float hbmRibbonLength;
flat in vec2 hbmRibbonRadii;
flat in vec2 hbmRibbonInvW;
flat in vec4 hbmRibbonHeadColor;
flat in vec4 hbmRibbonTailColor;
flat in vec4 hbmRibbonLight;
flat in vec4 hbmRibbonFog;

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
