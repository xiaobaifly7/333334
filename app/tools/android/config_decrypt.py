#!/usr/bin/env python
# -*- coding: utf-8 -*-

"""
配置解密脚本 (config_decrypt.py)
用于解密由 config_encrypt.py 加密的配置文件
"""

import argparse
import base64
import sys
from Crypto.Cipher import AES
from Crypto.Util.Padding import unpad

# 默认密钥和IV (与 config_encrypt.py 保持一致)
DEFAULT_KEY = b'\x5A\x58\x6F\x31\x4D\x33\x55\x31\x4E\x47\x49\x32\x5A\x54\x4E\x6A' # 16字节密钥
DEFAULT_IV = b'\x59\x57\x4A\x70\x5A\x32\x46\x70\x62\x47\x56\x69\x61\x57\x34\x75' # 16字节IV

def decrypt_config(encrypted_data, key=DEFAULT_KEY, iv=DEFAULT_IV):
    """
    使用 AES-CBC 解密配置数据

    Args:
        encrypted_data: base64 编码的加密数据 (字符串或字节)
        key: 16字节的AES密钥
        iv: 16字节的初始化向量

    Returns:
        解密后的UTF-8配置字符串，如果失败返回 None
    """
    try:
        # Base64 解码
        if isinstance(encrypted_data, str):
            encrypted_data = encrypted_data.strip()
            encrypted_bytes = base64.b64decode(encrypted_data)
        else: # Assuming bytes
            encrypted_bytes = base64.b64decode(encrypted_data)

        # 创建AES解密器
        cipher = AES.new(key, AES.MODE_CBC, iv)

        # 解密
        decrypted_padded = cipher.decrypt(encrypted_bytes)

        # 移除填充
        decrypted_data = unpad(decrypted_padded, AES.block_size)

        # 返回UTF-8字符串
        return decrypted_data.decode('utf-8')
    except Exception as e:
        print(f"解密过程中发生错误: {str(e)}")
        return None

def main():
    parser = argparse.ArgumentParser(description='配置解密工具')
    parser.add_argument('-i', '--input', help='包含加密数据的输入文件路径')
    parser.add_argument('-k', '--key', help='自定义密钥 (32位十六进制字符串)', default=None)
    parser.add_argument('-v', '--iv', help='自定义IV (32位十六进制字符串)', default=None)
    parser.add_argument('-e', '--encrypted', help='直接提供加密数据字符串', default=None)

    args = parser.parse_args()

    # 使用默认密钥和IV
    key = DEFAULT_KEY
    iv = DEFAULT_IV

    # 如果提供了自定义密钥，尝试解析
    if args.key:
        try:
            key = bytes.fromhex(args.key)
            if len(key) != 16:
                print("错误: 密钥长度必须为16字节 (32位十六进制)")
                return
        except ValueError:
            print("错误: 密钥格式无效，请输入32位十六进制字符串")
            return

    # 如果提供了自定义IV，尝试解析
    if args.iv:
        try:
            iv = bytes.fromhex(args.iv)
            if len(iv) != 16:
                print("错误: IV长度必须为16字节 (32位十六进制)")
                return
        except ValueError:
            print("错误: IV格式无效，请输入32位十六进制字符串")
            return

    # 获取加密数据
    encrypted_data = args.encrypted

    # 如果未直接提供加密数据，则从文件或标准输入读取
    if not encrypted_data and args.input:
        try:
            with open(args.input, 'r', encoding='utf-8') as f: # 指定UTF-8编码读取
                encrypted_data = f.read().strip()
            print(f"已读取加密文件: {args.input}")
        except Exception as e:
            print(f"读取输入文件时出错: {str(e)}")
            return

    if not encrypted_data:
        # 从标准输入读取
        print("请输入Base64编码的加密数据 (输入完成后按 Ctrl+D 结束): ")
        try:
            encrypted_data = sys.stdin.read().strip()
        except Exception as e:
             print(f"读取标准输入时出错: {str(e)}")
             return

    if not encrypted_data:
        print("错误: 未提供加密数据")
        return

    # 解密数据
    decrypted_config = decrypt_config(encrypted_data, key, iv)

    if not decrypted_config:
        print("解密失败，请检查输入数据、密钥和IV是否正确")
        return

    # 打印结果
    print("\n解密结果: ")
    print("-" * 40)
    print(decrypted_config)
    print("-" * 40)

    # 检查格式标记 (示例)
    # 注意：原始标记是乱码，这里假设修复后的标记
    if '〈配置〉' in decrypted_config and '〈/配置〉' in decrypted_config:
        print("\n[✓] 解密后的配置包含预期标记")
    else:
        print("\n[!] 解密后的配置未包含预期标记，请检查")

if __name__ == "__main__":
    main()
