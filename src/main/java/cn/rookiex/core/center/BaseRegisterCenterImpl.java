package cn.rookiex.core.center;

import cn.rookiex.core.RegistryConstants;
import cn.rookiex.core.factory.ServiceFactory;
import cn.rookiex.core.lister.WatchServiceLister;
import cn.rookiex.core.lister.ServiceChangeLister;
import cn.rookiex.core.registry.Registry;
import cn.rookiex.core.service.Service;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.log4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
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

    Set<ServiceChangeLister> serviceChangeListers = Sets.newConcurrentHashSet();

    Map<String, List<WatchServiceLister>> watchListMap = Maps.newConcurrentMap();

    @Override
    public void register(String serviceName, String ip) {
        try {
            registry.registerService(serviceName, ip);
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
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
            if (!serviceName.endsWith(RegistryConstants.SEPARATOR) && !serviceName.equals(RegistryConstants.WATCH_ALL)) {
                serviceName = serviceName + RegistryConstants.SEPARATOR;
            }
            registry.bandService(serviceName, ip);
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void watch(String serviceName, boolean usePrefix) {
        if (!serviceName.endsWith(RegistryConstants.SEPARATOR) && !serviceName.equals(RegistryConstants.WATCH_ALL)) {
            serviceName = serviceName + RegistryConstants.SEPARATOR;
        }
        registry.watch(serviceName, usePrefix, getWatchServiceListers(serviceName));
    }

    /**
     * 订阅服务,默认使用前缀
     *
     * @param serviceName
     */
    @Override
    public void watch(String serviceName, List<WatchServiceLister> listerList) {
        this.watch(serviceName, true, listerList);
    }

    @Override
    public void watch(String serviceName, boolean usePrefix, WatchServiceLister lister) {
        if (!serviceName.endsWith(RegistryConstants.SEPARATOR) && !serviceName.equals(RegistryConstants.WATCH_ALL)) {
            serviceName = serviceName + RegistryConstants.SEPARATOR;
        }
        registry.watch(serviceName, usePrefix, lister);
    }

    /**
     * 订阅服务,默认使用前缀
     *
     * @param serviceName
     */
    @Override
    public void watch(String serviceName, WatchServiceLister lister) {
        this.watch(serviceName, true, lister);
    }

    @Override
    public void watch(String serviceName, boolean usePrefix, List<WatchServiceLister> listerList) {
        if (!serviceName.endsWith(RegistryConstants.SEPARATOR) && !serviceName.equals(RegistryConstants.WATCH_ALL)) {
            serviceName = serviceName + RegistryConstants.SEPARATOR;
        }
        registry.watch(serviceName, usePrefix, listerList);
    }

    /**
     * 订阅服务,默认使用前缀
     *
     * @param serviceName
     */
    @Override
    public void watch(String serviceName) {
        this.watch(serviceName, true);
    }

    @Override
    public void unWatchAll(String serviceName, boolean usePrefix) {
        if (!serviceName.endsWith(RegistryConstants.SEPARATOR) && !serviceName.equals(RegistryConstants.WATCH_ALL)) {
            serviceName = serviceName + RegistryConstants.SEPARATOR;
        }
        registry.unWatch(serviceName, usePrefix, getWatchServiceListers(serviceName));
    }

    /**
     * 取消订阅服务,默认使用前缀
     *
     * @param serviceName
     */
    @Override
    public void unWatch(String serviceName, List<WatchServiceLister> listerList) {
        this.unWatch(serviceName, true, listerList);
    }

    @Override
    public void unWatch(String serviceName, boolean usePrefix, WatchServiceLister lister) {
        if (!serviceName.endsWith(RegistryConstants.SEPARATOR) && !serviceName.equals(RegistryConstants.WATCH_ALL)) {
            serviceName = serviceName + RegistryConstants.SEPARATOR;
        }
        registry.unWatch(serviceName, usePrefix, lister);
    }

    /**
     * 取消订阅服务,默认使用前缀
     *
     * @param serviceName
     */
    @Override
    public void unWatch(String serviceName, WatchServiceLister lister) {
        this.unWatch(serviceName, true, lister);
    }

    @Override
    public void unWatch(String serviceName, boolean usePrefix, List<WatchServiceLister> listerList) {
        if (!serviceName.endsWith(RegistryConstants.SEPARATOR) && !serviceName.equals(RegistryConstants.WATCH_ALL)) {
            serviceName = serviceName + RegistryConstants.SEPARATOR;
        }
        registry.unWatch(serviceName, usePrefix, listerList);
    }

    /**
     * 取消订阅服务,默认使用前缀
     *
     * @param serviceName
     */
    @Override
    public void unWatchAll(String serviceName) {
        this.unWatchAll(serviceName, true);
    }

    @Override
    public Service getRandomService(String serviceName) {
        if (!serviceName.endsWith(RegistryConstants.SEPARATOR) && !serviceName.equals(RegistryConstants.WATCH_ALL)) {
            serviceName = serviceName + RegistryConstants.SEPARATOR;
        }
        Map<String, Service> stringServiceMap = serviceMapMap.get(serviceName);
        if (stringServiceMap == null) {
            return null;
        }
        return stringServiceMap.values().stream().filter(service -> !service.isBanned() && service.isActive()).findAny().orElse(null);
    }

    @Override
    public List<Service> getServiceList(String serviceName) {
        if (!serviceName.endsWith(RegistryConstants.SEPARATOR) && !serviceName.equals(RegistryConstants.WATCH_ALL)) {
            serviceName = serviceName + RegistryConstants.SEPARATOR;
        }
        Map<String, Service> stringServiceMap = serviceMapMap.get(serviceName);
        return stringServiceMap != null ? Lists.newArrayList(stringServiceMap.values()
                .stream().filter(service -> !service.isBanned() && service.isActive()).collect(Collectors.toList())
        ) : Lists.newArrayList();
    }

    @Override
    public List<Service> getServiceListWithUnWork(String serviceName) {
        if (!serviceName.endsWith(RegistryConstants.SEPARATOR) && !serviceName.equals(RegistryConstants.WATCH_ALL)) {
            serviceName = serviceName + RegistryConstants.SEPARATOR;
        }
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

    public ServiceFactory getFactory() {
        return factory;
    }

    public static void setFactory(ServiceFactory factory) {
        BaseRegisterCenterImpl.factory = factory;
    }

    @Override
    public void initService(String serviceName, boolean usePrefix) {
        if (!serviceName.endsWith(RegistryConstants.SEPARATOR) && !serviceName.equals(RegistryConstants.WATCH_ALL)) {
            serviceName = serviceName + RegistryConstants.SEPARATOR;
        }
        List<Service> serviceList = registry.getServiceList(serviceName);
        for (Service service : serviceList) {
            addService(service);
            serverChange(service);
        }
        registry.watch(serviceName, usePrefix, getWatchServiceListers(serviceName));
    }

    /**
     * 初始化关注的服务,默认使用前缀
     *
     * @param serviceName
     */
    @Override
    public void initService(String serviceName) {
        this.initService(serviceName, true);
    }

    public void addWatchServiceLister(String serviceName, WatchServiceLister lister) {
        List<WatchServiceLister> objects = Lists.newCopyOnWriteArrayList();
        objects.add(lister);
        this.watchListMap.merge(serviceName, objects, (oldV, newV) -> {
            oldV.addAll(newV);
            return oldV;
        });
        if (serviceName.equals(RegistryConstants.WATCH_ALL)) {
            watchListMap.keySet().forEach(s -> this.watch(s, lister));
        } else {
            this.watch(serviceName, lister);
        }
    }

    public void removeWatchServiceLister(String serviceName, WatchServiceLister lister) {
        this.watchListMap.forEach((k, v) -> {
            if (serviceName.equals(RegistryConstants.WATCH_ALL)) {
                v.remove(lister);
                unWatch(k, lister);
            } else if (serviceName.equals(k)) {
                v.remove(lister);
                unWatch(k, lister);
            }
        });
    }

    public void addCenterLister(ServiceChangeLister lister) {
        this.serviceChangeListers.add(lister);
    }

    public void removeCenterLister(ServiceChangeLister lister) {
        this.serviceChangeListers.remove(lister);
    }

    public Map<String, Map<String, Service>> getServiceMapMap() {
        return serviceMapMap;
    }

    public List<WatchServiceLister> getWatchServiceListers(String serviceName) {
        List<WatchServiceLister> objects = Lists.newCopyOnWriteArrayList();
        List<WatchServiceLister> watchServiceListers = watchListMap.get(serviceName);
        List<WatchServiceLister> watchServiceListers1 = watchListMap.get(RegistryConstants.WATCH_ALL);
        if (watchServiceListers != null) {
            objects.addAll(watchServiceListers);
        }
        if (watchServiceListers1 != null) {
            objects.addAll(watchServiceListers1);
        }
        return objects;
    }

    public void serverChange(Service service) {
        if (service != null)
            this.serviceChangeListers.forEach(serviceChangeLister -> serviceChangeLister.serviceUpdate(service));
    }
}
