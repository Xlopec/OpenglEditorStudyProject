#version 300 es
precision mediump float;

uniform mat4 uMVPMatrix;
uniform vec2 textureRatio;
in vec4 aPosition;
in vec2 aTexPosition;
out vec2 vTexPosition;

void main()
{
    gl_Position = uMVPMatrix * aPosition;
    vTexPosition = textureRatio * (aTexPosition - 0.5f) + 0.5f;
}