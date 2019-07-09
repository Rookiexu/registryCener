package cn.rookiex.core.center;

import cn.rookiex.core.factory.ServiceFactory;
import cn.rookiex.core.registry.Registry;
import cn.rookiex.core.service.Service;
import cn.rookiex.core.updateEvent.ServiceUpdateEvent;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.log4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * @Author : Rookiex
 * @Date : 2019/07/08
 * @Describe :
 */
public abstract class BaseRegisterCenterImpl implements RegisterCenter {
    Logger log = Logger.getLogger(getClass());

    public static boolean NEED_REMOVE_DELETE_CHILD = false;

    private Registry registry;

    public static ServiceFactory factory;

    BaseRegisterCenterImpl(Registry registry) {
        this.registry = registry;
    }

    Map<String, Map<String, Service>> serviceMapMap = Maps.newConcurrentMap();

    @Override
    public void register(String serviceName, String ip) {
        try {
            registry.registerService(serviceName, ip);
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 服务注册取消
     *
     * @param serviceName s
     * @param ip          i
     */
    @Override
    public void unRegister(String serviceName, String ip) {
        try {
            registry.bandService(serviceName, ip);
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void watch(String serviceName,boolean usePrefix) {
        registry.watch(serviceName, usePrefix, this);
    }

    /**
     * 订阅服务,默认使用前缀
     *
     * @param serviceName
     */
    @Override
    public void watch(String serviceName) {
        this.watch(serviceName,true);
    }

    @Override
    public void unWatch(String serviceName,boolean usePrefix) {
        registry.unWatch(serviceName, usePrefix);
    }

    /**
     * 取消订阅服务,默认使用前缀
     *
     * @param serviceName
     */
    @Override
    public void unWatch(String serviceName) {
        this.unWatch(serviceName,true);
    }

    @Override
    public Service getRandomService(String serviceName) {
        Map<String, Service> stringServiceMap = serviceMapMap.get(serviceName);
        if (stringServiceMap == null) {
            return null;
        }
        return stringServiceMap.values().stream().filter(service -> !service.isBanned() && service.isActive()).findAny().orElse(null);
    }

    @Override
    public List<Service> getServiceList(String serviceName) {
        Map<String, Service> stringServiceMap = serviceMapMap.get(serviceName);
        return stringServiceMap != null ? Lists.newArrayList(stringServiceMap.values()
                .stream().filter(service -> !service.isBanned() && service.isActive()).collect(Collectors.toList())
        ) : Lists.newArrayList();
    }

    @Override
    public List<Service> getServiceListWithUnWork(String serviceName) {
        Map<String, Service> stringServiceMap = serviceMapMap.get(serviceName);
        return stringServiceMap != null ? Lists.newArrayList(stringServiceMap.values()) : Lists.newArrayList();
    }

    @Override
    public void addService(Service service) {
        String serviceName = service.getServiceName();
        Map<String, Service> serviceMap = serviceMapMap.computeIfAbsent(serviceName, k -> Maps.newConcurrentMap());
        serviceMap.put(service.getFullPath(), service);
        service.start();
    }

    /**
     * 处理订阅消息发来的连接
     *
     * @param events
     */
    @Override
    public void callback(List<ServiceUpdateEvent> events) {
        events.forEach(event -> {
            Map<String, Service> stringServiceMap = serviceMapMap.get(event.getServiceName());
            Service service1 = null;
            if (stringServiceMap != null)
                service1 = stringServiceMap.get(event.getFullPath());

            if (service1 == null) {
                addService(event);
            } else {
                service1.update(event, this);
            }
        });
    }

    public ServiceFactory getFactory() {
        return factory;
    }

    public static void setFactory(ServiceFactory factory) {
        BaseRegisterCenterImpl.factory = factory;
    }

    @Override
    public void initService(String serviceName,boolean usePrefix){
        List<Service> serviceList = registry.getServiceList(serviceName, usePrefix);
        serviceList.forEach(this::addService);
    }

    /**
     * 初始化关注的服务,默认使用前缀
     *
     * @param serviceName
     */
    @Override
    public void initService(String serviceName) {
        this.initService(serviceName,true);
    }
}
