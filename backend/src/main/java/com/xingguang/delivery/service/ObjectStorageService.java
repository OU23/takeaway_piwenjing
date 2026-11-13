package com.xingguang.delivery.service;

import com.xingguang.delivery.common.BizException;
import com.xingguang.delivery.config.DeliveryProperties;
import com.xingguang.delivery.vo.FileVO;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.Result;
import io.minio.http.Method;
import io.minio.messages.Item;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * MinIO 对象存储（S3 兼容）服务：商家资质 / 商品图 / 用户头像。
 *
 * <p>设计要点：
 * <ul>
 *   <li>AK/SK 未配置时不影响应用启动，仅在调用上传时返回明确错误（错误码 20001）；</li>
 *   <li>对象键用「业务前缀/年月/UUID.扩展名」，避免中文与用户原始文件名进入存储路径；</li>
 *   <li>仅允许图片类型并限制体积，防止把对象存储当通用网盘；</li>
 *   <li>桶公开读时返回直链，否则返回有时效的签名 URL。</li>
 * </ul>
 */
@Service
public class ObjectStorageService {

    private static final Set<String> ALLOWED_TYPES =
            Set.of("image/jpeg", "image/png", "image/webp", "image/gif");
    private static final Set<String> ALLOWED_EXT = Set.of("jpg", "jpeg", "png", "webp", "gif");
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("yyyyMM");

    private final DeliveryProperties props;
    private volatile MinioClient client;

    public ObjectStorageService(DeliveryProperties props) {
        this.props = props;
    }

    public DeliveryProperties.Oss oss() {
        return props.getOss();
    }

    public boolean ready() {
        return oss().ready();
    }

    /** 懒建客户端：配置为空时不创建，避免启动阶段因凭证缺失直接失败。 */
    private MinioClient client() {
        if (!ready()) {
            throw new BizException(20001,
                    "对象存储未配置：请设置环境变量 XG_OSS_ACCESS_KEY / XG_OSS_SECRET_KEY，"
                            + "并确认 xingguang.oss.endpoint 与 bucket 正确");
        }
        MinioClient c = client;
        if (c == null) {
            synchronized (this) {
                c = client;
                if (c == null) {
                    c = MinioClient.builder()
                            .endpoint(oss().getEndpoint())
                            .credentials(oss().getAccessKey(), oss().getSecretKey())
                            .build();
                    client = c;
                }
            }
        }
        return c;
    }

    /**
     * 上传图片。
     *
     * @param file 前端 multipart 文件
     * @param biz  业务前缀（dish / shop / avatar / license），非法字符会被剔除
     */
    public FileVO upload(MultipartFile file, String biz) {
        if (file == null || file.isEmpty()) {
            throw new BizException(20002, "上传文件为空");
        }
        long maxBytes = (long) oss().getMaxSizeMb() * 1024 * 1024;
        if (file.getSize() > maxBytes) {
            throw new BizException(20003, "文件超过 " + oss().getMaxSizeMb() + "MB 限制");
        }
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (!ALLOWED_TYPES.contains(contentType)) {
            throw new BizException(20004, "仅支持 JPG / PNG / WEBP / GIF 图片");
        }
        String ext = extOf(file.getOriginalFilename(), contentType);
        String prefix = normalizeBiz(biz);
        String objectKey = prefix + "/" + LocalDate.now().format(MONTH) + "/"
                + UUID.randomUUID().toString().replace("-", "") + "." + ext;

        try (InputStream in = file.getInputStream()) {
            client().putObject(PutObjectArgs.builder()
                    .bucket(oss().getBucket())
                    .object(objectKey)
                    .stream(in, file.getSize(), -1)
                    .contentType(contentType)
                    .build());
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException(20005, "上传失败：" + e.getMessage());
        }

        FileVO vo = new FileVO();
        vo.setObjectKey(objectKey);
        vo.setName(file.getOriginalFilename());
        vo.setSize(file.getSize());
        vo.setContentType(contentType);
        applyUrl(vo);
        return vo;
    }

    /** 删除对象；对象不存在时 MinIO 也返回成功，幂等。 */
    public void delete(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            throw new BizException(20006, "缺少对象键 objectKey");
        }
        try {
            client().removeObject(RemoveObjectArgs.builder()
                    .bucket(oss().getBucket())
                    .object(objectKey)
                    .build());
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException(20007, "删除失败：" + e.getMessage());
        }
    }

    /** 列举对象（媒体库预览用），按前缀过滤，最多 200 条。 */
    public List<FileVO> list(String prefix, int limit) {
        int max = Math.min(Math.max(limit, 1), 200);
        List<FileVO> out = new ArrayList<>();
        try {
            Iterable<Result<Item>> items = client().listObjects(ListObjectsArgs.builder()
                    .bucket(oss().getBucket())
                    .prefix(prefix == null ? "" : prefix)
                    .recursive(true)
                    .maxKeys(max)
                    .build());
            for (Result<Item> r : items) {
                Item it = r.get();
                if (it.isDir()) {
                    continue;
                }
                FileVO vo = new FileVO();
                vo.setObjectKey(it.objectName());
                vo.setName(it.objectName());
                vo.setSize(it.size());
                vo.setLastModified(it.lastModified() == null ? null : it.lastModified().toInstant().toEpochMilli());
                applyUrl(vo);
                out.add(vo);
                if (out.size() >= max) {
                    break;
                }
            }
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException(20009, "列举对象失败：" + e.getMessage());
        }
        return out;
    }

    /** 生成临时签名 URL；未指定有效期时用配置值，最大 7 天。 */
    public String presignedUrl(String objectKey, Integer expirySeconds) {
        if (objectKey == null || objectKey.isBlank()) {
            throw new BizException(20006, "缺少对象键 objectKey");
        }
        int seconds = expirySeconds == null || expirySeconds <= 0
                ? oss().getPresignExpirySeconds()
                : Math.min(expirySeconds, 7 * 24 * 3600);
        try {
            return client().getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(oss().getBucket())
                    .object(objectKey)
                    .expiry(seconds, TimeUnit.SECONDS)
                    .build());
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException(20008, "生成临时链接失败：" + e.getMessage());
        }
    }

    /** 按配置补全可访问地址：公开直链优先，否则签名 URL。 */
    public void applyUrl(FileVO vo) {
        String base = oss().getPublicBaseUrl();
        if (base != null && !base.isBlank()) {
            vo.setUrl(trimEnd(base) + "/" + oss().getBucket() + "/" + vo.getObjectKey());
            vo.setExpireAt(null);
            return;
        }
        vo.setUrl(presignedUrl(vo.getObjectKey(), null));
        vo.setExpireAt(System.currentTimeMillis() + oss().getPresignExpirySeconds() * 1000L);
    }

    private static String normalizeBiz(String biz) {
        String v = (biz == null || biz.isBlank()) ? "common" : biz.trim().toLowerCase(Locale.ROOT);
        v = v.replaceAll("[^a-z0-9_-]", "");
        return v.isBlank() ? "common" : v;
    }

    private static String extOf(String filename, String contentType) {
        if (filename != null) {
            int dot = filename.lastIndexOf('.');
            if (dot >= 0 && dot < filename.length() - 1) {
                String ext = filename.substring(dot + 1).toLowerCase(Locale.ROOT);
                if (ALLOWED_EXT.contains(ext)) {
                    return ext;
                }
            }
        }
        return switch (contentType) {
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            case "image/gif" -> "gif";
            default -> "jpg";
        };
    }

    private static String trimEnd(String s) {
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }
}
