package javax.microedition.sensor;

public final class RangeCondition implements Condition {

    public final double getLowerLimit() {
        return 0;
    }

    public final String getLowerOp() {
        return null;
    }

    public final double getUpperLimit() {
        return 0;
    }

    public final String getUpperOp() {
        return null;
    }

    public final boolean isMet(double d) {
        return false;
    }

    public final boolean isMet(Object obj) {
        return false;
    }
}
