/*
	lcd3x shader
	
	- Original code by Gigaherz, released into the public domain
	  [Code here derived from PPSSPP version of lcd3x made by LunaMoo]
	
	- This version 'tweaked' by jdgleaver...
	
	This program is free software; you can redistribute it and/or modify it
	under the terms of the GNU General Public License as published by the Free
	Software Foundation; either version 2 of the License, or (at your option)
	any later version.
	
	*** Requires rendering resolution to be set to 1xPSP
*/

uniform vec2 u_texelDelta;
attribute vec4 a_position;
attribute vec2 a_texcoord0;
varying vec2 v_texcoord0;

// Have to use standard names to ensure compatibility with
// Vulkan backend...
varying vec2 v_texcoord1; // TextureSize

void main()
{
	v_texcoord0 = a_texcoord0;
	gl_Position = a_position;
	
	v_texcoord1 = 1.0 / u_texelDelta;
}
