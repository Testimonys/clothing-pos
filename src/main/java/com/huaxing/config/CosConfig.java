package com.huaxing.config;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.http.HttpProtocol;
import com.qcloud.cos.region.Region;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

/** luohuai codeX generate: lazily reuse one HTTPS COS client and release connections on application shutdown. */
@Configuration
public class CosConfig {
    @Bean(destroyMethod = "shutdown")
    public COSClient cosClient(CosProperties properties) {
        ClientConfig config = new ClientConfig(new Region(properties.getRegion().trim()));
        config.setHttpProtocol(HttpProtocol.https);
        config.setConnectionTimeout(10_000);
        config.setSocketTimeout(60_000);
        return new COSClient(new BasicCOSCredentials(properties.getSecretId().trim(), properties.getSecretKey().trim()), config);
    }
}
