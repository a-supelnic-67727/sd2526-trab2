package sd2526.trab.impl.zoho;

import java.util.Set;

import sd2526.trab.api.Message;

public class ZohoMessageSerializer {

  public static final String SEPARATOR = "\n------\n";

  public static String serialize(Message msg) {
    return msg.getContents() + SEPARATOR
        + "id=" + msg.getId() + "\n"
        + "sender=" + msg.getSender() + "\n"
        + "destination=" + String.join(",", msg.getDestination()) + "\n"
        + "creationTime=" + msg.getCreationTime();
  }

  public static Message deserialize(String content, String subject) {
    if (content == null || !content.contains(SEPARATOR))
      return null;

    var parts = content.split(SEPARATOR, 2);
    var body = parts[0];
    var properties = parts[1].split("\n");

    var id = properties[0].split("=", 2)[1];
    var sender = properties[1].split("=", 2)[1];
    var destination = properties[2].split("=", 2)[1];
    var creationTime = Long.parseLong(properties[3].split("=", 2)[1]);

    var msg = new Message(id,
        sender,
        Set.of(destination.split(",")),
        subject,
        body);
    msg.setCreationTime(creationTime);
    return msg;
  }

  public static String extractId(String content) {
    if (content == null || !content.contains(SEPARATOR))
      return null;

    var parts = content.split(SEPARATOR, 2);
    var lines = parts[1].split("\n");
    return lines[0].split("=", 2)[1];
  }
}