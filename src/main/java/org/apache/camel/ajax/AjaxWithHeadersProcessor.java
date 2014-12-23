package org.apache.camel.ajax;

import com.codesnippets4all.json.parsers.JsonParserFactory;
import com.codesnippets4all.json.parsers.JSONParser;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;


/**
 * The AJAX servlet doesn't support passing through JMS headers. This processor
 * reads JSON messages with a known format, extracts the headers and adds them
 * to the exchange.
 */
public class AjaxWithHeadersProcessor implements Processor {
    private static final Logger LOGGER = LoggerFactory.getLogger(AjaxWithHeadersProcessor.class);

    @Override
    public void process(final Exchange exchange) throws Exception {

        try {
            final String incomingMessage = exchange.getIn().getBody(String.class).toString();

            final JsonParserFactory factory=JsonParserFactory.getInstance();
            final JSONParser parser = factory.newJsonParser();

            final Map jsonData = parser.parseJson(incomingMessage);

            final List headers= (List)jsonData.get("headers");

            for (final Object header : headers) {
                final Map headerMap = (Map)header;
                final String headerName = headerMap.get("name").toString();
                final String headerValue = headerMap.get("value").toString();

                LOGGER.info("Setting header " + headerName + " " + headerValue);

                exchange.getIn().setHeader(headerName, headerValue);
            }

            final String message = jsonData.get("message").toString();

            LOGGER.info("Setting body " + message);

            exchange.getIn().setBody(message);

        } catch (final Exception ex) {
            LOGGER.error(ex.toString());
        }

        /*exchange.getIn().setHeader("key", "MCASPERSON");
        exchange.getIn().setBody("hi");*/
    }
}
