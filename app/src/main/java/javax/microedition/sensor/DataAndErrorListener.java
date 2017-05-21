package javax.microedition.sensor;

public interface DataAndErrorListener extends DataListener {
    void errorReceived(SensorConnection sensorConnection, int i, long j);
}
