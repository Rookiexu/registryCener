package cn.rookiex.core.updateEvent;

/**
 * @Author : Rookiex
 * @Date : 2019/07/04
 * @Describe :
 */
public interface ServiceUpdateEvent {
    String getServiceName();

    void setServiceName(String serviceName);

    String getFullPath();

    void setFullPath(String fullPath);
}
