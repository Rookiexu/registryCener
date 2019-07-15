package cn.rookiex.core.service;

import cn.rookiex.core.center.RegisterCenter;
import cn.rookiex.core.registry.EtcdRegistryImpl;
import cn.rookiex.core.updateEvent.EtcdServiceUpdateEventImpl;
import cn.rookiex.core.updateEvent.ServiceUpdateEvent;
import com.coreos.jetcd.data.KeyValue;
import com.coreos.jetcd.watch.WatchEvent;
import org.apache.log4j.Logger;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * @Author : Rookiex
 * @Date : 2019/07/03
 * @Describe :
 */
public class EtcdServiceImpl extends BaseServiceImpl {

    Logger log = Logger.getLogger(getClass());

    public static ScheduledExecutorService executorService;

    public static void initExecutorService(int size) {
        executorService = Executors.newScheduledThreadPool(size);
    }

    public EtcdServiceImpl(String fullPath, boolean banned, long lease,long version) {
        super(fullPath, banned, lease,version);
    }

    public void init() {

    }

    /**
     * 重连服务器
     */
    @Override
    public void reConnect() {

    }

    /**
     * 服务状态变更event
     *
     * @param event          e
     * @param registerCenter r
     */
    @Override
    public void update(ServiceUpdateEvent event, RegisterCenter registerCenter) {
        if (event instanceof EtcdServiceUpdateEventImpl) {
            EtcdServiceUpdateEventImpl updateEvent = (EtcdServiceUpdateEventImpl) event;
            WatchEvent.EventType eventType = updateEvent.getEventType();
            long version = updateEvent.getVersion();
            KeyValue keyValue = updateEvent.getKeyValue();
            switch (eventType) {
                case PUT:
                    dealPutService(keyValue, version,registerCenter);
                    break;
                case DELETE:
                    dealDeleteService(keyValue,version, registerCenter);
                    break;
                default:
                    log.warn("etcd watch event err ==> " + keyValue.getKey().toStringUtf8());
            }
        }
    }

    /**
     * 启动方法,长连接的话就是创建长连接,默认为抽象方法,需要子类实现具体的业务逻辑
     */
    @Override
    public void start() {

    }

    /**
     * 处理删除节点时间,默认为抽象方法,需要子类实现具体的业务逻辑
     *  @param keyValue       k
     * @param version
     * @param registerCenter r
     */
    @Override
    public void dealDeleteService(KeyValue keyValue, long version, RegisterCenter registerCenter) {
        if (version > getVersion()) {
            String s = keyValue.getKey().toStringUtf8();
            if (!s.equals(getFullPath())) {
                return;
            }
            setDelete(true);
            registerCenter.removeService(this);
        }
    }

    /**
     * 处理put节点时间,默认为抽象方法,需要子类实现具体的业务逻辑
     *  @param keyValue       k
     * @param version
     * @param registerCenter r
     */
    @Override
    public void dealPutService(KeyValue keyValue, long version, RegisterCenter registerCenter) {
        if (version > getVersion()) {
            String s = keyValue.getKey().toStringUtf8();
            if (!s.equals(getFullPath())) {
                return;
            }
            setDelete(false);
            setBanned(keyValue.getValue().toStringUtf8().equals(EtcdRegistryImpl.BAN));
            registerCenter.addService(this);
        }
    }

    /**
     * 服务是否正常
     */
    @Override
    public boolean isActive() {
        return false;
    }


    private void checkState(EtcdServiceImpl etcdService) {
//        if (connect.isActive() && isBanned())
    }
}
