package javax.microedition.sensor;

public final class LimitCondition implements Condition {
	public final double getLimit() {
		return 0;
	}

	public final String getOperator() {
		return "";
	}

	@Override
	public boolean isMet(double d) {
		return false;
	}

	public final boolean isMet(Object obj) {
		return false;
	}
}
