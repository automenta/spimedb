package com.kixeye.kixmpp.tuple;

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


import java.util.Arrays;

/**
 * A tuple.
 * 
 * @author ebahtijaragic
 */
public class Tuple {
	private final Object[] values;

	protected Tuple(Object[] values) {
		this.values = values;
	}
	
	/**
	 * Forms a tuple from some varargs.
	 * 
	 * @param values
	 * @return
	 */
	public static Tuple from(Object... values) {
		return new Tuple(values);
	}
	
	/**
	 * Gets a value in a tuple at the given index.
	 * 
	 * @param i
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <T> T get(int i) {
		return (T)values[i];
	}
	
	/**
	 * Returns the size of the tuple.
	 * 
	 * @return
	 */
	public int size() {
		return values.length;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(values);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Tuple other = (Tuple) obj;
		if (!Arrays.equals(values, other.values))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Tuple [values=" + Arrays.toString(values) + "]";
	}
}
