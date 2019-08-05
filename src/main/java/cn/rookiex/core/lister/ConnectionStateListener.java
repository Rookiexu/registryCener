package cn.rookiex.core.lister;

import cn.rookiex.core.registry.EtcdRegistryImpl;
import cn.rookiex.core.registry.Registry;
import com.coreos.jetcd.Client;

/**
 * @Author : Rookiex
 * @Date : 2019/08/05
 * @Describe :
 */
public interface ConnectionStateListener {
    /**
     * Called when there is a state change in the connection
     *  @param client   the client
     * @param newState the new state
     */
    void stateChanged(Registry client, int newState);
}
