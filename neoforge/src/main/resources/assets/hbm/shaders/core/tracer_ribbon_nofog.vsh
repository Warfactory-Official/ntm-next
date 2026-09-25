#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>
#moj_import <minecraft:globals.glsl>
#moj_import <minecraft:sample_lightmap.glsl>

in vec3 Position;
in vec3 OtherPosition;
in vec4 Color;
in vec4 OtherColor;
in vec2 Widths;
in vec2 UV0;
in ivec2 UV2;

#ifndef EMISSIVE
uniform sampler2D Sampler2;
#endif

#define NTM_RIBBON_POSITION Position
#define NTM_RIBBON_OTHER_POSITION OtherPosition
#define NTM_RIBBON_COLOR Color
#define NTM_RIBBON_OTHER_COLOR OtherColor
#define NTM_RIBBON_WIDTHS Widths
#define NTM_RIBBON_SELECTOR UV0
#define NTM_RIBBON_LIGHT_COORDS UV2
#define NTM_RIBBON_MODEL_VIEW ModelViewMat
#define NTM_RIBBON_PROJECTION ProjMat
#define NTM_RIBBON_SCREEN ScreenSize
#define NTM_RIBBON_VANILLA
#moj_import <hbm:tracer_ribbon_vertex.glsl>

void main() {
    hbmRibbonVertex();
}
