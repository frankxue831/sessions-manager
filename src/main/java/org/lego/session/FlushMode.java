package org.lego.session;

/**
 * Supported modes of writing the session to session store.
 *
 * @author Rob Winch
 * @author Vedran Pavic
 * @since 2.2.0
 */
public enum FlushMode {

	/**
	 * 当{@link SessionRepository#save(Session)}被调用时才存入会话存储器
	 * Only writes to session store when {@link SessionRepository#save(Session)} is
	 * invoked. In a web environment this is typically done as soon as the HTTP response
	 * is committed.
	 */
	ON_SAVE,

	/**
	 * 立即存储会话，（例如调用{@link SessionRepository#createSession()}，
	 * {@link Session#setAttribute(String, Object)}会立刻写入会话存储器）。
	 */
	IMMEDIATE

}

