#ifndef NTM_RIBBON_PROJECT
#define NTM_RIBBON_PROJECT(position, view) (NTM_RIBBON_PROJECTION * view)
#endif

#ifndef NTM_RIBBON_DEPTH_ZERO_TO_ONE
#error NTM_RIBBON_DEPTH_ZERO_TO_ONE must describe the active projection
#endif

noperspective out vec2 hbmRibbonCoord;
flat out float hbmRibbonLength;
flat out vec2 hbmRibbonRadii;
flat out vec2 hbmRibbonInvW;
flat out vec4 hbmRibbonHeadColor;
flat out vec4 hbmRibbonTailColor;
flat out vec4 hbmRibbonLight;
flat out vec4 hbmRibbonFog;

bool hbmRibbonClipPlane(float headDistance, float tailDistance, inout float first, inout float last) {
    if (headDistance < 0.0 && tailDistance < 0.0) return false;
    if (headDistance < 0.0) first = max(first, headDistance / (headDistance - tailDistance));
    if (tailDistance < 0.0) last = min(last, headDistance / (headDistance - tailDistance));
    return first < last;
}

bool hbmRibbonClipDepth(vec4 head, vec4 tail, inout float first, inout float last) {
#if NTM_RIBBON_DEPTH_ZERO_TO_ONE
    if (!hbmRibbonClipPlane(head.z, tail.z, first, last)) return false;
#else
    if (!hbmRibbonClipPlane(head.w + head.z, tail.w + tail.z, first, last)) return false;
#endif
    return hbmRibbonClipPlane(head.w - head.z, tail.w - tail.z, first, last);
}

float hbmRibbonProjectedRadius(vec4 center, vec4 side, float width) {
    vec2 offset = (side.xy - center.xy / center.w * side.w) / center.w;
    return length(offset * NTM_RIBBON_SCREEN * 0.5) * width * 0.5;
}

void hbmRibbonVertex() {
    gl_Position = vec4(2.0, 2.0, 2.0, 1.0);
    hbmRibbonCoord = vec2(0.0);
    hbmRibbonLength = 0.0;
    hbmRibbonRadii = vec2(0.0);
    hbmRibbonInvW = vec2(1.0);
    hbmRibbonHeadColor = hbmRibbonTailColor = vec4(0.0);
    hbmRibbonLight = vec4(1.0);
    hbmRibbonFog = vec4(0.0);

    bool atTail = NTM_RIBBON_SELECTOR.x > 0.5;
    vec3 originalHead = atTail ? NTM_RIBBON_OTHER_POSITION : NTM_RIBBON_POSITION;
    vec3 originalTail = atTail ? NTM_RIBBON_POSITION : NTM_RIBBON_OTHER_POSITION;
    vec4 originalHeadColor = atTail ? NTM_RIBBON_OTHER_COLOR : NTM_RIBBON_COLOR;
    vec4 originalTailColor = atTail ? NTM_RIBBON_COLOR : NTM_RIBBON_OTHER_COLOR;
    vec2 widths = atTail ? NTM_RIBBON_WIDTHS.yx : NTM_RIBBON_WIDTHS;
    if (max(widths.x, widths.y) <= 0.0) return;

    vec4 headView = NTM_RIBBON_MODEL_VIEW * vec4(originalHead, 1.0);
    vec4 tailView = NTM_RIBBON_MODEL_VIEW * vec4(originalTail, 1.0);
    vec4 headClip;
    vec4 tailClip;
    
    if (atTail) {
        headClip = NTM_RIBBON_PROJECT(originalHead, headView);
        tailClip = NTM_RIBBON_PROJECT(originalTail, tailView);
    } else {
        tailClip = NTM_RIBBON_PROJECT(originalTail, tailView);
        headClip = NTM_RIBBON_PROJECT(originalHead, headView);
    }
    float first = 0.0;
    float last = 1.0;
    if (!hbmRibbonClipDepth(headClip, tailClip, first, last)) return;

    vec4 clippedHead = mix(headClip, tailClip, first);
    vec4 clippedTail = mix(headClip, tailClip, last);
    if (clippedHead.w <= 0.0 || clippedTail.w <= 0.0) return;
    vec3 headWorld = mix(originalHead, originalTail, first);
    vec3 tailWorld = mix(originalHead, originalTail, last);
    vec4 clippedHeadView = mix(headView, tailView, first);
    vec4 clippedTailView = mix(headView, tailView, last);
    vec3 side = cross(-clippedHeadView.xyz, clippedTailView.xyz - clippedHeadView.xyz);
    float sideLength = length(side);
    side = sideLength > 0.0 ? side / sideLength : vec3(1.0, 0.0, 0.0);
    vec4 projectedSide = NTM_RIBBON_PROJECTION * vec4(side, 0.0);

    vec2 headPixel = clippedHead.xy / clippedHead.w * NTM_RIBBON_SCREEN * 0.5;
    vec2 tailPixel = clippedTail.xy / clippedTail.w * NTM_RIBBON_SCREEN * 0.5;
    vec2 span = tailPixel - headPixel;
    hbmRibbonLength = length(span);
    vec2 direction = hbmRibbonLength > 0.0 ? span / hbmRibbonLength : vec2(1.0, 0.0);
    vec2 perpendicular = vec2(-direction.y, direction.x);
    vec2 clippedWidths = vec2(mix(widths.x, widths.y, first), mix(widths.x, widths.y, last));
    vec2 radii = vec2(hbmRibbonProjectedRadius(clippedHead, projectedSide, clippedWidths.x),
            hbmRibbonProjectedRadius(clippedTail, projectedSide, clippedWidths.y));

    
    float widest = max(max(radii.x, radii.y), MIN_RIBBON_WIDTH * 0.5);
    float originalWidest = max(radii.x, radii.y);
    radii = originalWidest > 0.0 ? radii * (widest / originalWidest) : vec2(widest);
    float resolvedLength = clamp(hbmRibbonLength / (2.0 * widest), 0.0, 1.0);
    hbmRibbonRadii = mix(vec2(widest), radii, resolvedLength);
    hbmRibbonHeadColor = mix(originalHeadColor, originalTailColor, first);
    hbmRibbonTailColor = mix(hbmRibbonHeadColor, mix(originalHeadColor, originalTailColor, last), resolvedLength);
    hbmRibbonInvW = 1.0 / vec2(clippedHead.w, clippedTail.w);
#ifdef NTM_RIBBON_VANILLA
#ifdef NTM_RIBBON_FOG
    hbmRibbonFog = vec4(fog_spherical_distance(headWorld), fog_spherical_distance(tailWorld),
            fog_cylindrical_distance(headWorld), fog_cylindrical_distance(tailWorld));
#endif
#ifndef EMISSIVE
    hbmRibbonLight = sample_lightmap(Sampler2, NTM_RIBBON_LIGHT_COORDS);
#endif
#endif

    
    float padding = widest + RIBBON_FILTER_PADDING;
    hbmRibbonCoord = vec2(atTail ? hbmRibbonLength + padding : -padding, NTM_RIBBON_SELECTOR.y * padding);
    vec2 pixel = headPixel + direction * hbmRibbonCoord.x + perpendicular * hbmRibbonCoord.y;
    vec4 anchor = atTail ? clippedTail : clippedHead;
    vec4 rasterPosition = vec4(pixel / (NTM_RIBBON_SCREEN * 0.5) * anchor.w, anchor.z, anchor.w);
#ifdef NTM_RIBBON_PACK
    if (atTail ? last != 1.0 : first != 0.0) hbmRibbonPackPosition(atTail ? tailWorld : headWorld);
#endif
    gl_Position = rasterPosition;
}
