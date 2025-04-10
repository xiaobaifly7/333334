#!/usr/bin/env python
# -*- coding: utf-8 -*-

"""
更新QuantumUrlShield.java中的URL矩阵
简单易用的非交互式脚本
"""

import re
import argparse
import sys

def generate_url_matrix(url):
    """将URL转换为字节数组格式的Java代码"""
    url_bytes = url.encode('utf-8')
    byte_array = []
    for b in url_bytes:
        byte_array.append(f"(byte)0x{b:02X}")
    
    # 格式化为每行8个字节的数组
    formatted_bytes = []
    for i in range(0, len(byte_array), 8):
        line = ", ".join(byte_array[i:i+8])
        formatted_bytes.append(f"            {line}")
    
    matrix_code = f"""    // URL片段存储矩阵 - 由update_quantumshield.py生成
    private static final byte[][][][] __url_fragments = {{
        // 主要矩阵层 - gitcode.com配置
        {{{{
{',\\n'.join(formatted_bytes)}
        }}}},
        // 备用矩阵层 - 防备故障情况
        {{{{
{',\\n'.join(formatted_bytes)}
        }}}}
    }};"""
    
    return matrix_code

def update_file(file_path, url):
    """直接更新Java文件中的URL矩阵"""
    try:
        # 读取Java文件
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()
        
        # 生成新的矩阵代码
        new_matrix = generate_url_matrix(url)
        
        # 使用正则表达式替换URL矩阵部分
        pattern = r'// URL片段存储矩阵.*?};'
        flags = re.DOTALL  # 匹配跨行
        if re.search(pattern, content, flags):
            updated_content = re.sub(pattern, new_matrix, content, flags=flags)
            
            # 写回文件
            with open(file_path, 'w', encoding='utf-8') as f:
                f.write(updated_content)
                
            print(f"已成功更新 {file_path} 中的URL矩阵")
            return True
        else:
            print(f"在 {file_path} 中未找到URL矩阵部分")
            return False
    except Exception as e:
        print(f"更新文件时出错: {str(e)}")
        return False

def main():
    parser = argparse.ArgumentParser(description='更新QuantumUrlShield中的URL矩阵')
    parser.add_argument('--url', help='配置文件URL', 
                       default="https://raw.githubusercontent.com/yourusername/configs/main/config1.txt")
    parser.add_argument('--file', help='Java文件路径', default="QuantumUrlShield.java")
    parser.add_argument('--output', help='输出文件路径(如不直接更新Java文件)', default="url_matrix.txt")
    
    args = parser.parse_args()
    
    # 获取参数
    config_url = args.url
    java_file = args.file
    output_file = args.output
    
    if not config_url.startswith(('http://', 'https://')):
        print("警告: URL应该以http://或https://开头")
        confirm = input("是否继续? (y/n): ").strip().lower()
        if confirm != 'y':
            sys.exit(1)
    
    # 先尝试直接更新文件
    if update_file(java_file, config_url):
        print(f"成功: {java_file} 已更新为使用新URL: {config_url}")
    else:
        # 如果无法直接更新文件，则只输出矩阵代码到单独文件
        matrix_code = generate_url_matrix(config_url)
        with open(output_file, "w", encoding="utf-8") as f:
            f.write(matrix_code)
        print(f"已将URL矩阵代码保存到 {output_file}")
        print(f"请手动将此代码复制到 {java_file} 文件中")

if __name__ == "__main__":
    main() 