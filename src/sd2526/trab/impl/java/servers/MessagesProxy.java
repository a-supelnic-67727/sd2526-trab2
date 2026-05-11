package sd2526.trab.impl.java.servers;

import static sd2526.trab.api.java.Result.error;
import static sd2526.trab.api.java.Result.ok;
import static sd2526.trab.api.java.Result.ErrorCode.BAD_REQUEST;
import static sd2526.trab.api.java.Result.ErrorCode.FORBIDDEN;
import static sd2526.trab.api.java.Result.ErrorCode.INTERNAL_ERROR;

import java.util.List;
import java.util.logging.Logger;

import sd2526.trab.api.Message;
import sd2526.trab.api.User;
import sd2526.trab.api.java.Messages;
import sd2526.trab.api.java.Result;
import sd2526.trab.api.java.Result.ErrorCode;
import sd2526.trab.impl.java.clients.Clients;
import sd2526.trab.impl.zoho.Zoho;
import sd2526.trab.impl.zoho.msgs.ZohoMessage;

public class MessagesProxy implements Messages {

  private static Logger Log = Logger.getLogger(JavaMessages.class.getName());

  private final Zoho zoho = Zoho.getInstance();

  @Override
  public Result<String> postMessage(String pwd, Message msg) {
    Log.info(() -> "postMessage : pwd = %s, msg = %s\n".formatted(pwd, msg));

    if (pwd == null || msg == null || msg.getSender() == null ||
        msg.getDestination() == null || msg.getDestination().isEmpty()) {
      return Result.error(BAD_REQUEST);
    }

    try {
      var userResult = getUser(msg.getSender(), pwd);
      if (!userResult.isOK())
        return Result.error(userResult.error());
      var user = userResult.value();
      String senderAddress = user.getName() + "@" + user.getDomain();
      msg.setSender("%s <%s@%s>".formatted(user.getDisplayName(), user.getName(), user.getDomain()));
      var zohoMsg = new ZohoMessage(
          null,
          senderAddress,
          String.join(",", msg.getDestination()),
          msg.getSubject(),
          msg.getContents(),
          null);
      var messageId = zoho.sendMessage(zohoMsg);
      if (messageId == null) {
        return Result.error(INTERNAL_ERROR);
      } else {
        msg.setId(messageId);
        return Result.ok(messageId);
      }
    } catch (Exception x) {
      x.printStackTrace();
      return Result.error(INTERNAL_ERROR);
    }
  }

  @Override
  public Result<Message> getInboxMessage(String name, String mid, String pwd) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'getInboxMessage'");
  }

  @Override
  public Result<List<String>> getAllInboxMessages(String name, String pwd) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'getAllInboxMessages'");
  }

  @Override
  public Result<Void> removeInboxMessage(String name, String mid, String pwd) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'removeInboxMessage'");
  }

  @Override
  public Result<Void> deleteMessage(String name, String mid, String pwd) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'deleteMessage'");
  }

  @Override
  public Result<List<String>> searchInbox(String name, String pwd, String query) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'searchInbox'");
  }

  protected Result<User> getUser(String user, String pwd) {
    try {
      var name = user.split("@", 2)[0];
      return Clients.UsersClient.get().getUser(name, pwd);
    } catch (Exception x) {
      x.printStackTrace();
      return Result.error(INTERNAL_ERROR);
    }
  }
}
