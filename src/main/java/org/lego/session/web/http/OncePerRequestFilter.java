package org.lego.session.web.http;

import java.io.IOException;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 此接口用来保证每个请求只会调用一次过滤器
 * @author Xue Fengxiang
 * @since 1.0
 */
abstract class OncePerRequestFilter implements Filter {

    /**
     *添加后缀“.FILTERED"
     * Suffix that gets appended to the filter name for the "already filtered" request
     * attribute.
     */
    public static final String ALREADY_FILTERED_SUFFIX = ".FILTERED";

    private String alreadyFilteredAttributeName = getClass().getName().concat(ALREADY_FILTERED_SUFFIX);

    /**
     * {@code doFilter}使用此方法保存请求的"already filtered"属性，只要有此属性，无需再过滤器过滤
     * This {@code doFilter} implementation stores a request attribute for "already
     * filtered", proceeding without filtering again if the attribute is already there.
     * @param request the request
     * @param response the response
     * @param filterChain the filter chain
     * @throws ServletException if request is not HTTP request
     * @throws IOException in case of I/O operation exception
     */
    @Override
    public final void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
            throw new ServletException("OncePerRequestFilter just supports HTTP requests");
        }
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        String alreadyFilteredAttributeName = getAlreadyFilteredAttributeName();
        boolean hasAlreadyFilteredAttribute = request.getAttribute(alreadyFilteredAttributeName) != null;

        if (hasAlreadyFilteredAttribute) {
            if (DispatcherType.ERROR.equals(request.getDispatcherType())) {
                doFilterNestedErrorDispatch(httpRequest, httpResponse, filterChain);
                return;
            }
            // Proceed without invoking this filter...
            filterChain.doFilter(request, response);
        }
        else {
            // Do invoke this filter...
            request.setAttribute(alreadyFilteredAttributeName, Boolean.TRUE);
            try {
                doFilterInternal(httpRequest, httpResponse, filterChain);
            }
            finally {
                // Remove the "already filtered" request attribute for this request.
                request.removeAttribute(alreadyFilteredAttributeName);
            }
        }
    }

    /**
     * 返回请求的”alreadyFilteredAttributeName“属性（用来判断请求是否已被过滤）
     * <p>
     * 默认会为原过滤器实例添加后缀”.FILTERED",如果过滤器没有被初始化，会回退为原来的类名
     * @return the name of request attribute indicating already filtered request
     * @see #ALREADY_FILTERED_SUFFIX
     */
    protected String getAlreadyFilteredAttributeName() {
        return this.alreadyFilteredAttributeName;
    }

    /**
     * 处理错误的请求配发（一般服务器会在错误配发后重新配发，但有时会错误配发叠加，
     * 此种情况下，还在当前线程的过滤器链中，但是请求和回应会被置换回原来状态
     * <p>
     * 继承类可以用此方法过滤错误配发，重新包装请求和回应。{@code ThreadLocal}应还在活跃中。
     * Sub-classes may use this method to filter such nested ERROR dispatches and re-apply
     * wrapping on the request or response. {@code ThreadLocal} context, if any, should
     * still be active as we are still nested within the filter chain.
     * @param request the request
     * @param response the response
     * @param filterChain the filter chain
     * @throws ServletException if request is not HTTP request
     * @throws IOException in case of I/O operation exception
     */
    protected void doFilterNestedErrorDispatch(HttpServletRequest request, HttpServletResponse response,
                                               FilterChain filterChain) throws ServletException, IOException {
        doFilter(request, response, filterChain);
    }

    /**
     * Same contract as for {@code doFilter}, but guaranteed to be just invoked once per
     * request within a single request thread.
     * <p>
     * Provides HttpServletRequest and HttpServletResponse arguments instead of the
     * default ServletRequest and ServletResponse ones.
     * @param request the request
     * @param response the response
     * @param filterChain the FilterChain
     * @throws ServletException thrown when a non-I/O exception has occurred
     * @throws IOException thrown when an I/O exception of some sort has occurred
     * @see Filter#doFilter
     */
    protected abstract void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                             FilterChain filterChain) throws ServletException, IOException;

    @Override
    public void init(FilterConfig config) {
    }

    @Override
    public void destroy() {
    }

}
