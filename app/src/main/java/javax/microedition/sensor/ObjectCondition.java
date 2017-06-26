package javax.microedition.sensor;

public final class ObjectCondition implements Condition {
	public ObjectCondition(Object obj) {
	}

	public final Object getLimit() {
		return null;
	}

	public final boolean isMet(double d) {
		return false;
	}

	public final boolean isMet(Object obj) {
		return false;
	}
}
