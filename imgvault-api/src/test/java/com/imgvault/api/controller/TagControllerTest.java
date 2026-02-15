package com.imgvault.api.controller;

import com.imgvault.api.config.GlobalExceptionHandler;
import com.imgvault.app.service.TagAppService;
import com.imgvault.common.exception.BusinessException;
import com.imgvault.domain.entity.TagEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * TagController 标签管理接口测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TagController 标签接口测试")
class TagControllerTest {

    private MockMvc mockMvc;

    @Mock
    private TagAppService tagAppService;

    @InjectMocks
    private TagController tagController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(tagController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private TagEntity mockTag(Long id, String name, int imageCount) {
        TagEntity tag = new TagEntity();
        tag.setId(id);
        tag.setName(name);
        tag.setImageCount(imageCount);
        return tag;
    }

    // ==================== 创建标签 ====================

    @Nested
    @DisplayName("POST /api/v1/tags - 创建标签")
    class CreateTests {

        @Test
        @DisplayName("创建标签成功")
        void shouldCreateTag() throws Exception {
            TagEntity tag = mockTag(1L, "风景", 0);
            when(tagAppService.createTag("风景")).thenReturn(tag);

            mockMvc.perform(post("/api/v1/tags")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"风景\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.name").value("风景"));
        }

        @Test
        @DisplayName("创建重复标签返回 409")
        void shouldRejectDuplicateTag() throws Exception {
            when(tagAppService.createTag("风景"))
                    .thenThrow(BusinessException.conflict("标签已存在: 风景"));

            mockMvc.perform(post("/api/v1/tags")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"风景\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(409));
        }
    }

    // ==================== 查询标签 ====================

    @Nested
    @DisplayName("GET /api/v1/tags - 查询标签")
    class QueryTests {

        @Test
        @DisplayName("获取所有标签列表")
        void shouldListAllTags() throws Exception {
            List<TagEntity> tags = Arrays.asList(
                    mockTag(1L, "风景", 10),
                    mockTag(2L, "人物", 5),
                    mockTag(3L, "美食", 3));
            when(tagAppService.listAllTags()).thenReturn(tags);

            mockMvc.perform(get("/api/v1/tags"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.length()").value(3))
                    .andExpect(jsonPath("$.data[0].name").value("风景"));
        }

        @Test
        @DisplayName("按 ID 查询标签详情")
        void shouldGetTagById() throws Exception {
            when(tagAppService.getTag(1L)).thenReturn(mockTag(1L, "风景", 10));

            mockMvc.perform(get("/api/v1/tags/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(1))
                    .andExpect(jsonPath("$.data.name").value("风景"))
                    .andExpect(jsonPath("$.data.imageCount").value(10));
        }

        @Test
        @DisplayName("查询不存在的标签返回 404")
        void shouldReturn404ForNonExistentTag() throws Exception {
            when(tagAppService.getTag(999L))
                    .thenThrow(BusinessException.notFound("标签不存在: 999"));

            mockMvc.perform(get("/api/v1/tags/999"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(404));
        }
    }

    // ==================== 图片标签关联 ====================

    @Nested
    @DisplayName("标签-图片关联操作")
    class ImageTagTests {

        @Test
        @DisplayName("为图片添加标签成功")
        void shouldAddTagToImage() throws Exception {
            doNothing().when(tagAppService).addTagToImage(1L, 1L);

            mockMvc.perform(post("/api/v1/tags/images/1/tags/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("标签已添加"));
        }

        @Test
        @DisplayName("获取图片的所有标签")
        void shouldGetImageTags() throws Exception {
            List<TagEntity> tags = Arrays.asList(
                    mockTag(1L, "风景", 10),
                    mockTag(2L, "日落", 3));
            when(tagAppService.getImageTags(1L)).thenReturn(tags);

            mockMvc.perform(get("/api/v1/tags/images/1/tags"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(2));
        }

        @Test
        @DisplayName("移除图片标签成功")
        void shouldRemoveTagFromImage() throws Exception {
            doNothing().when(tagAppService).removeTagFromImage(1L, 1L);

            mockMvc.perform(delete("/api/v1/tags/images/1/tags/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("标签已移除"));
        }
    }
}
