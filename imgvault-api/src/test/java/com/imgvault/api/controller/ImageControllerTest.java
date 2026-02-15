package com.imgvault.api.controller;

import com.imgvault.api.config.GlobalExceptionHandler;
import com.imgvault.app.service.ImageAppService;
import com.imgvault.common.dto.ImageDetailDTO;
import com.imgvault.common.dto.ImageUploadDTO;
import com.imgvault.common.dto.PageResult;
import com.imgvault.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ImageController 单元测试
 * 使用 MockMvc + Mockito，不启动 Spring 容器
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ImageController API 测试")
class ImageControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ImageAppService imageAppService;

    @InjectMocks
    private ImageController imageController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(imageController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ==================== 图片上传 ====================

    @Nested
    @DisplayName("POST /api/v1/images/upload - 图片上传")
    class UploadTests {

        @Test
        @DisplayName("上传 JPEG 图片成功")
        void shouldUploadJpegSuccessfully() throws Exception {
            // 准备 mock 数据
            MockMultipartFile mockFile = new MockMultipartFile(
                    "file", "test.jpg", "image/jpeg",
                    new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0});

            ImageUploadDTO dto = new ImageUploadDTO();
            dto.setId(1L);
            dto.setImageUuid("abc-123");
            dto.setOriginalName("test.jpg");
            dto.setFormat("jpeg");
            dto.setFileSize(4L);

            when(imageAppService.uploadImage(any())).thenReturn(dto);

            // 执行请求
            mockMvc.perform(multipart("/api/v1/images/upload")
                            .file(mockFile))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("上传成功"))
                    .andExpect(jsonPath("$.data.id").value(1))
                    .andExpect(jsonPath("$.data.imageUuid").value("abc-123"));
        }

        @Test
        @DisplayName("上传非图片文件被拒绝")
        void shouldRejectNonImageFile() throws Exception {
            MockMultipartFile mockFile = new MockMultipartFile(
                    "file", "malware.exe", "application/octet-stream",
                    new byte[]{0x4D, 0x5A}); // PE executable header

            when(imageAppService.uploadImage(any()))
                    .thenThrow(BusinessException.badRequest("不支持的文件类型"));

            mockMvc.perform(multipart("/api/v1/images/upload")
                            .file(mockFile))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(400))
                    .andExpect(jsonPath("$.message").value("不支持的文件类型"));
        }
    }

    // ==================== 图片查询 ====================

    @Nested
    @DisplayName("GET /api/v1/images/{id} - 图片查询")
    class QueryTests {

        @Test
        @DisplayName("按 ID 查询图片详情")
        void shouldGetImageById() throws Exception {
            ImageDetailDTO dto = new ImageDetailDTO();
            dto.setId(1L);
            dto.setImageUuid("abc-123");
            dto.setOriginalName("sunset.jpg");
            dto.setFormat("jpeg");
            dto.setFileSize(1024000L);
            dto.setWidth(1920);
            dto.setHeight(1080);

            when(imageAppService.getImageById(1L)).thenReturn(dto);

            mockMvc.perform(get("/api/v1/images/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.id").value(1))
                    .andExpect(jsonPath("$.data.originalName").value("sunset.jpg"))
                    .andExpect(jsonPath("$.data.width").value(1920))
                    .andExpect(jsonPath("$.data.height").value(1080));
        }

        @Test
        @DisplayName("查询不存在的图片返回 404")
        void shouldReturn404ForNonExistentImage() throws Exception {
            when(imageAppService.getImageById(999L))
                    .thenThrow(BusinessException.notFound("图片不存在: 999"));

            mockMvc.perform(get("/api/v1/images/999"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(404))
                    .andExpect(jsonPath("$.message").value("图片不存在: 999"));
        }

        @Test
        @DisplayName("按 UUID 查询图片详情")
        void shouldGetImageByUuid() throws Exception {
            ImageDetailDTO dto = new ImageDetailDTO();
            dto.setId(1L);
            dto.setImageUuid("abc-123-def-456");
            dto.setOriginalName("photo.png");

            when(imageAppService.getImageByUuid("abc-123-def-456")).thenReturn(dto);

            mockMvc.perform(get("/api/v1/images/uuid/abc-123-def-456"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.imageUuid").value("abc-123-def-456"));
        }
    }

    // ==================== 分页查询 ====================

    @Nested
    @DisplayName("GET /api/v1/images - 图片列表")
    class ListTests {

        @Test
        @DisplayName("分页查询图片列表")
        void shouldListImages() throws Exception {
            ImageDetailDTO img1 = new ImageDetailDTO();
            img1.setId(1L);
            img1.setOriginalName("img1.jpg");

            ImageDetailDTO img2 = new ImageDetailDTO();
            img2.setId(2L);
            img2.setOriginalName("img2.png");

            PageResult<ImageDetailDTO> pageResult = PageResult.of(
                    Arrays.asList(img1, img2), 50, 1, 20);

            when(imageAppService.listImages(any())).thenReturn(pageResult);

            mockMvc.perform(get("/api/v1/images")
                            .param("page", "1")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.total").value(50))
                    .andExpect(jsonPath("$.data.records").isArray())
                    .andExpect(jsonPath("$.data.records.length()").value(2));
        }

        @Test
        @DisplayName("空列表返回空分页")
        void shouldReturnEmptyPage() throws Exception {
            PageResult<ImageDetailDTO> emptyPage = PageResult.empty(1, 20);
            when(imageAppService.listImages(any())).thenReturn(emptyPage);

            mockMvc.perform(get("/api/v1/images"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.total").value(0))
                    .andExpect(jsonPath("$.data.records").isEmpty());
        }
    }

    // ==================== 图片删除 ====================

    @Nested
    @DisplayName("DELETE /api/v1/images/{id} - 图片删除")
    class DeleteTests {

        @Test
        @DisplayName("软删除图片成功")
        void shouldSoftDeleteSuccessfully() throws Exception {
            doNothing().when(imageAppService).softDeleteImage(1L);

            mockMvc.perform(delete("/api/v1/images/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("删除成功"));
        }

        @Test
        @DisplayName("删除不存在的图片返回 404")
        void shouldReturn404WhenDeletingNonExistent() throws Exception {
            doThrow(BusinessException.notFound("图片不存在: 999"))
                    .when(imageAppService).softDeleteImage(999L);

            mockMvc.perform(delete("/api/v1/images/999"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(404));
        }

        @Test
        @DisplayName("永久删除图片成功")
        void shouldHardDeleteSuccessfully() throws Exception {
            doNothing().when(imageAppService).hardDeleteImage(1L);

            mockMvc.perform(delete("/api/v1/images/1/permanent"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("永久删除成功"));
        }
    }

    // ==================== 下载 URL ====================

    @Nested
    @DisplayName("GET /api/v1/images/{id}/download-url - 获取下载URL")
    class DownloadUrlTests {

        @Test
        @DisplayName("获取下载 URL 成功")
        void shouldGetDownloadUrl() throws Exception {
            String url = "http://minio:9000/imgvault/originals/2026/02/14/abc-123.jpg?signature=xxx";
            when(imageAppService.getDownloadUrl(1L)).thenReturn(url);

            mockMvc.perform(get("/api/v1/images/1/download-url"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").value(url));
        }

        @Test
        @DisplayName("已删除图片获取下载 URL 返回 404")
        void shouldReturn404ForDeletedImage() throws Exception {
            when(imageAppService.getDownloadUrl(1L))
                    .thenThrow(BusinessException.notFound("图片不存在: 1"));

            mockMvc.perform(get("/api/v1/images/1/download-url"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(404));
        }
    }

    // ==================== 下载 (302 重定向) ====================

    @Nested
    @DisplayName("GET /api/v1/images/{id}/download - 下载图片(重定向)")
    class DownloadRedirectTests {

        @Test
        @DisplayName("下载图片返回 302 重定向")
        void shouldRedirectToMinioUrl() throws Exception {
            String url = "http://minio:9000/imgvault/originals/2026/02/14/abc-123.jpg";
            when(imageAppService.getDownloadUrl(1L)).thenReturn(url);

            mockMvc.perform(get("/api/v1/images/1/download"))
                    .andExpect(status().isFound())
                    .andExpect(redirectedUrl(url));
        }
    }
}
