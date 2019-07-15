package cn.rookiex.core.lister;

import cn.rookiex.core.updateEvent.ServiceUpdateEvent;

import java.util.List; /**
 * @Author : Rookiex
 * @Date : 2019/07/15
 * @Describe :
 */
public interface WatchServiceLister {
    void watchCallback(List<ServiceUpdateEvent> updateEvents);
}