#version 300 es
precision mediump float;

in vec4 aPosition;
in vec2 aTexPosition;
out vec2 vTexPosition;

void main()
{
    gl_Position = aPosition;
    vTexPosition = aTexPosition;
}