package sd2526.trab.impl.zoho;

import java.util.Set;

import sd2526.trab.api.Message;

public class ZohoMessageSerializer {

  public static final String SEPARATOR = "------";
  public static final String SUB_SEPARATOR = "&";

  public static String serialize(Message msg) {
    return msg.getContents() + SEPARATOR
        + "id=" + msg.getId() + SUB_SEPARATOR
        + "sender=" + msg.getSender() + SUB_SEPARATOR
        + "destination=" + String.join(",", msg.getDestination()) + SUB_SEPARATOR
        + "subject=" + msg.getSubject() + SUB_SEPARATOR
        + "creationTime=" + msg.getCreationTime();
  }

  public static Message deserialize(String content, String subject) {
    if (content == null || !content.contains(SEPARATOR))
      return null;

    var parts = content.split(SEPARATOR, 2);
    var rawBody = parts[0];
    var properties = parts[1].split(SUB_SEPARATOR);

    var mimeBodyStart = rawBody.indexOf("\r\n\r\n");
    if (mimeBodyStart == -1)
      mimeBodyStart = rawBody.indexOf("\n\n");
    var body = mimeBodyStart >= 0 ? rawBody.substring(mimeBodyStart).trim() : rawBody;

    var id = properties[0].split("=", 2)[1];
    var sender = properties[1].split("=", 2)[1];
    var destination = properties[2].split("=", 2)[1];
    var subjectMsg = properties[3].split("=", 2)[1];
    var creationTime = Long.parseLong(properties[4].split("=", 2)[1].trim());

    var msg = new Message(id,
        sender,
        Set.of(destination.split(",")),
        subjectMsg,
        body);
    msg.setCreationTime(creationTime);
    return msg;
  }

  public static String extractId(String content) {
    if (content == null || !content.contains(SEPARATOR))
      return null;

    var parts = content.split(SEPARATOR, 2);
    var lines = parts[1].split(SUB_SEPARATOR);
    return lines[0].split("=", 2)[1];
  }
}