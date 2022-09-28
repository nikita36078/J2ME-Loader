/*
	zfast_lcd+psp_color - A very simple LCD shader meant to be used at 1080p
	
	- Original 'zfast_lcd' code copyright (C) 2017 Greg Hogan (SoltanGris42)
	
	- Original 'psp_color' code written by hunterk, modified by Pokefan531 and
	  released into the public domain
	
	'Ported' (i.e. copy/paste) to PPSSPP format by jdgleaver
	
	This program is free software; you can redistribute it and/or modify it
	under the terms of the GNU General Public License as published by the Free
	Software Foundation; either version 2 of the License, or (at your option)
	any later version.
	
	Notes: This shader does nearest neighbor scaling of the game and then
		    darkens the border pixels to imitate an LCD screen. You can change
		    the darkness/thickness of the borders. It also applies colour
		    correction to replicate the LCD dynamics of the PSP 1000 and PSP 2000.
	
	*** REQUIRES PPSSPP rendering resolution to be set to 1 x PSP
	
	*** REQUIRES a display resolution of at least 1080p (otherwise grid
	    effect will vanish...)
*/


//=== Config
#define BORDERMULT 14.0 // "Border Multiplier" default: 14.0, min: -40.0, max: 40.0, step: 1.0

//================
#ifdef GL_ES
//precision mediump float;
//precision mediump int;
// For android, use this instead...
precision highp float;
precision highp int;
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

// Dirty hack: this should be called 'TextureSize', but when PPSSPP
// performs shader translation it only coverts variables with a fixed
// set of names - so in order for this shader to work with the Vulkan
// backend, we have to 'borrow' one of the v_texcoord<N> variables...
// (note that we could work around this cleanly by calculating texture
// size inside the fragment shader, but this would have a performance
// impact and would therefore be lame...)
varying vec2 v_texcoord1; // TextureSize

//================
void main()
{
	//
	// Note to self: u_texelDelta.xy == 1 / TextureSize.xy
	//
	
	// Generate grid pattern
	vec2 texcoordInPixels = v_texcoord0.xy * v_texcoord1.xy; // v_texcoord1 == TextureSize
	vec2 centerCoord = floor(texcoordInPixels.xy) + vec2(0.5, 0.5);
	vec2 distFromCenter = abs(centerCoord - texcoordInPixels);
	
	float Y = max(distFromCenter.x, distFromCenter.y);
	
	Y = Y * Y;
	float YY = Y * Y;
	float YYY = YY * Y;
	
	float LineWeight = YY - 2.7 * YYY;
	LineWeight = 1.0 - BORDERMULT * LineWeight;
	
	// Apply colour correction
	vec3 screen = pow(texture2D(sampler0, u_texelDelta.xy * centerCoord).rgb, vec3(target_gamma));
	screen = clamp(screen, 0.0, 1.0);
	screen = pow(
		mat3(r,  rg, rb,
			  gr, g,  gb,
			  br, bg, b) * screen,
		vec3(1.0 / display_gamma)
	);
	
	// Add grid lines
	gl_FragColor = vec4(screen * LineWeight, 1.0);
}
