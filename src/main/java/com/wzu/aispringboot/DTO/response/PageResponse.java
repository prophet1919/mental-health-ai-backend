package com.wzu.aispringboot.DTO.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 通用分页响应。避免依赖 MyBatis-Plus 的 IPage 直接出参。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {

    private List<T> records;

    private long total;

    private long page;

    private long size;

    public static <T> PageResponse<T> of(List<T> records, long total, long page, long size) {
        return new PageResponse<>(records, total, page, size);
    }

    public static <T> PageResponse<T> empty(long page, long size) {
        return new PageResponse<>(List.of(), 0L, page, size);
    }
}
