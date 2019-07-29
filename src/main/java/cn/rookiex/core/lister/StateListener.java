package cn.rookiex.core.lister;

/**
 * @Author : Rookiex
 * @Date : 2019/07/29
 * @Describe :
 */
public interface StateListener {
    int DISCONNECTED = 0;

    int CONNECTED = 1;

    void stateChanged(int connected);
}
