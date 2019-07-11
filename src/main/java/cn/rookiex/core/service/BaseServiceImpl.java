package cn.rookiex.core.service;

import cn.rookiex.core.RegistryConstants;
import org.apache.log4j.Logger;

/**
 * @Author : Rookiex
 * @Date : 2019/07/08
 * @Describe :
 */
public abstract class BaseServiceImpl  implements Service  {
    Logger log = Logger.getLogger(getClass());

    private String fullPath;
    private String serviceName;
    private String path;
    private long lease;
    private boolean banned;
    private boolean delete;

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
     * 服务是否正常
     */
    @Override
    public boolean isDelete() {
        return this.delete;
    }

    public void setDelete(boolean delete){
        this.delete = delete;
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

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public long getLease() {
        return lease;
    }

    public void setLease(long lease) {
        this.lease = lease;
    }
}
