package org.argmap.server;

import org.argmap.client.Argument;
import org.argmap.client.Change;
import org.argmap.client.Node;
import org.argmap.client.Proposition;

import com.googlecode.objectify.ObjectifyService;

public class ObjectifyRegistrator {
	private static boolean registered = false;

	public static void register() {
		if (!registered) {
			ObjectifyService.register(Node.class);
			ObjectifyService.register(Proposition.class);
			ObjectifyService.register(Argument.class);
			ObjectifyService.register(Change.class);
			ObjectifyService.register(Search.class);
			ObjectifyService.register(CombinationGenerator.class);
			ObjectifyService.register(Property.class);
			ObjectifyService.register(Vote.class);
			registered = true;
		}
	}
}
