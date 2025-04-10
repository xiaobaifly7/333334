#!/usr/bin/env python
# -*- coding: utf-8 -*-

"""
URL 加密工具 (url_encrypt.py)
用于将 URL 加密并编码成一个二维矩阵，以便嵌入到 Java 代码中 (例如 QuantumUrlShield.java)
"""

import argparse
import base64
import random
import sys
import hashlib
import json
import textwrap
import os
import string
import time
from Crypto.Cipher import AES
from Crypto.Util.Padding import pad, unpad

class AdvancedUrlEncryptor:
    """ 高级URL加密器 - 包含多种混淆和加密方法 """

    def __init__(self, matrix_size=3, fragment_count=4):
        """ 初始化加密器参数

        Args:
            matrix_size: 矩阵维度 (n x n), 默认: 3
            fragment_count: URL分割的片段数量, 默认: 4
        """
        self.matrix_size = matrix_size
        self.fragment_count = fragment_count
        self.encryption_methods = [
            self._method_aes_base64,
            self._method_aes_hex,
            self._method_custom_xor,
            self._method_base64_reverse,
            self._method_obfuscate_chars
        ]

    def encrypt_url_matrix(self, url, output_file=None, add_decoys=True):
        """ 将URL加密成二维矩阵

        Args:
            url: 需要加密的URL
            output_file: 输出Java代码的文件路径 (可选)
            add_decoys: 是否添加诱饵URL片段

        Returns:
            加密后的URL矩阵的Java数组声明字符串
        """
        # 确保URL以协议开头
        if not url.startswith(('http://', 'https://')):
            url = 'https://' + url

        # 生成随机的主密钥和IV
        master_key = self._generate_random_string(32)
        master_iv = self._generate_random_string(16)

        # 创建矩阵
        url_matrix = []
        url_fragments = self._split_url(url)

        # 填充矩阵
        for i in range(self.matrix_size):
            row = []
            for j in range(self.matrix_size):
                if i == 0 and j == 0:
                    # 左上角存储元数据
                    metadata = {
                        "v": "2.0", # 版本
                        "t": int(time.time()), # 时间戳
                        "f": self.fragment_count, # 片段数
                        "c": self._checksum(url) # URL校验和
                    }
                    row.append(self._encrypt_with_key_iv(
                        json.dumps(metadata),
                        self._derive_key(master_key, i, j),
                        self._derive_iv(master_iv, i, j),
                        method_index=0 # 使用AES+Base64
                    ))
                elif i == self.matrix_size - 1 and j == self.matrix_size - 1:
                    # 右下角存储验证信息
                    verification = {
                        "h": hashlib.sha256(url.encode()).hexdigest()[:16], # URL哈希前16位
                        "v": "2.0" # 版本
                    }
                    row.append(self._encrypt_with_key_iv(
                        json.dumps(verification),
                        self._derive_key(master_key, i, j),
                        self._derive_iv(master_iv, i, j),
                        method_index=1 # 使用AES+Hex
                    ))
                elif i * self.matrix_size + j - 1 < len(url_fragments):
                    # 存储URL片段
                    fragment_index = i * self.matrix_size + j - 1
                    fragment = url_fragments[fragment_index]

                    # 随机选择一种加密方法
                    method_index = random.randint(0, len(self.encryption_methods) - 1)

                    # 加密片段
                    row.append(self._encrypt_with_key_iv(
                        fragment,
                        self._derive_key(master_key, i, j),
                        self._derive_iv(master_iv, i, j),
                        method_index
                    ))
                else:
                    # 填充诱饵数据
                    if add_decoys:
                        decoy_url = self._generate_decoy_url_fragment()
                        method_index = random.randint(0, len(self.encryption_methods) - 1)
                        row.append(self._encrypt_with_key_iv(
                            decoy_url,
                            self._derive_key(master_key, i, j),
                            self._derive_iv(master_iv, i, j),
                            method_index
                        ))
                    else:
                        row.append("") # 不添加诱饵则填充空字符串
            url_matrix.append(row)

        # 生成混淆的密钥信息
        key_info = {
            "key": [
                self._obfuscate_string(master_key[:10]),
                self._obfuscate_string(master_key[10:20]),
                self._obfuscate_string(master_key[20:])
            ],
            "iv": [
                self._obfuscate_string(master_iv[:5]),
                self._obfuscate_string(master_iv[5:10]),
                self._obfuscate_string(master_iv[10:])
            ],
            "indices": self._generate_fragment_indices() # 生成片段索引（包含虚假索引）
        }

        # 组合结果
        result = {
            "matrix": url_matrix,
            "key_info": key_info,
            "timestamp": int(time.time()),
            "version": "2.0"
        }

        # 将结果转换为Java数组声明
        java_matrix = self._convert_to_java_array(result)

        # 保存到文件
        if output_file:
            try:
                with open(output_file, 'w', encoding='utf-8') as f:
                    f.write(java_matrix)
                print(f"URL矩阵已保存到: {output_file}")
            except Exception as e:
                print(f"保存文件 {output_file} 时出错: {e}")


        return java_matrix

    def _split_url(self, url):
        """ 将URL分割成指定数量的片段

        Args:
            url: 需要分割的URL

        Returns:
            URL片段列表
        """
        # 如果片段数为1或更少，直接返回整个URL
        if self.fragment_count <= 1:
            return [url]

        # 随机选择分割策略
        strategies = [
            self._split_by_characters,
            self._split_by_segments,
            self._split_by_mixed
        ]
        strategy = random.choice(strategies)
        fragments = strategy(url, self.fragment_count)

        # 确保片段数量正确，不足则补空字符串
        while len(fragments) < self.fragment_count:
            fragments.append("")

        return fragments[:self.fragment_count]

    def _split_by_characters(self, url, count):
        """ 按字符数大致均等地分割

        Args:
            url: 需要分割的URL
            count: 分割的片段数量

        Returns:
             分割后的URL片段列表
        """
        url_len = len(url)
        chunk_size = url_len // count

        if chunk_size == 0:
            return [url] + [""] * (count - 1)

        chunks = []
        for i in range(0, url_len, chunk_size):
            if len(chunks) < count - 1:
                chunks.append(url[i:i + chunk_size])
            else:
                chunks.append(url[i:]) # 最后一个片段包含所有剩余字符
                break

        return chunks

    def _split_by_segments(self, url, count):
        """ 按URL的语义部分分割 (协议, 域名, 路径, 查询参数)

        Args:
            url: 需要分割的URL
            count: 分割的片段数量

        Returns:
             分割后的URL片段列表
        """
        # 解析URL
        protocol_end = url.find("://")
        protocol = url[:protocol_end + 3] if protocol_end > 0 else ""

        rest = url[len(protocol):]
        path_start = rest.find("/")

        if path_start < 0:
            # 只有域名部分
            domain = rest
            path = ""
        else:
            domain = rest[:path_start]
            path = rest[path_start:]

        # 根据片段数量选择分割策略
        if count == 2:
            return [protocol + domain, path] if path else [protocol, domain]
        elif count == 3:
            return [protocol, domain, path] if path else [protocol, domain, ""]
        elif count == 4:
            # 尝试分割域名和路径
            if "." in domain:
                domain_parts = domain.split(".", 1)
                if path:
                    query_start = path.find("?")
                    if query_start > 0:
                        return [protocol, domain_parts[0], domain_parts[1], path[:query_start], path[query_start:]][:count]
                    else:
                        return [protocol, domain_parts[0], domain_parts[1], path]
                else:
                    return [protocol, domain_parts[0], domain_parts[1], ""]
            else:
                # 域名无法分割，尝试分割路径
                if path:
                    query_start = path.find("?")
                    if query_start > 0:
                        return [protocol, domain, path[:query_start], path[query_start:]]
                    else:
                        half = len(path) // 2
                        return [protocol, domain, path[:half], path[half:]]
                else:
                    return [protocol, domain, "", ""]
        else:
            # 默认回退到按字符分割
            return self._split_by_characters(url, count)

    def _split_by_mixed(self, url, count):
        """ 混合策略：先按语义分割，再按字符分割

        Args:
            url: 需要分割的URL
            count: 分割的片段数量

        Returns:
             分割后的URL片段列表
        """
        # 先按语义分割
        semantic_parts = []

        # 提取协议
        protocol_end = url.find("://")
        if protocol_end > 0:
            semantic_parts.append(url[:protocol_end + 3])
            url = url[protocol_end + 3:]

        # 提取域名
        path_start = url.find("/")
        if path_start >= 0: # Changed > 0 to >= 0 to handle cases like "domain.com/"
            semantic_parts.append(url[:path_start])
            url = url[path_start:]
        else:
            semantic_parts.append(url)
            url = ""

        # 提取路径和查询参数
        if url:
            query_start = url.find("?")
            if query_start >= 0: # Changed > 0 to >= 0
                semantic_parts.append(url[:query_start])
                semantic_parts.append(url[query_start:])
            else:
                semantic_parts.append(url)

        # 如果语义部分数量正好，直接返回
        if len(semantic_parts) == count:
            return semantic_parts

        # 如果语义部分太少， 递归分割最长的部分
        if len(semantic_parts) < count:
            while len(semantic_parts) < count:
                # 找到最长的部分进行分割
                longest_index, longest_part = max(enumerate(semantic_parts), key=lambda x: len(x[1]))
                if len(longest_part) <= 1:
                    # 无法再分割，补空字符串
                    semantic_parts.append("")
                else:
                    # 从中间分割
                    mid = len(longest_part) // 2
                    semantic_parts[longest_index] = longest_part[:mid]
                    semantic_parts.insert(longest_index + 1, longest_part[mid:])

        # 如果语义部分太多， 合并最短的相邻部分
        if len(semantic_parts) > count:
            while len(semantic_parts) > count:
                min_length = float('inf')
                min_index = 0

                for i in range(len(semantic_parts) - 1):
                    combined_length = len(semantic_parts[i]) + len(semantic_parts[i + 1])
                    if combined_length < min_length:
                        min_length = combined_length
                        min_index = i

                # 合并
                semantic_parts[min_index] = semantic_parts[min_index] + semantic_parts[min_index + 1]
                semantic_parts.pop(min_index + 1)

        return semantic_parts

    def _encrypt_with_key_iv(self, text, key, iv, method_index):
        """ 使用指定的密钥、IV和方法加密文本

        Args:
            text: 待加密的文本
            key: 加密密钥
            iv: 初始化向量
            method_index: 加密方法索引

        Returns:
            加密后的文本
        """
        if not text:
            return ""

        # 使用指定的加密方法
        try:
            return self.encryption_methods[method_index](text, key, iv)
        except IndexError:
             print(f"警告: 无效的加密方法索引 {method_index}, 使用默认方法")
             return self._fallback_encrypt(text)


    def _method_aes_base64(self, text, key, iv):
        """ AES 加密并 Base64 编码

        Args:
            text: 待加密的文本
            key: 加密密钥
            iv: 初始化向量

        Returns:
            加密后的文本
        """
        try:
            key_bytes = key.encode('utf-8')[:16] # Ensure 16 bytes for AES-128 key
            iv_bytes = iv.encode('utf-8')[:16]   # Ensure 16 bytes for IV
            cipher = AES.new(key_bytes, AES.MODE_CBC, iv_bytes)
            ct_bytes = cipher.encrypt(pad(text.encode('utf-8'), AES.block_size))
            return base64.b64encode(ct_bytes).decode('utf-8')
        except Exception as e:
            print(f"加密失败 (AES-Base64): {e}")
            return self._fallback_encrypt(text)

    def _method_aes_hex(self, text, key, iv):
        """ AES 加密并十六进制编码

        Args:
            text: 待加密的文本
            key: 加密密钥
            iv: 初始化向量

        Returns:
            加密后的文本
        """
        try:
            key_bytes = key.encode('utf-8')[:16]
            iv_bytes = iv.encode('utf-8')[:16]
            cipher = AES.new(key_bytes, AES.MODE_CBC, iv_bytes)
            ct_bytes = cipher.encrypt(pad(text.encode('utf-8'), AES.block_size))
            return ct_bytes.hex()
        except Exception as e:
            print(f"加密失败 (AES-Hex): {e}")
            return self._fallback_encrypt(text)

    def _method_custom_xor(self, text, key, iv):
        """ 自定义XOR加密

        Args:
            text: 待加密的文本
            key: 加密密钥
            iv: 初始化向量

        Returns:
            加密后的文本
        """
        try:
            key_iv = (key + iv).encode('utf-8')
            text_bytes = text.encode('utf-8')
            xored = []

            for i, char_byte in enumerate(text_bytes): # Changed 'char' to 'char_byte'
                key_char = key_iv[i % len(key_iv)]
                xored_char = char_byte ^ key_char
                xored.append(xored_char)

            return base64.b64encode(bytes(xored)).decode('utf-8')
        except Exception as e:
            print(f"加密失败 (Custom XOR): {e}")
            return self._fallback_encrypt(text)

    def _method_base64_reverse(self, text, key, iv):
        """ Base64 编码后反转，并添加随机前后缀

        Args:
            text: 待加密的文本
            key: 加密密钥 (未使用)
            iv: 初始化向量 (未使用)

        Returns:
            加密后的文本
        """
        try:
            # 使用密钥的第一个字符确定偏移量
            offset = ord(key[0]) % 10 if key else 5 # Default offset if key is empty

            # 生成随机的前后缀
            prefix = self._generate_random_string(offset)
            suffix = self._generate_random_string(offset)

            # Base64 编码
            encoded = base64.b64encode(text.encode('utf-8')).decode('utf-8')

            # 反转字符串
            reversed_text = encoded[::-1]

            # 添加前后缀
            result = f"{prefix}{reversed_text}{suffix}"

            return result
        except Exception as e:
            print(f"加密失败 (Base64-Reverse): {e}")
            return self._fallback_encrypt(text)

    def _method_obfuscate_chars(self, text, key, iv):
        """ 字符混淆加密

        Args:
            text: 待加密的文本
            key: 加密密钥
            iv: 初始化向量

        Returns:
            加密后的文本
        """
        try:
            # 生成偏移量映射
            key_sum = sum(ord(c) for c in key[:5]) if key else 1 # Default sum if key is empty
            offset_map = [(i * key_sum + ord(c)) % 95 + 32 for i, c in enumerate(text)] # Ensure offset is printable ASCII

            # 进行偏移
            obfuscated = []
            for i, char in enumerate(text):
                char_code = ord(char)
                shifted = (char_code + offset_map[i]) % 127
                if shifted < 32: # 确保是可打印字符
                    shifted += 32
                obfuscated.append(chr(shifted))

            # 编码结果
            result = ''.join(obfuscated)
            return base64.b64encode(result.encode('utf-8')).decode('utf-8')
        except Exception as e:
            print(f"加密失败 (Char Obfuscation): {e}")
            return self._fallback_encrypt(text)

    def _fallback_encrypt(self, text):
        """ 备用加密方法：简单的 Base64 编码

        Args:
            text: 待加密的文本

        Returns:
            加密后的文本
        """
        # 仅使用 Base64 编码
        try:
            return base64.b64encode(text.encode('utf-8')).decode('utf-8')
        except Exception:
            # 最坏情况，返回原始文本
            return text

    def _derive_key(self, master_key, i, j):
        """ 从主密钥派生子密钥

        Args:
            master_key: 主密钥
            i: 矩阵行索引
            j: 矩阵列索引

        Returns:
            派生出的子密钥 (32位十六进制)
        """
        # 确保 master_key 长度足够
        if len(master_key) < 32:
            master_key = master_key.ljust(32, '0')

        # 使用主密钥片段和行列索引生成种子
        seed = f"{master_key[i:i+8]}{j}{master_key[j:j+8]}{i}"

        # 计算哈希
        hash_obj = hashlib.sha256(seed.encode())
        derived = hash_obj.digest()

        # 返回十六进制字符串
        return derived.hex()[:32] # 取前32位十六进制字符 (16字节)

    def _derive_iv(self, master_iv, i, j):
        """ 从主IV派生子IV

        Args:
            master_iv: 主IV
            i: 矩阵行索引
            j: 矩阵列索引

        Returns:
            派生出的子IV (16位十六进制)
        """
        # 确保 master_iv 长度足够
        if len(master_iv) < 16:
            master_iv = master_iv.ljust(16, '0')

        # 组合行列索引和IV片段
        seed = f"{i}{master_iv[:8]}{j}{master_iv[8:]}"

        # 计算哈希 (使用 MD5 以获得 16 字节输出)
        hash_obj = hashlib.md5(seed.encode())
        derived = hash_obj.digest()

        # 返回十六进制字符串
        return derived.hex()[:16] # 取前16位十六进制字符 (8字节) - 注意：AES IV通常需要16字节

    def _obfuscate_string(self, s):
        """ 混淆字符串 (通过打乱字符顺序)

        Args:
            s: 待混淆的字符串

        Returns:
            包含混淆后字符串和原始顺序映射的字典
        """
        if not s:
            return {"s": "", "m": []}

        # 随机打乱字符索引映射
        chars = list(s)
        n = len(chars)
        swap_map = list(range(n))
        random.shuffle(swap_map)

        # 根据映射重排字符
        obfuscated = [''] * n
        for i, pos in enumerate(swap_map):
            obfuscated[pos] = chars[i]

        # 返回混淆后字符串和映射
        return {
            "s": ''.join(obfuscated),
            "m": swap_map
        }

    def _generate_random_string(self, length):
        """ 生成随机字符串

        Args:
            length: 字符串长度

        Returns:
            随机字符串
        """
        chars = string.ascii_letters + string.digits
        return ''.join(random.choice(chars) for _ in range(length))

    def _generate_decoy_url_fragment(self):
        """ 生成诱饵URL片段

        Args:
            无

        Returns:
            随机生成的诱饵URL片段
        """
        # 常见的URL片段模式
        patterns = [
            "https://", "www.", ".com", ".cn", ".org", ".net",
            "/api/", "/static/", "/index", "/page/", "?id=", "&token=",
            "/v2/", "/data/",
        ]

        # 随机选择一个模式并添加随机字符串
        pattern = random.choice(patterns)
        random_part = ''.join(random.choice(string.ascii_lowercase + string.digits)
                              for _ in range(random.randint(3, 10)))

        if pattern in [".com", ".cn", ".org", ".net"]:
            return random_part + pattern
        elif pattern in ["https://", "www."]:
            return pattern + random_part
        else:
            return pattern + random_part

    def _checksum(self, text):
        """ 计算文本的校验和 (MD5前8位)

        Args:
            text: 需要计算校验和的文本

        Returns:
            文本的校验和 (十六进制)
        """
        return hashlib.md5(text.encode()).hexdigest()[:8]

    def _generate_fragment_indices(self):
        """ 生成片段索引列表 (包含虚假索引)

        Args:
            无

        Returns:
            随机生成的片段索引列表
        """
        # 创建基础索引列表
        indices = []
        base_indices = []

        # 创建所有可用索引 (排除元数据和验证信息位置)
        for i in range(self.matrix_size):
            for j in range(self.matrix_size):
                if i == 0 and j == 0:
                    continue # 跳过元数据位置
                if i == self.matrix_size - 1 and j == self.matrix_size - 1:
                    continue # 跳过验证信息位置
                base_indices.append([i, j])

        # 取所需数量的真实片段索引
        base_indices = base_indices[:self.fragment_count]

        # 为每个真实片段生成包含1-2个虚假索引的列表
        for i in range(self.fragment_count):
            fragment_indices = [base_indices[i]] # 包含真实索引

            # 添加1-2个虚假位置
            for _ in range(random.randint(1, 2)):
                fake_i, fake_j = random.randint(0, self.matrix_size - 1), random.randint(0, self.matrix_size - 1)
                # 确保不与现有索引或特殊位置重复
                while [fake_i, fake_j] in fragment_indices or \
                      (fake_i == 0 and fake_j == 0) or \
                      (fake_i == self.matrix_size - 1 and fake_j == self.matrix_size - 1):
                    fake_i, fake_j = random.randint(0, self.matrix_size - 1), random.randint(0, self.matrix_size - 1)
                fragment_indices.append([fake_i, fake_j])

            # 打乱当前片段的索引顺序
            random.shuffle(fragment_indices)
            indices.append(fragment_indices)

        return indices

    def _convert_to_java_array(self, data):
        """ 将加密结果转换为Java数组声明字符串

        Args:
            data: 包含加密结果的字典

        Returns:
            Java数组声明字符串
        """
        matrix = data["matrix"]
        key_info = data["key_info"]

        # 构建Java代码字符串
        java_code = [
            "// 自动生成的URL矩阵 - 请勿手动修改",
            f"// 生成时间: {time.strftime('%Y-%m-%d %H:%M:%S')}",
            f"// 矩阵维度: {self.matrix_size}x{self.matrix_size}, 片段数量: {self.fragment_count}",
            "",
            "// URL 矩阵",
            f"private static final String[][] URL_MATRIX = new String[][] {{"
        ]

        # 添加矩阵内容
        for i, row in enumerate(matrix):
            line = "        { "
            for j, cell in enumerate(row):
                # Escape backslashes and quotes for Java string literal
                escaped_cell = cell.replace('\\', '\\\\').replace('"', '\\"')
                line += f"\"{escaped_cell}\""
                if j < len(row) - 1:
                    line += ", "
            line += " }"
            if i < len(matrix) - 1:
                line += ","
            java_code.append(line)

        java_code.append("};")
        java_code.append("")

        # 添加密钥信息
        java_code.append("// 密钥信息 (混淆)")
        java_code.append("private static final Object[][] KEY_PARTS = new Object[][] {") # Use Object for mixed types

        # 添加密钥片段
        key_parts = key_info["key"]
        for i, part in enumerate(key_parts):
            # Format: { {"obfuscated_string"}, {map_indices...} }
            map_str = ", ".join(map(str, part["m"]))
            line = f"        {{ \"{part['s'].replace('\"','\\\\\"')}\", new int[]{{{map_str}}} }}"
            if i < len(key_parts) - 1:
                line += ","
            java_code.append(line)

        java_code.append("};")
        java_code.append("")

        # 添加IV信息
        java_code.append("// IV 信息 (混淆)")
        java_code.append("private static final Object[][] IV_PARTS = new Object[][] {") # Use Object for mixed types

        # 添加IV片段
        iv_parts = key_info["iv"]
        for i, part in enumerate(iv_parts):
            map_str = ", ".join(map(str, part["m"]))
            line = f"        {{ \"{part['s'].replace('\"','\\\\\"')}\", new int[]{{{map_str}}} }}"
            if i < len(iv_parts) - 1:
                line += ","
            java_code.append(line)

        java_code.append("};")
        java_code.append("")

        # 添加片段索引
        java_code.append("// URL 片段索引 - (用于按顺序重组URL)")
        java_code.append("private static final int[][][] URL_FRAGMENT_INDICES = new int[][][] {")

        # 添加索引内容
        indices = key_info["indices"]
        for i, fragment_indices in enumerate(indices):
            line = "        { "
            for j, pos in enumerate(fragment_indices):
                line += f"{{ {pos[0]}, {pos[1]} }}"
                if j < len(fragment_indices) - 1:
                    line += ", "
            line += " }"
            if i < len(indices) - 1:
                line += ","
            java_code.append(line)

        java_code.append("};")

        return "\n".join(java_code)


def main():
    """ 主函数 - 解析命令行参数并执行URL加密 """
    parser = argparse.ArgumentParser(description="URL 矩阵加密工具 2.0")
    parser.add_argument("url", help="需要加密的URL")
    parser.add_argument("-o", "--output", help="保存Java代码的输出文件")
    parser.add_argument("-m", "--matrix-size", type=int, default=3, help="矩阵维度 (默认: 3)")
    parser.add_argument("-f", "--fragments", type=int, default=4, help="URL片段数量 (默认: 4)")
    parser.add_argument("--no-decoys", action="store_true", help="不添加诱饵URL片段")

    args = parser.parse_args()

    try:
        encryptor = AdvancedUrlEncryptor(matrix_size=args.matrix_size, fragment_count=args.fragments)
        java_code = encryptor.encrypt_url_matrix(args.url, args.output, not args.no_decoys)

        if not args.output:
            print(java_code) # 如果未指定输出文件，则打印到控制台
        # else: # Redundant print, already handled in encrypt_url_matrix
        #     print(f"URL已加密并保存到: {args.output}")

    except Exception as e:
        print(f"错误: {e}")
        return 1

    return 0


if __name__ == "__main__":
    sys.exit(main())
