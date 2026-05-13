package sd2526.trab.impl.rest.servers;

import java.util.List;

import jakarta.inject.Singleton;
import sd2526.trab.api.Message;
import sd2526.trab.api.rest.RestMessages;
import sd2526.trab.impl.java.servers.MessagesProxy;

@Singleton
public class RestMessagesProxyResource extends RestResource implements RestMessages {

  private final MessagesProxy impl = new MessagesProxy();

  @Override
  public String postMessage(String pwd, Message msg) {
    return super.resultOrThrow(impl.postMessage(pwd, msg));
  }

  @Override
  public Message getMessage(String name, String mid, String pwd) {
    return super.resultOrThrow(impl.getInboxMessage(name, mid, pwd));
  }

  @Override
  public List<String> getMessages(String name, String pwd, String query) {
    if (query != null && !query.isEmpty())
      return super.resultOrThrow(impl.searchInbox(name, pwd, query));
    else
      return super.resultOrThrow(impl.getAllInboxMessages(name, pwd));
  }

  @Override
  public void removeFromUserInbox(String name, String mid, String pwd) {
    super.resultOrThrow(impl.removeInboxMessage(name, mid, pwd));
  }

  @Override
  public void deleteMessage(String name, String mid, String pwd) {
    super.resultOrThrow(impl.deleteMessage(name, mid, pwd));
  }
}