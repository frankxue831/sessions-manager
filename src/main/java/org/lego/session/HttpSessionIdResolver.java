package org.lego.session;


import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 定义了sessionId解析策略的行为准则（Contract）。允许通过请求解析sessionId，并通过响应发送sessionId或终止会话。
 * HttpSessionIdResolver中必须包含三个方法：
 *
 * resolveSessionIds：解析与当前请求相关联的sessionId。sessionId可能来自Cookie或请求头。
 * setSessionId：将给定的sessionId发送给客户端。这个方法是在创建一个新session时被调用，并告知客户端新sessionId是什么。
 * expireSession：指示客户端结束当前session。当session无效时调用此方法，并应通知客户端sessionId不再有效。
 * 比如，它可能删除一个包含sessionId的Cookie，或者设置一个HTTP响应头，其值为空就表示客户端不再提交sessionId。
 * ————————————————
 * Contract for session id resolution strategies. Allows for session id resolution through
 * the request and for sending the session id or expiring the session through the
 * response.
 *
 * @author Rob Winch
 * @author Vedran Pavic
 * @since 2.0.0
 */
public interface HttpSessionIdResolver {

	/**
	 * Resolve the session ids associated with the provided {@link HttpServletRequest}.
	 * For example, the session id might come from a cookie or a request header.
	 * @param request the current request
	 * @return the session ids
	 */
	List<String> resolveSessionIds(HttpServletRequest request);

	/**
	 * Send the given session id to the client. This method is invoked when a new session
	 * is created and should inform a client what the new session id is. For example, it
	 * might create a new cookie with the session id in it or set an HTTP response header
	 * with the value of the new session id.
	 * @param request the current request
	 * @param response the current response
	 * @param sessionId the session id
	 */
	void setSessionId(HttpServletRequest request, HttpServletResponse response, String sessionId);

	/**
	 * Instruct the client to end the current session. This method is invoked when a
	 * session is invalidated and should inform a client that the session id is no longer
	 * valid. For example, it might remove a cookie with the session id in it or set an
	 * HTTP response header with an empty value indicating to the client to no longer
	 * submit that session id.
	 * @param request the current request
	 * @param response the current response
	 */
	void expireSession(HttpServletRequest request, HttpServletResponse response);

}

