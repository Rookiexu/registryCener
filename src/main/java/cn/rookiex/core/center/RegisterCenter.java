package cn.rookiex.core.center;

import cn.rookiex.core.lister.WatchServiceLister;
import cn.rookiex.core.service.Service;
import cn.rookiex.core.updateEvent.ServiceUpdateEvent;

import java.util.List;

/**
 * @Author : Rookiex
 * @Date : 2019/07/03
 * @Describe :
 */
public interface RegisterCenter {
    /**
     * 服务注册
     * */
    void register(String serviceName, String ip);

    /**
     * 服务注册取消
     * */
    void unRegister(String serviceName, String ip);

    /**
     * 订阅服务
     * */
    void watch(String serviceName,boolean usePrefix);

    void watch(String serviceName, List<WatchServiceLister> listerList);

    void watch(String serviceName, boolean usePrefix, WatchServiceLister lister);

    void watch(String serviceName, WatchServiceLister lister);

    void watch(String serviceName, boolean usePrefix, List<WatchServiceLister> listerList);

    /**
     * 订阅服务,默认使用前缀
     * */
    void watch(String serviceName);

    /**
     * 取消订阅服务
     * */
    void unWatchAll(String serviceName, boolean usePrefix);

    void unWatch(String serviceName, List<WatchServiceLister> listerList);

    void unWatch(String serviceName, boolean usePrefix, WatchServiceLister lister);

    void unWatch(String serviceName, WatchServiceLister lister);

    void unWatch(String serviceName, boolean usePrefix, List<WatchServiceLister> listerList);

    /**
     * 取消订阅服务,默认使用前缀
     * */
    void unWatchAll(String serviceName);

    /**
     * 获得随机的service节点
     * */
    Service getRandomService(String serviceName);

    /**
     * 获得所有的service节点
     * */
    List<Service> getServiceList(String serviceName);

    List<Service> getServiceListWithUnWork(String serviceName);

    /**
     * 添加service节点
     * */
    void addService(Service service);

    /**
     * 删除服务节点
     * */
    void removeService(Service service);

    void checkServiceState();

    Service addService(ServiceUpdateEvent event);

    /**
     * 初始化关注的服务
     * */
    void initService(String serviceName,boolean usePrefix);

    /**
     * 初始化关注的服务,默认使用前缀
     * */
    void initService(String serviceName);
}
