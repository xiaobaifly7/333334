package com.hanbing.wltc.core.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 编码修复主类
 * 用于批量修复源文件编码
 */
public class EncodingFixerMain {
    
    public static void main(String[] args) {
        // 获取项目根目录
        String projectRoot = System.getProperty("user.dir");
        System.out.println("项目根目录: " + projectRoot);
        
        // 查找所有Java文件
        List<File> javaFiles = new ArrayList<>();
        findJavaFiles(new File(projectRoot), javaFiles);
        
        System.out.println("共找到 " + javaFiles.size() + " 个Java文件");
        
        // 逐个文件尝试修复
        int fixedCount = 0;
        for (File file : javaFiles) {
            System.out.println("正在处理: " + file.getPath());
            if (EncodingFixer.fixFileEncoding(file)) {
                fixedCount++;
                System.out.println("修复成功: " + file.getPath());
            } else {
                System.out.println("修复失败: " + file.getPath());
            }
        }
        
        System.out.println("处理完成！");
        System.out.println("共修复了 " + fixedCount + " 个文件");
    }
    
    /**
     * 递归搜索所有Java文件
     */
    private static void findJavaFiles(File dir, List<File> javaFiles) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    // 跳过.git、.gradle等隐藏目录
                    if (!file.getName().startsWith(".")) {
                        findJavaFiles(file, javaFiles);
                    }
                } else if (file.getName().endsWith(".java")) {
                    javaFiles.add(file);
                }
            }
        }
    }
}