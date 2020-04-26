package com.zhupeng.location;

/**
 * 定时任务策略静态工厂类
 */
public class StrategyFactory {

    public static Strategy create() {
        return new BDLocationStrategy();
    }
}
