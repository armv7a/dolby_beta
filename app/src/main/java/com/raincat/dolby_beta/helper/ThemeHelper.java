package com.raincat.dolby_beta.helper;

import android.content.Context;
import android.graphics.Color;
import android.view.ContextThemeWrapper;

/**
 * <pre>
 *     author : armv7a
 *     e-mail : neko@outlook.lv
 *     time   : 2026/09/14
 *     desc   : 深浅色主题
 *     version: 1.0
 * </pre>
 */
public class ThemeHelper {
    private static boolean isDark = false;

    private ThemeHelper() {
    }
    
    public static void updateDarkMode(int textColor) {
        isDark = luminance(textColor) > 0.5f;
    }

    public static boolean isDarkMode() {
        return isDark;
    }

    public static int getPrimaryTextColor() {
        return isDark ? Color.WHITE : Color.BLACK;
    }

    public static int getSecondaryTextColor() {
        return isDark ? 0xFF9E9E9E : Color.DKGRAY;
    }

    public static int getDisabledTextColor() {
        return isDark ? 0xFF555555 : Color.LTGRAY;
    }

    public static Context wrapDialogTheme(Context context) {
        if (isDark) {
            return new ContextThemeWrapper(context, android.R.style.Theme_Material_Dialog);
        }
        return context;
    }

    private static double luminance(int color) {
        double r = Color.red(color) / 255.0;
        double g = Color.green(color) / 255.0;
        double b = Color.blue(color) / 255.0;
        return 0.2126 * r + 0.7152 * g + 0.0722 * b;
    }
}
