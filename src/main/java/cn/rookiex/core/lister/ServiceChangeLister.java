package cn.rookiex.core.lister;

import cn.rookiex.core.service.Service;

/**
 * @Author : Rookiex
 * @Date : 2019/07/15
 * @Describe :
 */
public interface ServiceChangeLister {
    void serviceUpdate(Service service);
}
