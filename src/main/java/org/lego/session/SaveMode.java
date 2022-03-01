package org.lego.session;

/**
 * Supported modes of tracking and saving session changes to session store.
 *
 * @author Rob Winch
 * @author Vedran Pavic
 * @since 2.2.0
 */
public enum SaveMode {

	/**
	 * <p>只在会话改变（实例调用{@link Session#setAttribute(String, Object)}）时保存</p>
	 *
	 * Save only changes made to session, for instance using
	 * {@link Session#setAttribute(String, Object)}. In highly concurrent environments,
	 * this mode minimizes the risk of attributes being overwritten during processing of
	 * parallel requests.
	 */
	ON_SET_ATTRIBUTE,

	/**
	 * <p>只在会话被读取（实例调用{@link Session#getAttribute(String, Object)}）时保存</p>
	 * Same as {@link #ON_SET_ATTRIBUTE} with addition of saving attributes that have been
	 * read using {@link Session#getAttribute(String)}.
	 */
	ON_GET_ATTRIBUTE,

	/**
	 * <p>总是保存</p>
	 * Always save all session attributes, regardless of the interaction with the session.
	 * In highly concurrent environments, this mode increases the risk of attributes being
	 * overwritten during processing of parallel requests.
	 */
	ALWAYS

}

