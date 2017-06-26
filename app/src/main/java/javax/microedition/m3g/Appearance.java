package javax.microedition.m3g;

public class Appearance extends Object3D { 

	int numTextureUnits = 8;

	private int layer = 0;
	private CompositingMode compositingMode;
	private Fog fog;
	private PolygonMode polygonMode;
	private Material material;
	private Texture2D[] textures;

	public Appearance() {
		textures = new Texture2D[numTextureUnits];
	}
	
	Object3D duplicateImpl() {
		Appearance copy = new Appearance();
		copy.layer = layer;
		copy.compositingMode = compositingMode;
		copy.fog = fog;
		copy.polygonMode = polygonMode;
		copy.material = material;
		copy.textures = textures;
		return copy;
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

	public void setCompositingMode(CompositingMode compositingMode) {
		this.compositingMode = compositingMode;
	}

	public CompositingMode getCompositingMode() {
		return this.compositingMode;
	}

	public void setTexture(int index, Texture2D texture) {
		if (index < 0 || index >= numTextureUnits)
			throw new IndexOutOfBoundsException("index must be in [0," + numTextureUnits + "]");
		textures[index] = texture;
	}

	public Texture2D getTexture(int index) {
		if (index < 0 || index >= numTextureUnits)
			throw new IndexOutOfBoundsException("index must be in [0," + numTextureUnits + "]");
		return textures[index];
	}
}
