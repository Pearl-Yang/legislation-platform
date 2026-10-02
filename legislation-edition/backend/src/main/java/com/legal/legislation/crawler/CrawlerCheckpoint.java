package com.legal.legislation.crawler;

import com.legal.legislation.entity.Regulation;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 一次爬取任务的状态。
 *
 * 由 CrawlerCheckpointStore 持久化,实现"断点续抓"。
 * 字段:
 *  - source       :  数据源标识,例如 "gov.cn/zhengce/2024"
 *  - lastUrl      :  上次成功抓取的 URL(下次从这里开始)
 *  - lastPageNo   :  列表页的页码
 *  - totalCount   :  已累计入库的法规数
 *  - status       :  PENDING / RUNNING / PAUSED / SUCCESS / FAILED
 *  - errorMessage :  上次失败原因
 *  - updatedAt    :  状态更新时间
 */
@Data
@Builder
public class CrawlerCheckpoint {
    private String        source;
    private String        lastUrl;
    private int           lastPageNo;
    private int           totalCount;
    private String        status;
    private String        errorMessage;
    private LocalDateTime updatedAt;
}
