// IServiceInterface.aidl
package com.zhupeng.location;
import com.zhupeng.location.IResultListener;

// Declare any non-default types here with import statements

interface IServiceInterface {
    //添加结果监听接口
    void addResultListener(in IResultListener listener);
    //移除指定结果监听接口
    void removeResultListener(in IResultListener listener);
    //移除所有结果监听接口
    void removeAllResultListeners();
}
