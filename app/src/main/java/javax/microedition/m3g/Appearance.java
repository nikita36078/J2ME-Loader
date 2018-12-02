package javax.microedition.m3g;

import javax.microedition.khronos.opengles.GL10;

public class Appearance extends Object3D {
	private int layer = 0;
	private CompositingMode compositingMode = null;
	private Fog fog = null;
	private PolygonMode polygonMode = null;
	private Material material = null;
	private Texture2D[] textures = new Texture2D[Graphics3D.getInstance().getTextureUnitCount()];

	@Override
	Object3D duplicateImpl() {
		Appearance copy = new Appearance();
		copy.layer = layer;
		copy.compositingMode = compositingMode;
		copy.fog = fog;
		copy.polygonMode = polygonMode;
		copy.material = material;
		copy.textures = new Texture2D[textures.length];
		System.arraycopy(this.textures, 0, copy.textures, 0, textures.length);
		return copy;
	}

	@Override
	int doGetReferences(Object3D[] references) {
		int num = super.doGetReferences(references);
		if (compositingMode != null) {
			if (references != null)
				references[num] = compositingMode;
			num++;
		}
		if (polygonMode != null) {
			if (references != null)
				references[num] = polygonMode;
			num++;
		}
		if (fog != null) {
			if (references != null)
				references[num] = fog;
			num++;
		}
		if (material != null) {
			if (references != null)
				references[num] = material;
			num++;
		}
		for (int i = 0; i < textures.length; i++) {
			if (textures[i] != null) {
				if (references != null)
					references[num] = textures[i];
				num++;
			}
		}
		return num;
	}

	@Override
	Object3D findID(int userID) {
		Object3D found = super.findID(userID);

		if ((found == null) && (compositingMode != null))
			found = compositingMode.findID(userID);
		if ((found == null) && (polygonMode != null))
			found = polygonMode.findID(userID);
		if ((found == null) && (fog != null))
			found = fog.findID(userID);
		if ((found == null) && (material != null))
			found = material.findID(userID);
		for (int i = 0; (found == null) && (i < textures.length); i++)
			if (textures[i] != null)
				found = textures[i].find(userID);
		return found;
	}

	@Override
	int applyAnimation(int time) {
		int minValidity = 0x7FFFFFFF;
		int validity;
		if (compositingMode != null) {
			validity = compositingMode.applyAnimation(time);
			minValidity = Math.min(validity, minValidity);
		}
		if (fog != null) {
			validity = fog.applyAnimation(time);
			minValidity = Math.min(validity, minValidity);
		}
		if (material != null) {
			validity = material.applyAnimation(time);
			minValidity = Math.min(validity, minValidity);
		}
		for (int i = 0; i < textures.length; i++)
			if (textures[i] != null) {
				validity = textures[i].applyAnimation(time);
				minValidity = Math.min(validity, minValidity);
			}
		return minValidity;
	}

	public void setLayer(int layer) {
		this.layer = layer;
	}

	public int getLayer() {
		return layer;
	}

	public void setFog(Fog fog) {
		this.fog = fog;
	}

	public Fog getFog() {
		return fog;
	}

	public void setPolygonMode(PolygonMode polygonMode) {
		this.polygonMode = polygonMode;
	}

	public PolygonMode getPolygonMode() {
		return polygonMode;
	}

	public void setMaterial(Material material) {
		this.material = material;
	}

	public Material getMaterial() {
		return material;
	}

	public void setCompositingMode(CompositingMode comp) {
		this.compositingMode = comp;
	}

	public CompositingMode getCompositingMode() {
		return this.compositingMode;
	}

	public void setTexture(int index, Texture2D texture) {
		if (index < 0 || index >= textures.length)
			throw new IndexOutOfBoundsException("index must be in [0," + textures.length + "]");
		textures[index] = texture;
	}

	public Texture2D getTexture(int index) {
		if (index < 0 || index >= textures.length)
			throw new IndexOutOfBoundsException("index must be in [0," + textures.length + "]");
		return textures[index];
	}

	void setupGL(GL10 gl) {
		if (compositingMode != null)
			compositingMode.setupGL(gl);
		else {
			gl.glDepthFunc(GL10.GL_LEQUAL);
			gl.glDepthMask(true);

			gl.glColorMask(true, true, true, true);

			gl.glAlphaFunc(GL10.GL_GEQUAL, 0.0f);
			gl.glDisable(GL10.GL_ALPHA_TEST);

			//gl.glDisable(GL10.GL_BLEND);
			gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
			gl.glEnable(GL10.GL_BLEND);

			gl.glDisable(GL10.GL_POLYGON_OFFSET_FILL);
		}

		if (polygonMode != null)
			polygonMode.setupGL(gl);
		else {
			gl.glCullFace(GL10.GL_BACK);
			gl.glEnable(GL10.GL_CULL_FACE);

			gl.glShadeModel(GL10.GL_SMOOTH);

			gl.glFrontFace(GL10.GL_CCW);

			gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_FASTEST);

			gl.glLightModelf(GL10.GL_LIGHT_MODEL_TWO_SIDE, GL10.GL_FALSE);
		}

		if (material != null)
			material.setupGL(gl);
		else {
			gl.glDisable(GL10.GL_COLOR_MATERIAL);
			gl.glDisable(GL10.GL_LIGHTING);
		}

		if (fog != null)
			fog.setupGL(gl);
		else
			gl.glDisable(GL10.GL_FOG);
	}
}
