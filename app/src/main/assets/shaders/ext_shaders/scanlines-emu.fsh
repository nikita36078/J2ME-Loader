// Scanline shader
// Author: Themaister
// This code is hereby placed in the public domain.
 
#ifdef GL_FRAGMENT_PRECISION_HIGH
precision highp float;
#else
precision mediump float;
#endif

uniform sampler2D sampler0;
varying vec2 v_texcoord0;
varying vec2 omega;

const float base_brightness = 0.95;
const vec2 sine_comp = vec2(0.05, 0.15);

void main () {
	vec4 c11 = texture2D(sampler0, v_texcoord0);

	vec4 scanline = c11 * (base_brightness + dot(sine_comp * sin(v_texcoord0 * omega), vec2(1.0)));
	gl_FragColor = clamp(scanline, 0.0, 1.0);
}
