package com.huaxing;

import com.huaxing.config.CosProperties;
import com.huaxing.service.CosStorageService;
import com.qcloud.cos.COSClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** luohuai codeX generate: validate upload boundaries without sending data or credentials to Tencent COS. */
class CosStorageServiceTest {

    @Test
    void emptyFileIsRejectedBeforeCosClientCreation() {
        CosStorageService storage = storage(new CosProperties());
        assertStatus(storage, new MockMultipartFile("file", "empty.png", "image/png", new byte[0]), HttpStatus.BAD_REQUEST);
    }

    @Test
    void imageExtensionAndMimeTypeMustAgree() {
        CosStorageService storage = storage(new CosProperties());
        assertStatus(storage, new MockMultipartFile("file", "photo.png", "image/jpeg", new byte[]{1}), HttpStatus.BAD_REQUEST);
    }

    @Test
    void validImageWithPlaceholdersExplainsMissingConfiguration() {
        CosStorageService storage = storage(new CosProperties());
        MockMultipartFile image = new MockMultipartFile("file", "photo.png", "image/png", new byte[]{1, 2, 3});
        assertThatThrownBy(() -> storage.uploadImage(image))
                .isInstanceOfSatisfying(ResponseStatusException.class, error -> {
                    assertThat(error.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    assertThat(error.getReason()).contains("COS尚未配置");
                });
    }

    @Test
    void generalFileAlsoRequiresCosConfiguration() {
        CosStorageService storage = storage(new CosProperties());
        MockMultipartFile file = new MockMultipartFile("file", "manual.pdf", "application/pdf", new byte[]{1});
        assertThatThrownBy(() -> storage.uploadFile(file))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        error -> assertThat(error.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
    }

    private static CosStorageService storage(CosProperties properties) {
        // luohuai codeX generate: an empty provider proves validation completes without constructing a remote client.
        return new CosStorageService(properties, new DefaultListableBeanFactory().getBeanProvider(COSClient.class));
    }

    private static void assertStatus(CosStorageService storage, MockMultipartFile file, HttpStatus status) {
        assertThatThrownBy(() -> storage.uploadImage(file))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        error -> assertThat(error.getStatusCode()).isEqualTo(status));
    }
}
