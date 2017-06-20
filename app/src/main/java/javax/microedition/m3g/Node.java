package javax.microedition.m3g;

public abstract class Node extends Transformable {

	public static final int NONE = 144;
	public static final int ORIGIN = 145;
	public static final int X_AXIS = 146;
	public static final int Y_AXIS = 147;
	public static final int Z_AXIS = 148;
	public static final int RENDER = 0;
	public static final int PICK = 1;

	Node parent = null;
	Node left;
	Node right;
	int scope = -1;
	Node zReference;
	Node yReference;
	int alphaFactor = 0xFFFF;
	int zTarget = NONE;
	int yTarget = NONE;
	boolean[] enableBits = new boolean[]{true, true};
	boolean hasRenderables = false;
	boolean hasBones = false;
	boolean[] dirty = new boolean[2];

	void duplicate(Node copy) {
		super.duplicate((Transformable)copy);
		copy.parent = parent;
		copy.left = left;
		copy.right = right;
		copy.scope = scope;
		copy.zReference = zReference;
		copy.yReference = yReference;
		copy.alphaFactor = alphaFactor;
		copy.zTarget = zTarget;
		copy.yTarget = yTarget;
		System.arraycopy(enableBits, 0, copy.enableBits, 0, 2);
		copy.hasRenderables = hasRenderables;
		copy.hasBones = hasBones;
		System.arraycopy(dirty, 0, copy.dirty, 0, 2);
	}

	static boolean isChildOf(Node parent, Node child) {
		Node n;
		for (n = child; n != null; n = n.parent)
			if (n.parent == parent)
				return true;

		return false;
	}

	@Override
	void updateProperty(int property, float[] value) {
		switch (property) {
			case AnimationTrack.ALPHA:
				alphaFactor = (int)(Math.max(0.f, Math.min(1.f, value[0]) * 0xFFFF));
				break;
			case AnimationTrack.PICKABILITY:
				enableBits[PICK] = (value[0] >= 0.5f);
				break;
			case AnimationTrack.VISIBILITY:
				enableBits[RENDER] = (value[0] >= 0.5f);
				break;
			default:
				super.updateProperty(property, value);
		}
	}
	
	boolean compareFlags(boolean[] flags) {
		if (dirty[0] == flags[0] && dirty[1] == flags[1])
			return true;
		return false;
	}

	void invalidateNode(boolean[] flags) {
		Node node = this;
		while (node != null && !compareFlags(flags)) {
			System.arraycopy(flags, 0, dirty, 0, 2);
			node = node.parent;
		}
	}

	boolean rayIntersect(boolean[] mask, float[] ray, RayIntersection[] ri, Matrix[] toGroup) {
		return true;
	}

	int getBBox(AABB bbox) {
		return 0;
	}

	boolean validate(boolean[] state, int scope) {
		if (dirty != null && parent != null)
			parent.invalidateNode(dirty);
		dirty[0] = false;
		dirty[1] = false;
		return true;
	}

	static void transformAlignmentTarget(int target, Matrix transform, QVec4 out) {
		switch (target) {
			case ORIGIN:
				out.setVec4(0, 0, 0, 1);
				break;
			case X_AXIS:
				out.setVec4(1, 0, 0, 0);
				break;
			case Y_AXIS:
				out.setVec4(0, 1, 0, 0);
				break;
			case Z_AXIS:
				out.setVec4(0, 0, 1, 0);
				break;
		}
		transform.transformVec4(out);
	}

	boolean computeAlignmentRotation(Vector3 srcAxis, Node targetNode, int targetAxisName, int constraint) {
		Node parent = this.parent;
		Matrix transform = new Matrix();
		QVec4 targetAxis = new QVec4();

		if (!targetNode.getTransformTo(parent, transform))
			return false;

		transform.preTranslateMatrix(-tx, -ty, -tz);

		if (constraint != NONE) {
			QVec4 rot = new QVec4(orientation);
			rot.w = -rot.w;
			transform.preRotateMatrixQuat(rot);
		}

		transformAlignmentTarget(targetAxisName, transform, targetAxis);

		if (constraint == Z_AXIS) {
			float norm = targetAxis.x * targetAxis.x + targetAxis.y * targetAxis.y;

			if (norm < 1.0e-5f)
				return true;

			norm = (float)(1.0d / Math.sqrt(norm));

			targetAxis.x *= norm;
			targetAxis.y *= norm;
			targetAxis.z = 0.0f;
		} else {
			Vector3 tvec = new Vector3(targetAxis);
			tvec.normalizeVec3();
			targetAxis.x = tvec.x;
			targetAxis.y = tvec.y;
			targetAxis.z = tvec.z;
		}

		if (constraint != NONE) {
			QVec4 rot = new QVec4();
			rot.setQuatRotation(srcAxis, new Vector3(targetAxis));
			orientation.mulQuat(rot);
		} else
			orientation.setQuatRotation(srcAxis, new Vector3(targetAxis));

		invalidateTransformable();
		return false;
	}

	Node getRoot() {
		Node n = this;
		while (n.parent != null)
			n = n.parent;
		return n;
	}

	boolean computeAlignment(Node refNode) {
		Node root = this.getRoot();
		Node zRef = this.zReference;
		Node yRef = this.yReference;
		int zTarget = this.zTarget;
		int yTarget = this.yTarget;

		if (zTarget == NONE && yTarget == NONE)
			return true;

		if (zRef != null && isChildOf(this, zRef) || zRef.getRoot() != root)
			return false;
		if (yRef != null && isChildOf(this, yRef) || yRef.getRoot() != root)
			return false;

		if (this.zTarget != NONE) {
			if (zRef == null && refNode == this)
				return false;
			if (!computeAlignmentRotation(new Vector3(0, 0, 1), zRef != null ? zRef : refNode, zTarget, NONE))
				return false;
		}
		
		if (this.yTarget != NONE) {
			if (yRef == null && refNode == this)
				return false;
			if (!computeAlignmentRotation(new Vector3(0, 1, 0), yRef != null ? yRef : refNode, yTarget, zTarget != NONE ? Z_AXIS : NONE))
				return false;
		}
		
		return true;
	}

	boolean doAlign(Node ref) {
		if (ref == null)
			return this.computeAlignment(this);
		else
			return this.computeAlignment(ref);
	}

	public final void align(Node ref) {
		if (ref != null && (this.getRoot() != ref.getRoot()))
			throw new IllegalArgumentException();
		doAlign(ref == null ? this : ref);
	}

	public float getAlphaFactor() {
		return (float)(alphaFactor * (1.d / 0xFFFF));
	}

	public Node getParent() {
		return parent;
	}

	void updateNodeCounters(int nonCullableChange, int renderableChange) {
		boolean hasRenderables = (renderableChange > 0);
		Node node = this;
		while (node != null) {
			if (node instanceof Group || node instanceof World) {
				((Group)node).numNonCullables += nonCullableChange;
				((Group)node).numRenderables += renderableChange;
				hasRenderables = ((Group)node).numRenderables > 0;
			}
			node.hasRenderables = hasRenderables;
			node = node.parent;
		}
	}

	void enable(int which, boolean enable) {
		enableBits[which] = enable;
	}

	boolean isEnabled(int which) {
		return enableBits[which];
	}

	void setParent(Node parent) {
		int nonCullableChange = 0, renderableChange = 0;
		 
		if (this instanceof Group) {
			nonCullableChange = ((Group)this).numNonCullables;
			renderableChange = ((Group)this).numRenderables;
		} else if (this instanceof Sprite3D) {
			renderableChange = 1;
			if (!((Sprite3D)this).isScaled())
				nonCullableChange = 1;
		} else if (this instanceof Light)
			nonCullableChange = 1;
		else if (this instanceof SkinnedMesh) {
			nonCullableChange += ((SkinnedMesh)this).skeleton.numNonCullables;
			renderableChange += ((SkinnedMesh)this).skeleton.numRenderables + 1;
		} else if (this instanceof Mesh || this instanceof MorphingMesh)
			renderableChange = 1;

		if (this.parent != null) {
			this.parent.updateNodeCounters(-nonCullableChange, -renderableChange);
			if (renderableChange != 0)
				this.parent.invalidateNode(new boolean[]{true, true});
		}

		this.parent = parent;

		if (parent != null) {
			boolean[] dirty = new boolean[2];
			System.arraycopy(this.dirty, 0, dirty, 0, 2);
			if (renderableChange != 0)
				dirty[0] = true;
			if (hasBones)
				dirty[1] = true;
			parent.updateNodeCounters(nonCullableChange, renderableChange);
			parent.invalidateNode(dirty);
		}
	}

	static void getTestPoints(Vector3 planeNormal, AABB box, Vector3 vNear, Vector3 vFar) {
		float[] fNormal = new float[]{planeNormal.x, planeNormal.y, planeNormal.z};
		float[] fNear = new float[]{vNear.x, vNear.y, vNear.z};
		float[] fFar = new float[]{vFar.x, vFar.y, vFar.z};

		for (int i = 0; i < 3; i++) {
			if (fNormal[i] < 0) {
				fNear[i] = box.max[i];
				fFar[i] = box.min[i];
			} else {
				fNear[i] = box.min[i];
				fFar[i] = box.max[i];
			}
		}

		vNear.setVec3(fNear);
		vFar.setVec3(fFar);
	}

	public int getScope() {
		return scope;
	}

	static void getTransformUpPath(Node node, Node ancestor, Matrix transform) {
		if (node == ancestor)
			transform.identityMatrix();
		else {
			if (node.parent == ancestor)
				node.getCompositeTransform(transform);
			else {
				getTransformUpPath(node.parent, ancestor, transform);
				Matrix mtx = new Matrix();
				node.getCompositeTransform(mtx);
				transform.mulMatrix(mtx);
			}
		}
	}

	int getTotalAlphaFactor(Node root) {
		Node n = this;
		int f = alphaFactor;

		while (n.parent != null && n != root) {
			n = n.parent;
			f = ((f + 1) * n.alphaFactor) >>> 16;
		}
		return f;
	}

	boolean hasEnabledPath(Node root) {
		Node n;
		for (n = this; n != null; n = n.parent) {
			if (!(n.enableBits[RENDER]))
				return false;
			if (n == root)
				break;
		}
		return true;
	}

	boolean hasPickablePath(Node root) {
		Node n;
		for (n = this; n != null; n = n.parent) {
			if (!(n.enableBits[PICK]))
				return false;
			if (n == root)
				break;
		}
		return true;
	}

	boolean getTransformTo(Node target, Matrix transform) {
		if (this == target) {
			transform.identityMatrix();
			return true;
		}
		Node pivot = null;
		Node s = this;
		Node t = target;
		int sd = s.getDepth();
		int td = t.getDepth();

		while (sd > td) {
			s = s.parent;
			--sd;
		}
		while (td > sd) {
			t = t.parent;
			--td;
		}

		while (s != t) {
			s = s.parent;
			t = t.parent;
		}
		pivot = s;
		if (pivot == null)
			return false;

		if (pivot != target) {
			Matrix targetPath = new Matrix();
			Matrix sourcePath = new Matrix();

			getTransformUpPath(target, pivot, targetPath);

			if (!targetPath.invertMatrix())
				return false;

			if (pivot != this) {
				getTransformUpPath(this, pivot, sourcePath);
				targetPath.rightMulMatrix(sourcePath);
				transform.copyMatrix(targetPath);
			} else
				transform.copyMatrix(targetPath);
		} else
			getTransformUpPath(this, pivot, transform);

		return true;
	}

	public boolean getTransformTo(Node target, Transform transform) {
		return getTransformTo(target, transform.mtx);
	}

	public boolean isPickingEnabled() {
		return enableBits[PICK];
	}

	public boolean isRenderingEnabled() {
		return enableBits[RENDER];
	}

	public void setAlignment(Node zReference, int zTarget, Node yReference, int yTarget) {/*
		if (((zTarget != Node.NONE) &&  (zTarget != Node.Z_AXIS)) || ((yTarget != Node.NONE) &&  (yTarget != Node.Y_AXIS)))
			throw new IllegalArgumentException();
		if ((zRef == yRef) && (zTarget != NONE || yTarget != NONE))
			throw new IllegalArgumentException();
		if (zRef == this || yRef == this)
			throw new IllegalArgumentException("can not use this as refnode");*/

		this.zReference = (zTarget != NONE) ? zReference : null;
		this.yReference = (yTarget != NONE) ? yReference : null;
		this.zTarget = zTarget;
		this.yTarget = yTarget;
	}

	public void setAlphaFactor(float alphaFactor) {
		if (alphaFactor < 0 || alphaFactor > 1)
			throw new IllegalArgumentException("alphaFactor must be in [0,1]");
		this.alphaFactor = (int)(alphaFactor * 0xFFFF);
	}

	public void setPickingEnable(boolean enable) {
		enableBits[PICK] = enable;
	}

	public void setRenderingEnable(boolean enable) {
		enableBits[RENDER] = enable;
	}

	public void setScope(int scope) {
		this.scope = scope;
	}

	Node getZRef() {
		return zReference;
	}

	Node getYRef() {
		return yReference;
	}

	public Node getAlignmentReference(int axis) {
		switch (axis) {
			case Y_AXIS:
				return yReference;
			case Z_AXIS:
				return zReference;
			default:
				throw new IllegalArgumentException();
		}
	}

	public int getAlignmentTarget(int axis) {
		switch (axis) {
			case Y_AXIS:
				return yTarget;
			case Z_AXIS:
				return zTarget;
			default:
				throw new IllegalArgumentException();
		}
	}

	private int getDepth() {
		int depth = 0;
		Node n = this;
		while (n.parent != null) {
			n = n.parent;
			++depth;
		}
		return depth;
	}
	
	boolean isCompatible(AnimationTrack track) {
		switch (track.getTargetProperty()) {
		case AnimationTrack.ALPHA:
		case AnimationTrack.VISIBILITY:
		case AnimationTrack.PICKABILITY:
		    return true;
		default:
		    return super.isCompatible(track);
		}
	}
	
}
