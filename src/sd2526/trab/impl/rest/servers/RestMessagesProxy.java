package sd2526.trab.impl.rest.servers;

import java.net.UnknownHostException;
import java.util.logging.Logger;

import org.glassfish.jersey.server.ResourceConfig;

import sd2526.trab.api.java.Messages;

public class RestMessagesProxy extends AbstractRestServer {
  public static final int PORT = 6767;

  private static Logger Log = Logger.getLogger(RestMessagesProxy.class.getName());

  RestMessagesProxy() throws UnknownHostException {
    super(Log, Messages.SERVICE_NAME, PORT);
  }

  @Override
  void registerResources(ResourceConfig config) {
    config.register(RestMessagesProxyResource.class);
  }

  public static void main(String[] args) throws UnknownHostException {
    new RestMessagesProxy().start();
  }
}
