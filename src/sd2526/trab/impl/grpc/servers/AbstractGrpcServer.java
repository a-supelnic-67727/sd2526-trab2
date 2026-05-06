package sd2526.trab.impl.grpc.servers;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.util.List;
import java.util.logging.Logger;

import javax.net.ssl.KeyManagerFactory;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NettyServerBuilder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import sd2526.trab.impl.discovery.Discovery;
import sd2526.trab.impl.java.servers.AbstractServer;
import sd2526.trab.impl.utils.IP;

public abstract class AbstractGrpcServer extends AbstractServer {
	private static final String SERVER_BASE_URI = "grpc://%s:%s%s";

	private static final String GRPC_CTX = "/grpc";

	protected final Server server;

	protected final int port;

	protected AbstractGrpcServer(Logger log, String service, int port) {
		super(log, service, String.format(SERVER_BASE_URI, IP.hostAddress(), port, GRPC_CTX));

		var builder = ServerBuilder.forPort(port);
		for (var s : controllers(super.serverURI))
			builder.addService(s);

		this.server = builder.build();
		this.port = port;
	}

	protected abstract List<GrpcController> controllers(String uri);

	protected void start() throws IOException {

		try {
			String keyStoreFilename = System.getProperty("javax.net.ssl.keyStore");
			String keyStorePassword = System.getProperty("javax.net.ssl.keyStorePassword");
			KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
			try (FileInputStream input = new FileInputStream(keyStoreFilename)) {
				keystore.load(input, keyStorePassword.toCharArray());
			}
			KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(
					KeyManagerFactory.getDefaultAlgorithm());
			keyManagerFactory.init(keystore, keyStorePassword.toCharArray());
			SslContext context = GrpcSslContexts.configure(
					SslContextBuilder.forServer(keyManagerFactory)).build();

			NettyServerBuilder builder = NettyServerBuilder.forPort(port).sslContext(context);
			for (var c : controllers(super.serverURI))
				builder.addService(c);

			Server server = builder.build().start();

			Discovery.getInstance().announce(serviceName(), super.serverURI);
			Log.info(String.format("%s gRPC Server ready @ %s\n", service, serverURI));

			server.awaitTermination();
			Runtime.getRuntime().addShutdownHook(new Thread(() -> {
				System.err.println("*** shutting down gRPC server since JVM is shutting down");
				server.shutdownNow();
				System.err.println("*** server shut down");
			}));
		} catch (Exception e) {
			Log.severe(String.format("Failed to start server: %s".formatted(e.getMessage())));
			throw new IOException("Failed to start gRPC server", e);
		}
	}

}
