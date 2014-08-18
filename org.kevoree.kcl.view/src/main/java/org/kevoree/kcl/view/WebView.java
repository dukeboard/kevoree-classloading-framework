package org.kevoree.kcl.view;

import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import org.kevoree.microkernel.BootInfo;
import org.kevoree.microkernel.BootInfoLine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;

/**
 * Created by duke on 8/14/14.
 */
public class WebView {

    public void display(final BootInfo bootInfo) {
        Undertow server = Undertow.builder()
                .addHttpListener(8080, "localhost")
                .setHandler(new HttpHandler() {
                    @Override
                    public void handleRequest(final HttpServerExchange exchange) throws Exception {
                        if (exchange.getRequestPath().contains("jquery")) {
                            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/javascript");
                            exchange.getResponseSender().send(fromStream(this.getClass().getClassLoader().getResourceAsStream("jquery.js")));
                        } else if (exchange.getRequestPath().contains("vivagraph")) {
                            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/javascript");
                            exchange.getResponseSender().send(fromStream(this.getClass().getClassLoader().getResourceAsStream("vivagraph.js")));
                        } else {
                            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/html");
                            StringBuilder buffer = new StringBuilder();
                            buffer.append("<!DOCTYPE html><html>\n");
                            buffer.append("<head><title>Kevoree Boot Info Viewer</title><script type=\"text/javascript\" src=\"vivagraph.js\"></script><script type=\"text/javascript\" src=\"jquery.js\"></script>\n");
                            buffer.append("<style type=\"text/css\" media=\"screen\">html, body, svg { width: 100%; height: 100%;}</style>\n");
                            buffer.append("</head>\n");
                            buffer.append("<script type=\"text/javascript\">\n");
                            buffer.append("function main () {\n");

                            buffer.append("var graph = Viva.Graph.graph();\n");
                            HashMap<String, Integer> maps = new HashMap<String, Integer>();
                            for (BootInfoLine line : bootInfo.getLines()) {
                                maps.put(line.getURL(), maps.size());
                            }
                            for (String url : maps.keySet()) {
                                buffer.append("graph.addNode(" + maps.get(url) + ",'" + url + "');\n");
                            }
                            for (BootInfoLine line : bootInfo.getLines()) {
                                for (String dep : line.getDependencies()) {
                                    buffer.append("graph.addLink(" + maps.get(line.getURL()) + "," + maps.get(dep) + ");\n");
                                }
                            }
                            buffer.append("var graphics = Viva.Graph.View.svgGraphics();\n");
                            buffer.append("highlightRelatedNodes = function(node,nodeId, isOn) { if(isOn){ $(desc).html(node.data); } else { $(desc).html(''); }  var nodeUI = graphics.getNodeUI(nodeId);nodeUI.children['1'].attr('fill', isOn ? 'orange' : 'black'); nodeUI.children['0'].attr('style', isOn ? 'stroke: orange' : ''); nodeUI.attr('color', isOn ? 'orange' : 'gray'); graph.forEachLinkedNode(nodeId, function(node, link){ var linkUI = graphics.getLinkUI(link.id); if (link && linkUI) { linkUI.attr('stroke', isOn ? 'orange' : 'gray'); } }); };\n");

                            buffer.append("graphics.node(function(node) {\n");
                            buffer.append("var ui = Viva.Graph.svg('g');\n");
                            buffer.append("svgText = Viva.Graph.svg('text').attr('y', '4px').text(node.data);\n");
                            buffer.append("rect = Viva.Graph.svg('rect').attr('width', 10).attr('height', 10).attr('y', '10px').attr('x', '10px').attr('fill', node.data ? node.data : '#00a2e8');\n");
                            buffer.append("ui.append(svgText);ui.append(rect);");
                            buffer.append("$(ui).hover(function() { highlightRelatedNodes(node,node.id, true);}, function() { highlightRelatedNodes(node,node.id, false); });\n");
                            buffer.append("return ui;\n");


                            buffer.append("}).placeNode(function(nodeUI, pos) {\n");
                            buffer.append("nodeUI.attr('transform','translate(' +(pos.x - 16) + ',' + (pos.y - 16) +')');\n");
                            buffer.append("})\n");

                            buffer.append("var layout = Viva.Graph.Layout.forceDirected(graph, {springLength : 200});\n");

                            buffer.append("renderer = Viva.Graph.View.renderer(graph, {layout : layout,graphics : graphics,renderLinks : true});\n");


                            buffer.append("renderer.run();\n");

                            buffer.append("}\n");
                            buffer.append("</script>\n");
                            buffer.append("<body onload='main()'><div id=\"desc\" style=\"position:absolute;bottom:0px;left:0px;background:orange;width:100%;color:#FFF;padding-left:10%;\"></div></body></html>\n");
                            exchange.getResponseSender().send(buffer.toString());

                        }


                    }
                }).build();
        server.start();
    }

    private static String fromStream(InputStream in) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        StringBuilder out = new StringBuilder();
        String newLine = System.getProperty("line.separator");
        String line;
        while ((line = reader.readLine()) != null) {
            out.append(line);
            out.append(newLine);
        }
        return out.toString();
    }


}
