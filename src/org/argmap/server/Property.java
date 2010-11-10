package org.argmap.server;

import java.io.Serializable;

import javax.persistence.Id;

import com.googlecode.objectify.NotFoundException;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.annotation.Cached;

@Cached
public class Property implements Serializable {

	/**
	 * added to suppress warnings
	 */
	private static final long serialVersionUID = 1L;

	static {
		ObjectifyRegistrator.register();
	}

	@Id
	private String id;

	private String value;

	private static Objectify ofy = ObjectifyService.begin();

	public static void put(String name, Object value) {
		Property property = new Property();
		property.id = name;
		property.value = "" + value;
		ofy.put(property);
	}

	public static String getString(String name) {
		try {
			return ofy.get(Property.class, name).value;
		} catch (NotFoundException e) {
			return null;
		}
	}

	public static Integer getInteger(String name) {
		try {
			return Integer.parseInt(ofy.get(Property.class, name).value);
		} catch (NotFoundException e) {
			return null;
		}
	}

	public static Long getLong(String name) {
		try {
			return Long.parseLong(ofy.get(Property.class, name).value);
		} catch (NotFoundException e) {
			return null;
		}
	}
}
