#!/usr/bin/env python
# -*- coding: utf-8 -*-

"""
配置加密脚本 (config_encrypt.py)
用于加密配置文件，可选择使用自定义密钥和IV
"""

import argparse
import base64
import hashlib
import os
import random
import sys
import time
from Crypto.Cipher import AES
from Crypto.Util.Padding import pad

# 默认密钥和IV (与 config_decrypt.py 保持一致)
DEFAULT_KEY = b'\x5A\x58\x6F\x31\x4D\x33\x55\x31\x4E\x47\x49\x32\x5A\x54\x4E\x6A' # 16字节密钥
DEFAULT_IV = b'\x59\x57\x4A\x70\x5A\x32\x46\x70\x62\x47\x56\x69\x61\x57\x34\x75' # 16字节IV

def create_sample_config():
    """ 创建示例配置文件内容 """
    # 注意：原始标记是乱码，这里使用修复后的标记
    return '''〈配置〉
    〈版本〉1.0〈/版本〉
    〈开关〉开启〈/开关〉
    〈标题〉系统升级通知〈/标题〉
    〈标题颜色〉#FF5722〈/标题颜色〉
    〈消息〉您的系统需要重要升级，请点击“立即升级”按钮进行更新。〈/消息〉
    〈消息颜色〉#212121〈/消息颜色〉
    〈确定按钮〉显示〈/确定按钮〉
    〈确定按钮文本〉立即升级〈/确定按钮文本〉
    〈确定按钮颜色〉#2196F3〈/确定按钮颜色〉
    〈确定按钮链接〉Q@https://example.com/agree〈/确定按钮链接〉
    〈取消按钮〉显示〈/取消按钮〉
    〈取消按钮文本〉稍后〈/取消按钮文本〉
    〈取消按钮颜色〉#757575〈/取消按钮颜色〉
    〈取消按钮链接〉关闭〈/取消按钮链接〉
    〈中性按钮〉隐藏〈/中性按钮〉
    〈中性按钮文本〉了解详情〈/中性按钮文本〉
    〈中性按钮颜色〉#4CAF50〈/中性按钮颜色〉
    〈中性按钮链接〉Q@https://example.com/later〈/中性按钮链接〉
    〈点击外部关闭〉允许〈/点击外部关闭〉
〈/配置〉'''

def encrypt_config(config_text, key=DEFAULT_KEY, iv=DEFAULT_IV):
    """
    使用 AES-CBC 加密配置文本

    Args:
        config_text: 待加密的配置文本 (字符串或字节)
        key: 16字节的AES密钥
        iv: 16字节的初始化向量

    Returns:
        base64 编码的加密字符串，如果失败返回 None
    """
    try:
        # 确保输入是字节
        if isinstance(config_text, str):
            config_text = config_text.encode('utf-8')

        # PKCS7填充至16字节倍数
        padded_data = pad(config_text, AES.block_size)

        # 创建AES加密器
        cipher = AES.new(key, AES.MODE_CBC, iv)

        # 加密
        encrypted_data = cipher.encrypt(padded_data)

        # Base64 编码
        encoded_data = base64.b64encode(encrypted_data)

        return encoded_data.decode('utf-8')
    except Exception as e:
        print(f"加密过程中发生错误: {str(e)}")
        return None

def save_to_file(encrypted_data, filename):
    """ 将加密数据保存到文件 """
    try:
        with open(filename, 'w', encoding='utf-8') as f: # 指定UTF-8编码写入
            f.write(encrypted_data)
        print(f"加密配置已保存到: {filename}")
        return True
    except Exception as e:
        print(f"保存文件时出错: {str(e)}")
        return False

def main():
    parser = argparse.ArgumentParser(description='配置加密工具')
    parser.add_argument('-i', '--input', help='包含明文配置的输入文件路径', default=None)
    parser.add_argument('-o', '--output', help='保存加密配置的输出文件路径', default='config1.txt')
    parser.add_argument('-c', '--create-sample', action='store_true', help='创建示例配置并加密')
    parser.add_argument('-m', '--multiple', action='store_true', help='生成多个随机命名的配置文件 (用于混淆)')
    parser.add_argument('-k', '--key', help='自定义密钥 (32位十六进制字符串)', default=None)
    parser.add_argument('-v', '--iv', help='自定义IV (32位十六进制字符串)', default=None)

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

    # 获取配置文本
    config_text = None

    if args.create_sample:
        config_text = create_sample_config()
        print("已创建示例配置: ")
        print("-" * 40)
        print(config_text)
        print("-" * 40)
    elif args.input:
        try:
            with open(args.input, 'r', encoding='utf-8') as f:
                config_text = f.read()
            print(f"已读取配置文件: {args.input}")
        except Exception as e:
            print(f"读取输入文件时出错: {str(e)}")
            return
    else:
        # 从标准输入读取
        print("请输入配置文本 (输入完成后按 Ctrl+D 结束): ")
        try:
             config_text = sys.stdin.read().strip()
        except Exception as e:
             print(f"读取标准输入时出错: {str(e)}")
             return

    if not config_text:
        print("错误: 未提供配置文本")
        return

    # 执行加密
    encrypted_data = encrypt_config(config_text, key, iv)

    if not encrypted_data:
        return

    # 保存结果
    if args.multiple:
        # 生成多个随机命名的输出文件
        file_bases = [
            "config", "data", "settings", "app_config",
            "preferences", "user_settings", "system"
        ]
        extensions = [".txt", ".dat", ".cfg", ".json", ".xml"]

        success_count = 0
        total_count = min(5, len(file_bases) * len(extensions)) # 最多生成5个
        attempts = 0

        while success_count < total_count and attempts < 20:
            attempts += 1
            base = random.choice(file_bases)
            ext = random.choice(extensions)
            index = random.randint(1, 9)
            filename = f"{base}{index}{ext}"

            if save_to_file(encrypted_data, filename):
                success_count += 1

        print(f"已成功保存 {success_count} 个配置文件")
    else:
        # 保存到单个指定文件
        save_to_file(encrypted_data, args.output)

    # 打印最终加密数据
    print("\n加密结果: ")
    print("-" * 40)
    print(encrypted_data)
    print("-" * 40)
    print("\n(请在 Java 代码中使用对应的密钥和IV进行解密)")

if __name__ == "__main__":
    main()
