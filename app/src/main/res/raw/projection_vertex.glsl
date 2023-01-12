#version 300 es
precision mediump float;

uniform mat4 uMVPMatrix;
in vec4 aPosition;
in vec2 aTexPosition;
out vec2 vTexPosition;

void main()
{
    gl_Position = uMVPMatrix * aPosition;
    vTexPosition = aTexPosition;
}