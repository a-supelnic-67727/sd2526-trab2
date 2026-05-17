package sd2526.trab.impl.java.servers;

import static sd2526.trab.api.java.Result.ErrorCode.BAD_REQUEST;
import static sd2526.trab.api.java.Result.ErrorCode.INTERNAL_ERROR;
import static sd2526.trab.api.java.Result.ErrorCode.NOT_FOUND;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import sd2526.trab.api.Message;
import sd2526.trab.api.User;
import sd2526.trab.api.java.Result;
import sd2526.trab.impl.java.clients.Clients;
import sd2526.trab.impl.utils.IP;
import sd2526.trab.impl.zoho.Zoho;
import sd2526.trab.impl.zoho.ZohoMessageSerializer;
import sd2526.trab.impl.zoho.msgs.ZohoMessage;

public class MessagesProxy extends JavaMessages1 {

  private static Logger Log = Logger.getLogger(MessagesProxy.class.getName());
  private final ConcurrentHashMap<String, String> idMap = new ConcurrentHashMap<>();

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
      if (idMap.containsKey(msg.getId()))
        return Result.ok();
      var user = userResult.value();
      String senderAddress = user.getName() + "@" + user.getDomain();
      msg.setSender("%s <%s@%s>".formatted(user.getDisplayName(), user.getName(), user.getDomain()));
      var zohoMsg = new ZohoMessage(
          null,
          senderAddress,
          String.join(",", msg.getDestination()),
          msg.getSubject(),
          ZohoMessageSerializer.serialize(msg),
          null);
      var messageId = zoho.sendMessage(zohoMsg);
      var zohoInboxMsgId = zoho.updateZohoId(msg.getId());
      idMap.put(msg.getId(), zohoInboxMsgId);
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
      var zohoId = idMap.get(mid);
      if (zohoId == null) {
        zohoId = zoho.updateZohoId(mid);
        if (zohoId != null) {
          idMap.put(mid, zohoId);
        }
      }
      ZohoMessage zohoMsg = zoho.getMessage(zohoId);
      if (zohoMsg == null) {
        return Result.error(NOT_FOUND);
      } else {
        Log.info("Content: " + zohoMsg.content());
        Message msg = ZohoMessageSerializer.deserialize(zohoMsg.content(), zohoMsg.subject());
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
      if (!userResult.isOK()) {
        return Result.error(userResult.error());
      }

      var originalIds = zoho.getMessages(null).stream()
          .map(zohoMsgId -> {
            try {
              Log.info("Retrieved message: " + zohoMsgId);
              ZohoMessage zohoMsg = zoho.getMessage(zohoMsgId);
              Log.info("Message id: " + ZohoMessageSerializer.extractId(zohoMsg.content()));
              return ZohoMessageSerializer.extractId(zohoMsg.content());
            } catch (Exception x) {
              x.printStackTrace();
              return null;
            }
          }).toList();
      return Result.ok(originalIds);
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

      var zohoId = idMap.get(mid);
      if (zohoId == null) {
        zohoId = zoho.updateZohoId(mid);
        if (zohoId != null) {
          idMap.put(mid, zohoId);
        }
      }
      Log.info("Zoho message id: " + zohoId);
      ZohoMessage zohoMsg = zoho.getMessage(zohoId);
      if (zohoMsg == null) {
        return Result.error(NOT_FOUND);
      }

      zoho.removeInboxMessage(zohoId);
      idMap.remove(mid);
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

      var zohoId = idMap.get(mid);
      ZohoMessage zohoMsg = zoho.getMessage(zohoId);
      if (zohoMsg == null) {
        return Result.error(NOT_FOUND);
      }

      Message originalMsg = ZohoMessageSerializer.deserialize(zohoMsg.content(), zohoMsg.subject());
      if (originalMsg == null) {
        return Result.error(NOT_FOUND);
      }

      if (System.currentTimeMillis() - originalMsg.getCreationTime() > 30_000) {
        return Result.ok();
      }

      for (String dest : originalMsg.getDestination()) {
        dest = dest.trim();
        var domain = dest.split("@", 2)[1];

        if (domain.equals(IP.domain())) {
          zoho.removeInboxMessage(zohoId);
          idMap.remove(mid);
        } else {
          Clients.AdminMessagesClient.get(domain).remoteDeleteMessage(originalMsg.getId());
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
      var originalIds = zoho.getMessages(query).stream()
          .map(zohoMsgId -> {
            try {
              ZohoMessage zohoMsg = zoho.getMessage(zohoMsgId);
              return ZohoMessageSerializer.extractId(zohoMsg.content());
            } catch (Exception x) {
              x.printStackTrace();
              return null;
            }
          }).toList();
      return Result.ok(originalIds);
    } catch (Exception x) {
      x.printStackTrace();
      return Result.error(INTERNAL_ERROR);
    }
  }

  @Override
  public Result<Void> remotePostMessage(Message msg) {
    Log.info(() -> "remotePostMessage : msg = %s\n".formatted(msg));
    try {
      if (idMap.putIfAbsent(msg.getId(), "pending") != null)
        return Result.ok();
      var zohoMsg = new ZohoMessage(
          msg.getId(),
          msg.senderAddress(),
          String.join(",", msg.getDestination()),
          msg.getSubject(),
          ZohoMessageSerializer.serialize(msg),
          null);
      zoho.sendMessage(zohoMsg);
      var zohoInboxMsgId = zoho.updateZohoId(msg.getId());
      idMap.put(msg.getId(), zohoInboxMsgId);
      return Result.ok();
    } catch (Exception x) {
      x.printStackTrace();
      return Result.error(INTERNAL_ERROR);
    }
  }

  @Override
  public Result<Void> remoteDeleteMessage(String mid) {
    Log.info(() -> "remoteDeleteMessage : mid = %s\n".formatted(mid));
    try {
      var zohoId = idMap.get(mid);
      zoho.removeInboxMessage(zohoId);
      idMap.remove(mid);
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
