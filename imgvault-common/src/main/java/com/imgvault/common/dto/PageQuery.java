package com.imgvault.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.io.Serializable;

/**
 * 分页查询参数
 */
@Data
@Schema(description = "分页查询参数")
public class PageQuery implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "页码", example = "1", defaultValue = "1")
    @Min(value = 1, message = "页码最小为1")
    private int page = 1;

    @Schema(description = "每页大小", example = "20", defaultValue = "20")
    @Min(value = 1, message = "每页大小最小为1")
    @Max(value = 100, message = "每页大小最大为100")
    private int size = 20;

    @Schema(description = "排序字段", example = "created_at")
    private String sortBy = "created_at";

    @Schema(description = "排序方向: asc/desc", example = "desc")
    private String sortOrder = "desc";

    /**
     * 获取偏移量
     */
    public int getOffset() {
        return (page - 1) * size;
    }
}
