package com.hanbing.wltc.core.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * 字节数组组合器
 * 用于高效组合多个字节数组
 */
public final class ByteArrayCombiner {
    private final ByteArrayOutputStream outputStream;
    
    public ByteArrayCombiner() {
        outputStream = new ByteArrayOutputStream();
    }
    
    /**
     * 添加字节数组
     */
    public ByteArrayCombiner add(byte[] data) {
        if (data != null) {
            try {
                outputStream.write(data);
            } catch (IOException e) {
                // 不可能发生
            }
        }
        return this;
    }
    
    /**
     * 获取组合后的字节数组
     */
    public byte[] toByteArray() {
        return outputStream.toByteArray();
    }
    
    /**
     * 清空数据
     */
    public void clear() {
        outputStream.reset();
    }
}