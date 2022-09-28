/*
	dot vertex shader
	
	Original code by Themaister, released into the public domain
	
	'Ported' (i.e. copy/paste) to PPSSPP format by jdgleaver
	
	This program is free software; you can redistribute it and/or modify it
	under the terms of the GNU General Public License as published by the Free
	Software Foundation; either version 2 of the License, or (at your option)
	any later version.
*/

uniform vec2 u_texelDelta;
attribute vec4 a_position;
attribute vec2 a_texcoord0;
varying vec2 v_texcoord0;

// OK - brace yourselves! This is going to get ugly...
// In order for this to work with the Vukan backend, we can only use
// variables with specific names (determined by the automated shader
// translation). So everything gets called v_texcoord<N>...
varying vec4 v_texcoord1; // c00_10
varying vec4 v_texcoord2; // c20_01
varying vec4 v_texcoord3; // c21_02
varying vec4 v_texcoord4; // c12_22
varying vec2 v_texcoord5; // c11
varying vec2 v_texcoord6; // pixel_no

void main()
{
	v_texcoord0 = a_texcoord0;
	gl_Position = a_position;
	
	float dx = u_texelDelta.x;
	float dy = u_texelDelta.y;
	
	// c00_10
	v_texcoord1 = vec4(v_texcoord0 + vec2(-dx, -dy), v_texcoord0 + vec2(0.0, -dy));
	
	// c20_01
	v_texcoord2 = vec4(v_texcoord0 + vec2(dx, -dy), v_texcoord0 + vec2(-dx, 0.0));
	
	// c21_02
	v_texcoord3 = vec4(v_texcoord0 + vec2(dx, 0.0), v_texcoord0 + vec2(-dx, dy));
	
	// c12_22
	v_texcoord4 = vec4(v_texcoord0 + vec2(0.0, dy), v_texcoord0 + vec2(dx, dy));
	
	// c11
	v_texcoord5 = v_texcoord0;
	
	// pixel_no
	v_texcoord6 = v_texcoord0 * (1.0 / u_texelDelta.xy);
}
