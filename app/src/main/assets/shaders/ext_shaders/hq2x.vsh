    uniform vec2 u_texelDelta;
    attribute vec2 a_position;
	attribute vec2 a_texcoord0;
	varying vec4 v_texcoord0[5];

    void main() {
      vec2 dg1 = 0.5 * u_texelDelta;
      vec2 dg2 = vec2(-dg1.x, dg1.y);
      vec2 dx = vec2(dg1.x, 0.0);
      vec2 dy = vec2(0.0, dg1.y);

      v_texcoord0[0].xy = a_texcoord0;
      v_texcoord0[1].xy = a_texcoord0 - dg1;
      v_texcoord0[1].zw = a_texcoord0 - dy;
      v_texcoord0[2].xy = a_texcoord0 - dg2;
      v_texcoord0[2].zw = a_texcoord0 + dx;
      v_texcoord0[3].xy = a_texcoord0 + dg1;
      v_texcoord0[3].zw = a_texcoord0 + dy;
      v_texcoord0[4].xy = a_texcoord0 + dg2;
      v_texcoord0[4].zw = a_texcoord0 - dx;

      gl_Position = vec4(a_position, 0.0, 1.0);
    }