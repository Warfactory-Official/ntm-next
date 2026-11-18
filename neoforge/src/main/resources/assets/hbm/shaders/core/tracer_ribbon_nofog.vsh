#version 330
#extension GL_ARB_separate_shader_objects : require

#include <minecraft:dynamictransforms.glsl>
#include <minecraft:projection.glsl>
#include <minecraft:globals.glsl>
#include <minecraft:sample_lightmap.glsl>

layout(location = 0) in vec3 Position;
layout(location = 1) in vec3 OtherPosition;
layout(location = 2) in vec4 Color;
layout(location = 3) in vec4 OtherColor;
layout(location = 4) in vec2 Widths;
layout(location = 5) in vec2 UV0;
layout(location = 6) in ivec2 UV2;

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
#define NTM_RIBBON_LAYOUT(slot) layout(location = slot)
#include <hbm:tracer_ribbon_vertex.glsl>

void main() {
    hbmRibbonVertex();
}
