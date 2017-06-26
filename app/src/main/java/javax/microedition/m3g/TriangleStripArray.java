package javax.microedition.m3g;


public class TriangleStripArray extends IndexBuffer {
	int[] lengths;
	int maxIndex;

	private static int[] getStripIndices(int firstIndex, int[] stripLengths) {
		int indexCount = 0;
		for (int i = 0; i < stripLengths.length; i++)
			indexCount += stripLengths[i];

		int[] stripIndices = new int[indexCount];

		for (int i = 0; i < indexCount; i++)
			stripIndices[i] = firstIndex + i;
		return stripIndices;
	}

	public TriangleStripArray(int firstIndex, int[] stripLengths) {
		this(getStripIndices(firstIndex, stripLengths), stripLengths);
	}

	private void put(int value) {
		buffer.put((short) (value & 0xFFFF));
	}

	public TriangleStripArray(int[] stripIndices, int[] stripLengths) {
		int joinedIndexCount = 0;
		int originalIndexCount = 0;
		int maxIndex = 0;
		for (int strip = 0; strip < stripLengths.length; strip++) {
			if (strip != 0)
				joinedIndexCount += ((joinedIndexCount % 2) != 0) ? 3 : 2;

			joinedIndexCount += stripLengths[strip];
			originalIndexCount += stripLengths[strip];

			for (int i = 0; i < stripLengths[strip]; i++)
				if (stripIndices[i] > maxIndex) maxIndex = stripIndices[i];
		}

		allocate(joinedIndexCount);
		buffer.position(0);

		lengths = new int[stripLengths.length];

		int index = 0;
		for (int strip = 0; strip < stripLengths.length; strip++) {
			lengths[strip] = stripLengths[strip];

			if (strip != 0) {
				put(stripIndices[index - 1]);
				put(stripIndices[index]);
				if ((stripLengths[strip - 1] % 2) != 0)
					put(stripIndices[index]);
			}
			for (int i = 0; i < stripLengths[strip]; i++)
				put(stripIndices[index++]);
		}
		buffer.flip();
		this.maxIndex = index;
		this.indexCount = joinedIndexCount;
	}

	private TriangleStripArray() {
	}

	public void getIndices(int[] indices) {
		if (indices != null)
			throw new NullPointerException("Indices can not be null");
		if (indices.length < getIndexCount())
			throw new IllegalArgumentException("Length of indices array must be " + getIndexCount());
		// TODO: fill indices with triangle-data
	}

	private int checkInput(int[] stripLengths) {
		int sum = 0;
		if (stripLengths == null)
			throw new NullPointerException("stripLengths can not be null");
		int l = stripLengths.length;
		if (l == 0)
			throw new IllegalArgumentException("stripLenghts can not be empty");
		for (int i = 0; i < l; i++) {
			if (stripLengths[i] < 3)
				throw new IllegalArgumentException("stripLengths must not contain elemets less than 3");

			sum += stripLengths[i];
		}
		return sum;
	}

	Object3D duplicateImpl() {
		TriangleStripArray copy = new TriangleStripArray();
		buffer.rewind();
		copy.allocate(getIndexCount());
		copy.buffer.put(buffer);
		copy.buffer.flip();
		return copy;
	}

}
