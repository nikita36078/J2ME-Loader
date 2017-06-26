package javax.microedition.m3g;

public class SkinnedMesh extends Mesh {

	Group skeleton;

	public SkinnedMesh(VertexBuffer vertices, IndexBuffer[] submeshes, Appearance[] appearances, Group skeleton) {
		super(vertices, submeshes, appearances);
		checkSkeleton(skeleton);
		this.skeleton = skeleton;
	}

	public SkinnedMesh(VertexBuffer vertices, IndexBuffer submeshes, Appearance appearances, Group skeleton) {
		super(vertices, submeshes, appearances);
		checkSkeleton(skeleton);
		this.skeleton = skeleton;
	}

	public void addTransform(Node bone, int weight, int firstVertex, int numVertices) {
		if (bone == null)
			throw new NullPointerException();
		if ((weight <= 0) || (numVertices <= 0))
			throw new IllegalArgumentException();
		if ((firstVertex < 0) || (firstVertex + numVertices > 65535))
			throw new IndexOutOfBoundsException();
	}

	public void getBoneTransform(Node bone, Transform transform) {
		if ((bone == null) || (transform == null))
			throw new NullPointerException();
	}

	public int getBoneVertices(Node bone, int[] indices, float[] weights) {
		if (bone == null)
			throw new NullPointerException();
		return 0;
	}

	public Group getSkeleton() {
		return skeleton;
	}

	private void checkSkeleton(Group skeleton) {
		if (skeleton == null)
			throw new NullPointerException();
		if (skeleton.getParent() != null)
			throw new IllegalArgumentException("Skeleton already has a parent");
	}

}
