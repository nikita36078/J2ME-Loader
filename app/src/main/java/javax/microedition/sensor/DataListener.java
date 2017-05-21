package javax.microedition.sensor;

public interface DataListener {
    void dataReceived(SensorConnection sensorConnection, Data[] dataArr, boolean z);
}
