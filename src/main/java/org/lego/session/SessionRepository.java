package org.lego.session;

/**
 * A repository interface for managing {@link Session} instances.
 *
 * @param <S> the {@link Session} type
 * @author Fengxiang Xue
 * @since 2022-02-26
 */
public interface SessionRepository<S extends Session> {

    /**
     * Creates a new {@link Session} that is capable of being persisted by this
     * {@link SessionRepository}.
     *新建一条会话并通过{@link SessionRepository}持久化
     * <p>
     * This allows optimizations and customizations in how the {@link Session} is
     * persisted. For example, the implementation returned might keep track of the changes
     * ensuring that only the delta needs to be persisted on a save.
     * </p>
     * @return a new {@link Session} that is capable of being persisted by this
     * {@link SessionRepository}
     */
    S createSession();

    /**
     *保存{@link org.lego.session.SessionRepository#createSession()}创捷的会话
     * <p>
     * Some implementations may choose to save as the {@link Session} is updated by
     * returning a {@link Session} that immediately persists any changes. In this case,
     * this method may not actually do anything.
     * </p>
     * @param session the {@link Session} to save
     */
    void save(S session);

    /**
     * 通过{@link Session#getId()}获取会话，如果没找到就返回null
     * @param id the {@link org.lego.session.Session#getId()} to lookup
     * @return the {@link Session} by the {@link Session#getId()} or null if no
     * {@link Session} is found.
     */
    S findById(String id);

    /**
     * 通过会话ID删除会话，如果没找到ID则什么也不做
     * Deletes the {@link Session} with the given {@link Session#getId()} or does nothing
     * if the {@link Session} is not found.
     * @param id the {@link org.lego.session.Session#getId()} to delete
     */
    void deleteById(String id);

}
