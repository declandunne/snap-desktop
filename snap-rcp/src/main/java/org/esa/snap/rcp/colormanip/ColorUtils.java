package org.esa.snap.rcp.colormanip;

import org.esa.snap.rcp.SnapApp;

/**
 * Utility class containing methods for the Color Manipulation Tool.
 *
 *
 * @author Jean Coravu
 * @author Daniel Knowles
 */
public class ColorUtils {

    /**
     * Private constructor to avoid creating new objects.
     */
    private  ColorUtils() {
    }

    /**
     * Returns the RGB color as int.
     *
     * @param red the red component
     * @param green the green component
     * @param blue the blue component
     * @return the RGB color as int
     */
    public static int rgba(int red, int green, int blue) {
        int rgba = 255;
        rgba = (rgba << 8) + red;
        rgba = (rgba << 8) + green;
        rgba = (rgba << 8) + blue;
        return rgba;
    }

    /**
     * Returns the alpha component.
     *
     * @param color the RGB color
     * @return the alpha component
     */
    public static int alpha(int color) {
        return color >> 24 & 0x0FF;
    }

    /**
     * Returns the red component.
     *
     * @param color the RGB color
     * @return the red component
     */
    public static int red(int color) {
        return color >> 16 & 0x0FF;
    }

    /**
     * Returns the green component.
     *
     * @param color the RGB color
     * @return the green component
     */
    public static int green(int color) {
        return color >> 8 & 0x0FF;
    }

    /**
     * Returns the blue component.
     *
     * @param color the RGB color
     * @return the blue component
     */
    public static int blue(int color) {
        return color & 0x0FF;
    }







    public static boolean isNumber(String string) {
        try {
            double d = Double.parseDouble(string);
        } catch (NumberFormatException nfe) {
            return false;
        }

        return true;
    }


    public static boolean checkRangeCompatibility(String minStr, String maxStr) {

        if (!isNumber(minStr)) {
            SnapApp.getDefault().setStatusBarMessage("INPUT ERROR!!: Min Textfield is not a number");
            return false;
        }

        if (!isNumber(maxStr)) {
            SnapApp.getDefault().setStatusBarMessage("INPUT ERROR!!: Max Textfield is not a number");
            return false;
        }

        double min = Double.parseDouble(minStr);
        double max = Double.parseDouble(maxStr);
        if (!checkRangeCompatibility(min, max)) {
            return false;
        }

        SnapApp.getDefault().setStatusBarMessage("");
        return true;
    }



    public static boolean checkRangeCompatibility(double min, double max) {

        if (min >= max) {
            SnapApp.getDefault().setStatusBarMessage("INPUT ERROR!!: Max must be greater than Min");
            return false;
        }

        SnapApp.getDefault().setStatusBarMessage("");
        return true;
    }



    public static boolean checkRangeCompatibility(double min, double max, boolean isLogScaled) {

        if (!checkRangeCompatibility(min, max)) {
            return false;
        }

        if (!checkLogCompatibility(min, "Min Textfield", isLogScaled)) {
            return false;
        }

        SnapApp.getDefault().setStatusBarMessage("");
        return true;
    }


    public static boolean checkSliderRangeCompatibility(double value, double min, double max) {
        if (value <= min || value >= max) {
            SnapApp.getDefault().setStatusBarMessage("INPUT ERROR!!: Slider outside range of adjacent sliders");
            return false;
        }
        return true;
    }



    public static boolean checkLogCompatibility(double value, String componentName, boolean isLogScaled) {

        if ((isLogScaled) && value <= 0) {
            SnapApp.getDefault().setStatusBarMessage("INPUT ERROR!!: " + componentName + " must be greater than zero in log scaling mode");
            return false;
        }

        SnapApp.getDefault().setStatusBarMessage("");

        return true;
    }





}
