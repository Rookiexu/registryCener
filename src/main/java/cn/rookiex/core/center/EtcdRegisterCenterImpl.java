package cn.rookiex.core.center;

import cn.rookiex.core.RegistryConstants;
import cn.rookiex.core.lister.EtcdWatchServiceLister;
import cn.rookiex.core.registry.EtcdRegistryImpl;
import cn.rookiex.core.registry.Registry;
import cn.rookiex.core.service.Service;
import cn.rookiex.core.updateEvent.EtcdServiceUpdateEventImpl;
import cn.rookiex.core.updateEvent.ServiceUpdateEvent;
import com.coreos.jetcd.data.ByteSequence;
import com.coreos.jetcd.data.KeyValue;
import com.coreos.jetcd.watch.WatchEvent;
import com.google.common.collect.Maps;
import org.apache.log4j.Logger;

import java.util.Map;

/**
 * @Author : Rookiex
 * @Date : 2019/07/03
 * @Describe :
 */
public class EtcdRegisterCenterImpl extends BaseRegisterCenterImpl {

    Logger log = Logger.getLogger(getClass());
    private Map<String, Map<String, Service>> errServiceListMap = Maps.newConcurrentMap();

    public EtcdRegisterCenterImpl(Registry registry) {
        super(registry);
        EtcdWatchServiceLister etcdWatchServiceLister = new EtcdWatchServiceLister();
        this.watch(RegistryConstants.WATCH_ALL, etcdWatchServiceLister);
    }

    public EtcdRegisterCenterImpl(Registry registry,boolean needBaseWatchLister) {
        super(registry);
        if (needBaseWatchLister){
            EtcdWatchServiceLister etcdWatchServiceLister = new EtcdWatchServiceLister();
            this.watch(RegistryConstants.WATCH_ALL, etcdWatchServiceLister);
        }
    }

    @Override
    public Service addService(ServiceUpdateEvent event) {
        if (event instanceof EtcdServiceUpdateEventImpl) {
            EtcdServiceUpdateEventImpl serviceUpdateEvent = (EtcdServiceUpdateEventImpl) event;
            WatchEvent.EventType eventType = serviceUpdateEvent.getEventType();
            if (eventType == WatchEvent.EventType.PUT) {
                KeyValue keyValue = serviceUpdateEvent.getKeyValue();
                ByteSequence key = keyValue.getKey();
                ByteSequence value = keyValue.getValue();
                long lease = keyValue.getLease();
                String keyS = key.toStringUtf8();
                long version = keyValue.getVersion();
                Service service = factory.getService(keyS, value.toStringUtf8().equals(EtcdRegistryImpl.BAN), lease, version);
                if (service != null)
                    addService(service);
                return service;
            }
        }
        return null;
    }

    /**
     * 删除服务节点
     *
     * @param service
     */
    @Override
    public void removeService(Service service) {
        if (!service.isActive() && NEED_REMOVE_DELETE_CHILD) {
            Map<String, Service> serviceMap = serviceMapMap.get(service.getServiceName());
            if (serviceMap != null) {
                serviceMap.remove(service.getFullPath());
            }
        }
    }

    @Override
    public void checkServiceState() {
        for (String serviceName : serviceMapMap.keySet()) {
            Map<String, Service> errServiceMap = errServiceListMap.computeIfAbsent(serviceName, k -> Maps.newConcurrentMap());
            Map<String, Service> serviceMap = serviceMapMap.get(serviceName);
            if (serviceMap != null) {
                serviceMap.values().forEach(service -> {
                    if (!service.isActive() || service.isBanned()) {
                        errServiceMap.put(service.getFullPath(), service);
                    }
                });
            }
        }
    }
}
