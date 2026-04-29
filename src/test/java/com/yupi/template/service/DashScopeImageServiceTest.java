package com.yupi.template.service;

import com.yupi.template.config.DashScopeImageConfig;
import com.yupi.template.model.dto.image.ImageData;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
class DashScopeImageServiceTest {

    @InjectMocks
    private DashScopeImageService dashScopeImageService;

    @Mock
    private DashScopeImageConfig dashScopeImageConfig;

    @Test
    @DisplayName("下载预签名 URL 时保持原始查询串不变")
    void testDownloadImagePreservesPresignedUrlQuery() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            byte[] expectedBytes = new byte[]{1, 2, 3};
            server.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "image/png")
                    .setBody(new okio.Buffer().write(expectedBytes)));
            server.start();

            String rawPath = "/image.png";
            String rawQuery = "Expires=1777532623"
                    + "&OSSAccessKeyId=STS.NY8s5atN3yHF29cqVTuAFnzL7"
                    + "&Signature=pQu%2Fa9H%2BAsnB9tSP6WeuE42iKi4%3D"
                    + "&security-token=CAIS0QJ1q6Ft5B2y%2Ffoo%2Bbar%3D%3D";
            String presignedUrl = server.url(rawPath + "?" + rawQuery).toString();

            ImageData imageData = ReflectionTestUtils.invokeMethod(
                    dashScopeImageService, "downloadImage", presignedUrl);

            RecordedRequest recordedRequest = server.takeRequest();
            assertEquals(rawPath + "?" + rawQuery, recordedRequest.getPath());
            assertNotNull(imageData);
            assertArrayEquals(expectedBytes, imageData.getBytes());
            assertEquals("image/png", imageData.getMimeType());
        }
    }
}
