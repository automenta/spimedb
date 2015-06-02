package com.kixeye.kixmpp.date;

/*
 * #%L
 * KIXMPP
 * %%
 * Copyright (C) 2014 KIXEYE, Inc
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * Some helpful date utils.
 * 
 * @author ebahtijaragic
 */
public final class XmppDateUtils {
	private XmppDateUtils() { }

    private static final DateTimeFormatter xmppDateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
    
    /**
     * Format a long from epoch in UTC to a string.
     * 
     * @param dateTime
     * @return
     */
	public static String format(long dateTime) {
		return xmppDateTimeFormatter.print(dateTime);
	}
    
    /**
     * Format a DateTime to a string.
     * 
     * @param dateTime
     * @return
     */
	public static String format(DateTime dateTime) {
		return dateTime.toString(xmppDateTimeFormatter);
	}
	
	/**
     * Parse a string into a DateTime.
     * 
     * @param dateTime
     * @return
     */
	public static DateTime parse(String dateTime) {
		return DateTime.parse(dateTime, xmppDateTimeFormatter);
	}
}
