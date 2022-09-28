/*
   Author: Gigaherz
   License: Public domain
*/
	uniform vec2 u_texelDelta;
	attribute vec2 a_position;
	attribute vec2 a_texcoord0;
	varying vec2 v_texcoord0;
	varying vec2 omega;

	void main() {
		gl_Position = vec4(a_position, 0.0, 1.0);
		v_texcoord0 = a_texcoord0;
		omega = 3.141592654 * 2.0 / u_texelDelta;
	}