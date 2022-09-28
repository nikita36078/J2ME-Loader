/*
	zfast_lcd vertex shader
	
	Original code copyright (C) 2017 Greg Hogan (SoltanGris42)
	
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

// Dirty hack: this should be called 'TextureSize', but when PPSSPP
// performs shader translation it only coverts variables with a fixed
// set of names - so in order for this shader to work with the Vulkan
// backend, we have to 'borrow' one of the v_texcoord<N> variables...
// (note that we could work around this cleanly by calculating texture
// size inside the fragment shader, but this would have a performance
// impact and would therefore be lame...)
varying vec2 v_texcoord1; // TextureSize

void main()
{
	v_texcoord0 = a_texcoord0; // + 0.000001; // HLSL precision workaround (is this needed???)
	gl_Position = a_position;
	v_texcoord1 = 1.0 / u_texelDelta.xy; // TextureSize
}
