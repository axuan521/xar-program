package com.ruoyi.web.shop.pay;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * 微信支付 V3 签名/验签/解密工具
 * <p>
 * 仅使用 JDK 原生 API（SHA256withRSA + AES-256-GCM），不引入第三方 SDK，
 * 以控制依赖体积与 Metaspace 占用。
 * <p>
 * <b>签名串拼接规则极易出错，务必严格按官方顺序</b>：
 * <ul>
 * <li>请求签名：{@code HTTP方法\n URL路径\n 时间戳\n 随机串\n 请求报文主体\n}（共 5 段，末尾也有换行）</li>
 * <li>回调验签：{@code 时间戳\n 随机串\n 回调报文主体\n}（共 3 段）</li>
 * <li>调起支付：{@code appId\n 时间戳\n 随机串\n prepay_id=预支付ID\n}</li>
 * </ul>
 * 待签名的 JSON body 必须与实际发送的字节完全一致，不能有空格或换行差异。
 *
 * @author ruoyi
 */
public final class WxPayCryptoUtils
{
    /** GCM 认证标签长度（位） */
    private static final int GCM_TAG_LENGTH_BIT = 128;

    /** PEM 头尾标记正则 */
    private static final String PEM_BOUNDARY = "-----[^-]+-----";

    private WxPayCryptoUtils()
    {
    }

    /**
     * 加载商户私钥（apiclient_key.pem，PKCS#8 格式）
     */
    public static PrivateKey loadPrivateKey(String path) throws Exception
    {
        byte[] der = readPemBody(path);
        return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(der));
    }

    /**
     * 加载微信支付公钥，用于回调验签。
     * <p>
     * 兼容两种文件：微信支付公钥（-----BEGIN PUBLIC KEY-----）
     * 与平台证书（-----BEGIN CERTIFICATE-----）。推荐用公钥模式，
     * 因平台证书会定期轮换，而微信支付公钥无过期时间。
     */
    public static PublicKey loadPublicKey(String path) throws Exception
    {
        String content = new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
        if (content.contains("BEGIN CERTIFICATE"))
        {
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            X509Certificate certificate = (X509Certificate) factory
                    .generateCertificate(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
            return certificate.getPublicKey();
        }
        byte[] der = readPemBody(path);
        return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(der));
    }

    /**
     * 读取 PEM 文件并去掉头尾标记与全部空白，返回 DER 字节
     */
    private static byte[] readPemBody(String path) throws Exception
    {
        String content = new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
        String base64 = content.replaceAll(PEM_BOUNDARY, "").replaceAll("\\s", "");
        if (base64.isEmpty())
        {
            throw new IllegalStateException("PEM 文件内容为空或格式不正确: " + path);
        }
        return Base64.getDecoder().decode(base64);
    }

    /**
     * SHA256withRSA 签名，返回 Base64 字符串
     */
    public static String sign(String message, PrivateKey privateKey) throws Exception
    {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);
        signature.update(message.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(signature.sign());
    }

    /**
     * SHA256withRSA 验签（用于校验微信回调是否真的来自微信）
     *
     * @param message   待验签串（时间戳\n随机串\n报文主体\n）
     * @param signature Base64 签名值，取自 Wechatpay-Signature 请求头
     */
    public static boolean verify(String message, String signature, PublicKey publicKey)
    {
        if (publicKey == null || signature == null || signature.isEmpty())
        {
            return false;
        }
        try
        {
            Signature verifier = Signature.getInstance("SHA256withRSA");
            verifier.initVerify(publicKey);
            verifier.update(message.getBytes(StandardCharsets.UTF_8));
            return verifier.verify(Base64.getDecoder().decode(signature));
        }
        catch (Exception e)
        {
            return false;
        }
    }

    /**
     * 解密回调报文（AES-256-GCM）
     * <p>
     * 微信把支付结果加密在 resource.ciphertext 中，密钥为商户平台的 APIv3 密钥。
     * GCM 的认证标签附在密文末尾，Java 的 Cipher 会自动处理。
     *
     * @param apiV3Key       APIv3 密钥（32 字节）
     * @param nonce          resource.nonce
     * @param associatedData resource.associated_data，可能为空
     * @param ciphertext     resource.ciphertext，Base64 编码
     * @return 解密后的 JSON 明文
     */
    public static String decryptAesGcm(String apiV3Key, String nonce, String associatedData, String ciphertext)
            throws Exception
    {
        if (apiV3Key == null || apiV3Key.getBytes(StandardCharsets.UTF_8).length != 32)
        {
            throw new IllegalArgumentException("APIv3 密钥长度必须为 32 位");
        }
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        SecretKeySpec key = new SecretKeySpec(apiV3Key.getBytes(StandardCharsets.UTF_8), "AES");
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH_BIT, nonce.getBytes(StandardCharsets.UTF_8));
        cipher.init(Cipher.DECRYPT_MODE, key, spec);
        if (associatedData != null && !associatedData.isEmpty())
        {
            cipher.updateAAD(associatedData.getBytes(StandardCharsets.UTF_8));
        }
        byte[] plain = cipher.doFinal(Base64.getDecoder().decode(ciphertext));
        return new String(plain, StandardCharsets.UTF_8);
    }

    /**
     * 组装 V3 请求的 Authorization 头
     * <p>
     * 格式（单行，字段顺序无强制要求但不可缺项）：
     * {@code WECHATPAY2-SHA256-RSA2048 mchid="..",nonce_str="..",signature="..",timestamp="..",serial_no=".."}
     *
     * @param method      HTTP 方法，大写
     * @param urlPath     请求路径（含 query，不含域名），如 /v3/pay/transactions/jsapi
     * @param body        请求报文主体，GET 请求传空串
     * @param mchid       商户号
     * @param serialNo    商户证书序列号
     * @param privateKey  商户私钥
     */
    public static String buildAuthorization(String method, String urlPath, String body,
            String mchid, String serialNo, PrivateKey privateKey) throws Exception
    {
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        String nonceStr = randomNonce();
        // 严格按官方顺序拼接：方法、路径、时间戳、随机串、报文主体，每段后各一个换行
        String message = method + "\n" + urlPath + "\n" + timestamp + "\n" + nonceStr + "\n"
                + (body == null ? "" : body) + "\n";
        String signature = sign(message, privateKey);
        return "WECHATPAY2-SHA256-RSA2048 "
                + "mchid=\"" + mchid + "\","
                + "nonce_str=\"" + nonceStr + "\","
                + "signature=\"" + signature + "\","
                + "timestamp=\"" + timestamp + "\","
                + "serial_no=\"" + serialNo + "\"";
    }

    /**
     * 生成小程序 wx.requestPayment 所需签名
     * <p>
     * 待签串为 {@code appId\n时间戳\n随机串\nprepay_id=预支付ID\n}，注意第 4 段带 prepay_id= 前缀。
     *
     * @return 长度为 5 的数组：{timeStamp, nonceStr, package, signType, paySign}
     */
    public static String[] buildRequestPaymentSign(String appId, String prepayId, PrivateKey privateKey)
            throws Exception
    {
        String timeStamp = String.valueOf(System.currentTimeMillis() / 1000);
        String nonceStr = randomNonce();
        String packageStr = "prepay_id=" + prepayId;
        String message = appId + "\n" + timeStamp + "\n" + nonceStr + "\n" + packageStr + "\n";
        String paySign = sign(message, privateKey);
        return new String[] { timeStamp, nonceStr, packageStr, "RSA", paySign };
    }

    /**
     * 生成 32 位随机串（仅含大写字母与数字，避免转义问题）
     */
    public static String randomNonce()
    {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        java.security.SecureRandom random = new java.security.SecureRandom();
        StringBuilder sb = new StringBuilder(32);
        for (int i = 0; i < 32; i++)
        {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
