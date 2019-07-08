package cn.rookiex.core.factory;

import cn.rookiex.core.service.Service;

/**
 * @Author : Rookiex
 * @Date : 2019/07/08
 * @Describe :
 */
public interface ServiceFactory {
    Service getService(String fullPath, boolean isBanned, long lease);
}
