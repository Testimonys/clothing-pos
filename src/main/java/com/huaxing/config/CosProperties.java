package com.huaxing.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** luohuai codeX generate: bind backend COS settings without exposing credentials through toString. */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "cos")
public class CosProperties {
    private String secretId = "";
    private String secretKey = "";
    private String bucketName = "";
    private String region = "ap-shanghai";
    private String publicUrl = "";
    private String keyPrefix = "clothing-pos";
}
