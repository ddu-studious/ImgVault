package com.imgvault.app.service;

import com.imgvault.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

/**
 * IOPaint 水印去除服务
 * 调用 IOPaint HTTP API 进行 AI 驱动的 inpainting
 */
@Slf4j
@Service
public class IoPaintWatermarkService {

    private static final String INPAINT_PATH = "/api/v1/inpaint";

    @Value("${iopaint.base-url:http://localhost:8085}")
    private String baseUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 调用 IOPaint API 去除水印
     *
     * @param imageData 源图片字节
     * @param maskData  遮罩 PNG 字节，白色区域为水印
     * @return 处理后的图片字节
     */
    public byte[] removeWatermark(byte[] imageData, byte[] maskData) throws Exception {
        String url = baseUrl.endsWith("/") ? baseUrl + INPAINT_PATH.substring(1) : baseUrl + INPAINT_PATH;

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<String, Object>();
        body.add("image", new ByteArrayResource(imageData) {
            @Override
            public String getFilename() {
                return "image.png";
            }
        });
        body.add("mask", new ByteArrayResource(maskData) {
            @Override
            public String getFilename() {
                return "mask.png";
            }
        });

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<MultiValueMap<String, Object>>(body, headers);

        try {
            ResponseEntity<byte[]> response = restTemplate.postForEntity(url, requestEntity, byte[].class);
            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                throw BusinessException.badRequest("IOPaint 返回异常: " + response.getStatusCode());
            }
            return response.getBody();
        } catch (ResourceAccessException e) {
            log.warn("IOPaint 服务不可用: {}", e.getMessage());
            throw BusinessException.badRequest("IOPaint 服务不可用");
        }
    }
}
