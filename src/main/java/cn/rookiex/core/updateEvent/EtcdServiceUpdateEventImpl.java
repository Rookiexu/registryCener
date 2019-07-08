package cn.rookiex.core.updateEvent;

import com.coreos.jetcd.data.KeyValue;
import com.coreos.jetcd.watch.WatchEvent;

/**
 * @Author : Rookiex
 * @Date : 2019/07/04
 * @Describe :
 */
public class EtcdServiceUpdateEventImpl implements ServiceUpdateEvent {
    private WatchEvent.EventType eventType;
    private KeyValue keyValue;
    private KeyValue prevKV;
    private String serviceName;
    private String fullPath;

    public WatchEvent.EventType getEventType() {
        return eventType;
    }

    public void setEventType(WatchEvent.EventType eventType) {
        this.eventType = eventType;
    }

    public KeyValue getKeyValue() {
        return keyValue;
    }

    public void setKeyValue(KeyValue keyValue) {
        this.keyValue = keyValue;
    }

    public KeyValue getPrevKV() {
        return prevKV;
    }

    public void setPrevKV(KeyValue prevKV) {
        this.prevKV = prevKV;
    }

    @Override
    public String getServiceName() {
        return serviceName;
    }

    @Override
    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    @Override
    public String getFullPath() {
        return fullPath;
    }

    @Override
    public void setFullPath(String fullPath) {
        this.fullPath = fullPath;
    }
}
