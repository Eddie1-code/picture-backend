package com.xcw.picturebackend.utils;


/**
 * @author 2340129326 许灿炜
 * @date 2025/9/14
 */

/**
 * 颜色转换工具类
 */
public class ColorTransformUtils {
    private ColorTransformUtils() {
        // 私有构造函数，防止实例化（工具类不需要实例化）
    }

    public static String getStandardColor(String color) {
        // 每一种 rgb 色值都有可能只有一个 0， 要转换为00）
        // 如果是六位，不用转换，如果是五位，要给第三位后面加个 0
        // eg: 0x080e0 => 0x0800e
        if(color.length() == 0){
            color = color.substring(0, 4) + "0" + color.substring(4, 7);
        }
        return color;
    }
}
