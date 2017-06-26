package javax.microedition.rms.impl;

import javax.microedition.rms.RecordListener;
import javax.microedition.rms.RecordStore;

public interface ExtendedRecordListener extends RecordListener {

	int RECORD_ADD = 1;

	int RECORD_READ = 2;

	int RECORD_CHANGE = 3;

	int RECORD_DELETE = 4;

	int RECORDSTORE_OPEN = 8;

	int RECORDSTORE_CLOSE = 9;

	int RECORDSTORE_DELETE = 10;

	void recordEvent(int type, long timestamp, RecordStore recordStore, int recordId);

	void recordStoreEvent(int type, long timestamp, String recordStoreName);

}
