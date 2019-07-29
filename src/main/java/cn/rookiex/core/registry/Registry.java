package cn.rookiex.core.registry;

import cn.rookiex.core.lister.WatchServiceLister;
import cn.rookiex.core.service.Service;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * @Author : Rookiex
 * @Date : 2019/07/03
 * @Describe :
 */
public interface Registry {

    void init(String url);

    void init(String url, String user, String password);

    /**
     * 获得serviceName下的所有服务
     * */
    List<Service> getServiceList(String serviceName);

    /**
     * 获得serviceName下的所有服务,默认开启前缀条件
     * */
    List<Service> getServiceList(String serviceName, boolean usePrefix);

    /**
     * 注册服务
     * */
    void registerService(String serviceName,String ip) throws ExecutionException, InterruptedException, TimeoutException;

    /**
     * 屏蔽服务
     * */
    void bandService(String serviceName,String ip) throws ExecutionException, InterruptedException, TimeoutException;

    /**
     * 监听服务
     * */
    void watch(String serviceName, boolean usePrefix, List<WatchServiceLister> watchList);

    /**
     * 监听服务
     * */
    void watch(String serviceName, boolean usePrefix, WatchServiceLister lister);

    /**
     * 监听服务,默认开启前缀条件
     * */
    void watch(String serviceName,List<WatchServiceLister> watchList);

    /**
     * 取消监听服务
     * */
    void unWatch(String serviceName, boolean usePrefix,List<WatchServiceLister> watchList);

    /**
     * 取消监听服务,默认开启前缀条件
     * */
    void unWatch(String serviceName,List<WatchServiceLister> watchList);

    /**
     * 取消监听服务,默认开启前缀条件
     * */
    void unWatch(String serviceName, boolean usePrefix, WatchServiceLister lister);

    /**
     * 是否活跃
     * */
    boolean isConnected();

    /**
     * 连接
     * */
    void startConnect();

    /**
     * 重连接
     * */
    void reConnect();
}
