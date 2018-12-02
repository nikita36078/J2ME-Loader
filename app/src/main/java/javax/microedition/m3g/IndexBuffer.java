package javax.microedition.m3g;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

public abstract class IndexBuffer extends Object3D {
	protected IntBuffer buffer = null;
	int indexCount;

	public abstract int getIndexCount();

	public abstract void getIndices(int[] indices);

	protected void allocate(int numElements) {
		ByteBuffer buf = ByteBuffer.allocateDirect(numElements * 4).order(ByteOrder.nativeOrder());
		buffer = buf.asIntBuffer();
	}

	IntBuffer getBuffer() {
		return buffer;
	}
}
