package cn.rookiex.core.lister;

import cn.rookiex.core.center.BaseRegisterCenterImpl;
import cn.rookiex.core.service.Service;
import cn.rookiex.core.updateEvent.ServiceUpdateEvent;

import java.util.List;
import java.util.Map;

/**
 * @Author : Rookiex
 * @Date : 2019/07/15
 * @Describe :
 */
public class EtcdWatchServiceLister implements WatchServiceLister {

    private BaseRegisterCenterImpl baseRegisterCenter;

    @Override
    public void watchCallback(List<ServiceUpdateEvent> updateEvents) {
        updateEvents.forEach(event -> {
            Map<String, Service> stringServiceMap = baseRegisterCenter.getServiceMapMap().get(event.getServiceName());
            Service service1 = null;
            if (stringServiceMap != null)
                service1 = stringServiceMap.get(event.getFullPath());

            if (service1 == null) {
                service1 = baseRegisterCenter.addService(event);
            } else {
                service1.update(event, baseRegisterCenter);
            }
            baseRegisterCenter.serverChange(service1);
        });
    }

    public void setBaseRegisterCenter(BaseRegisterCenterImpl baseRegisterCenter) {
        this.baseRegisterCenter = baseRegisterCenter;
    }
}
