package cn.rookiex.core.service;

import cn.rookiex.core.RegistryConstants;
import cn.rookiex.core.center.RegisterCenter;
import cn.rookiex.core.updateEvent.EtcdServiceUpdateEventImpl;
import cn.rookiex.core.updateEvent.ServiceUpdateEvent;
import com.coreos.jetcd.data.KeyValue;
import com.coreos.jetcd.watch.WatchEvent;
import util.log.LogFactory;
import util.log.Logger;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * @Author : Rookiex
 * @Date : 2019/07/03
 * @Describe :
 */
public class EtcdServiceImpl implements Service {

    Logger log = LogFactory.getLogger(getClass());

    private String fullPath;
    private String serviceName;
    private String path;
    private long lease;
    private boolean banned;


    public static ScheduledExecutorService executorService;

    public static void initExecutorService(int size){
        executorService = Executors.newScheduledThreadPool(size);
    }

    public EtcdServiceImpl(String fullPath, boolean banned, long lease) {
        String[] split = fullPath.split(RegistryConstants.SEPARATOR);
        this.fullPath = fullPath;
        this.serviceName = split[0];
        this.banned = banned;
        this.lease = lease;
        this.path = split[1];
    }

    public void init() {

    }

    /**
     * 获得服务名称
     */
    @Override
    public String getServiceName() {
        return serviceName;
    }

    /**
     * 保存服务名称
     */
    @Override
    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    /**
     * 服务是否被屏蔽
     */
    @Override
    public boolean isBanned() {
        return banned;
    }

    /**
     * 设置服务器的屏蔽状态
     *
     * @param banned
     */
    @Override
    public void setBanned(boolean banned) {
        this.banned = banned;
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
     * @param event
     * @param registerCenter
     */
    @Override
    public void update(ServiceUpdateEvent event, RegisterCenter registerCenter) {
        if (event instanceof EtcdServiceUpdateEventImpl) {
            EtcdServiceUpdateEventImpl updateEvent = (EtcdServiceUpdateEventImpl) event;
            WatchEvent.EventType eventType = updateEvent.getEventType();
            KeyValue keyValue = updateEvent.getKeyValue();
            switch (eventType) {
                case DELETE:
                    dealPutService(keyValue, registerCenter);
                    break;
                case PUT:
                    dealDeleteService(keyValue, registerCenter);
                    break;
                default:
                    log.warn("etcd watch event err ==> " + keyValue.getKey().toStringUtf8());
            }
        }
    }

    /**
     * path
     */
    @Override
    public String getFullPath() {
        return this.fullPath;
    }

    /**
     * @param fullPath
     */
    @Override
    public void setFullPath(String fullPath) {
        this.fullPath = fullPath;
    }

    /**
     * 启动方法,长连接的话就是创建长连接,默认为抽象方法,需要子类实现具体的业务逻辑
     */
    @Override
    public void start() {

    }

    /**
     * 处理删除节点时间,默认为抽象方法,需要子类实现具体的业务逻辑
     *
     * @param keyValue
     * @param registerCenter
     */
    @Override
    public void dealDeleteService(KeyValue keyValue, RegisterCenter registerCenter) {

    }

    /**
     * 处理put节点时间,默认为抽象方法,需要子类实现具体的业务逻辑
     *
     * @param keyValue
     * @param registerCenter
     */
    @Override
    public void dealPutService(KeyValue keyValue, RegisterCenter registerCenter) {

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

//    @Override
//    public void dealDeleteService(KeyValue keyValue, RegisterCenter registerCenter) {
//        String s = keyValue.getKey().toStringUtf8();
//        if (!s.equals(fullPath)) {
//            return;
//        }
//        String value = keyValue.getValue().toStringUtf8();
//        //检查新value是不是屏蔽,如果是屏蔽就把状态改成屏蔽
//        //如果不是屏蔽,
//    }
//
//    @Override
//    public void dealPutService(KeyValue keyValue, RegisterCenter registerCenter) {
//
//    }
}
