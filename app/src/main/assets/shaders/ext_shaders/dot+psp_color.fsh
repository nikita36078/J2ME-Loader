/*
	dot+psp_color fragment shader
	
	- Original 'dot' code by Themaister, released into the public domain
	
	- Original 'psp_color' code written by hunterk, modified by Pokefan531 and
	  released into the public domain
	
	'Ported' (i.e. copy/paste) to PPSSPP format by jdgleaver
	
	This program is free software; you can redistribute it and/or modify it
	under the terms of the GNU General Public License as published by the Free
	Software Foundation; either version 2 of the License, or (at your option)
	any later version.
*/

//=== Config
#define gamma 2.4  // "Dot Gamma" - default: 2.4,  min: 0.0, max: 5.0, step: 0.05
#define shine 0.05 // "Dot Shine" - default: 0.05, min: 0.0, max: 0.5, step: 0.01
#define blend 0.65 // "Dot Blend" - default: 0.65, min: 0.0, max: 1.0, step: 0.01

//================
#ifdef GL_ES
precision mediump float;
precision mediump int;
// For android, use this instead...
//precision highp float;
//precision highp int;
//
#endif

//================
#define target_gamma 2.21
#define display_gamma 2.2
#define r 0.98
#define g 0.795
#define b 0.98
#define rg 0.04
#define rb 0.01
#define gr 0.20
#define gb 0.01
#define br -0.18
#define bg 0.165

//================
uniform sampler2D sampler0;
uniform vec2 u_texelDelta;
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

//================
float dist(vec2 coord, vec2 source)
{
	vec2 delta = coord - source;
	return sqrt(dot(delta, delta));
}

float color_bloom(vec3 color)
{
	const vec3 gray_coeff = vec3(0.30, 0.59, 0.11);
	float bright = dot(color, gray_coeff);
	return mix(1.0 + shine, 1.0 - shine, bright);
}

vec3 lookup(vec2 pixel_no, float offset_x, float offset_y, vec3 color)
{
	vec2 offset = vec2(offset_x, offset_y);
	float delta = dist(fract(pixel_no), offset + vec2(0.5, 0.5));
	return color * exp(-gamma * delta * color_bloom(color));
}

//================
void main()
{
	// pixel_no == v_texcoord6
	vec3 mid_color = lookup(v_texcoord6, 0.0, 0.0, texture2D(sampler0, v_texcoord5).rgb); // c11
	
	vec3 color = vec3(0.0, 0.0, 0.0);
	
	color += lookup(v_texcoord6, -1.0, -1.0, texture2D(sampler0, v_texcoord1.xy).rgb); // c00_10
	color += lookup(v_texcoord6,  0.0, -1.0, texture2D(sampler0, v_texcoord1.zw).rgb); // c00_10
	color += lookup(v_texcoord6,  1.0, -1.0, texture2D(sampler0, v_texcoord2.xy).rgb); // c20_01
	color += lookup(v_texcoord6, -1.0,  0.0, texture2D(sampler0, v_texcoord2.zw).rgb); // c20_01
	color += mid_color;
	color += lookup(v_texcoord6,  1.0,  0.0, texture2D(sampler0, v_texcoord3.xy).rgb); // c21_02
	color += lookup(v_texcoord6, -1.0,  1.0, texture2D(sampler0, v_texcoord3.zw).rgb); // c21_02
	color += lookup(v_texcoord6,  0.0,  1.0, texture2D(sampler0, v_texcoord4.xy).rgb); // c12_22
	color += lookup(v_texcoord6,  1.0,  1.0, texture2D(sampler0, v_texcoord4.zw).rgb); // c12_22
	
	// Apply colour correction
	// > This is not quite right, but should be good enough...
	vec3 out_color = pow(mix(1.2 * mid_color, color, blend), vec3(target_gamma));
	out_color = clamp(out_color, 0.0, 1.0);
	out_color = pow(
		mat3(r,  rg, rb,
			  gr, g,  gb,
			  br, bg, b) * out_color,
		vec3(1.0 / display_gamma)
	);
	
	gl_FragColor = vec4(out_color, 1.0);
}
