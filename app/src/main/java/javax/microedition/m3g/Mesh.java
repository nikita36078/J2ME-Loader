package javax.microedition.m3g;

import java.util.Vector;
import java.util.Enumeration;

public class Mesh extends Node {
	private VertexBuffer vertices;
	private Vector submeshes = new Vector();
	private Vector appearances = new Vector();

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

		if (!appearances.isEmpty()) {
			for (int i = 0; i < submeshes.size() && minValidity > 0; i++) {
				Appearance app = (Appearance) appearances.elementAt(i);
				if (app != null) {
					validity = app.applyAnimation(time);
					minValidity = Math.min(validity, minValidity);
				}
			}
		}

		return minValidity;
	}

	public Mesh(VertexBuffer vertices, IndexBuffer submesh, Appearance appearance) {
		if ((vertices == null) || (submesh == null)) {
			throw new NullPointerException();
		}
		this.vertices = vertices;
		this.submeshes.addElement(submesh);
		this.appearances.addElement(appearance);
	}

	public Mesh(VertexBuffer vertices, IndexBuffer[] submeshes, Appearance[] appearances) {
		if ((vertices == null) || (submeshes == null) || hasArrayNullElement(submeshes)) {
			throw new NullPointerException();
		}
		if ((submeshes.length == 0) || ((appearances != null) && (appearances.length < submeshes.length))) {
			throw new IllegalArgumentException();
		}
		this.vertices = vertices;
		for (int i = 0; i < submeshes.length; ++i)
			this.submeshes.addElement(submeshes[i]);
		for (int i = 0; i < appearances.length; ++i)
			this.appearances.addElement(appearances[i]);
	}

	Object3D duplicateImpl() {
		Mesh copy = new Mesh();
		duplicate((Node) copy);
		copy.vertices = vertices;
		copy.submeshes = submeshes;
		copy.appearances = new Vector();
		Enumeration e = appearances.elements();
		while (e.hasMoreElements())
			copy.appearances.add(e.nextElement());
		return copy;
	}

	public Appearance getAppearance(int index) {
		return (Appearance) appearances.elementAt(index);
	}

	public IndexBuffer getIndexBuffer(int index) {
		return (IndexBuffer) submeshes.elementAt(index);
	}

	public int getSubmeshCount() {
		return submeshes.size();
	}

	public VertexBuffer getVertexBuffer() {
		return vertices;
	}

	public void setAppearance(int index, Appearance appearance) {
		appearances.setElementAt(appearance, index);
	}

	@Override
	int doGetReferences(Object3D[] references) throws IllegalArgumentException {
		int parentCount = super.getReferences(references);

		if (vertices != null) {
			if (references != null)
				references[parentCount] = vertices;
			++parentCount;
		}

		for (int i = 0; i < submeshes.size(); ++i) {
			if (references != null)
				references[parentCount] = (Object3D) submeshes.elementAt(i);
			++parentCount;
		}

		for (int i = 0; i < appearances.size(); ++i) {
			if (references != null)
				references[parentCount] = (Object3D) appearances.elementAt(i);
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
