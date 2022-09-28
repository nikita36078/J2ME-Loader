/*
	zfast_lcd - A very simple LCD shader meant to be used at 1080p
	
	Original code copyright (C) 2017 Greg Hogan (SoltanGris42)
	
	'Ported' (i.e. copy/paste) to PPSSPP format by jdgleaver
	
	This program is free software; you can redistribute it and/or modify it
	under the terms of the GNU General Public License as published by the Free
	Software Foundation; either version 2 of the License, or (at your option)
	any later version.
	
	Notes: This shader just does nearest neighbor scaling of the game and then
		    darkens the border pixels to imitate an LCD screen. You can change
		    the darkness/thickness of the borders.
	
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
	
	// Get colour sample and apply grid effect
	vec3 colour = texture2D(sampler0, u_texelDelta.xy * centerCoord).rgb * LineWeight;
	
	gl_FragColor = vec4(colour.rgb, 1.0);
}
