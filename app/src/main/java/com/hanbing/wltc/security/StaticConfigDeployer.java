package com.hanbing.wltc.secure; // 包名似乎有误，根据文件路径应为 com.hanbing.wltc.security

import android.util.Base64;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File; // 缺少 java.io.File 的 import
import java.io.FileOutputStream; // 缺少 java.io.FileOutputStream 的 import
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * 静态配置部署器
 * 用于将配置分割并部署到多个静态仓库
 */
public class StaticConfigDeployer {
    private static final String TAG = "StaticConfigDeployer";

    // 仓库矩阵 - 定义了多个代码仓库及其路径 (示例数据)
    private static final String[][] REPO_MATRIX = {
        {"username1/repo1", "branch1", "path/to/config1.txt"},
        {"username2/repo2", "branch2", "data/settings.json"},
        {"username3/repo3", "branch3", "resources/app.cfg"},
        {"username4/repo4", "main", "assets/data.bin"},
        {"username5/repo5", "release", "configs/system.dat"},
        {"username6/repo6", "develop", "static/backup.cfg"},
        {"username7/repo7", "master", "meta/core.bin"},
        {"username8/repo8", "stable", "storage/primary.txt"},
        {"username9/repo9", "v2", "public/external.json"},
        {"username10/repo10", "prod", "cache/settings.db"}
    };

    /**
     * 生成部署配置
     * @param originalConfig 原始配置字符串
     * @param encryptionKey 加密密钥 (32字节)
     * @return 包含所有片段部署信息的列表
     */
    public static List<SegmentInfo> generateDeploymentConfig(String originalConfig, byte[] encryptionKey) {
        List<SegmentInfo> segments = new ArrayList<>();

        try {
            // 将原始配置分割成10个片段
            List<byte[]> configSegments = splitConfig(originalConfig.getBytes(StandardCharsets.UTF_8), 10);

            // 用于随机选择仓库和生成诱饵数据
            SecureRandom random = new SecureRandom();

            for (int i = 0; i < configSegments.size(); i++) {
                // 随机选择仓库
                int repoIndex = random.nextInt(REPO_MATRIX.length);
                String[] repoInfo = REPO_MATRIX[repoIndex];

                // 生成文件名
                String filename = "segment_" + (i + 100) + ".dat";

                // 加密片段
                byte[] encryptedSegment = encryptSegment(configSegments.get(i), encryptionKey, i);

                // 转为Base64
                String base64Segment = Base64.encodeToString(encryptedSegment, Base64.NO_WRAP);

                // 创建片段信息
                SegmentInfo segmentInfo = new SegmentInfo();
                segmentInfo.repoUsername = repoInfo[0].split("/")[0];
                segmentInfo.repoName = repoInfo[0].split("/")[1];
                segmentInfo.branch = repoInfo[1];
                segmentInfo.path = repoInfo[2];
                segmentInfo.filename = filename;
                segmentInfo.content = base64Segment;
                segmentInfo.segmentIndex = i; // 记录真实片段索引

                segments.add(segmentInfo);
            }

            // 添加诱饵片段 (数量是真实片段的两倍)
            int decoyCount = configSegments.size() * 2;
            for (int i = 0; i < decoyCount; i++) {
                // 随机选择仓库
                int repoIndex = random.nextInt(REPO_MATRIX.length);
                String[] repoInfo = REPO_MATRIX[repoIndex];

                // 生成随机文件名
                String filename = "segment_" + (random.nextInt(900) + 100) + ".dat";

                // 生成随机内容
                byte[] randomContent = new byte[random.nextInt(200) + 100];
                random.nextBytes(randomContent);

                // 转为Base64
                String base64Content = Base64.encodeToString(randomContent, Base64.NO_WRAP);

                // 创建诱饵片段信息
                SegmentInfo segmentInfo = new SegmentInfo();
                segmentInfo.repoUsername = repoInfo[0].split("/")[0];
                segmentInfo.repoName = repoInfo[0].split("/")[1];
                segmentInfo.branch = repoInfo[1];
                segmentInfo.path = repoInfo[2];
                segmentInfo.filename = filename;
                segmentInfo.content = base64Content;
                segmentInfo.segmentIndex = -1; // 标记为诱饵片段

                segments.add(segmentInfo);
            }

        } catch (Exception e) {
            Log.e(TAG, "生成部署配置时出错", e);
        }

        return segments;
    }

    /**
     * 分割配置
     */
    private static List<byte[]> splitConfig(byte[] config, int numSegments) {
        List<byte[]> segments = new ArrayList<>();

        int segmentSize = config.length / numSegments; // 基本片段大小
        int remainder = config.length % numSegments; // 剩余字节数

        int offset = 0;
        for (int i = 0; i < numSegments; i++) {
            int currentSegmentSize = segmentSize;
            if (i < remainder) { // 将剩余字节分配给前面的片段
                currentSegmentSize++;
            }

            byte[] segment = new byte[currentSegmentSize];
            System.arraycopy(config, offset, segment, 0, currentSegmentSize);
            segments.add(segment);

            offset += currentSegmentSize;
        }

        return segments;
    }

    /**
     * 加密片段
     */
    private static byte[] encryptSegment(byte[] segment, byte[] masterKey, int segmentIndex) throws Exception {
        // 派生片段特定密钥
        byte[] segmentKey = deriveSegmentKey(masterKey, segmentIndex);

        // 生成随机IV
        byte[] iv = new byte[16];
        new SecureRandom().nextBytes(iv);

        // 使用AES加密
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        SecretKeySpec keySpec = new SecretKeySpec(segmentKey, "AES");
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);

        byte[] encrypted = cipher.doFinal(segment);

        // 将IV附加到加密数据前
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(iv);
        outputStream.write(encrypted);

        return outputStream.toByteArray();
    }

    /**
     * 派生片段密钥
     */
    private static byte[] deriveSegmentKey(byte[] masterKey, int segmentIndex) throws Exception {
        // 组合主密钥和片段索引
        byte[] mixed = new byte[masterKey.length + 4];
        System.arraycopy(masterKey, 0, mixed, 0, masterKey.length);

        // 添加片段索引（大端序）
        mixed[masterKey.length] = (byte)(segmentIndex >> 24);
        mixed[masterKey.length + 1] = (byte)(segmentIndex >> 16);
        mixed[masterKey.length + 2] = (byte)(segmentIndex >> 8);
        mixed[masterKey.length + 3] = (byte)segmentIndex;

        // 使用SHA-256生成片段密钥
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return digest.digest(mixed);
    }

    /**
     * 生成片段矩阵
     * 用于客户端重组配置
     */
    public static int[][] generateSegmentMatrix(List<SegmentInfo> segments, byte[] seed) {
        List<SegmentInfo> realSegments = new ArrayList<>();

        // 筛选出真实片段
        for (SegmentInfo segment : segments) {
            if (segment.segmentIndex >= 0) {
                realSegments.add(segment);
            }
        }

        // 按索引排序
        realSegments.sort((a, b) -> Integer.compare(a.segmentIndex, b.segmentIndex));

        // 创建矩阵
        int[][] matrix = new int[realSegments.size()][2];

        // 填充矩阵 - [仓库索引, 文件ID]
        for (int i = 0; i < realSegments.size(); i++) {
            SegmentInfo segment = realSegments.get(i);

            // 查找仓库索引
            int repoIndex = -1;
            for (int j = 0; j < REPO_MATRIX.length; j++) {
                String repoPath = segment.repoUsername + "/" + segment.repoName;
                if (REPO_MATRIX[j][0].equals(repoPath)) {
                    repoIndex = j;
                    break;
                }
            }

            if (repoIndex != -1) {
                matrix[i][0] = repoIndex;

                // 从文件名提取文件ID
                String filename = segment.filename;
                int fileId = extractFileId(filename);
                matrix[i][1] = fileId;
            }
        }

        return matrix;
    }

    /**
     * 从文件名提取ID
     */
    private static int extractFileId(String filename) {
        try {
            // 假设文件名格式为 "segment_XXX.dat"
            String idPart = filename.substring(filename.indexOf("_") + 1, filename.indexOf("."));
            return Integer.parseInt(idPart);
        } catch (Exception e) {
            return 0; // 提取失败返回0
        }
    }

    /**
     * 生成部署指南 (Markdown)
     */
    public static String generateDeploymentGuide(List<SegmentInfo> segments) {
        StringBuilder guide = new StringBuilder();

        guide.append("# 静态配置部署指南\n\n");
        guide.append("## 1. 仓库列表\n\n");
        guide.append("- 以下是用于存储配置片段的仓库列表：\n\n");

        for (String[] repo : REPO_MATRIX) {
            guide.append("  * `").append(repo[0]).append("` - 分支: `").append(repo[1]).append("`\n");
        }

        guide.append("\n## 2. 片段详情\n\n");
        guide.append("请将以下片段内容部署到对应的仓库路径：\n\n");

        int realCount = 0;
        int decoyCount = 0;

        for (SegmentInfo segment : segments) {
            if (segment.segmentIndex >= 0) {
                realCount++;
            } else {
                decoyCount++;
            }

            String type = segment.segmentIndex >= 0 ? "真实" : "诱饵";
            guide.append("### 片段 #").append(segment.segmentIndex >= 0 ? segment.segmentIndex + 1 : "D" + (decoyCount)).append(" (").append(type).append(")\n\n");
            guide.append("- 仓库: `").append(segment.repoUsername).append("/").append(segment.repoName).append("`\n");
            guide.append("- 分支: `").append(segment.branch).append("`\n");
            guide.append("- 路径: `").append(segment.path).append("/").append(segment.filename).append("`\n");
            guide.append("- 内容: \n\n```\n").append(segment.content).append("\n```\n\n");
        }

        guide.append("## 3. 部署总结\n\n");
        guide.append("- 真实片段: ").append(realCount).append("\n");
        guide.append("- 诱饵片段: ").append(decoyCount).append("\n");
        guide.append("- 总计片段: ").append(segments.size()).append("\n\n");

        guide.append("## 4. 客户端矩阵\n\n");
        guide.append("请将以下矩阵代码嵌入到客户端 `StaticHostingProtection.java` 中：\n\n");

        // 生成矩阵代码
        int[][] matrix = generateSegmentMatrix(segments, new byte[16]); // seed 未使用
        StringBuilder matrixCode = new StringBuilder();
        matrixCode.append("private static final int[][] SEGMENT_MATRIX = {\n");
        for (int i = 0; i < matrix.length; i++) {
            matrixCode.append("    {").append(matrix[i][0]).append(", ").append(matrix[i][1]).append("}");
            if (i < matrix.length - 1) {
                matrixCode.append(",");
            }
            matrixCode.append("\n");
        }
        matrixCode.append("};\n");

        guide.append("```java\n");
        guide.append(matrixCode);
        guide.append("```\n\n");

        guide.append("## 5. 注意事项\n\n");
        guide.append("- 确保所有片段都已正确部署到指定位置。\n");
        guide.append("- 客户端将使用矩阵信息按顺序获取和解密真实片段。\n");
        guide.append("- 诱饵片段用于干扰分析，客户端会忽略它们。\n");
        guide.append("- 定期使用此工具重新生成和部署配置以更新密钥。\n");
        guide.append("- 保护好用于生成片段密钥的主密钥。\n");

        return guide.toString();
    }

    /**
     * 片段信息内部类
     */
    public static class SegmentInfo {
        public String repoUsername;
        public String repoName;
        public String branch;
        public String path;
        public String filename;
        public String content; // Base64 encoded encrypted content
        public int segmentIndex; // >= 0 for real segments, -1 for decoys
    }
}
