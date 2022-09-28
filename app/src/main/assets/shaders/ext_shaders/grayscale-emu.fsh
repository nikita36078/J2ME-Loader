
    #ifdef GL_FRAGMENT_PRECISION_HIGH
    precision highp float;
    #else
    precision mediump float;
    #endif
    uniform sampler2D sampler0;
	varying vec2 v_texcoord0;

    void main() {
		const vec3 coef = vec3(0.299, 0.587, 0.114);
		vec4 color = texture2D(sampler0, v_texcoord0);
		gl_FragColor.rgb = vec3(dot(color.rgb, coef));
    }

