package javax.microedition.rms.impl;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import javax.microedition.rms.InvalidRecordIDException;
import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreException;
import javax.microedition.rms.RecordStoreFullException;
import javax.microedition.rms.RecordStoreNotOpenException;

public class RecordStoreImplTest {

	/** Header of a real save file written by J2ME-Loader 1.8.2 (store "bomb", 1 record). */
	private static final byte[] REAL_HEADER = hex(
			"4d49445 24d53 0300 00 0004626f6d62 000001 7e1a02ae89 00000001 00000000 00 00000001 00000001");
	/** Matching record file: id=1, tag=0, 45 bytes of payload. */
	private static final byte[] REAL_RECORD;

	static {
		byte[] payload = new byte[45];
		payload[0] = 0x03;
		payload[2] = 0x79;
		payload[3] = 0x63;
		payload[4] = 0x6e;
		payload[5] = (byte) 0xc8;
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(bos);
		try {
			dos.writeInt(1);
			dos.writeInt(0);
			dos.writeInt(payload.length);
			dos.write(payload);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
		REAL_RECORD = bos.toByteArray();
	}

	private int saved;
	private int deleted;
	private long sizeAvailable;
	private RecordStoreImpl store;

	private final RecordStoreManager manager = new RecordStoreManager() {
		@Override
		public void deleteRecord(RecordStoreImpl s, int id) {
			deleted++;
		}

		@Override
		public void deleteRecordStore(String name) {
		}

		@Override
		public String getName() {
			return "test";
		}

		@Override
		public int getSizeAvailable(RecordStoreImpl s) {
			return (int) sizeAvailable;
		}

		@Override
		public String[] listRecordStores() {
			return new String[0];
		}

		@Override
		public void loadRecord(RecordStoreImpl s, int id) {
		}

		@Override
		public RecordStore openRecordStore(String name, boolean create) {
			return store;
		}

		@Override
		public void saveRecord(RecordStoreImpl s, int id) {
			saved++;
		}
	};

	@Before
	public void setUp() {
		sizeAvailable = 1 << 20;
		store = new RecordStoreImpl(manager, "test");
		store.setOpen();
	}

	private static byte[] hex(String s) {
		s = s.replace(" ", "");
		byte[] out = new byte[s.length() / 2];
		for (int i = 0; i < out.length; i++) {
			out[i] = (byte) Integer.parseInt(s.substring(2 * i, 2 * i + 2), 16);
		}
		return out;
	}

	@Test
	public void readHeader_parsesRealSaveFile() throws Exception {
		RecordStoreImpl impl = new RecordStoreImpl(manager);
		impl.readHeader(new DataInputStream(new ByteArrayInputStream(REAL_HEADER)));
		impl.setOpen();
		assertEquals("bomb", impl.getName());
		assertEquals(1, impl.getVersion());
		assertEquals(1, impl.getNextRecordID() - 1);
	}

	@Test
	public void readRecord_parsesRealRecord() throws Exception {
		RecordStoreImpl impl = new RecordStoreImpl(manager);
		impl.readRecord(new DataInputStream(new ByteArrayInputStream(REAL_RECORD)));
		impl.setOpen();
		byte[] data = impl.getRecord(1);
		assertEquals(45, data.length);
		assertEquals(0x79, data[2]);
		assertEquals(2, impl.getNextRecordID());
	}

	@Test
	public void readHeader_rejectsBadMagic() {
		byte[] bad = REAL_HEADER.clone();
		bad[0] = 'X';
		RecordStoreImpl impl = new RecordStoreImpl(manager);
		assertThrows(IOException.class, () ->
				impl.readHeader(new DataInputStream(new ByteArrayInputStream(bad))));
	}

	@Test
	public void headerAndRecord_roundTrip() throws Exception {
		int id = store.addRecord(new byte[]{1, 2, 3}, 0, 3);
		ByteArrayOutputStream hb = new ByteArrayOutputStream();
		store.writeHeader(new DataOutputStream(hb));
		ByteArrayOutputStream rb = new ByteArrayOutputStream();
		store.writeRecord(new DataOutputStream(rb), id);

		RecordStoreImpl copy = new RecordStoreImpl(manager);
		copy.readHeader(new DataInputStream(new ByteArrayInputStream(hb.toByteArray())));
		copy.readRecord(new DataInputStream(new ByteArrayInputStream(rb.toByteArray())));
		copy.setOpen();
		assertEquals("test", copy.getName());
		assertArrayEquals(new byte[]{1, 2, 3}, copy.getRecord(id));
		assertEquals(store.getNextRecordID(), copy.getNextRecordID());
	}

	@Test
	public void addRecord_assignsIncreasingIdsAndCopiesData() throws Exception {
		byte[] src = {9, 8, 7, 6};
		int a = store.addRecord(src, 1, 2);
		int b = store.addRecord(src, 0, 4);
		assertEquals(1, a);
		assertEquals(2, b);
		src[1] = 0;
		assertArrayEquals(new byte[]{8, 7}, store.getRecord(a));
		assertEquals(2, store.getNumRecords());
		assertEquals(2, saved);
	}

	@Test
	public void deleteRecord_doesNotReuseId() throws Exception {
		int a = store.addRecord(new byte[]{1}, 0, 1);
		store.deleteRecord(a);
		assertEquals(1, deleted);
		assertThrows(InvalidRecordIDException.class, () -> store.getRecord(a));
		assertThrows(InvalidRecordIDException.class, () -> store.deleteRecord(a));
		assertEquals(a + 1, store.addRecord(new byte[]{2}, 0, 1));
	}

	@Test
	public void setRecord_replacesDataAndBumpsVersion() throws Exception {
		int id = store.addRecord(new byte[]{1}, 0, 1);
		int v = store.getVersion();
		store.setRecord(id, new byte[]{5, 6}, 0, 2);
		assertArrayEquals(new byte[]{5, 6}, store.getRecord(id));
		assertEquals(v + 1, store.getVersion());
		assertThrows(InvalidRecordIDException.class,
				() -> store.setRecord(99, new byte[]{1}, 0, 1));
	}

	@Test
	public void emptyRecord_returnsNull() throws Exception {
		int id = store.addRecord(null, 0, 0);
		assertNull(store.getRecord(id));
	}

	@Test
	public void addRecord_throwsWhenFull() {
		sizeAvailable = 2;
		assertThrows(RecordStoreFullException.class, () -> store.addRecord(new byte[3], 0, 3));
	}

	@Test
	public void operationsOnClosedStore_throw() throws Exception {
		store.closeRecordStore();
		assertThrows(RecordStoreNotOpenException.class, () -> store.addRecord(new byte[1], 0, 1));
		assertThrows(RecordStoreNotOpenException.class, () -> store.getRecord(1));
		assertThrows(RecordStoreNotOpenException.class, () -> store.getNumRecords());
	}

	@Test
	public void nestedOpens_requireMatchingCloses() throws Exception {
		store.setOpen();
		store.closeRecordStore();
		store.addRecord(new byte[1], 0, 1);
		store.closeRecordStore();
		assertThrows(RecordStoreException.class, () -> store.closeRecordStore());
	}
}
