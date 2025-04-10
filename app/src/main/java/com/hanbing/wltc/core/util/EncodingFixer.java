package com.hanbing.wltc.core.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * 编码修复工具
 * 用于修复Java源文件的编码问题
 */
public class EncodingFixer {
    
    /**
     * 修复文件编码
     * @param file 要修复的文件
     * @return 是否修复成功
     */
    public static boolean fixFileEncoding(File file) {
        try {
            // 读取文件
            String content = readFile(file);
            
            // 修复编码
            String fixedContent = fixEncoding(content);
            
            // 写回文件
            writeFile(file, fixedContent);
            
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 修复字符串的编码
     * @param text 需要修复的文本
     * @return 修复后的文本
     */
    public static String fixEncoding(String text) {
        try {
            // 先按照ISO-8859-1解码
            byte[] bytes = text.getBytes("ISO-8859-1");
            // 按照UTF-8编码回来
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return text;
        }
    }
    
    /**
     * 读取文件内容
     */
    private static String readFile(File file) throws Exception {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[(int) file.length()];
            fis.read(buffer);
            return new String(buffer, StandardCharsets.UTF_8);
        }
    }
    
    /**
     * 写入文件内容
     */
    private static void writeFile(File file, String content) throws Exception {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(content.getBytes(StandardCharsets.UTF_8));
        }
    }
}