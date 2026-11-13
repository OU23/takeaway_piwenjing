package com.xingguang.delivery.common;

import lombok.Data;

import java.util.List;

/**
 * 分页响应（SRS 9.6：pageNum / pageSize，响应含 total，默认 pageSize=20，上限 100）。
 */
@Data
public class PageResult<T> {

    private long total;
    private long pageNum;
    private long pageSize;
    private List<T> list;

    public static <T> PageResult<T> of(long total, long pageNum, long pageSize, List<T> list) {
        PageResult<T> p = new PageResult<>();
        p.total = total;
        p.pageNum = pageNum;
        p.pageSize = pageSize;
        p.list = list;
        return p;
    }

    public static long normalizePageNum(Long pageNum) {
        return pageNum == null || pageNum < 1 ? 1 : pageNum;
    }

    public static long normalizePageSize(Long pageSize) {
        if (pageSize == null || pageSize < 1) {
            return 20;
        }
        return Math.min(pageSize, 100);
    }
}
