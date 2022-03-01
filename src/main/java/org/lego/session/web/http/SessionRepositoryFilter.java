package org.lego.session.web.http;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

import javax.servlet.FilterChain;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.lego.session.*;


/**
 * 使用 Filter 把请求拦截，
 * 然后包装 Request 和 Response，
 * 使得 Request.getSession 返回的 Session 也是包装过的，
 * 改变了原有 Session 的行为，重新实现Session接口。
 * 将原本的{@link HttpSession}实现换成lego session实现。
 *
 * <p>
 * {@link SessionRepositoryFilter} 通过 {@link HttpSessionIdResolver} (默认
 * {@link CookieHttpSessionIdResolver})
 * 实现{@link HttpSession} 到
 * {@link Session}映射. 例如:
 * </p>
 *
 * <ul>
 * <li>
 *     通过{@link HttpSessionIdResolver#resolveSessionIds(HttpServletRequest)}
 *     查找session ID
 *     （默认查找名称为SESSION的cookie）
 * </li>
 * <li>
 *     创建会话{@link Session}并通过
 *     {@link HttpSessionIdResolver#setSessionId(HttpServletRequest, HttpServletResponse, String)}
 *     通知客户端session ID。
 * <li>
 *     客户端会通过{@link HttpSessionIdResolver#expireSession(HttpServletRequest, HttpServletResponse)}被提示sessionID已失效。
 * </li>
 * </ul>
 *
 * <p>
 *     SessionRepositoryFilter过滤器需要放置在过滤器链的最前端，保证会话被重载和持久化
 * </p>
 *
 * @param <S> {@link Session}的类型.
 * @author Rob Winch
 * @author Vedran Pavic
 * @author Josh Cummings
 * @author Fengxiang Xue
 * @since 2022-02-28
 */
//@Order(SessionRepositoryFilter.DEFAULT_ORDER)
public class SessionRepositoryFilter<S extends Session> extends OncePerRequestFilter {

    private static final String SESSION_LOGGER_NAME = SessionRepositoryFilter.class.getName().concat(".SESSION_LOGGER");

    private static final Log SESSION_LOGGER = LogFactory.getLog(SESSION_LOGGER_NAME);

    /**
     * The session repository request attribute name.
     */
    public static final String SESSION_REPOSITORY_ATTR = SessionRepository.class.getName();

    /**
     * Invalid session id (not backed by the session repository) request attribute name.
     */
    public static final String INVALID_SESSION_ID_ATTR = SESSION_REPOSITORY_ATTR + ".invalidSessionId";

    private static final String CURRENT_SESSION_ATTR = SESSION_REPOSITORY_ATTR + ".CURRENT_SESSION";

    /**
     * The default filter order.
     * 默认的过滤器优先顺序
     */
    public static final int DEFAULT_ORDER = Integer.MIN_VALUE + 50;

    private final SessionRepository<S> sessionRepository;

    private HttpSessionIdResolver httpSessionIdResolver = new CookieHttpSessionIdResolver();

    /**
     * 构造器，用来创建一个过滤器SessionRepositoryFilter实例。
     * @param sessionRepository ，不能为空.
     */
    public SessionRepositoryFilter(SessionRepository<S> sessionRepository) {
        if (sessionRepository == null) {
            throw new IllegalArgumentException("sessionRepository cannot be null");
        }
        this.sessionRepository = sessionRepository;
    }

    /**
     * 设置{@link HttpSessionIdResolver}，默认设置{@link CookieHttpSessionIdResolver}。
     * @param httpSessionIdResolver 不能为空。
     */
    public void setHttpSessionIdResolver(HttpSessionIdResolver httpSessionIdResolver) {
        if (httpSessionIdResolver == null) {
            throw new IllegalArgumentException("httpSessionIdResolver cannot be null");
        }
        this.httpSessionIdResolver = httpSessionIdResolver;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        request.setAttribute(SESSION_REPOSITORY_ATTR, sessionRepository);

        SessionRepositoryRequestWrapper wrappedRequest = new SessionRepositoryRequestWrapper(request, response);
        SessionRepositoryResponseWrapper wrappedResponse = new SessionRepositoryResponseWrapper(wrappedRequest,
                response);

        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse);
        }
        finally {
            wrappedRequest.commitSession();
        }
    }

    @Override
    protected void doFilterNestedErrorDispatch(HttpServletRequest request, HttpServletResponse response,
                                               FilterChain filterChain) throws ServletException, IOException {
        doFilterInternal(request, response, filterChain);
    }

    /**
     *当 Response 提交后，确保保存会话信息
     * Allows ensuring that the session is saved if the response is committed.
     *
     * @author Fengxiang Xue
     * @since 2022-02-28
     */
    private final class SessionRepositoryResponseWrapper extends OnCommittedResponseWrapper {

        private final SessionRepositoryRequestWrapper request;

        /**
         * 创建一个新的{@link SessionRepositoryResponseWrapper}.
         * @param request the request to be wrapped
         * @param response the response to be wrapped
         */
        SessionRepositoryResponseWrapper(SessionRepositoryRequestWrapper request, HttpServletResponse response) {
            super(response);
            if (request == null) {
                throw new IllegalArgumentException("request cannot be null");
            }
            this.request = request;
        }

        @Override
        protected void onResponseCommitted() {
            request.commitSession();
        }

    }

    /**
     * 重新包装HttpServletRequest，改变原有实现 Session 接口的方式，譬如 getSession、isRequestedSessionIdValid等，
     * 用来达到改变session存储的方式
     * @author Rob Winch
     * @since 1.0
     */
    private final class SessionRepositoryRequestWrapper extends HttpServletRequestWrapper {

        private final HttpServletResponse response;

        private S requestedSession;

        private boolean requestedSessionCached;

        private String requestedSessionId;

        private Boolean requestedSessionIdValid;

        private boolean requestedSessionInvalidated;

        private SessionRepositoryRequestWrapper(HttpServletRequest request, HttpServletResponse response) {
            super(request);
            this.response = response;
        }

        /**
         * <p>
         *     通过{@link HttpSessionIdResolver}将sessionID 写入到response中，并将session持久化。
         *
         * </p>
         * Uses the {@link HttpSessionIdResolver} to write the session id to the response
         * and persist the Session.
         */
        private void commitSession() {
            HttpSessionWrapper wrappedSession = getCurrentSession();
            if (wrappedSession == null) {
                //判断ClientSession是否有效，无效则废除对应的session
                if (isInvalidateClientSession()) {
                    SessionRepositoryFilter.this.httpSessionIdResolver.expireSession(this, this.response);
                }
            }
            else {
                S session = wrappedSession.getSession();
                clearRequestedSessionCache();
                SessionRepositoryFilter.this.sessionRepository.save(session);
                String sessionId = session.getId();
                if (!isRequestedSessionIdValid() || !sessionId.equals(getRequestedSessionId())) {
                    SessionRepositoryFilter.this.httpSessionIdResolver.setSessionId(this, this.response, sessionId);
                }
            }
        }

        @SuppressWarnings("unchecked")
        private HttpSessionWrapper getCurrentSession() {
            return (HttpSessionWrapper) getAttribute(CURRENT_SESSION_ATTR);
        }

        private void setCurrentSession(HttpSessionWrapper currentSession) {
            if (currentSession == null) {
                removeAttribute(CURRENT_SESSION_ATTR);
            }
            else {
                setAttribute(CURRENT_SESSION_ATTR, currentSession);
            }
        }

        @Override
        @SuppressWarnings("unused")
        public String changeSessionId() {
            HttpSession session = getSession(false);

            if (session == null) {
                throw new IllegalStateException(
                        "Cannot change session ID. There is no session associated with this request.");
            }

            return getCurrentSession().getSession().changeSessionId();
        }

        @Override
        public boolean isRequestedSessionIdValid() {
            if (requestedSessionIdValid == null) {
                S requestedSession = getRequestedSession();
                if (requestedSession != null) {
                    requestedSession.setLastAccessedTime(Instant.now());
                }
                return isRequestedSessionIdValid(requestedSession);
            }
            return requestedSessionIdValid;
        }

        private boolean isRequestedSessionIdValid(S session) {
            if (requestedSessionIdValid == null) {
                requestedSessionIdValid = session != null;
            }
            return requestedSessionIdValid;
        }

        private boolean isInvalidateClientSession() {
            return getCurrentSession() == null && requestedSessionInvalidated;
        }

        @Override
        public HttpSessionWrapper getSession(boolean create) {
            HttpSessionWrapper currentSession = getCurrentSession();
            if (currentSession != null) {
                return currentSession;
            }
            S requestedSession = getRequestedSession();
            if (requestedSession != null) {
                if (getAttribute(INVALID_SESSION_ID_ATTR) == null) {
                    requestedSession.setLastAccessedTime(Instant.now());
                    requestedSessionIdValid = true;
                    currentSession = new HttpSessionWrapper(requestedSession, getServletContext());
                    currentSession.markNotNew();
                    setCurrentSession(currentSession);
                    return currentSession;
                }
            }
            else {
                // This is an invalid session id. No need to ask again if
                // request.getSession is invoked for the duration of this request
                if (SESSION_LOGGER.isDebugEnabled()) {
                    SESSION_LOGGER.debug(
                            "No session found by id: Caching result for getSession(false) for this HttpServletRequest.");
                }
                setAttribute(INVALID_SESSION_ID_ATTR, "true");
            }
            if (!create) {
                return null;
            }
            if (SessionRepositoryFilter.this.httpSessionIdResolver instanceof CookieHttpSessionIdResolver
                    && this.response.isCommitted()) {
                throw new IllegalStateException("Cannot create a session after the response has been committed");
            }
            if (SESSION_LOGGER.isDebugEnabled()) {
                SESSION_LOGGER.debug(
                        "A new session was created. To help you troubleshoot where the session was created we provided a StackTrace (this is not an error). You can prevent this from appearing by disabling DEBUG logging for "
                                + SESSION_LOGGER_NAME,
                        new RuntimeException("For debugging purposes only (not an error)"));
            }
            S session = SessionRepositoryFilter.this.sessionRepository.createSession();
            session.setLastAccessedTime(Instant.now());
            currentSession = new HttpSessionWrapper(session, getServletContext());
            setCurrentSession(currentSession);
            return currentSession;
        }

        @Override
        public HttpSessionWrapper getSession() {
            return getSession(true);
        }

        @Override
        public String getRequestedSessionId() {
            if (requestedSessionId == null) {
                getRequestedSession();
            }
            return requestedSessionId;
        }

        @Override
        public RequestDispatcher getRequestDispatcher(String path) {
            RequestDispatcher requestDispatcher = super.getRequestDispatcher(path);
            return new SessionCommittingRequestDispatcher(requestDispatcher);
        }

        private S getRequestedSession() {
            if (!this.requestedSessionCached) {
                List<String> sessionIds = SessionRepositoryFilter.this.httpSessionIdResolver.resolveSessionIds(this);
                for (String sessionId : sessionIds) {
                    if (this.requestedSessionId == null) {
                        this.requestedSessionId = sessionId;
                    }
                    S session = SessionRepositoryFilter.this.sessionRepository.findById(sessionId);
                    if (session != null) {
                        this.requestedSession = session;
                        this.requestedSessionId = sessionId;
                        break;
                    }
                }
                this.requestedSessionCached = true;
            }
            return this.requestedSession;
        }

        private void clearRequestedSessionCache() {
            this.requestedSessionCached = false;
            this.requestedSession = null;
            this.requestedSessionId = null;
        }

        /**
         *
         * Allows creating an HttpSession from a Session instance.
         * 新建HttpSession
         * @author Rob Winch
         * @since 1.0
         */
        private final class HttpSessionWrapper extends HttpSessionAdapter<S> {

            HttpSessionWrapper(S session, ServletContext servletContext) {
                super(session, servletContext);
            }

            @Override
            public void invalidate() {
                super.invalidate();
                SessionRepositoryRequestWrapper.this.requestedSessionInvalidated = true;
                setCurrentSession(null);
                clearRequestedSessionCache();
                SessionRepositoryFilter.this.sessionRepository.deleteById(getId());
            }

        }

        /**
         * Ensures session is committed before issuing an include.
         *
         * @since 1.3.4
         */
        private final class SessionCommittingRequestDispatcher implements RequestDispatcher {

            private final RequestDispatcher delegate;

            SessionCommittingRequestDispatcher(RequestDispatcher delegate) {
                this.delegate = delegate;
            }

            @Override
            public void forward(ServletRequest request, ServletResponse response) throws ServletException, IOException {
                this.delegate.forward(request, response);
            }

            @Override
            public void include(ServletRequest request, ServletResponse response) throws ServletException, IOException {
                SessionRepositoryRequestWrapper.this.commitSession();
                this.delegate.include(request, response);
            }

        }

    }

}

