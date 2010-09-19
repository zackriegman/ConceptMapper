package com.appspot.conceptmapper.server;

import java.util.List;
import java.util.Map;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.Query;
import com.appspot.conceptmapper.client.Argument;
import com.appspot.conceptmapper.client.Proposition;
import com.appspot.conceptmapper.client.PropositionService;

public class PropositionServiceImpl extends RemoteServiceServlet implements
		PropositionService {

	private static final long serialVersionUID = 1L; // just to get rid of the
	// warnings...
	private Objectify ofy;

	@Override
	public void test() {
		// printAllPropsAndArgs();
		// deleteAllArgsAndProps();
		// printAllPropsAndArgs();

	}

	@Override
	public Proposition[] getRootPropositions() {
		return null;
	}

	public void print(String message) {
		System.out.println(message);
		// log.log(Level.SEVERE, message);
	}

	public void deleteAllArgsAndProps() {
		ofy = ObjectifyService.begin();
		ofy.delete(ofy.query(Argument.class));
		ofy.delete(ofy.query(Proposition.class));
	}

	@Override
	public void makePropChanges(Proposition[] newProps,
			Proposition[] changedProps, Proposition[] deletedProps,
			Argument[] newArgs, Argument[] deletedArgs) {
		ofy = ObjectifyService.begin();

		if (changedProps != null)
			ofy.put(changedProps);

		if (deletedProps != null)
			ofy.delete(deletedProps);
		if (deletedArgs != null)
			ofy.delete(deletedArgs);

		if (newProps != null)
			for (Proposition prop : newProps) {
				recursivePutProp(prop);
			}

		if (newArgs != null)
			for (Argument arg : newArgs) {
				recursivePutArg(arg);
			}

		// TO DO: for new Props and Args must get the new ids back to client!!!
	}

	public void printAllPropsAndArgs() {
		print("Arguments: ");
		for (Argument arg : ofy.query(Argument.class)) {
			printArgument(arg);
		}
		print("Propositions: ");
		for (Proposition prop : ofy.query(Proposition.class)) {
			printProposition(prop);
		}

	}

	public void printArgument(Argument arg) {
		print("id:" + arg.id + " - propKeys:" + arg.propIDs
				+ " - aboutPropKey:" + arg.aboutPropID + " - pro:" + arg.pro);
	}

	public void printProposition(Proposition prop) {
		print("id:" + prop.id + " - content:" + prop.content + " - topLevel:"
				+ prop.topLevel);
	}

	public Long recursivePutProp(Proposition prop) {
		ofy.put(prop);
		List<Argument> args = prop.getArgs();
		for (Argument arg : args) {
			arg.aboutPropID = prop.getID();
			recursivePutArg(arg);
		}
		return prop.getID();
	}

	public void recursivePutArg(Argument arg) {
		List<Proposition> props = arg.getProps();
		for (Proposition prop : props) {
			arg.propIDs.add(recursivePutProp(prop));
		}
		ofy.put(arg);
	}

	@Override
	public Proposition[] getAllProps() {
		ofy = ObjectifyService.begin();
		Query<Proposition> propQuery = ofy.query(Proposition.class).filter(
				"topLevel", true);
		Proposition[] returnProps = new Proposition[propQuery.countAll()];
		print("propQuery.countAll(): " + propQuery.countAll());
		int i = 0;
		for (Proposition prop : propQuery) {
			returnProps[i] = prop;
			recursiveBuildProp(prop);
			i++;
		}

		printAllPropsAndArgs();
		return returnProps;
	}

	public void recursiveBuildProp(Proposition prop) {
		if (prop == null)
			print("prop is null?");
		Query<Argument> argQuery = ofy.query(Argument.class).filter(
				"aboutPropID", prop.id);
		for (Argument arg : argQuery) {
			print("processing argument:");
			printArgument(arg);
			prop.args.add(arg);
			Map<Long, Proposition> propMap = ofy.get(Proposition.class,
					arg.propIDs);
			for (Long id : arg.propIDs) {
				Proposition gotProp = propMap.get(id);
				arg.props.add(gotProp);
				recursiveBuildProp(gotProp);
			}

		}
	}

	public Long addProposition(Long parentArgID, int position) throws Exception {
		ofy = ObjectifyService.begin();

		Proposition newProposition = new Proposition();
		Argument parentArg = null;

		if (parentArgID != null) {
			// exception will be generated if there is a bogus parentArgID
			parentArg = ofy.get(Argument.class, parentArgID);
			newProposition.topLevel = false;
			ofy.put(newProposition);
			parentArg.propIDs.add(position, newProposition.id);
			ofy.put(parentArg);
		} else {
			newProposition.topLevel = true;
			ofy.put(newProposition);
		}

		print("added proposition:");
		printProposition(newProposition);
		if (parentArg != null) {
			print("updated argument:");
			printArgument(parentArg);
		}

		return newProposition.id;

	}

	@Override
	public void removeProposition(Long propID) throws Exception {
		ofy = ObjectifyService.begin();
		if (ofy.query(Argument.class).filter("aboutPropID", propID).countAll() != 0) {
			throw new Exception(
					"cannot delete proposition with arguments; delete arguments first");
		}

		print("Proposition ID to delete:" + propID);
		print("Arguments that use this proposition:");
		for (Argument arg : ofy.query(Argument.class).filter("propIDs", propID)) {
			print("before");
			printArgument(arg);

			arg.propIDs.remove(propID);

			print("after");
			printArgument(arg);
			if (arg.propIDs.isEmpty()) {
				ofy.delete(arg);
			} else {
				ofy.put(arg);
			}
		}

		ofy.delete(Proposition.class, propID);

	}

	@Override
	public Argument addArgument(Long parentPropID, boolean pro)
			throws Exception {
		ofy = ObjectifyService.begin();
		ofy.get(Proposition.class, parentPropID); // trigger an exception if the
													// ID is valid

		Proposition newProp = new Proposition();
		newProp.topLevel = false;
		ofy.put(newProp);

		Argument newArg = new Argument();
		newArg.aboutPropID = parentPropID;
		newArg.propIDs.add(newProp.id);
		newArg.pro = pro;

		ofy.put(newArg);

		newArg.props.add(newProp);
		return newArg;
	}

	@Override
	public void updateProposition(Long propID, String content) throws Exception {
		ofy = ObjectifyService.begin();
		Proposition prop = ofy.get(Proposition.class, propID);
		prop.setContent(content);
		ofy.put(prop);
	}

}
