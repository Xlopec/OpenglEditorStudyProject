#version 300 es
precision mediump float;

uniform sampler2D uTexture;
uniform vec2 offset;
// top left; bottom right (x, y)
uniform vec4 cropRegion;
uniform float borderWidth;
in vec2 vTexPosition;
out vec4 outColor;

bool inside(const float value, const float a, const float b) {
    return value >= min(a, b) && value <= max(a, b);
}

bool horizontal(const vec2 point, const vec4 region) {
    return (abs(point.y - region.y) <= borderWidth || abs(point.y - region.w) <= borderWidth) && inside(point.x, region.x, region.z);
}

bool vertical(const vec2 point, const vec4 region) {
    return (abs(vTexPosition.x - cropRegion.x) <= borderWidth || abs(vTexPosition.x - cropRegion.z) <= borderWidth) && inside(vTexPosition.y, cropRegion.y, cropRegion.w);
}

bool drawSelectionRect() {
    return cropRegion.x != 0.0f || cropRegion.y != 0.0f || cropRegion.z != 0.0f || cropRegion.w != 0.0f;
}

const vec4 cropRegionColor = vec4(1.0f, 0.0f, 0.0f, 1.0f);

void main()
{
    if (drawSelectionRect() && (horizontal(vTexPosition, cropRegion) || vertical(vTexPosition, cropRegion)))
    {
        outColor = cropRegionColor;
    }
    else
    {
        outColor = texture(uTexture, vTexPosition + offset);
    }
}