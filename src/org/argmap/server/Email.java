package org.argmap.server;

import java.io.UnsupportedEncodingException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.argmap.client.ServiceException;

import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserServiceFactory;

public class Email {

	private static final Logger log = Logger.getLogger(Email.class.getName());

	public static void sendFromCurrentUser(String subjectPlainText,
			String contentHTMLText, String contentPlainText, String toEmail,
			String toName) throws ServiceException {
		send(subjectPlainText, contentHTMLText, contentPlainText, getUser()
				.getEmail(), getUser().getNickname(), toEmail, toName);
	}

	private static User getUser() throws ServiceException {
		User user = UserServiceFactory.getUserService().getCurrentUser();
		if (user == null) {
			throw new ServiceException(
					"cannot send email from user: user not logged in");
		}
		return user;
	}

	public static void sendFromAndToCurrentUser(String subjectPlainText,
			String contentHTMLText, String contentPlainText)
			throws ServiceException {
		send(subjectPlainText, contentHTMLText, contentPlainText, getUser()
				.getEmail(), getUser().getNickname(), getUser().getEmail(),
				getUser().getNickname());
	}

	public static void send(String subjectPlainText, String contentHTMLText,
			String contentPlainText, String fromEmail, String fromName,
			String toEmail, String toName) throws ServiceException {
		Properties props = new Properties();
		Session session = Session.getDefaultInstance(props, null);

		try {
			Message message = new MimeMessage(session);
			message.setFrom(new InternetAddress(fromEmail, fromName));
			message.addRecipient(Message.RecipientType.TO, new InternetAddress(
					toEmail, toName));
			message.setSubject(subjectPlainText);

			MimeMultipart content = new MimeMultipart("alternative");
			MimeBodyPart text = new MimeBodyPart();
			MimeBodyPart html = new MimeBodyPart();
			text.setText(contentPlainText);
			html.setContent(contentHTMLText, "text/html");
			content.addBodyPart(text);
			content.addBodyPart(html);
			message.setContent(content);

			Transport.send(message);

		} catch (AddressException e) {
			log.log(Level.SEVERE, "Exception while processing email from \""
					+ fromEmail + "\" to \"" + toEmail + "\"", e);
			throw new ServiceException("AddressException on Server");
		} catch (MessagingException e) {
			log.log(Level.SEVERE, "Exception while processing email from \""
					+ fromEmail + "\" to \"" + toEmail
					+ "\" with contentPlainText \"" + contentPlainText
					+ "\" and contentHTMLText \"" + contentHTMLText + "\"", e);
			throw new ServiceException("MessagingException on Server");
		} catch (UnsupportedEncodingException e) {
			log.log(Level.SEVERE, "Exception while processing email from \""
					+ fromEmail + "\" to \"" + toEmail + "\"", e);
			throw new ServiceException("UnsupportedEncodingException on Server");
		}
	}
}
