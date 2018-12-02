package javax.microedition.m3g;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

import javax.microedition.khronos.opengles.GL10;

public class VertexArray extends Object3D {

	private int vertexCount;
	private int elementSize;
	private int elementType;
	private int numElements;
	int stride;

	private Buffer buffer;

	private VertexArray() {
	}

	public VertexArray(int numVertices, int numComponents, int componentSize) {
		if (numVertices < 1 || numVertices > 65535)
			throw new IllegalArgumentException("numVertices must be in [1,65535]");
		if (numComponents < 2 || numComponents > 4)
			throw new IllegalArgumentException("numComponents must be in [2,4]");
		if (componentSize < 1 || componentSize > 2)
			throw new IllegalArgumentException("componentSize must be in [1,2]");

		this.vertexCount = numVertices;
		this.elementSize = numComponents;
		this.elementType = componentSize;

		numElements = numVertices * numComponents;

		if (componentSize == 1) {
			buffer = ByteBuffer.allocateDirect(numElements * 4).order(ByteOrder.nativeOrder());
			this.stride = 4;
		} else {
			buffer = ByteBuffer.allocateDirect(numElements * 2).order(ByteOrder.nativeOrder()).asShortBuffer();
			this.stride = numComponents * 2;
		}
	}

	public void set(int firstVertex, int numVertices, short[] values) {
		int numElements = numVertices * elementSize;
		checkShortInput(firstVertex, numVertices, numElements, values);

		ShortBuffer shortBuffer = (ShortBuffer) buffer;
		shortBuffer.position(firstVertex * stride / 2);
		int index = 0;
		for (int count = numElements; count > 0; count--)
			shortBuffer.put(values[index++]);
		shortBuffer.position(0);
	}

	public void set(int firstVertex, int numVertices, byte[] values) {
		int numElements = numVertices * elementSize;
		checkByteInput(firstVertex, numVertices, numElements, values);

		ByteBuffer byteBuffer = (ByteBuffer) buffer;
		byteBuffer.position(firstVertex * stride);
		int index = 0;
		for (int count = numVertices * elementSize; count > 0; count -= elementSize) {
			byteBuffer.put(values[index++]);
			byteBuffer.put(values[index++]);
			byteBuffer.put((elementSize >= 3) ? values[index++] : (byte) 0x00);
			byteBuffer.put((elementSize == 4) ? values[index++] : (byte) 0xFF);
		}
		byteBuffer.position(0);
	}

	@Override
	Object3D duplicateImpl() {
		VertexArray copy = new VertexArray();
		copy.vertexCount = vertexCount;
		copy.elementSize = elementSize;
		copy.elementType = elementType;
		copy.numElements = numElements;
		copy.stride = stride;
		int length = numElements * stride;
		buffer.rewind();
		if (buffer instanceof ByteBuffer) {
			copy.buffer = ByteBuffer.allocateDirect(length).order(ByteOrder.nativeOrder());
			copy.buffer.rewind();
			((ByteBuffer) copy.buffer).put((ByteBuffer) buffer);
		} else if (buffer instanceof ShortBuffer) {
			copy.buffer = ByteBuffer.allocateDirect(length).order(ByteOrder.nativeOrder()).asShortBuffer();
			copy.buffer.rewind();
			((ShortBuffer) copy.buffer).put((ShortBuffer) buffer);
		}
		return copy;
	}

	public int getVertexCount() {
		return this.vertexCount;
	}

	public int getComponentCount() {
		return this.elementSize;
	}

	public int getComponentType() {
		return this.elementType;
	}

	public void get(int firstVertex, int numVertices, short[] values) {
		int numElements = numVertices * elementSize;
		checkShortInput(firstVertex, numVertices, numElements, values);

		ShortBuffer shortBuffer = (ShortBuffer) buffer;
		shortBuffer.position(firstVertex * stride / 2);
		shortBuffer.get(values, 0, numElements);
	}

	public void get(int firstVertex, int numVertices, byte[] values) {
		int numElements = numVertices * elementSize;
		checkByteInput(firstVertex, numVertices, numElements, values);

		ByteBuffer byteBuffer = (ByteBuffer) buffer;
		byteBuffer.position(firstVertex * stride);
		int index = 0;
		for (int i = 0; i < numVertices; i++) {
			values[index++] = byteBuffer.get();
			values[index++] = byteBuffer.get();
			if (elementSize >= 3)
				values[index++] = byteBuffer.get();
			else
				byteBuffer.get();
			if (elementSize == 4)
				values[index++] = byteBuffer.get();
			else
				byteBuffer.get();
		}
	}

	private void checkShortInput(int firstVertex, int numVertices, int numElements, short[] values) {
		if (values == null)
			throw new NullPointerException("values can not be null");
		if (elementType != 2)
			throw new IllegalStateException("vertexarray created as short array. can not get byte values");
		checkInput(firstVertex, numVertices, numElements, values.length);
	}

	private void checkByteInput(int firstVertex, int numVertices, int numElements, byte[] values) {
		if (values == null)
			throw new NullPointerException("values can not be null");
		if (elementType != 1)
			throw new IllegalStateException("vertexarray created as short array. can not set byte values");
		checkInput(firstVertex, numVertices, numElements, values.length);
	}

	private void checkInput(int firstVertex, int numVertices, int numElements, int arrayLength) {
		if (numVertices < 0)
			throw new IllegalArgumentException("numVertices must be > 0");
		if (arrayLength < numElements)
			throw new IllegalArgumentException("number of elements i values does not match numVertices");
		if (firstVertex < 0 || firstVertex + numVertices > this.vertexCount)
			throw new IndexOutOfBoundsException("index out of bounds");
	}

	int getComponentTypeGL() {
		if (elementType == 1)
			return GL10.GL_BYTE;
		else
			return GL10.GL_SHORT;
	}

	Buffer getBuffer() {
		return buffer;
	}
}
