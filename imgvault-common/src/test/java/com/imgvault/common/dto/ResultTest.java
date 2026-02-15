package com.imgvault.common.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Result 和 PageResult 统一响应体单元测试
 */
@DisplayName("统一响应体测试")
class ResultTest {

    // ==================== Result 成功场景 ====================

    @Nested
    @DisplayName("Result - 成功响应")
    class SuccessTests {

        @Test
        @DisplayName("success() 无数据")
        void successWithoutData() {
            Result<Void> result = Result.success();
            assertEquals(200, result.getCode());
            assertEquals("success", result.getMessage());
            assertNull(result.getData());
            assertTrue(result.getTimestamp() > 0);
        }

        @Test
        @DisplayName("success(data) 有数据")
        void successWithData() {
            Result<String> result = Result.success("hello");
            assertEquals(200, result.getCode());
            assertEquals("success", result.getMessage());
            assertEquals("hello", result.getData());
        }

        @Test
        @DisplayName("success(message, data) 自定义消息")
        void successWithMessageAndData() {
            Result<Integer> result = Result.success("上传成功", 42);
            assertEquals(200, result.getCode());
            assertEquals("上传成功", result.getMessage());
            assertEquals(42, result.getData());
        }

        @Test
        @DisplayName("success 支持泛型集合")
        void successWithGenericList() {
            List<String> tags = Arrays.asList("风景", "自然", "旅行");
            Result<List<String>> result = Result.success(tags);
            assertEquals(200, result.getCode());
            assertEquals(3, result.getData().size());
            assertEquals("风景", result.getData().get(0));
        }
    }

    // ==================== Result 失败场景 ====================

    @Nested
    @DisplayName("Result - 失败响应")
    class FailureTests {

        @Test
        @DisplayName("fail(message) 默认 500 错误")
        void failWithMessage() {
            Result<Void> result = Result.fail("服务器内部错误");
            assertEquals(500, result.getCode());
            assertEquals("服务器内部错误", result.getMessage());
            assertNull(result.getData());
        }

        @Test
        @DisplayName("fail(code, message) 自定义错误码")
        void failWithCodeAndMessage() {
            Result<Void> result = Result.fail(403, "权限不足");
            assertEquals(403, result.getCode());
            assertEquals("权限不足", result.getMessage());
        }

        @Test
        @DisplayName("badRequest 返回 400")
        void badRequest() {
            Result<Void> result = Result.badRequest("参数无效");
            assertEquals(400, result.getCode());
            assertEquals("参数无效", result.getMessage());
        }

        @Test
        @DisplayName("notFound 返回 404")
        void notFound() {
            Result<Void> result = Result.notFound("资源不存在");
            assertEquals(404, result.getCode());
            assertEquals("资源不存在", result.getMessage());
        }
    }

    // ==================== Result 时间戳测试 ====================

    @Nested
    @DisplayName("Result - 时间戳")
    class TimestampTests {

        @Test
        @DisplayName("时间戳应为当前时间附近")
        void timestampShouldBeNearCurrentTime() {
            long before = System.currentTimeMillis();
            Result<Void> result = Result.success();
            long after = System.currentTimeMillis();

            assertTrue(result.getTimestamp() >= before);
            assertTrue(result.getTimestamp() <= after);
        }

        @Test
        @DisplayName("默认构造函数设置时间戳")
        void defaultConstructorSetsTimestamp() {
            Result<Object> result = new Result<>();
            assertTrue(result.getTimestamp() > 0);
        }
    }

    // ==================== PageResult 测试 ====================

    @Nested
    @DisplayName("PageResult - 分页结果")
    class PageResultTests {

        @Test
        @DisplayName("创建正常分页结果")
        void createNormalPageResult() {
            List<String> data = Arrays.asList("img1", "img2", "img3");
            PageResult<String> page = PageResult.of(data, 25, 1, 10);

            assertEquals(3, page.getRecords().size());
            assertEquals(25, page.getTotal());
            assertEquals(1, page.getPage());
            assertEquals(10, page.getSize());
            assertEquals(3, page.getPages()); // ceil(25/10) = 3
        }

        @Test
        @DisplayName("创建空分页结果")
        void createEmptyPageResult() {
            PageResult<String> page = PageResult.empty(1, 20);

            assertTrue(page.getRecords().isEmpty());
            assertEquals(0, page.getTotal());
            assertEquals(1, page.getPage());
            assertEquals(20, page.getSize());
            assertEquals(0, page.getPages());
        }

        @Test
        @DisplayName("总页数计算 - 整除")
        void pagesCalculationExactDivision() {
            PageResult<String> page = new PageResult<>(
                    Arrays.asList("a", "b"), 20, 1, 10);
            assertEquals(2, page.getPages()); // 20/10 = 2
        }

        @Test
        @DisplayName("总页数计算 - 有余数")
        void pagesCalculationWithRemainder() {
            PageResult<String> page = new PageResult<>(
                    Arrays.asList("a"), 21, 3, 10);
            assertEquals(3, page.getPages()); // ceil(21/10) = 3
        }

        @Test
        @DisplayName("size 为 0 时 pages 为 0")
        void pagesCalculationWithZeroSize() {
            PageResult<String> page = new PageResult<>(
                    Arrays.asList("a"), 10, 1, 0);
            assertEquals(0, page.getPages());
        }

        @Test
        @DisplayName("单条记录分页")
        void singleRecordPage() {
            PageResult<Long> page = PageResult.of(
                    Arrays.asList(1L), 1, 1, 20);
            assertEquals(1, page.getRecords().size());
            assertEquals(1, page.getTotal());
            assertEquals(1, page.getPages());
        }
    }
}
