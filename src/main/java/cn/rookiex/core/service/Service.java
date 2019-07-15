package cn.rookiex.core.service;

import cn.rookiex.core.center.RegisterCenter;
import cn.rookiex.core.updateEvent.ServiceUpdateEvent;
import com.coreos.jetcd.data.KeyValue;

/**
 * @Author : Rookiex
 * @Date : 2019/07/03
 * @Describe :
 */
public interface Service {
    /**
     * 获得服务名称
     * */
    String getServiceName();

    /**
     * 保存服务名称
     * */
    void setServiceName(String serviceName);

    /**
     * 服务是否被屏蔽
     * */
    boolean isBanned();

    /**
     * 设置服务器的屏蔽状态
     * */
    void setBanned(boolean banned);

    /**
     * 重连服务器
     * */
    void reConnect();

    /**
     * 服务状态变更event
     * */
    void update(ServiceUpdateEvent event, RegisterCenter registerCenter);

    /**
     * path
     * */
    String getFullPath();

    /**
     *
     * */
    void setFullPath(String fullPath);

    /**
     * 启动方法,长连接的话就是创建长连接,默认为抽象方法,需要子类实现具体的业务逻辑
     * */
    void start();

    /**
     * 处理删除节点时间,默认为抽象方法,需要子类实现具体的业务逻辑
     * */
    void dealDeleteService(KeyValue keyValue, long version, RegisterCenter registerCenter);

    /**
     * 处理put节点时间,默认为抽象方法,需要子类实现具体的业务逻辑
     * */
    void dealPutService(KeyValue keyValue, long version, RegisterCenter registerCenter);

    /**
     * 服务是否正常
     * */
    boolean isActive();

    /**
     * 服务是否正常
     * */
    boolean isDelete();
}
