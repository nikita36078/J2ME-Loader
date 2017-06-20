package javax.microedition.m3g;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.nio.ByteOrder;

public abstract class IndexBuffer extends Object3D {
	protected ShortBuffer buffer = null;
	int indexCount;

	public int getIndexCount() {
		//return buffer.limit();
		return indexCount;
	}

	public abstract void getIndices(int[] indices);

	protected void allocate(int numElements) {
		ByteBuffer buf = ByteBuffer.allocateDirect(numElements * 2).order(ByteOrder.nativeOrder());
		buffer = buf.asShortBuffer();
	}

	ShortBuffer getBuffer() {
		return buffer;
	}
}
