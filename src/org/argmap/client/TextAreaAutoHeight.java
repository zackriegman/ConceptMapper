package org.argmap.client;

import static com.google.gwt.query.client.GQuery.$;

import com.google.gwt.core.client.GWT;
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

	/*
	 * keeps track of the last TextAreaAutoHeight that was resized. If the
	 * StyleContext or style name was different from that of the last one then
	 * the shadow div needs to be updated with the CSS values from the current
	 * TextAreaAutoHeight. The StyleContext is simply a string that is given to
	 * each TextAreaAutoHeight when it is created, that identifies what context
	 * it is being used in, to ensure that two TextAreaAutHeights used in
	 * different contexts and thus inheriting different styles will update the
	 * style info between their resizes. It's not a perfect solution but I think
	 * it will work... and speed up the resize which is currently a bit slow due
	 * in part to updating this info.
	 */
	private static String lastStyleContext;
	private static String lastStyleName;
	private static int lastPaddingLeft;
	private static int lastPaddingRight;

	private final String styleContext;

	public TextAreaAutoHeight(String styleContext) {
		super();
		this.styleContext = styleContext;
		DOM.setStyleAttribute(getElement(), "overflow", "hidden");
		if (shadow == null) {
			shadow = $(DOM.createDiv());
			shadow.appendTo(GQuery.body);
			// shadow.css("position", "absolute").css("top", "-1000px")
			// .css("left", "-1000px").css("resize", "none")
			// .appendTo(GQuery.body);
			shadow.css("position", "absolute").css("top", "0px")
					.css("left", "0px").css("resize", "none")
					.appendTo(GQuery.body);
		}

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

	@Override
	public void setText(String text) {
		super.setText(text);
		if (isAttached()) {
			resize();
		}
	}

	public void resize() {
		GQuery elem = $(getElement());

		String styleName = getStyleName();
		int paddingLeft;
		int paddingRight;
		if (styleContext != lastStyleContext
				|| !styleName.equals(lastStyleName)) {
			paddingLeft = cssAsInt(elem, "paddingLeft");
			paddingRight = cssAsInt(elem, "paddingRight");

			shadow.css("fontSize", elem.css("fontSize", true));
			shadow.css("fontFamily", elem.css("fontFamily", true));
			shadow.css("lineHeight", elem.css("lineHeight", true));

			lastStyleContext = styleContext;
			lastStyleName = styleName;
			lastPaddingLeft = paddingLeft;
			lastPaddingRight = paddingRight;
		} else {
			paddingLeft = lastPaddingLeft;
			paddingRight = lastPaddingRight;
		}

		int shadowWidth = elem.width() - paddingLeft - paddingRight;
		shadowWidth = shadowWidth > 0 ? shadowWidth : 700;
		shadow.css("width", "" + shadowWidth + "px");

		GWT.log("resize node with text: " + getText());
		shadow.html(toHTML(getText()));
		$(getElement()).css("height", shadow.height() + "px");
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