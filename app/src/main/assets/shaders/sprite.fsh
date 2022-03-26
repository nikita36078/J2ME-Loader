precision mediump float;
uniform sampler2D uTexUnit;
uniform bool uIsTransparency;
varying vec2 vTexture;

void main() {
    vec4 color = texture2D(uTexUnit, vTexture);
    if (uIsTransparency && color.a < 1.0)
            discard;
    gl_FragColor = vec4(color.rgb, 1.0);
}