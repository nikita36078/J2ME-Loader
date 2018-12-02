package javax.microedition.m3g;

public class Mesh extends Node {
	private VertexBuffer vertices;
	private IndexBuffer[] submeshes;
	private Appearance[] appearances;

	protected Mesh() {
	}

	@Override
	int applyAnimation(int time) {
		int minValidity = super.applyAnimation(time);
		int validity;
		if (vertices != null && minValidity > 0) {
			validity = vertices.applyAnimation(time);
			minValidity = Math.min(validity, minValidity);
		}

		if (appearances != null) {
			for (int i = 0; i < submeshes.length && minValidity > 0; i++) {
				Appearance app = appearances[i];
				if (app != null) {
					validity = app.applyAnimation(time);
					minValidity = Math.min(validity, minValidity);
				}
			}
		}

		return minValidity;
	}

	@Override
	Object3D findID(int userID) {
		Object3D found = super.findID(userID);

		if (found == null)
			found = vertices.findID(userID);
		for (int i = 0; (found == null) && (i < submeshes.length); i++) {
			if (submeshes[i] != null)
				found = submeshes[i].findID(userID);
			if ((found == null) && (appearances[i] != null))
				found = appearances[i].findID(userID);
		}
		return found;
	}

	public Mesh(VertexBuffer vertices, IndexBuffer submesh, Appearance appearance) {
		if ((vertices == null) || (submesh == null)) {
			throw new NullPointerException();
		}
		this.vertices = vertices;
		this.submeshes = new IndexBuffer[]{submesh};
		this.appearances = new Appearance[]{appearance};
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
		this.appearances = new Appearance[submeshes.length];
		System.arraycopy(submeshes, 0, this.submeshes, 0, submeshes.length);
		if (appearances != null)
			System.arraycopy(appearances, 0, this.appearances, 0, appearances.length);
	}

	void duplicate(Mesh copy) {
		super.duplicate(copy);
		copy.vertices = vertices;
		copy.submeshes = submeshes;
		/*copy.appearances = new Appearance[appearances.length];
		for (int i = 0; i < appearances.length; ++i)
			copy.appearances[i] = appearances[i];*/
		copy.appearances = appearances;
	}

	@Override
	Object3D duplicateImpl() {
		Mesh copy = new Mesh();
		duplicate(copy);
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
	int doGetReferences(Object3D[] references) {
		int parentCount = super.doGetReferences(references);

		if (vertices != null) {
			if (references != null)
				references[parentCount] = vertices;
			++parentCount;
		}

		for (int i = 0; i < submeshes.length; ++i) {
			if (references != null)
				references[parentCount] = submeshes[i];
			++parentCount;
		}

		for (int i = 0; i < appearances.length; ++i) {
			if (references != null)
				references[parentCount] = appearances[i];
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
