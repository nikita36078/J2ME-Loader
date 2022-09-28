/*
   4xGLSLHqFilter shader
   
   Copyright (C) 2005 guest(r) - guest.r@gmail.com

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
        uniform vec2 u_texelDelta;
    	attribute vec2 a_position;
		attribute vec2 a_texcoord0;
		varying vec4 v_texcoord0[7];

        void main()
        {
                vec2 dg1 = 0.5 * u_texelDelta;
                vec2 dg2 = vec2(-dg1.x, dg1.y);
                vec2 sd1 = dg1 * 0.5;
				vec2 sd2 = dg2 * 0.5;
                vec2 ddx = vec2(dg1.x, 0.0);
				vec2 ddy = vec2(0.0, dg1.y);

                gl_Position = vec4(a_position, 0.0, 1.0);
                v_texcoord0[0].xy = a_texcoord0;
                v_texcoord0[1].xy = a_texcoord0 - sd1;
                v_texcoord0[2].xy = a_texcoord0 - sd2;
                v_texcoord0[3].xy = a_texcoord0 + sd1;
                v_texcoord0[4].xy = a_texcoord0 + sd2;
                v_texcoord0[5].xy = a_texcoord0 - dg1;
                v_texcoord0[6].xy = a_texcoord0 + dg1;
                v_texcoord0[5].zw = a_texcoord0 - dg2;
                v_texcoord0[6].zw = a_texcoord0 + dg2;
                v_texcoord0[1].zw = a_texcoord0 - ddy;
                v_texcoord0[2].zw = a_texcoord0 + ddx;
                v_texcoord0[3].zw = a_texcoord0 + ddy;
                v_texcoord0[4].zw = a_texcoord0 - ddx;
        }