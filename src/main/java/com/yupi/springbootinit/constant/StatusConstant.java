package com.yupi.springbootinit.constant;

/**
 * 状态常量
 */
public interface StatusConstant {

    /**
     * 排队等待
     */
    String WAIT = "wait";
    /**
     * 正在运行
     */
    String RUNNING = "running";
    /**
     * 生成成功
     */
    String SUCCEED = "succeed";
    /**
     * 生成失败
     */
    String FAILED = "failed";
}
