package org.lego.session;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

/**
 *提供session接口，规定session的行为，需要实现的方法。
 * @author Fengxiang Xue
 * @since 2022-2-22
 */
public interface Session {

    /**
     *获取{@link Session}的唯一ID
     * @return a unique string that identifies the {@link Session}
     */
    String getId();

    /**
     * 改变session的ID，之后调用{@link #getId()}返回新的ID
     * @return the new session id which {@link #getId()} will now return
     */
    String changeSessionId();

    /**
     * 根据属性名称返回属性的值
     * @param <T> the return type of the attribute
     * @param attributeName the name of the attribute to get
     * @return the Object associated with the specified name or null if no Object is
     * associated to that name
     */
    <T> T getAttribute(String attributeName);

    /**
     * 返回必要属性或者抛出异常{@link IllegalArgumentException}。
     * @param name the attribute name
     * @param <T> the attribute type
     * @return the attribute value
     */
    @SuppressWarnings("unchecked")
    default <T> T getRequiredAttribute(String name) {
        T result = getAttribute(name);
        if (result == null) {
            throw new IllegalArgumentException("Required attribute '" + name + "' is missing.");
        }
        return result;
    }

    /**
     * 取得属性值或者是默认属性值
     * @param name the attribute name
     * @param defaultValue a default value to return instead
     * @param <T> the attribute type
     * @return the attribute value
     */
    @SuppressWarnings("unchecked")
    default <T> T getAttributeOrDefault(String name, T defaultValue) {
        T result = getAttribute(name);
        return (result != null) ? result : defaultValue;
    }

    /**
     * 取得属性名称，再传入{@link org.lego.session.Session#getAttribute(String)}可以取得属性值
     * @return the attribute names that have a value associated with it.
     * @see #getAttribute(String)
     */
    Set<String> getAttributeNames();

    /**
     * 根据属性名称设定属性值，如果设定属性值为null，则效果等价于{@link org.lego.session.Session#removeAttribute(String)}。
     * @param attributeName the attribute name to set
     * @param attributeValue the value of the attribute to set. If null, the attribute
     * will be removed.
     */
    void setAttribute(String attributeName, Object attributeValue);

    /**
     * 根据属性名称移除该属性
     * @param attributeName the name of the attribute to remove
     */
    void removeAttribute(String attributeName);

    /**
     * 取得属性创建的时间
     * @return the time when this session was created.
     */
    Instant getCreationTime();

    /**
     * 设置最后访问的时间
     * @param lastAccessedTime the last accessed time
     */
    void setLastAccessedTime(Instant lastAccessedTime);

    /**
     * 获取{@link Session}最后访问的时间
     * @return the last time the client sent a request associated with the session
     */
    Instant getLastAccessedTime();

    /**
     * 设置最大的会话保留时间，一旦超过则session变为无效，负值表明不设定超时时间
     * @param interval the amount of time that the {@link Session} should be kept alive
     * between client requests.
     */
    void setMaxInactiveInterval(Duration interval);

    /**
     * 取得最大会话保留时间，负值表示没有
     * @return the maximum inactive interval between requests before this session will be
     * invalidated. A negative time indicates that the session will never timeout.
     */
    Duration getMaxInactiveInterval();

    /**
     * 如果会话过期则返回true
     * @return true if the session is expired, else false.
     */
    boolean isExpired();
}
