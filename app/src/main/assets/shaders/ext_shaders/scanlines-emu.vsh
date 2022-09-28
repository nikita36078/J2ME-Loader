// Scanline shader
// Author: Themaister
// This code is hereby placed in the public domain.

uniform vec2 u_texelDelta;
uniform vec2 u_pixelDelta;
attribute vec2 a_position;
attribute vec2 a_texcoord0;
varying vec2 v_texcoord0;
varying vec2 omega;

void main() {
	gl_Position = vec4(a_position, 0.0, 1.0);
	v_texcoord0 = a_texcoord0;
	omega = vec2(3.1415 / u_pixelDelta.x / u_texelDelta.x * u_texelDelta.x, 2.0 * 3.1415 / u_texelDelta.y);
}