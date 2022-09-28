/*
    Fragment shader based on "Improved texture interpolation" by Iñigo Quílez

    Original description:
        http://www.iquilezles.org/www/articles/texture/texture.htm
*/

	#ifdef GL_FRAGMENT_PRECISION_HIGH
	precision highp float;
	#else
	precision mediump float;
	#endif
	uniform vec2 u_texelDelta;
	uniform sampler2D sampler0;
	varying vec2 v_texcoord0;

	vec4 getTexel(vec2 p) {
		p = p / u_texelDelta + vec2(0.5);

		vec2 i = floor(p);
		vec2 f = p - i;
		f = f * f * f * (f * (f * 6.0 - vec2(15.0)) + vec2(10.0));
		p = i + f;

		p = (p - vec2(0.5)) * u_texelDelta;
		return texture2D(sampler0, p);
	}

    void main() {
		gl_FragColor = getTexel(v_texcoord0);
    }

