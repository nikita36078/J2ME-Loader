package javax.microedition.m3g;

public class Mesh extends Node {
	private VertexBuffer vertices;
	private IndexBuffer[] submeshes;
	private Appearance[] appearances;

	private Mesh() {
	}

	@Override
	int applyAnimation(int time) {
		int minValidity = super.applyAnimation(time);
		int validity;
		if (vertices != null && minValidity > 0) {
			validity = vertices.applyAnimation(time);
			minValidity = Math.min(validity, minValidity);
		}

		for (int i = 0; i < submeshes.length && minValidity > 0; i++) {
			Appearance app = appearances[i];
			if (app != null) {
				validity = app.applyAnimation(time);
				minValidity = Math.min(validity, minValidity);
			}
		}

		return minValidity;
	}

	public Mesh(VertexBuffer vertices, IndexBuffer submesh, Appearance appearance) {
		if ((vertices == null) || (submesh == null)) {
			throw new NullPointerException();
		}
		this.vertices = vertices;
		this.submeshes = new IndexBuffer[1];
		this.submeshes[0] = submesh;
		this.appearances = new Appearance[1];
		this.appearances[0] = appearance;
	}

	public Mesh(VertexBuffer vertices, IndexBuffer[] submeshes, Appearance[] appearances) {
		if ((vertices == null) || (submeshes == null) || hasArrayNullElement(submeshes)) {
			throw new NullPointerException();
		}
		if ((submeshes.length == 0) || ((appearances != null) && (appearances.length < submeshes.length))) {
			throw new IllegalArgumentException();
		}
		this.vertices = vertices;
		this.submeshes = new IndexBuffer[submeshes.length];
		this.appearances = new Appearance[appearances.length];
		for (int i = 0; i < submeshes.length; ++i)
			this.submeshes[i] = submeshes[i];
		for (int i = 0; i < appearances.length; ++i)
			this.appearances[i] = appearances[i];
	}

	Object3D duplicateImpl() {
		Mesh copy = new Mesh();
		duplicate((Node) copy);
		copy.vertices = vertices;
		copy.submeshes = submeshes;
		copy.appearances = new Appearance[appearances.length];
		for (int i = 0; i < appearances.length; ++i)
			copy.appearances[i] = appearances[i];
		return copy;
	}

	public Appearance getAppearance(int index) {
		return appearances[index];
	}

	public IndexBuffer getIndexBuffer(int index) {
		return submeshes[index];
	}

	public int getSubmeshCount() {
		return submeshes.length;
	}

	public VertexBuffer getVertexBuffer() {
		return vertices;
	}

	public void setAppearance(int index, Appearance appearance) {
		appearances[index] = appearance;
	}

	@Override
	int doGetReferences(Object3D[] references) throws IllegalArgumentException {
		int parentCount = super.getReferences(references);

		if (vertices != null) {
			if (references != null)
				references[parentCount] = vertices;
			++parentCount;
		}

		for (int i = 0; i < submeshes.length; ++i) {
			if (references != null)
				references[parentCount] = (Object3D) submeshes[i];
			++parentCount;
		}

		for (int i = 0; i < appearances.length; ++i) {
			if (references != null)
				references[parentCount] = (Object3D) appearances[i];
			++parentCount;
		}

		return parentCount;
	}

	private boolean hasArrayNullElement(IndexBuffer[] buffer) {
		for (int i = 0; i < buffer.length; i++) {
			if (buffer[i] == null) {
				return true;
			}
		}
		return false;
	}
}
