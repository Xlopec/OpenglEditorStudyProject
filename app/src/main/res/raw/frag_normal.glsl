#version 300 es
precision mediump float;

uniform sampler2D uTexture;
in vec2 vTexPosition;
out vec4 outColor;

void main()
{
    if (vTexPosition.x < 0.0f || vTexPosition.x > 1.0f || vTexPosition.y < 0.0f || vTexPosition.y > 1.0f) {
        discard;
    }
    outColor = texture(uTexture, vTexPosition);
}