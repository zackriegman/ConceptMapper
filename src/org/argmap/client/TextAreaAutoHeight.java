package org.argmap.client;

import static com.google.gwt.query.client.GQuery.$;

import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.query.client.GQuery;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.TextArea;

/*technique copied from:
 https://github.com/jaz303/jquery-grab-bag/blob/f1a3cc1e86cbb248bcb41391d6eff115b1be6d89/javascripts/jquery.autogrow-textarea.js
 */
public class TextAreaAutoHeight extends TextArea {
	private static GQuery shadow;

	public TextAreaAutoHeight() {
		DOM.setStyleAttribute(getElement(), "overflow", "hidden");
		if (shadow == null) {
			shadow = $(DOM.createDiv());
			shadow.appendTo(GQuery.body);
			shadow.css("position", "absolute").css("top", "-1000px")
					.css("left", "-1000px").css("resize", "none")
					.appendTo(GQuery.body);
		}

		addKeyUpHandler(new KeyUpHandler() {

			@Override
			public void onKeyUp(KeyUpEvent event) {
				resize();
			}
		});
	}

	// private long lastTime;
	//
	// private void timerStart() {
	// lastTime = System.currentTimeMillis();
	// }
	//
	// private void timerLap(String string) {
	// long thisTime = System.currentTimeMillis();
	// GWT.log(string + " " + (thisTime - lastTime));
	// lastTime = thisTime;
	// }

	@Override
	public void onLoad() {
		resize();
	}

	@Override
	public void setText(String text) {
		super.setText(text);
		if (isAttached()) {
			resize();
		}
	}

	public void resize() {
		// timerStart();
		// timerLap("a");
		GQuery elem = $(getElement());

		// timerLap("b");
		int shadowWidth = elem.width() - cssAsInt(elem, "paddingLeft")
				- cssAsInt(elem, "paddingRight");
		shadowWidth = shadowWidth > 0 ? shadowWidth : 700;
		// GWT.log(elem.width() + " - " + cssAsInt(elem, "paddingLeft") + " - "
		// + cssAsInt(elem, "paddingRight") + " - " + shadowWidth);
		// timerLap("c");
		shadow.css("width", "" + shadowWidth + "px");
		// timerLap("1");
		shadow.css("fontSize", elem.css("fontSize", true));
		// timerLap("2");
		shadow.css("fontFamily", elem.css("fontFamily", true));
		// timerLap("3");
		shadow.css("lineHeight", elem.css("lineHeight", true));
		// timerLap("4");

		// timerLap("e");
		shadow.html(toHTML(getText()));
		// timerLap("f");
		$(getElement()).css("height", shadow.height() + "px");
		// timerLap("g");
	}

	private String toHTML(String text) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < text.length(); i++) {
			switch (text.charAt(i)) {
			case '<':
				sb.append("&lt;");
				break;
			case '>':
				sb.append("&gt;");
				break;
			case '&':
				sb.append("&amp;");
				break;
			case '\n':
				sb.append("<br/>");
				if (i == text.length() - 1) {
					sb.append("&nbsp;");
				}
				break;
			case ' ':
				if (i + 1 < text.length() && text.charAt(i + 1) == ' ') {
					sb.append("&nbsp;");
				} else {
					sb.append(' ');
				}
				break;
			default:
				sb.append(text.charAt(i));
				break;
			}
		}
		String html = sb.toString();
		if (html.trim().length() == 0) {
			return "Mj";
		} else
			return html;
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