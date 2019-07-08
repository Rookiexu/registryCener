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
 * @Date : 2019/07/08
 * @Describe :
 */
public abstract class BaseServiceImpl implements Service {
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

    public BaseServiceImpl(String fullPath, boolean banned, long lease) {
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
     * path
     */
    @Override
    public String getFullPath() {
        return this.fullPath;
    }

    /**
     * @param fullPath f
     */
    @Override
    public void setFullPath(String fullPath) {
        this.fullPath = fullPath;
    }

}
