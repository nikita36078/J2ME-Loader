package javax.microedition.sensor;

public interface SensorListener {
	void sensorAvailable(SensorInfo sensorInfo);

	void sensorUnavailable(SensorInfo sensorInfo);
}
