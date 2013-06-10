package biweekly.property.marshaller;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import biweekly.io.text.ICalWriter;
import biweekly.parameter.ICalParameters;
import biweekly.property.ICalProperty;


/*
 Copyright (c) 2013, Michael Angstadt
 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met: 

 1. Redistributions of source code must retain the above copyright notice, this
 list of conditions and the following disclaimer. 
 2. Redistributions in binary form must reproduce the above copyright notice,
 this list of conditions and the following disclaimer in the documentation
 and/or other materials provided with the distribution. 

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/**
 * Base class for iCalendar property marshallers.
 * @author Michael Angstadt
 */
public abstract class ICalPropertyMarshaller<T extends ICalProperty> {
	private static final String NEWLINE = System.getProperty("line.separator");
	protected final Class<T> clazz;
	protected final String propertyName;

	/**
	 * Creates a new marshaller.
	 * @param clazz the property class
	 * @param propertyName the property name (e.g. "VERSION")
	 */
	public ICalPropertyMarshaller(Class<T> clazz, String propertyName) {
		this.clazz = clazz;
		this.propertyName = propertyName.toUpperCase();
	}

	/**
	 * Gets the property class.
	 * @return the property class
	 */
	public Class<T> getPropertyClass() {
		return clazz;
	}

	/**
	 * Gets the property name.
	 * @return the property name (e.g. "VERSION")
	 */
	public String getPropertyName() {
		return propertyName;
	}

	/**
	 * Sanitizes a property's parameters (called before the property is
	 * written). Note that a copy of the parameters is returned so that the
	 * property object does not get modified.
	 * @param property the property
	 * @return the sanitized parameters
	 */
	public final ICalParameters prepareParameters(T property) {
		//make a copy because the property should not get modified when it is marshalled
		ICalParameters copy = new ICalParameters(property.getParameters());
		_prepareParameters(property, copy);
		return copy;
	}

	/**
	 * Marshals a property's value to a string.
	 * @param property the property
	 * @return the marshalled value
	 */
	public final Result<String> writeText(T property) {
		List<String> warnings = new ArrayList<String>(0);
		String value = _writeText(property, warnings);
		return new Result<String>(value, warnings);
	}

	/**
	 * Unmarshals a property's value.
	 * @param value the value
	 * @param parameters the property's parameters
	 * @return the unmarshalled property object
	 */
	public final Result<T> parseText(String value, ICalParameters parameters) {
		List<String> warnings = new ArrayList<String>(0);
		T property = _parseText(value, parameters, warnings);
		property.setParameters(parameters);
		return new Result<T>(property, warnings);
	}

	/**
	 * Sanitizes a property's parameters (called before the property is
	 * written). This should be overridden by child classes when required.
	 * @param property the property
	 * @param copy the list of parameters to make modifications to (it is a copy
	 * of the property's parameters)
	 */
	protected void _prepareParameters(T property, ICalParameters copy) {
		//do nothing
	}

	/**
	 * Marshals a property's value to a string.
	 * @param property the property
	 * @param warnings allows the programmer to alert the user to any
	 * note-worthy (but non-critical) issues that occurred during the
	 * marshalling process
	 * @return the marshalled value
	 */
	protected abstract String _writeText(T property, List<String> warnings);

	/**
	 * Unmarshals a property's value.
	 * @param value the value
	 * @param parameters the property's parameters
	 * @param warnings allows the programmer to alert the user to any
	 * note-worthy (but non-critical) issues that occurred during the
	 * unmarshalling process
	 * @return the unmarshalled property object
	 */
	protected abstract T _parseText(String value, ICalParameters parameters, List<String> warnings);

	/**
	 * Unescapes all special characters that are escaped with a backslash, as
	 * well as escaped newlines.
	 * @param text the text to unescape
	 * @return the unescaped text
	 */
	protected static String unescape(String text) {
		StringBuilder sb = new StringBuilder(text.length());
		boolean escaped = false;
		for (int i = 0; i < text.length(); i++) {
			char ch = text.charAt(i);
			if (escaped) {
				if (ch == 'n' || ch == 'N') {
					//newlines appear as "\n" or "\N" (see RFC 2426 p.7)
					sb.append(NEWLINE);
				} else {
					sb.append(ch);
				}
				escaped = false;
			} else if (ch == '\\') {
				escaped = true;
			} else {
				sb.append(ch);
			}
		}
		return sb.toString();
	}

	/**
	 * Escapes all special characters within a iCalendar value.
	 * <p>
	 * These characters are:
	 * </p>
	 * <ul>
	 * <li>backslashes (<code>\</code>)</li>
	 * <li>commas (<code>,</code>)</li>
	 * <li>semi-colons (<code>;</code>)</li>
	 * <li>(newlines are escaped by {@link ICalWriter})</li>
	 * </ul>
	 * @param text the text to escape
	 * @return the escaped text
	 */
	protected static String escape(String text) {
		String chars = "\\,;";
		for (int i = 0; i < chars.length(); i++) {
			String ch = chars.substring(i, i + 1);
			text = text.replace(ch, "\\" + ch);
		}
		return text;
	}

	/**
	 * Splits a string by a character, taking escaped characters into account.
	 * Each split value is also trimmed.
	 * <p>
	 * Example:
	 * <p>
	 * <code>splitBy("HE\:LLO::WORLD", ':', false, true)</code>
	 * <p>
	 * returns
	 * <p>
	 * <code>["HE:LLO", "", "WORLD"]</code>
	 * @param str the string to split
	 * @param ch the character to split by
	 * @param removeEmpties true to remove empty elements, false not to
	 * @param unescape true to unescape each split string, false not to
	 * @return the split string
	 * @see <a
	 * href="http://stackoverflow.com/q/820172">http://stackoverflow.com/q/820172</a>
	 */
	protected static String[] splitBy(String str, char ch, boolean removeEmpties, boolean unescape) {
		str = str.trim();
		String split[] = str.split("\\s*(?<!\\\\)" + Pattern.quote(ch + "") + "\\s*", -1);

		List<String> list = new ArrayList<String>(split.length);
		for (String s : split) {
			if (s.length() == 0 && removeEmpties) {
				continue;
			}

			if (unescape) {
				s = unescape(s);
			}

			list.add(s);
		}

		return list.toArray(new String[0]);
	}

	/**
	 * Parses a comma-separated list of values.
	 * @param str the string to parse (e.g. "one,two,th\,ree")
	 * @return the parsed values
	 */
	protected static String[] parseList(String str) {
		return splitBy(str, ',', true, true);
	}

	/**
	 * Parses a component value.
	 * @param str the string to parse (e.g. "one;two,three;four")
	 * @return the parsed values
	 */
	protected static String[][] parseComponent(String str) {
		String split[] = splitBy(str, ';', false, false);
		String ret[][] = new String[split.length][];
		int i = 0;
		for (String s : split) {
			String split2[] = parseList(s);
			ret[i++] = split2;
		}
		return ret;
	}

	/**
	 * Represents the result of a marshal or unmarshal operation.
	 * @author Michael Angstadt
	 * @param <T> the marshalled/unmarshalled value (e.g. "String" if a property
	 * was marshalled)
	 */
	public static class Result<T> {
		private final T value;
		private final List<String> warnings;

		/**
		 * Creates a new result.
		 * @param value the value
		 * @param warnings the warnings
		 */
		public Result(T value, List<String> warnings) {
			this.value = value;
			this.warnings = warnings;
		}

		/**
		 * Gets the warnings.
		 * @return the warnings
		 */
		public List<String> getWarnings() {
			return warnings;
		}

		/**
		 * Gets the value.
		 * @return the value
		 */
		public T getValue() {
			return value;
		}
	}
}
