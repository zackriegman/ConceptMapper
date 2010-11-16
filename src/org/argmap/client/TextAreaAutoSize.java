package org.argmap.client;

import static com.google.gwt.query.client.GQuery.$;

import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.logical.shared.AttachEvent;
import com.google.gwt.query.client.GQuery;
import com.google.gwt.user.client.ui.TextArea;

/*
 * using this method:
 * http://james.padolsey.com/demos/plugins/jQuery/autoresize.jquery.js
 */
public class TextAreaAutoSize extends TextArea implements KeyUpHandler,
		AttachEvent.Handler {
	private GQuery clone;
	private int origHeight;
	private final int extraSpace = 1;
	private int lastScrollTop;
	boolean initialized = false;

	public TextAreaAutoSize() {
		// GWT.log("constructed");
	}

	/*
	 * TODO currently this never cleans up the garbage it appends to the
	 * document... in a long lived application that is constantly creating new
	 * text areas and throwing them away (like this one) that might be a
	 * problem.
	 */
	public void init() {
		if (initialized == false) {
			// GWT.log("initilized");
			GQuery textArea = $(getElement());
			textArea.css("resize", "none").css("overflow-y", "hidden");
			origHeight = textArea.height();

			clone = textArea.clone().removeAttr("id").removeAttr("name");

			clone.css("position", "absolute");
			clone.css("top", "0px");
			clone.css("left", "-9999px");
			clone.attr("tabIndex", "-1");

			clone.appendTo(GQuery.body);
			// clone.insertBefore(textArea);

			addKeyUpHandler(this);
			addAttachHandler(this);
			initialized = true;
		}
	}

	public void resize() {
		GQuery textArea = $(getElement());
		clone.css("height", textArea.height() + "px");
		clone.css("width", textArea.width() + "px");

		if (!textArea.css("lineHeight").equals("")) {
			clone.css("lineHeight", cssAsInt(textArea, "lineHeight") + "px");
		}
		if (!textArea.css("textDecoration").equals("")) {
			clone.css("textDecoration", textArea.css("textDecoration"));
		}
		if (!textArea.css("letterSpacing").equals("")) {
			clone.css("letterSpacing", cssAsInt(textArea, "letterSpacing")
					+ "px");
		}

		// Prepare the clone:
		clone.height(0).val(getText()).scrollTop(10000);

		// Find the height of text:
		int scrollTop = Math.max(clone.scrollTop(), origHeight) + extraSpace;

		// Don't do anything if scrollTop hasen't changed:
		if (lastScrollTop == scrollTop) {
			return;
		}
		lastScrollTop = scrollTop;

		textArea.height(scrollTop);
	}

	@Override
	public void onAttachOrDetach(AttachEvent event) {
		if (event.isAttached()) {
			// GWT.log("attached");
			resize();
			init();
		}
		// GWT.log("attach event");
	}

	@Override
	public void onKeyUp(KeyUpEvent event) {
		resize();
	}

	@Override
	public void setText(String text) {
		super.setText(text);
		if (isAttached()) {
			resize();
		}
	}

	private int cssAsInt(GQuery element, String property) {
		return extractInt(element.css(property, true));
	}

	private int extractInt(String string) {
		int start = 0;
		int end = 0;
		for (int i = 0; i < string.length(); i++) {
			if (Character.isDigit(string.charAt(i))) {
				start = i;
				break;
			}
		}
		for (int i = start; i < string.length(); i++) {
			if (!Character.isDigit(string.charAt(i))) {
				end = i;
				break;
			}
		}
		return Integer.parseInt(string.substring(start, end));
	}
}
