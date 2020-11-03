package javax.microedition.rms.impl;

import javax.microedition.rms.RecordListener;
import javax.microedition.rms.RecordStore;

public interface ExtendedRecordListener extends RecordListener {
	int RECORD_ADD = 1;
	int RECORD_READ = 2;
	int RECORD_CHANGE = 3;
	int RECORD_DELETE = 4;

	void recordEvent(int type, long timestamp, RecordStore recordStore, int recordId);
}
