package javax.microedition.m3g;

import java.util.Vector;

public class SkinnedMesh extends Mesh {

	Group skeleton;
	int bonesPerVertex;
	int[][] boneIndices = new int[4][1];
	int[][] boneWeights = new int[4][1];
	int[][] normalizedWeights = new int[4][1];
	int[] weightShifts = new int[1];
	int weightedVertexCount;
	Vector<Bone> boneArray = new Vector<>();
	boolean weightsDirty;

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

	private SkinnedMesh() {
	}

	@Override
	Object3D duplicateImpl() {
		Group skeleton = (Group) this.skeleton.duplicate();
		SkinnedMesh copy = new SkinnedMesh();
		super.duplicate(copy);
		copy.skeleton = skeleton;
		return copy;
	}

	@Override
	int doGetReferences(Object3D[] references) {
		int num = super.doGetReferences(references);
		if (skeleton != null) {
			if (references != null)
				references[num] = skeleton;
			num++;
		}
		return num;
	}

	@Override
	Object3D findID(int userID) {
		Object3D found = super.findID(userID);

		if ((found == null) && (skeleton != null))
			found = skeleton.findID(userID);
		return found;
	}

	@Override
	int applyAnimation(int time) {
		int validity = super.applyAnimation(time);

		if (validity > 0) {
			int validity2 = skeleton.applyAnimation(time);
			return Math.min(validity, validity2);
		}
		return 0;
	}

	public void addTransform(Node bone, int weight, int firstVertex, int numVertices) {
		int lastVertex = firstVertex + numVertices;
		if (bone == null)
			throw new NullPointerException();
		if ((weight <= 0) || (numVertices <= 0))
			throw new IllegalArgumentException();
		if ((firstVertex < 0) || (lastVertex > 65535))
			throw new IndexOutOfBoundsException();
		ensureVertexCount(lastVertex);
		if (bonesPerVertex < 4) {
			int numBones = bonesPerVertex;
			int maxBones = 0;
			for (int vertex = firstVertex; vertex < lastVertex; ++vertex) {
				int k;
				for (k = numBones; k > 0; --k) {
					if (boneWeights[k - 1][vertex] > 0) {
						maxBones = Math.max(maxBones, k);
						break;
					}
				}
			}

			ensureBonesPerVertex(maxBones + 1);
		}

		int boneIndex = boneIndex(bone);
		if (boneIndex < 0) {
			return;
		}

		for (int i = firstVertex; i < lastVertex; i++) {
			addInfluence(i, boneIndex, weight);
		}

		bone.hasBones = true;
	}

	private void addInfluence(int vertexIndex, int boneIndex, int weight) {
		int minWeight = weight;
		int minWeightIndex = -1;

		weight >>= weightShifts[vertexIndex];

		for (int i = 0; i < bonesPerVertex; ++i) {
			if (boneIndices[i][vertexIndex] == boneIndex) {
				weight += boneWeights[i][vertexIndex];
				minWeightIndex = i;
				break;
			}
			else {
				int tempWeight = boneWeights[i][vertexIndex];
				if (tempWeight < minWeight) {
					minWeight = tempWeight;
					minWeightIndex = i;
				}
			}
		}

		while (weight >= (1 << 8)) {
			weight >>= 1;
			weightShifts[vertexIndex] += 1;
			for (int i = 0; i < bonesPerVertex; ++i) {
				boneWeights[i][vertexIndex] >>= 1;
			}
		}

		if (minWeightIndex >= 0) {
			boneIndices[minWeightIndex][vertexIndex] = boneIndex;
			boneWeights[minWeightIndex][vertexIndex] = weight;

			weightsDirty = true;
			invalidateNode(new boolean[]{false, false});
		}
	}

	private int boneIndex(Node node) {
		int numBones = boneArray.size();
		for (int i = 0; i < numBones; ++i) {
			Bone b = boneArray.elementAt(i);
			if (b.node == node) {
				return i;
			}
		}

		if (numBones >= 256) {
			return -1;
		} else {
			boneArray.add(new Bone());
			return boneArray.size() - 1;
		}
	}

	private void ensureVertexCount(int count) {
		if (count > weightedVertexCount) {
			int[] array = new int[count];
			System.arraycopy(weightShifts, 0, array, 0, weightedVertexCount);
			weightShifts = array;

			for (int i = 0; i < bonesPerVertex; ++i) {
				array = new int[count];
				System.arraycopy(boneIndices[i], 0, array, 0, weightedVertexCount);
				boneIndices[i] = array;

				array = new int[count];
				System.arraycopy(boneWeights[i], 0, array, 0, weightedVertexCount);
				boneWeights[i] = array;

				array = new int[count];
				System.arraycopy(normalizedWeights[i], 0, array, 0, weightedVertexCount);
				normalizedWeights[i] = array;
			}

			weightedVertexCount = count;
		}
	}

	private void ensureBonesPerVertex(int count) {
		if (count > bonesPerVertex) {
			for (int i = bonesPerVertex; i < count; ++i) {
				int[] array = new int[count];
				boneIndices[i] = array;

				array = new int[count];
				boneWeights[i] = array;

				array = new int[count];
				normalizedWeights[i] = array;
			}

			bonesPerVertex = count;
		}
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

	class Bone {
		Node node;

		Matrix toBone;

		short[] baseMatrix = new short[9];
		short[] posVec = new short[3];
		short baseExp, posExp, maxExp;

		short[] normalMatrix = new short[9];
	}

	;

}
