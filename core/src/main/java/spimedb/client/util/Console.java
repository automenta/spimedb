/*
 *  Copyright 2015 Felix Wittmann
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package spimedb.client.util;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;

/*
 * The console object provides access to the browser's debugging console. The
 * specifics of how it works vary from browser to browser, but there is a
 * factual set of features that are typically provided.
 * from: https://github.com/leobm/teavm-console
 */
public final class Console implements JSObject {

	private Console() {
	}

	@JSBody(params = {}, script = "return typeof console !== 'undefined'")
	public static native boolean isAvailable();

	@JSBody(params = { "expression", "msg" }, script = "return console.assert(expression, msg)")
	public static native void assertTrue(boolean expression, String msg);

	@JSBody(params = {}, script = "return window.console.clear()")
	public static native void clear();

	@JSBody(params = {}, script = "return window.console.count()")
	public static native void count();

	@JSBody(params = { "label" }, script = "return  window.console.count(label)")
	public static native void count(String label);

	@JSBody(params = { "objs" }, script = "return console.debug.apply(this, objs)")
	public static native void debug(JSObject... objs);

	@JSBody(params = { "msg", "objs" }, script = "return console.debug.apply(this,[msg].concat.apply([msg], objs))")
	public static native void debug(String msg, JSObject... objs);

	@JSBody(params = { "obj" }, script = "return console.dir.apply(this, obj)")
	public static native void dir(JSObject obj);

	@JSBody(params = { "obj" }, script = "return console.dirxml.apply(this, obj)")
	public static native void dirxml(JSObject obj);

	@JSBody(params = { "objs" }, script = "return console.error.apply(this, objs)")
	public static native void error(JSObject... objs);

	@JSBody(params = { "objs" }, script = "return console.group.apply(this, objs)")
	public static native void group(JSObject... objs);

	@JSBody(params = { "objs" }, script = "return console.groupCollapsed.apply(this, objs)")
	public static native void groupCollapsed(JSObject... objs);

	@JSBody(params = {}, script = "return console.groupEnd()")
	public static native void groupEnd();

	@JSBody(params = { "objs" }, script = "return console.info.apply(this, objs)")
	public static native void info(JSObject... objs);

	@JSBody(params = { "msg", "objs" }, script = "return console.info.apply(this, [msg].concat.apply([msg], objs))")
	public static native void info(String msg, JSObject... objs);

	@JSBody(params = { "msg", "objs" }, script = "return console.log.apply(this, [msg].concat.apply([msg], objs))")
	public static native void log(String msg, JSObject... objs);

	@JSBody(params = { "objs" }, script = "return console.log.apply(this, objs)")
	public static native void log(JSObject... objs);

	@JSBody(params = {}, script = "return console.profile()")
	public static native void profile();

	@JSBody(params = { "label" }, script = "return console.profile(label)")
	public static native void profile(String label);

	@JSBody(params = {}, script = "return console.profileEnd()")
	public static native void profileEnd();

	@JSBody(params = { "timerName" }, script = "return console.time(timerName)")
	public static native void time(String timerName);

	@JSBody(params = {}, script = "return console.timeEnd()")
	public static native void timeEnd();

	@JSBody(params = {}, script = "return console.timeStamp()")
	public static native void timeStamp();

	@JSBody(params = { "label" }, script = "return console.timeStamp(label)")
	public static native void timeStamp(String label);

	@JSBody(params = {}, script = "return console.trace()")
	public static native void trace();

	@JSBody(params = { "objs" }, script = "return console.warn.apply(this, objs)")
	public static native void warn(JSObject... objs);

	@JSBody(params = { "msg", "objs" }, script = "return console.warn.apply(this, [msg].concat.apply([msg], objs))")
	public static native void warn(String msg, JSObject... objs);

}