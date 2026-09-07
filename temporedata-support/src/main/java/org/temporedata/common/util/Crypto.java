package org.temporedata.common.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 集中持有 AES 密钥，避免各调用方重复 @Value 注入。
 *
 * <p>密钥来源为配置键 {@code temporedata.crypto.key}（默认 {@code temporedata-p0-key}），
 * 封装 {@link CryptoUtil} 的加解密能力，供各处数据源密码加解密调用。</p>
 */
@Component
public class Crypto {

    private final String key;

    public Crypto(@Value("${temporedata.crypto.key:temporedata-p0-key}") String key) {
        this.key = key;
    }

    public String encrypt(String plain) {
        return CryptoUtil.encrypt(plain, key);
    }

    public String decrypt(String enc) {
        return CryptoUtil.decrypt(enc, key);
    }
}