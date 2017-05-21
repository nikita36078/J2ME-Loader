package ua.naiksoftware.util;

/**
 *
 * @author Naik
 */
public class MathUtils {

    /**
     *
     * @param sourceNum
     * @return rounded to two decimal places
     */
    public static float round(float sourceNum) {
        int temp = (int) (sourceNum / 0.01f);
        return temp / 100f;
    }
}
