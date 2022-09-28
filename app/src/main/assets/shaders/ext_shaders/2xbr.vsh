/*
   Hyllian's 2xBR Shader
   
   Copyright (C) 2011 Hyllian/Jararaca - sergiogdb@gmail.com

   This program is free software; you can redistribute it and/or
   modify it under the terms of the GNU General Public License
   as published by the Free Software Foundation; either version 2
   of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with this program; if not, write to the Free Software
   Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
*/

uniform mediump vec2 u_texelDelta;
attribute vec2 a_position;
attribute vec2 a_texcoord0;
varying vec2 v_texcoord0[3];

void main() {
	vec2 ps = u_texelDelta;
	v_texcoord0[0] = a_texcoord0;
	v_texcoord0[1] = vec2(0.0, -ps.y);
	v_texcoord0[2] = vec2(-ps.x, 0.0);

	gl_Position = vec4(a_position, 0.0, 1.0);
}
