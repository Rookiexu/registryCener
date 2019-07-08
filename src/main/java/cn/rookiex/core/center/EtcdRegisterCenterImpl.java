package cn.rookiex.core.center;

import cn.rookiex.core.registry.EtcdRegistryImpl;
import cn.rookiex.core.registry.Registry;
import cn.rookiex.core.service.Service;
import cn.rookiex.core.updateEvent.EtcdServiceUpdateEventImpl;
import cn.rookiex.core.updateEvent.ServiceUpdateEvent;
import com.coreos.jetcd.data.ByteSequence;
import com.coreos.jetcd.data.KeyValue;
import com.coreos.jetcd.watch.WatchEvent;
import com.google.common.collect.Maps;
import util.log.LogFactory;
import util.log.Logger;

import java.util.Map;

/**
 * @Author : Rookiex
 * @Date : 2019/07/03
 * @Describe :
 */
public class EtcdRegisterCenterImpl extends BaseRegisterCenterImpl {

    Logger log = LogFactory.getLogger(getClass());
    private Map<String, Map<String, Service>> errServiceListMap = Maps.newConcurrentMap();
    public EtcdRegisterCenterImpl(Registry registry) {
        super(registry);
    }

    @Override
    public void addService(ServiceUpdateEvent event) {
        if (event instanceof EtcdServiceUpdateEventImpl) {
            EtcdServiceUpdateEventImpl serviceUpdateEvent = (EtcdServiceUpdateEventImpl) event;
            WatchEvent.EventType eventType = serviceUpdateEvent.getEventType();
            if (eventType == WatchEvent.EventType.PUT) {
                KeyValue keyValue = serviceUpdateEvent.getKeyValue();
                ByteSequence key = keyValue.getKey();
                ByteSequence value = keyValue.getValue();
                long lease = keyValue.getLease();
                String keyS = key.toStringUtf8();
                Service service = factory.getService(keyS, value.toStringUtf8().equals(EtcdRegistryImpl.BAN), lease);
                if (service != null)
                    addService(service);
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
                        serviceMap.remove(service.getFullPath());
                        errServiceMap.put(service.getFullPath(), service);
                    }
                });
            }
        }
    }
}
