/*
	lcd3x+psp_color shader
	
	- Original 'lcd3x' code by Gigaherz, released into the public domain
	  [Code here derived from PPSSPP version of lcd3x made by LunaMoo]
	
	- Original 'psp_color' code written by hunterk, modified by Pokefan531 and
	  released into the public domain
	
	'Ported' (i.e. copy/paste) to PPSSPP format by jdgleaver
	
	This program is free software; you can redistribute it and/or modify it
	under the terms of the GNU General Public License as published by the Free
	Software Foundation; either version 2 of the License, or (at your option)
	any later version.
	
	*** Requires rendering resolution to be set to 1xPSP
*/

//=== Config
#define BRIGHTEN_SCANLINES 16.0 // "Brighten Scanlines" - default: 16.0, min: 1.0, max: 32.0, step: 0.5
										  // (brightness of horizontal lines)
#define BRIGHTEN_LCD 4.0        // "Brighten LCD"       - default:  4.0, min: 1.0, max: 12.0, step: 0.1
										  // (brightness of vertical lines)

//================
#ifdef GL_ES
//precision mediump float;
//precision mediump int;
// For android, use this instead...
precision highp float;
precision highp int;
#endif

//================
#define PI 3.141592654

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
varying vec2 v_texcoord0;

// Have to use standard names to ensure compatibility with
// Vulkan backend...
varying vec2 v_texcoord1; // TextureSize

//================
const float INV_BRIGHTEN_SCANLINES_INC = 1.0 / (BRIGHTEN_SCANLINES + 1.0);
const float INV_BRIGHTEN_LCD_INC = 1.0 / (BRIGHTEN_LCD + 1.0);

//================
void main()
{
	// Generate LCD effect
	const vec3 offsets = PI * vec3(0.5, 0.5 - (2.0 / 3.0), 0.5 - (4.0 / 3.0));
	
	// > Originally had a defined 'scanline size' here, but we can determine this
	//   from the current texture size...
	vec2 angle = PI * v_texcoord0.xy * 2.0 * v_texcoord1.xy;
	
	float yfactor = (BRIGHTEN_SCANLINES + sin(angle.y)) * INV_BRIGHTEN_SCANLINES_INC;
	vec3 xfactors = (BRIGHTEN_LCD + sin(angle.x + offsets)) * INV_BRIGHTEN_LCD_INC;
	
	// Get colour sample and apply colour correction
	vec3 colour = pow(texture2D(sampler0, v_texcoord0.xy).rgb, vec3(target_gamma));
	colour = clamp(colour, 0.0, 1.0);
	colour = pow(
		mat3(r,  rg, rb,
			  gr, g,  gb,
			  br, bg, b) * colour,
		vec3(1.0 / display_gamma)
	);
	
	// Apply LCD effect
	colour.rgb = yfactor * xfactors * colour.rgb;
	
	gl_FragColor = vec4(colour.rgb, 1.0);
}
