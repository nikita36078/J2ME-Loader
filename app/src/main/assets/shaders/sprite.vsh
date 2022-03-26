uniform vec2 uTexSize;
attribute vec4 aPosition;
attribute vec2 aColorData;
varying vec2 vTexture;

void main() {
    gl_Position = aPosition;
    vTexture = aColorData / uTexSize;
}