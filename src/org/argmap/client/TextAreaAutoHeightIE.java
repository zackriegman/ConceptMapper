package org.argmap.client;

import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.TextArea;

/* this works better than TextAreaAutoHeight for many cases in IE, but
 * seems to leave childless nodes sized close to zero, making the
 * current implementation pretty much useless.  Keeping it around
 * in case I ever get around to trying to fix it...
 */
public class TextAreaAutoHeightIE extends TextArea {
	private final Element element;

	@Override
	public void setText(String text) {
		super.setText(text);
		if (isAttached()) {
			resize();
		}
	}

	private void resize() {
		DOM.setStyleAttribute(element, "height", "1em");
		DOM.setStyleAttribute(element, "height", "" + element.getScrollHeight()
				+ "px");
	}

	public TextAreaAutoHeightIE() {
		element = getElement();
		DOM.setStyleAttribute(element, "overflow", "hidden");
		addKeyUpHandler(new KeyUpHandler() {

			@Override
			public void onKeyUp(KeyUpEvent event) {
				resize();
			}
		});
	}

	@Override
	public void onLoad() {
		resize();
	}
}
