package org.argmap.client;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.dom.client.MouseEvent;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseMoveHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.Widget;

/* approach based on:
 * http://code.google.com/p/gwtquery-plugins/wiki/RatingsPluginGettingStarted
 */
public class StarRating extends Widget implements MouseOverHandler,
		MouseOutHandler, MouseMoveHandler, MouseDownHandler {

	public static interface RatingHandler {
		public void rate(Integer rating);
	}

	private final List<Element> stars = new ArrayList<Element>();
	private final Element cancel;
	private int selectedIndex = -1;
	private int hoverIndex = -1;
	private RatingHandler ratingHandler;
	private boolean hasBeenSet = false;

	public StarRating(String[] messages) {

		// Element span = DOM.createSpan();
		Element span = DOM.createDiv();
		span.addClassName("star-rating-control");

		cancel = DOM.createDiv();
		cancel.addClassName("rating-cancel");
		span.appendChild(cancel);
		Element cancelAnchor = DOM.createAnchor();
		cancelAnchor.setTitle("no opinion");
		cancelAnchor.setInnerHTML("no opinion");
		cancel.appendChild(cancelAnchor);

		for (int i = 0; i < messages.length; i++) {
			Element star = DOM.createDiv();
			star.addClassName("star-rating");
			star.addClassName("star-rating-applied");
			star.addClassName("star-rating-live");
			// star.addClassName("star-rating-on");

			Element starAnchor = DOM.createAnchor();
			starAnchor.setTitle(messages[i]);
			starAnchor.setInnerHTML(messages[i]);
			star.appendChild(starAnchor);

			span.appendChild(star);

			stars.add(star);

		}

		Element end = DOM.createDiv();
		end.addClassName("start-rating-end");

		span.appendChild(end);

		setElement(span);

		addDomHandler(this, MouseOverEvent.getType());
		addDomHandler(this, MouseOutEvent.getType());
		addDomHandler(this, MouseMoveEvent.getType());
		addDomHandler(this, MouseDownEvent.getType());
	}

	public void setRatingHandler(RatingHandler ratingHandler) {
		this.ratingHandler = ratingHandler;
	}

	public Integer getRating() {
		if (selectedIndex == 0) {
			return null;
		} else {
			return selectedIndex - 1;
		}

	}

	public void setRating(Integer rating) {
		hasBeenSet = true;

		if (rating == null) {
			selectedIndex = 0;
		} else {
			selectedIndex = rating + 1;
		}
		fill(selectedIndex, "star-rating-on");
	}

	public boolean hasBeenSet() {
		return hasBeenSet;
	}

	private void drain() {
		for (Element star : stars) {
			star.removeClassName("star-rating-on");
			star.removeClassName("star-rating-hover");
		}
	}

	private void fill(int index, String style) {
		drain();
		for (int i = 0; i < index; i++) {
			stars.get(i).addClassName(style);
		}
	}

	private <H extends EventHandler> int calculateStarIndex(MouseEvent<H> event) {
		Element star = stars.get(0);
		int relativeX = event.getRelativeX(star);
		if (relativeX < 0) {
			return 0;
		} else {
			return relativeX / star.getClientWidth() + 1;
		}
	}

	private <H extends EventHandler> void hoverFill(MouseEvent<H> event) {
		int index = calculateStarIndex(event);
		if (hoverIndex != index) {
			hoverIndex = index;
			fill(hoverIndex, "star-rating-hover");
		}

		int relativeX = event.getRelativeX(cancel);
		if (relativeX > 0 && relativeX < cancel.getClientWidth()) {
			cancel.addClassName("star-rating-hover");
		} else {
			cancel.removeClassName("star-rating-hover");
		}
	}

	@Override
	public void onMouseOver(MouseOverEvent event) {
		hoverFill(event);
	}

	@Override
	public void onMouseOut(MouseOutEvent event) {
		hoverIndex = -1;
		fill(selectedIndex, "star-rating-on");
		cancel.removeClassName("star-rating-hover");
	}

	@Override
	public void onMouseMove(MouseMoveEvent event) {
		hoverFill(event);
	}

	@Override
	public void onMouseDown(MouseDownEvent event) {
		int newSelectedIndex = calculateStarIndex(event);
		if (newSelectedIndex != selectedIndex) {
			selectedIndex = newSelectedIndex;
			if (ratingHandler != null) {
				ratingHandler.rate(getRating());
			}
		}
		fill(selectedIndex, "star-rating-on");
	}
}