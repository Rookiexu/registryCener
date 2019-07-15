package cn.rookiex.core.factory;

import cn.rookiex.core.service.EtcdServiceImpl;
import cn.rookiex.core.service.Service;

/**
 * @Author : Rookiex
 * @Date : 2019/07/08
 * @Describe :
 */
public class EtcdServiceFactoryImpl implements ServiceFactory {
    @Override
    public Service getService(String fullPath, boolean isBanned, long lease,long version) {
        return new EtcdServiceImpl(fullPath,isBanned,lease, version);
    }
}
