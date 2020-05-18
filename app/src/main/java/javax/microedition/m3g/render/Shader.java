package javax.microedition.m3g.render;

public class Shader {

	public static final String textureVertex =
			// This matrix member variable provides a hook to manipulate
			// the coordinates of the objects that use this vertex shader
			"uniform mat4 uMVPMatrix;" +
					"attribute vec4 vPosition;" +
					"attribute vec2 aTexture;" +
					"varying vec2 vTexture;" +
					"void main() {" +
					"  gl_Position = uMVPMatrix * vPosition;" +
					"  vTexture = aTexture;" +
					"}";

	public static final String textureFragment =
			"precision mediump float;" +
					"uniform sampler2D uTextureUnit;" +
					"varying vec2 vTexture;" +
					"void main() {" +
					"  gl_FragColor = texture2D(uTextureUnit, vTexture);" +
					"}";

	public static final String colorVertex =
			"uniform mat4 uMVPMatrix;" +
					"attribute vec4 vPosition;" +
					"attribute vec4 aColor;" +
					"varying vec4 vColor;" +
					"void main() {" +
					"  gl_Position = uMVPMatrix * vPosition;" +
					"  vColor = aColor;" +
					"}";

	public static final String colorFragment =
			"precision mediump float;" +
					"varying vec4 vColor;" +
					"void main() {" +
					"  gl_FragColor = vColor;" +
					"}";
}
