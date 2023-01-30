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

bool vertical(const vec2 point, const vec4 region) {
    return (abs(point.y - region.y) <= borderWidth || abs(point.y - region.w) <= borderWidth) && inside(point.x, region.x, region.z);
}

bool horizontal(const vec2 point, const vec4 region) {
    return (abs(vTexPosition.x - cropRegion.x) <= borderWidth || abs(vTexPosition.x - cropRegion.z) <= borderWidth) && inside(vTexPosition.y, cropRegion.y, cropRegion.w);
}

const vec4 cropRegionColor = vec4(1.0f, 0.0f, 0.0f, 1.0f);

void main()
{
    if (vertical(vTexPosition, cropRegion) || horizontal(vTexPosition, cropRegion)) {
        outColor = cropRegionColor;
    } else {
        outColor = texture(uTexture, vTexPosition + offset);
    }
    //outColor = texture(uTexture, vTexPosition);
    //ivec2 texSize = textureSize(uTexture, 2);
    //if (vTexPosition.x * float(texSize.x) <= float(texSize.x / 2)) {
    //   vec4 rgba = pixelAt(100, 0, vTexPosition, texSize);
    //   outColor = rgba;
    // } else {
    //     discard;
    //outColor = vec4(1.0f, 1.0f, 1.0f, 0.0f);
    // }

}