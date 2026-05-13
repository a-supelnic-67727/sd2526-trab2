package sd2526.trab.impl.java.servers;

import static sd2526.trab.api.java.Result.error;
import static sd2526.trab.api.java.Result.ok;
import static sd2526.trab.api.java.Result.ErrorCode.BAD_REQUEST;
import static sd2526.trab.api.java.Result.ErrorCode.FORBIDDEN;
import static sd2526.trab.api.java.Result.ErrorCode.INTERNAL_ERROR;
import static sd2526.trab.api.java.Result.ErrorCode.NOT_FOUND;

import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import sd2526.trab.api.Message;
import sd2526.trab.api.User;
import sd2526.trab.api.java.Messages;
import sd2526.trab.api.java.Result;
import sd2526.trab.api.java.Result.ErrorCode;
import sd2526.trab.impl.java.clients.Clients;
import sd2526.trab.impl.utils.IP;
import sd2526.trab.impl.zoho.Zoho;
import sd2526.trab.impl.zoho.msgs.ZohoMessage;

public class MessagesProxy extends JavaMessages1 {

  private static Logger Log = Logger.getLogger(MessagesProxy.class.getName());

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
    Log.info(() -> "getInboxMessage : name = %s, mid = %s, pwd = %s\n".formatted(name, mid, pwd));
    if (name == null || mid == null || pwd == null) {
      return Result.error(BAD_REQUEST);
    }

    try {
      var userResult = getUser(name, pwd);
      if (!userResult.isOK())
        return Result.error(userResult.error());

      ZohoMessage zohoMsg = zoho.getMessage(mid);
      if (zohoMsg == null) {
        return Result.error(NOT_FOUND);
      } else {
        Message msg = new Message(
            zohoMsg.messageId(),
            zohoMsg.fromAddress(),
            Set.of(zohoMsg.toAddress().split(",")),
            zohoMsg.subject(),
            zohoMsg.content());
        return Result.ok(msg);
      }
    } catch (Exception x) {
      x.printStackTrace();
      return Result.error(INTERNAL_ERROR);
    }
  }

  @Override
  public Result<List<String>> getAllInboxMessages(String name, String pwd) {
    Log.info(() -> "getAllInboxMessages : name = %s, pwd = %s\n".formatted(name, pwd));

    if (name == null || pwd == null) {
      return Result.error(BAD_REQUEST);
    }

    try {
      var userResult = getUser(name, pwd);
      if (!userResult.isOK())
        return Result.error(userResult.error());
      var inboxMessages = zoho.getMessages(null);
      return Result.ok(inboxMessages);
    } catch (Exception x) {
      x.printStackTrace();
      return Result.error(INTERNAL_ERROR);
    }

  }

  @Override
  public Result<Void> removeInboxMessage(String name, String mid, String pwd) {
    Log.info(() -> "removeInboxMessage : name = %s, mid = %s, pwd = %s\n".formatted(name, mid, pwd));
    if (name == null || mid == null || pwd == null) {
      return Result.error(BAD_REQUEST);
    }
    try {
      var userResult = getUser(name, pwd);
      if (!userResult.isOK())
        return Result.error(userResult.error());

      ZohoMessage zohoMsg = zoho.getMessage(mid);
      if (zohoMsg == null) {
        return Result.error(NOT_FOUND);
      }
      zoho.removeInboxMessage(mid);
      return Result.ok();
    } catch (Exception x) {
      x.printStackTrace();
      return Result.error(INTERNAL_ERROR);
    }
  }

  @Override
  public Result<Void> deleteMessage(String name, String mid, String pwd) {
    Log.info(() -> "deleteMessage : name = %s, mid = %s, pwd = %s\n".formatted(name, mid, pwd));
    if (name == null || mid == null || pwd == null) {
      return Result.error(BAD_REQUEST);
    }
    try {
      var userResult = getUser(name, pwd);
      if (!userResult.isOK())
        return Result.error(userResult.error());

      ZohoMessage zohoMsg = zoho.getMessageMetadata(mid);
      if (zohoMsg.receivedTime() == null
          || System.currentTimeMillis() - Long.parseLong(zohoMsg.receivedTime()) > 30_000) {
        return Result.ok();
      }
      for (String dest : zohoMsg.toAddress().split(",")) {
        dest = dest.trim();
        var domain = dest.split("@", 2)[1];

        if (domain.equals(IP.domain())) {
          zoho.removeInboxMessage(mid);
        } else {
          Clients.AdminMessagesClient.get(domain).remoteDeleteMessage(mid); // not sure if works
        }
      }
      return Result.ok();

    } catch (Exception x) {
      x.printStackTrace();
      return Result.error(INTERNAL_ERROR);
    }
  }

  @Override
  public Result<List<String>> searchInbox(String name, String pwd, String query) {
    Log.info(() -> "searchInbox : name = %s, pwd = %s, query=%s\n".formatted(name, pwd, query));

    if (name == null || pwd == null || query == null) {
      return Result.error(BAD_REQUEST);
    }

    try {
      var userResult = getUser(name, pwd);
      if (!userResult.isOK())
        return Result.error(userResult.error());
      var inboxMessages = zoho.getMessages(query);
      return Result.ok(inboxMessages);
    } catch (Exception x) {
      x.printStackTrace();
      return Result.error(INTERNAL_ERROR);
    }
  }

  @Override
  public Result<Void> remotePostMessage(Message msg) {
    Log.info(() -> "remotePostMessage : msg = %s\n".formatted(msg));
    try {
      var zohoMsg = new ZohoMessage(
          msg.getId(),
          msg.senderAddress(),
          String.join(",", msg.getDestination()),
          msg.getSubject(),
          msg.getContents(),
          null);
      zoho.sendMessage(zohoMsg);
      return Result.ok();
    } catch (Exception x) {
      x.printStackTrace();
      return Result.error(INTERNAL_ERROR);
    }
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
