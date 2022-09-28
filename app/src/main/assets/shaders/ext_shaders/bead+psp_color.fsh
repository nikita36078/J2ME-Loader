/*
	bead+psp_color fragment shader
	
	- Original 'bead' code by Themaister, released into the public domain
	
	- Original 'psp_color' code written by hunterk, modified by Pokefan531 and
	  released into the public domain
	
	'Ported' (i.e. copy/paste) to PPSSPP format by jdgleaver
	
	This program is free software; you can redistribute it and/or modify it
	under the terms of the GNU General Public License as published by the Free
	Software Foundation; either version 2 of the License, or (at your option)
	any later version.
	
	*** Requires rendering resolution to be set to 1xPSP
	
	*** Absolutely requires a display resolution of at least 1080p
*/

//=== Config
#define BEAD_HIGH 0.35 // "Bead High" - default: 0.35, min: 0.0, max: 1.0, step: 0.01
//#define BEAD_LOW 0.2   // "Bead Low"  - default: 0.2,  min: 0.0, max: 1.0, step: 0.01

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

// Have to use standard names to ensure compatibility with
// Vulkan backend...
varying vec2 v_texcoord1; // pixel_no

//================
float dist(vec2 coord, vec2 source)
{
	vec2 delta = coord - source;
	return sqrt(dot(delta, delta));
}

float rolloff(float len)
{
	return exp(-6.0 * len);
}

vec3 lookup(vec2 pixel_no, vec3 color)
{
	float delta = dist(fract(pixel_no), vec2(0.5, 0.5));

//	if (delta > BEAD_LOW && delta < BEAD_HIGH)
//		return color;
//	else if (delta >= BEAD_HIGH)
//		return color * rolloff(delta - BEAD_HIGH);
//	else if (delta <= BEAD_LOW)
//		return color * rolloff(BEAD_LOW - delta);
//	else
//		return vec3(0.0, 0.0, 0.0);
	
	// The PSP has such a 'high' resolution that the
	// 'hole' in the bead will never be visible on any
	// conventional display. We can therefore reduce the
	// performance impact of this shader by ignoring the
	// 'BEAD_LOW' stuff
	if (delta < BEAD_HIGH)
		return color;
	else
		return color * rolloff(delta - BEAD_HIGH);
}

//================
void main()
{	
	// Get colour sample and apply colour correction
	vec3 mid_color = pow(
		lookup(v_texcoord1, texture2D(sampler0, v_texcoord0.xy).rgb), // pixel_no == v_texcoord1
		vec3(target_gamma)
	);
	mid_color = clamp(mid_color, 0.0, 1.0);
	mid_color = pow(
		mat3(r,  rg, rb,
			  gr, g,  gb,
			  br, bg, b) * mid_color,
		vec3(1.0 / display_gamma)
	);
	
	gl_FragColor = vec4(mid_color, 1.0);
}
