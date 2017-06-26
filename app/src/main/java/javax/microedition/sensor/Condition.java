package javax.microedition.sensor;

public interface Condition {
	public static final String OP_EQUALS = "eq";
	public static final String OP_GREATER_THAN = "gt";
	public static final String OP_GREATER_THAN_OR_EQUALS = "ge";
	public static final String OP_LESS_THAN = "lt";
	public static final String OP_LESS_THAN_OR_EQUALS = "le";

	boolean isMet(double d);

	boolean isMet(Object obj);
}
