package com.imgvault.api.controller;

import com.imgvault.api.config.GlobalExceptionHandler;
import com.imgvault.app.service.AlbumAppService;
import com.imgvault.common.dto.PageResult;
import com.imgvault.common.exception.BusinessException;
import com.imgvault.domain.entity.AlbumEntity;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AlbumController 相册管理接口测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AlbumController 相册接口测试")
class AlbumControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AlbumAppService albumAppService;

    @InjectMocks
    private AlbumController albumController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(albumController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private AlbumEntity mockAlbum(Long id, String name, String description) {
        AlbumEntity album = new AlbumEntity();
        album.setId(id);
        album.setName(name);
        album.setDescription(description);
        album.setImageCount(0);
        album.setOwnerId(0L);
        return album;
    }

    // ==================== 创建相册 ====================

    @Nested
    @DisplayName("POST /api/v1/albums - 创建相册")
    class CreateTests {

        @Test
        @DisplayName("创建相册成功")
        void shouldCreateAlbum() throws Exception {
            AlbumEntity album = mockAlbum(1L, "旅行照片", "2026春节旅行");
            when(albumAppService.createAlbum("旅行照片", "2026春节旅行")).thenReturn(album);

            mockMvc.perform(post("/api/v1/albums")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"旅行照片\",\"description\":\"2026春节旅行\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.name").value("旅行照片"))
                    .andExpect(jsonPath("$.data.description").value("2026春节旅行"));
        }

        @Test
        @DisplayName("创建相册名称为空返回 400")
        void shouldRejectEmptyName() throws Exception {
            when(albumAppService.createAlbum(null, null))
                    .thenThrow(BusinessException.badRequest("相册名称不能为空"));

            mockMvc.perform(post("/api/v1/albums")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(400));
        }
    }

    // ==================== 查询相册 ====================

    @Nested
    @DisplayName("GET /api/v1/albums - 查询相册")
    class QueryTests {

        @Test
        @DisplayName("获取相册列表")
        void shouldListAlbums() throws Exception {
            PageResult<AlbumEntity> pageResult = PageResult.of(
                    Arrays.asList(
                            mockAlbum(1L, "旅行照片", "2026春节"),
                            mockAlbum(2L, "美食集锦", "各地美食")),
                    2, 1, 20);
            when(albumAppService.listAlbums(1, 20)).thenReturn(pageResult);

            mockMvc.perform(get("/api/v1/albums")
                            .param("page", "1")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.records.length()").value(2));
        }

        @Test
        @DisplayName("按 ID 查询相册详情")
        void shouldGetAlbumById() throws Exception {
            when(albumAppService.getAlbum(1L))
                    .thenReturn(mockAlbum(1L, "旅行照片", "描述"));

            mockMvc.perform(get("/api/v1/albums/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.name").value("旅行照片"));
        }

        @Test
        @DisplayName("查询不存在的相册返回 404")
        void shouldReturn404ForNonExistentAlbum() throws Exception {
            when(albumAppService.getAlbum(999L))
                    .thenThrow(BusinessException.notFound("相册不存在: 999"));

            mockMvc.perform(get("/api/v1/albums/999"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(404));
        }
    }

    // ==================== 相册图片管理 ====================

    @Nested
    @DisplayName("相册图片管理操作")
    class AlbumImageTests {

        @Test
        @DisplayName("添加图片到相册成功")
        void shouldAddImageToAlbum() throws Exception {
            doNothing().when(albumAppService).addImageToAlbum(1L, 1L);

            mockMvc.perform(post("/api/v1/albums/1/images/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("图片已添加"));
        }

        @Test
        @DisplayName("从相册移除图片成功")
        void shouldRemoveImageFromAlbum() throws Exception {
            doNothing().when(albumAppService).removeImageFromAlbum(1L, 1L);

            mockMvc.perform(delete("/api/v1/albums/1/images/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("图片已移除"));
        }

        @Test
        @DisplayName("设置相册封面成功")
        void shouldSetAlbumCover() throws Exception {
            doNothing().when(albumAppService).setAlbumCover(1L, 5L);

            mockMvc.perform(put("/api/v1/albums/1/cover/5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("封面已设置"));
        }

        @Test
        @DisplayName("获取相册图片列表")
        void shouldGetAlbumImages() throws Exception {
            PageResult<Long> pageResult = PageResult.of(
                    Arrays.asList(1L, 2L, 3L), 3, 1, 20);
            when(albumAppService.getAlbumImages(1L, 1, 20)).thenReturn(pageResult);

            mockMvc.perform(get("/api/v1/albums/1/images")
                            .param("page", "1")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.records.length()").value(3))
                    .andExpect(jsonPath("$.data.total").value(3));
        }
    }

    // ==================== 删除相册 ====================

    @Nested
    @DisplayName("DELETE /api/v1/albums/{id} - 删除相册")
    class DeleteTests {

        @Test
        @DisplayName("删除相册成功")
        void shouldDeleteAlbum() throws Exception {
            doNothing().when(albumAppService).deleteAlbum(1L);

            mockMvc.perform(delete("/api/v1/albums/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("删除成功"));
        }

        @Test
        @DisplayName("删除不存在的相册返回 404")
        void shouldReturn404ForNonExistentAlbum() throws Exception {
            doThrow(BusinessException.notFound("相册不存在: 999"))
                    .when(albumAppService).deleteAlbum(999L);

            mockMvc.perform(delete("/api/v1/albums/999"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(404));
        }
    }
}
