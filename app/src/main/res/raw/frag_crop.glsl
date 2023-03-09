#version 300 es
precision mediump float;

uniform sampler2D uTexture;
// top left; bottom right (x, y)
uniform vec4 cropRegion;
uniform float borderWidth;
in vec2 vTexPosition;
out vec4 outColor;
uniform vec2 pointer;

const vec4 cropRegionLineColor = vec4(1.0f, 1.0f, 1.0f, 1.0f);
const vec4 cropRegionContentTint = vec4(1.0f, 1.0f, 1.0f, 0.6f);

bool inside(const float value, const float a, const float b) {
    return value >= min(a, b) && value <= max(a, b);
}

bool horizontalLine(const vec2 point, const vec4 region) {
    return inside(point.x, region.x, region.z) && (
    abs(point.y - region.y) <= borderWidth ||
    abs(point.y - region.w) <= borderWidth ||
    abs(point.y - (region.y + (region.w - region.y) / 3.f)) <= borderWidth ||
    abs(point.y - (region.y + 2.f * (region.w - region.y) / 3.f)) <= borderWidth
    );
}

bool verticalLine(const vec2 point, const vec4 region) {
    return inside(point.y, region.y, region.w) && (
    abs(point.x - region.x) <= borderWidth ||
    abs(point.x - region.z) <= borderWidth ||
    abs(point.x - (region.x + (region.z - region.x) / 3.f)) <= borderWidth ||
    abs(point.x - (region.x + 2.f * (region.z - region.x) / 3.f)) <= borderWidth
    );
}

bool drawSelectionRect() {
    return cropRegion.x != 0.0f || cropRegion.y != 0.0f || cropRegion.z != 0.0f || cropRegion.w != 0.0f;
}

bool rectLine() {
    return horizontalLine(vTexPosition, cropRegion) || verticalLine(vTexPosition, cropRegion);
}

bool rectContent() {
    return inside(vTexPosition.x, cropRegion.x, cropRegion.z) && inside(vTexPosition.y, cropRegion.y, cropRegion.w);
}

// see https://en.wikipedia.org/wiki/Alpha_compositing
vec4 tint(vec4 foreground, vec4 background)
{
    float a = foreground.a + background.a * (1.0f - foreground.a);
    return vec4(
    (foreground.r * foreground.a  + background.r * background.a * (1.0f - foreground.a)) / a,
    (foreground.g * foreground.a  + background.g * background.a * (1.0f - foreground.a)) / a,
    (foreground.b * foreground.a  + background.b * background.a * (1.0f - foreground.a)) / a,
    a
    );
}

void main()
{
    if (abs(vTexPosition.x - pointer.x) <= borderWidth || abs(vTexPosition.y - pointer.y) <= borderWidth) {
        outColor = vec4(0.0f, 1.0f, 0.0f, 1.0f);
    } else if (drawSelectionRect() && rectLine()) {
        outColor = cropRegionLineColor;
    } else if (drawSelectionRect() && rectContent()) {
        outColor = tint(cropRegionContentTint, texture(uTexture, vTexPosition));
    } else {
        outColor = texture(uTexture, vTexPosition);
    }
}