package org.kevoree.kcl.samples.webserver;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by duke on 8/18/14.
 */
public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws Exception {
        logger.info("KevoreeKernel sample HTTP server, browse to http://localhost:8080/");
        Server server = new Server(8080);
        server.setHandler(new AbstractHandler() {

            @Override
            public void handle(String s, Request request, HttpServletRequest httpServletRequest, HttpServletResponse response) throws IOException, ServletException {
                response.setContentType("text/html;charset=utf-8");
                response.setStatus(HttpServletResponse.SC_OK);
                request.setHandled(true);
                response.getWriter().println("<h1>Hello World</h1>");
            }
        });
        server.start();
        server.join();
    }

}
