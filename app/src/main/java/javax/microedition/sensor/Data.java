package javax.microedition.sensor;

public interface Data {
    ChannelInfo getChannelInfo();

    double[] getDoubleValues();

    int[] getIntValues();

    Object[] getObjectValues();

    long getTimestamp(int i);

    float getUncertainty(int i);

    boolean isValid(int i);
}
