package com.demo.common.api;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 业务错误码集中表。
 * 区段：1xxx 通用业务、2xxx 鉴权（预留）、9xxx 系统。
 */
@Getter
@AllArgsConstructor
public enum ErrorCode {

    GAME_NOT_FOUND      (1001, "游戏不存在或已过期"),
    INVALID_COORDINATES (1002, "坐标越界"),
    INVALID_MINES       (1003, "雷数配置非法"),
    INVALID_PARAMETER   (1004, "请求参数非法"),

    INTERNAL_ERROR      (9000, "服务器内部错误");

    private final int code;
    private final String msg;
}
