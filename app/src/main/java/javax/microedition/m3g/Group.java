package javax.microedition.m3g;

import java.util.Enumeration;
import java.util.Vector;

public class Group extends Node {

	protected Vector children;
	int numNonCullables = 0, numRenderables = 0;

	public Group() {
		children = new Vector();
	}

	Object3D duplicateImpl() {
		Group copy = new Group();
		duplicate((Node) copy);
		Enumeration e = children.elements();
		while (e.hasMoreElements()) {
			Node nodeCopy = (Node) ((Object3D) e.nextElement()).duplicate();
			copy.addChild(nodeCopy);
		}
		return copy;
	}

	public void addChild(Node child) {
		if (child == null)
			throw new NullPointerException("child can not be null");
		if (child == this)
			throw new IllegalArgumentException("can not add self as child");
		if (child.parent != null)
			throw new IllegalArgumentException("child already has a parent");

		children.addElement(child);
		child.parent = this;
	}

	public Node getChild(int index) {
		return (Node) children.elementAt(index);
	}

	public int getChildCount() {
		return children.size();
	}

	@Override
	int doGetReferences(Object3D[] references) {
		int parentCount = super.doGetReferences(references);
		if (references != null)
			for (int i = 0; i < children.size(); ++i)
				references[parentCount + i] = (Object3D) children.elementAt(i);
		return parentCount + children.size();
	}

	Object3D findID(int userID) {
		Object3D found = super.findID(userID);

		for (int i = 0; (found == null) && (i < children.size()); i++)
			found = ((Object3D) children.elementAt(i)).findID(userID);
		return found;
	}

	@Override
	int applyAnimation(int time) {
		int minValidity = super.applyAnimation(time);
		for (int i = 0; (minValidity > 0) && (i < children.size()); i++) {
			int validity = ((Object3D) children.elementAt(i)).applyAnimation(time);
			minValidity = Math.min(validity, minValidity);
		}
		return minValidity;
	}

	@Override
	boolean doAlign(Node ref) {
		for (int i = 0; i < children.size(); i++)
			if (!((Node)children.elementAt(i)).doAlign(ref))
				return false;
		return true;
	}

	public boolean pick(int scope, float x, float y, Camera camera, RayIntersection ri) {
		return false;
	}

	public boolean pick(int scope, float ox, float oy, float oz, float dx, float dy, float dz, RayIntersection ri) {
		return false;
	}

	public void removeChild(Node child) {
		children.removeElement(child);
		child.parent = null;
	}
}
