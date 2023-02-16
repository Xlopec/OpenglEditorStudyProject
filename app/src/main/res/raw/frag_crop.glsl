#version 300 es
precision mediump float;

uniform sampler2D uTexture;
uniform vec2 offset;
// top left; bottom right (x, y)
uniform vec4 cropRegion;
uniform float borderWidth;
uniform vec2 pointer;
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

const vec4 cropRegionColor = vec4(1.0f, 0.0f, 0.0f, 1.0f);

void main()
{
    if (inside(vTexPosition.x, pointer.x - borderWidth, pointer.x + borderWidth) && inside(vTexPosition.y, pointer.y - borderWidth, pointer.y + borderWidth))
    {
        outColor = vec4(0.0f, 1.0f, 0.0f, 1.0f);
    } else if (inside(vTexPosition.x, cropRegion.x, cropRegion.z) && inside(vTexPosition.y, cropRegion.y, cropRegion.w)/*horizontal(vTexPosition, cropRegion) || vertical(vTexPosition, cropRegion)*/) {
        outColor = cropRegionColor;
    } else {
        outColor = texture(uTexture, vTexPosition + offset);
    }
}