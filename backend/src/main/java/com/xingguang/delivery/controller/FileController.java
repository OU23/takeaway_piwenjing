package com.xingguang.delivery.controller;

import com.xingguang.delivery.common.Result;
import com.xingguang.delivery.config.DeliveryProperties;
import com.xingguang.delivery.entity.AppUser;
import com.xingguang.delivery.entity.Dish;
import com.xingguang.delivery.entity.Shop;
import com.xingguang.delivery.service.MediaService;
import com.xingguang.delivery.service.ObjectStorageService;
import com.xingguang.delivery.support.CurrentAdmin;
import com.xingguang.delivery.vo.FileVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 文件与对象存储（MinIO）。
 *
 * <p>上传 / 删除 / 绑定为管理写操作，需 SUPER_ADMIN 或 OPERATOR；状态查询所有登录角色可读。
 */
@Tag(name = "08 文件与对象存储")
@RestController
@RequestMapping("/api/v1/admin/files")
public class FileController {

    private final ObjectStorageService storage;
    private final MediaService media;
    private final CurrentAdmin currentAdmin;

    public FileController(ObjectStorageService storage, MediaService media, CurrentAdmin currentAdmin) {
        this.storage = storage;
        this.media = media;
        this.currentAdmin = currentAdmin;
    }

    @Operation(summary = "对象存储配置状态（不返回密钥）")
    @GetMapping("/status")
    public Result<Map<String, Object>> status() {
        DeliveryProperties.Oss oss = storage.oss();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("enabled", oss.isEnabled());
        data.put("ready", storage.ready());
        data.put("endpoint", oss.getEndpoint());
        data.put("bucket", oss.getBucket());
        data.put("publicBaseUrl", oss.getPublicBaseUrl());
        data.put("maxSizeMb", oss.getMaxSizeMb());
        data.put("allowedTypes", new String[]{"image/jpeg", "image/png", "image/webp", "image/gif"});
        return Result.ok(data);
    }

    @Operation(summary = "对象列表（媒体库预览）")
    @GetMapping("/list")
    public Result<List<FileVO>> list(@RequestParam(required = false) String prefix,
                                     @RequestParam(defaultValue = "60") int limit) {
        return Result.ok(storage.list(prefix, limit));
    }

    @Operation(summary = "上传图片（multipart，字段名 file）",
            description = "biz 为业务前缀：dish 商品图 / shop 门店 Logo / avatar 头像 / license 资质")
    @PostMapping
    public Result<FileVO> upload(@RequestPart("file") MultipartFile file,
                                 @RequestParam(defaultValue = "common") String biz) {
        currentAdmin.requireManage();
        return Result.ok(storage.upload(file, biz));
    }

    @Operation(summary = "删除对象")
    @DeleteMapping
    public Result<Void> delete(@RequestParam String key) {
        currentAdmin.requireManage();
        storage.delete(key);
        return Result.ok();
    }

    @Operation(summary = "生成临时签名 URL")
    @GetMapping("/presign")
    public Result<Map<String, Object>> presign(@RequestParam String key,
                                               @RequestParam(required = false) Integer expiry) {
        String url = storage.presignedUrl(key, expiry);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("objectKey", key);
        data.put("url", url);
        return Result.ok(data);
    }

    @Operation(summary = "绑定商品图")
    @PostMapping("/bind/dish/{id}")
    public Result<Map<String, Object>> bindDish(@PathVariable Long id, @RequestParam String objectKey) {
        currentAdmin.requireManage();
        Dish dish = media.bindDishImage(id, objectKey);
        return Result.ok(bindResult("dish", dish.getId(), objectKey));
    }

    @Operation(summary = "绑定门店 Logo")
    @PostMapping("/bind/shop/{id}")
    public Result<Map<String, Object>> bindShop(@PathVariable Long id, @RequestParam String objectKey) {
        currentAdmin.requireManage();
        Shop shop = media.bindShopLogo(id, objectKey);
        return Result.ok(bindResult("shop", shop.getId(), objectKey));
    }

    @Operation(summary = "绑定用户头像")
    @PostMapping("/bind/user/{id}")
    public Result<Map<String, Object>> bindUser(@PathVariable Long id, @RequestParam String objectKey) {
        currentAdmin.requireManage();
        AppUser user = media.bindUserAvatar(id, objectKey);
        return Result.ok(bindResult("user", user.getId(), objectKey));
    }

    private Map<String, Object> bindResult(String type, Long id, String objectKey) {
        FileVO vo = new FileVO();
        vo.setObjectKey(objectKey);
        storage.applyUrl(vo);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("type", type);
        data.put("id", id);
        data.put("objectKey", objectKey);
        data.put("url", vo.getUrl());
        return data;
    }
}
