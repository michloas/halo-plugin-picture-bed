package site.muyin.picturebed.service.Impl;

import static site.muyin.picturebed.config.PictureBedConfig.GROUP;
import static site.muyin.picturebed.constant.CommonConstant.PictureBedType.JUHE;

import io.netty.channel.ChannelOption;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import run.halo.app.plugin.ReactiveSettingFetcher;
import site.muyin.picturebed.config.PictureBedConfig;
import site.muyin.picturebed.domain.JhImage;
import site.muyin.picturebed.domain.JuHeAlbum;
import site.muyin.picturebed.query.CommonQuery;
import site.muyin.picturebed.service.JuHeService;
import site.muyin.picturebed.utils.PictureBedUtil;
import site.muyin.picturebed.vo.PageResult;
import site.muyin.picturebed.vo.ResultsVO;

/**
 * <br>
 * 文件名称： JuHeServiceImpl<br>
 * 初始作者： 张宇 <br>
 * 创建日期： 2026/1/12 10:10<br>
 * 功能说明：  <br>
 * <br>
 * ================================================<br>
 * 修改记录：<br>
 * 修改作者 日期 修改内容<br>
 * <br>
 * ================================================<br>
 * Copyright  2026/1/12 .All rights reserved.<br>
 *
 * @author 张宇
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JuHeServiceImpl implements JuHeService {

    private final ReactiveSettingFetcher settingFetcher;

    private final WebClient webClient = WebClient.builder()
        .clientConnector(new ReactorClientHttpConnector(
            HttpClient.create()
                .responseTimeout(Duration.ofSeconds(20)) // 设置响应超时时间
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000) // 设置连接超时时间
        ))
        .defaultHeader(HttpHeaders.CACHE_CONTROL, "no-cache")
        .defaultHeader(HttpHeaders.PRAGMA, "no-cache")
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        .build();

    /**
     * 上传图片
     *
     * @param query
     * @param multipartData :
     * @return: reactor.core.publisher.Mono<site.muyin.picturebed.vo.ResultsVO>
     * @author: lywq
     * @date: 2024/05/22 21:33
     **/
    @Override
    public Mono<ResultsVO> uploadImage(CommonQuery query, MultiValueMap<String, ?> multipartData) {
        Map<String, Object> paramMap = new HashMap(1);
        paramMap.put("file", multipartData);
        paramMap.put("categories", query.getCategories());
        return req(query.getPictureBedId(), "upload", paramMap)
            .map(response -> {
                if (response.err == 0) {
                    return ResultsVO.success("上传成功", response.url);
                }
                return ResultsVO.failure(response.msg);
            });
    }

    /**
     * 获取图片列表
     *
     * @param query :
     * @return: reactor.core.publisher.Mono<site.muyin.picturebed.vo.PageResult < site.muyin.picturebed.domain.SmmsImage>>
     * @author: lywq
     * @date: 2024/05/22 21:33
     **/
    @Override
    public Mono<PageResult<JhImage>> getImageList(CommonQuery query) {
        Map<String, Object> paramMap = new HashMap(1);
        paramMap.put("page", query.getPage());
        paramMap.put("size", query.getSize());
        paramMap.put("categories", query.getCategories());
        return req(query.getPictureBedId(), "timeline", paramMap)
            .mapNotNull(response -> {
                List<JhImage> imageList =
                    PictureBedUtil.convertObjectToList(response.docs, JhImage.class);
                if (imageList != null) {
                    return new PageResult<>(response.page, response.size, response.pages,
                        response.total, imageList);
                }
                return null;
            });
    }


    /**
     * 获取图库列表
     *
     * @param query :
     * @return: reactor.core.publisher.Mono<java.util.List < site.muyin.picturebed.domain.LskyProAlbum>>
     * @author: lywq
     * @date: 2024/05/22 21:31
     **/
    @Override
    public Mono<List<JuHeAlbum>> getAlbumList(CommonQuery query) {
        Map<String, Object> paramMap = new HashMap(1);
        return req(query.getPictureBedId(), "categories", paramMap)
            .map(response -> {
                List<JuHeAlbum> albumList = Collections.emptyList();
                if (response.err == 0) {
                    Map<String, Integer> categoriesMap = (Map<String, Integer>) response.categories;
                    albumList = categoriesMap.entrySet().stream()
                        .map(entry -> {
                            JuHeAlbum album = new JuHeAlbum();
                            album.setId(entry.getValue());
                            album.setName(entry.getKey());
                            return album;
                        })
                        .collect(Collectors.toList());
                }
                return albumList;
            });
    }

    /**
     * 删除图片
     *
     * @param query :
     * @return: reactor.core.publisher.Mono<java.lang.Boolean>
     * @author: lywq
     * @date: 2024/05/22 21:33
     **/
    @Override
    public Mono<Boolean> deleteImage(CommonQuery query) {
        Map<String, Object> paramMap = new HashMap(1);
        // 将 imageId 包装成数组添加到 paramMap 中
        paramMap.put("ids", new String[] {query.getImageId()});
        return req(query.getPictureBedId(), "delete", paramMap)
            .map(response -> {
                return response.err == 0;
            });
    }

    private Mono<JhResponseRecord> req(String pictureBedId, String path,
        Map<String, Object> paramMap) {
        if (path == null) {
            return Mono.error(new IllegalArgumentException("Path cannot be null"));
        }

        return settingFetcher.fetch(GROUP, PictureBedConfig.class)
            .flatMap(pictureBedConfig -> {

                PictureBedConfig.PictureBed config = pictureBedConfig.getPictureBeds().stream()
                    .filter(p -> p.getPictureBedType().equals(JUHE) && p.getPictureBedId()
                        .equals(pictureBedId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                        "PictureBed config not found for ID: " + pictureBedId));

                String url = config.getPictureBedUrl();
                String authorization = config.getPictureBedToken();

                WebClient client = webClient.mutate()
                    .defaultHeader("token", authorization)
                    .build();

                switch (path) {
                    case "categories":
                    case "timeline":
                        paramMap.put("token", authorization);
                        paramMap.put("f", "json");
                        String params = PictureBedUtil.convertMapToUrlParams(paramMap);
                        return client.get()
                            .uri(url + path + "?" + params)
                            .retrieve()
                            .bodyToMono(new ParameterizedTypeReference<JhResponseRecord>() {
                            })
                            .doOnError(error -> log.error("GET request failed", error))
                            .onErrorResume(error -> Mono.empty());

                    case "delete":
                        paramMap.put("token", authorization);
                        return client.post()
                            .uri(url + path)
                            .bodyValue(paramMap)
                            .retrieve()
                            .bodyToMono(new ParameterizedTypeReference<JhResponseRecord>() {
                            })
                            .doOnError(error -> log.error("POST request failed", error))
                            .onErrorResume(error -> Mono.empty());

                    case "upload":
                        MultiValueMap<String, ?> multiValueMap =
                            (MultiValueMap<String, ?>) paramMap.get("file");
                        if (multiValueMap == null || multiValueMap.isEmpty()) {
                            return Mono.error(
                                new IllegalArgumentException("File parameter is missing"));
                        }

                        MultiValueMap<String, Object> multipartData = new LinkedMultiValueMap<>();
                        multipartData.add("file", multiValueMap.getFirst("file"));
                        multipartData.add("token", authorization);
                        multipartData.add("categories", paramMap.get("categories"));

                        return client.post()
                            .uri(url + path)
                            .header(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_JPEG_VALUE)
                            .contentType(MediaType.MULTIPART_FORM_DATA)
                            .body(BodyInserters.fromMultipartData(multipartData))
                            .retrieve()
                            .bodyToMono(new ParameterizedTypeReference<JhResponseRecord>() {
                            })
                            .doOnError(error -> log.error("POST request failed", error))
                            .onErrorResume(error -> Mono.empty());

                    default:
                        return Mono.error(
                            new IllegalArgumentException("Unsupported path: " + path));
                }
            })
            .onErrorResume(error -> {
                log.error("Configuration fetch failed", error);
                return Mono.empty();
            });
    }

    @Data
    static class JhImagePageRes {
        Integer page;
        Integer size;
        Integer pages;
        Integer total;
        List<JhImage> docs;
    }

    public record JhResponseRecord(Integer err, String msg, String url, Object docs, Integer page,
                                   Integer size,
                                   Integer pages,
                                   Integer total, Object categories) {
    }
}
