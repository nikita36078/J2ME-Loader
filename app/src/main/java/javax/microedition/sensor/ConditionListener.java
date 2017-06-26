package javax.microedition.sensor;

public interface ConditionListener {
	void conditionMet(SensorConnection sensorConnection, Data data, Condition condition);
}
