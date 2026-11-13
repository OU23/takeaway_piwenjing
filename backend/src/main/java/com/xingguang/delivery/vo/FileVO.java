package com.xingguang.delivery.vo;

import lombok.Data;

/**
 * 文件上传结果。
 *
 * <p>{@code objectKey} 用于落库（可迁移、可换 CDN）；{@code url} 供前端直接展示，
 * 桶公开读时为直链，否则为带有效期的临时签名 URL。
 */
@Data
public class FileVO {

    /** 对象键，形如 dish/202610/9f2c....png */
    private String objectKey;

    /** 可访问地址（直链或签名 URL） */
    private String url;

    /** 原始文件名 */
    private String name;

    /** 字节数 */
    private Long size;

    private String contentType;

    /** 签名 URL 的过期时间戳（毫秒）；直链时为 null */
    private Long expireAt;

    /** 最后修改时间（毫秒），列表查询时返回 */
    private Long lastModified;
}
