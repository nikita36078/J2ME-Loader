package javax.microedition.sensor;

public interface ChannelInfo {
    public static final int TYPE_DOUBLE = 1;
    public static final int TYPE_INT = 2;
    public static final int TYPE_OBJECT = 4;

    float getAccuracy();

    int getDataType();

    MeasurementRange[] getMeasurementRanges();

    String getName();

    int getScale();

    Unit getUnit();
}
