uniform mat4 uMVPMatrix;
attribute vec4 aPosition;
attribute vec2 aTexPosition;
varying vec2 vTexPosition;

void main() {
    gl_Position = uMVPMatrix * aPosition;
    vTexPosition = aTexPosition;
}