package io.jooby.utow;

import io.jooby.Mode;
import io.jooby.Route;
import io.jooby.Router;
import io.jooby.Server;
import io.jooby.internal.utow.UtowContext;
import io.undertow.Undertow;
import io.undertow.UndertowOptions;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.BlockingHandler;
import io.undertow.util.HttpString;

import javax.annotation.Nonnull;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Utow implements Server {

  private Undertow server;

  private int port = 8080;

  private Mode mode = Mode.WORKER;

  private Path tmpdir = Paths.get(System.getProperty("java.io.tmpdir"));

  @Override public Server port(int port) {
    this.port = port;
    return this;
  }

  @Override public Server mode(Mode mode) {
    this.mode = mode;
    return this;
  }

  @Nonnull @Override public Server tmpdir(@Nonnull Path tmpdir) {
    this.tmpdir = tmpdir;
    return this;
  }

  @Override public Server start(Router router) {
    HttpHandler uhandler = exchange -> {
      HttpString method = exchange.getRequestMethod();
      Route route = router.match(method.toString().toUpperCase(), exchange.getRequestPath());
      Route.RootHandler handler = route.pipeline();
      handler.apply(
          new UtowContext(exchange, exchange.getConnection().getWorker(), router.errorHandler(),
              route, tmpdir));
    };
    if (mode == Mode.WORKER) {
      uhandler = new BlockingHandler(uhandler);
    }
    server = Undertow.builder()
        .setServerOption(UndertowOptions.DECODE_URL, false)
        .addHttpListener(port, "0.0.0.0")
        .setHandler(uhandler).build();

    server.start();

    return this;
  }

  @Override public Server stop() {
    server.stop();
    return this;
  }
}
