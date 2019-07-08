package cn.rookiex.core.registry;

import cn.rookiex.core.center.RegisterCenter;
import cn.rookiex.core.service.Service;

import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * @Author : Rookiex
 * @Date : 2019/07/03
 * @Describe :
 */
public interface Registry {

    /**
     * 获得serviceName下的所有服务
     * */
    List<Service> getServiceList(String serviceName);

    /**
     * 注册服务
     * */
    void registerService(String serviceName,String ip) throws ExecutionException, InterruptedException;

    /**
     * 屏蔽服务
     * */
    void bandService(String serviceName,String ip) throws ExecutionException, InterruptedException;

    /**
     * 监听服务
     * */
    void watch(String serviceName, RegisterCenter center);

    /**
     * 取消监听服务
     * */
    void unWatch(String serviceName);
}