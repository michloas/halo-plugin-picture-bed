package site.muyin.picturebed.domain;

import java.util.List;
import java.util.Map;
import lombok.Data;

/**
 * <br>
 * 文件名称： JhImage<br>
 * 初始作者： 张宇 <br>
 * 创建日期： 2026/1/13 10:08<br>
 * 功能说明：  <br>
 * <br>
 * ================================================<br>
 * 修改记录：<br>
 * 修改作者 日期 修改内容<br>
 * <br>
 * ================================================<br>
 * Copyright  2026/1/13 .All rights reserved.<br>
 *
 * @author 张宇
 */
@Data
public class JhImage {
    /**
     * 图片ID
     */
    private String _id;

    /**
     * 分类列表
     */
    private List<String> categories;

    /**
     * 创建时间
     */
    private String created_at;

    /**
     * 用户ID
     */
    private Integer uid;

    /**
     * 文件大小(字节)
     */
    private Integer size;

    /**
     * 文件格式
     */
    private String format;

    /**
     * 原始文件名
     */
    private String filename;

    /**
     * 是否为性感内容
     */
    private Boolean sexy;

    /**
     * 图片高度
     */
    private Integer height;

    /**
     * 图片宽度
     */
    private Integer width;

    /**
     * URL映射
     */
    private Map<String, Object> urls;

    /**
     * VIP等级
     */
    private Integer vip;

    /**
     * 访问次数
     */
    private Integer visit;

    /**
     * 图片URL
     */
    private String url;
}
