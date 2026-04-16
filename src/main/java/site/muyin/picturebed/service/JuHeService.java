package site.muyin.picturebed.service;

import reactor.core.publisher.Mono;
import site.muyin.picturebed.domain.JhImage;
import site.muyin.picturebed.domain.JuHeAlbum;
import site.muyin.picturebed.domain.LskyProAlbum;
import site.muyin.picturebed.query.CommonQuery;
import java.util.List;

/**
 * <br>
 * 文件名称： JhService<br>
 * 初始作者： 张宇 <br>
 * 创建日期： 2026/1/13 10:09<br>
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
public interface JuHeService extends BaseImageService<JhImage> {

    /**
     * 获取图库列表
     *
     * @param query:
     * @return: reactor.core.publisher.Mono<java.util.List < site.muyin.picturebed.domain.LskyProAlbum>>
     * @author: lywq
     * @date: 2024/05/22 21:31
     **/
    Mono<List<JuHeAlbum>> getAlbumList(CommonQuery query);
}
