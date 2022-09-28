// filter="nearest" output_width="300%" output_height="300%"
/*
   Author: Gigaherz
   License: Public domain
*/
	#ifdef GL_FRAGMENT_PRECISION_HIGH
	precision highp float;
	#else
	precision mediump float;
	#endif
	uniform sampler2D sampler0;
	varying vec2 v_texcoord0;
	varying vec2 omega;

	/* configuration (higher values mean brighter image but reduced effect depth) */
	const float brighten_scanlines = 16.0;
	const float brighten_lcd = 4.0;

	const vec3 offsets = 3.141592654 * vec3(1.0/2.0,1.0/2.0 - 2.0/3.0,1.0/2.0-4.0/3.0);

	void main() {
		vec2 angle = v_texcoord0 * omega;

		float yfactor = (brighten_scanlines + sin(angle.y)) / (brighten_scanlines + 1.0);
		vec3 xfactors = (brighten_lcd + sin(angle.x + offsets)) / (brighten_lcd + 1.0);

		gl_FragColor.rgb = yfactor * xfactors * texture2D(sampler0, v_texcoord0).rgb;
	}

