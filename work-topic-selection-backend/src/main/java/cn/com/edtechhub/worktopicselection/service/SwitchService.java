package cn.com.edtechhub.worktopicselection.service;

/**
 * 系统功能开关服务接口
 *
 * @author wobushi041
 */
public interface SwitchService {

    /**
     * 判断指定键名的功能开关是否开启
     *
     * @param key 功能开关标识键名
     * @return 是否处于开启状态
     */
    boolean isEnabled(String key);

    /**
     * 设置指定键名的功能开关启停状态
     *
     * @param key     功能开关标识键名
     * @param enabled 是否开启功能开关
     */
    void setEnabled(String key, boolean enabled);

}
