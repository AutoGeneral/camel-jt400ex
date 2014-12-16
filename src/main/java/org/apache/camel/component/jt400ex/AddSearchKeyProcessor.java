package org.apache.camel.component.jt400ex;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.logging.Level;


/**
 * A camel processor that adds keys to the SearchKeysProvider. Create it with something like
 *
 * <bean id="searchKeysProvider" class="org.apache.camel.component.jt400ex.SearchKeysProviderImpl"/>
 *
 * <bean id="addSearchKeyProcessor" class="org.apache.camel.component.jt400ex.AddSearchKeyProcessor">
 * <property name="searchKeysProvider" ref="searchKeysProvider"/>
 * </bean>
 *
 * <camel:route>
 * <camel:from uri="activemq:queue:AddKeyQueue"/>
 * <camel:process ref="addSearchKeyProcessor"/>
 * </camel:route>
 */
public class AddSearchKeyProcessor implements Processor {
    private static final Logger LOGGER = LoggerFactory.getLogger(AddSearchKeyProcessor.class);

    private SearchKeysProvider searchKeysProvider;

    public AddSearchKeyProcessor() {
        LOGGER.info("Constructed AddSearchKeyProcessor");
    }

    @Override
    public void process(final Exchange exchange) throws Exception {
        final String keyName = exchange.getIn().getBody().toString();

        LOGGER.info("Added key with name " + keyName);
        searchKeysProvider.addKey(keyName);
    }

    /**
     *
     * @return The Spring bean that holds the keys
     */
    public SearchKeysProvider getSearchKeysProvider() {
        return searchKeysProvider;
    }

    /**
     *
     * @param searchKeysProvider The Spring bean that holds the keys
     */
    public void setSearchKeysProvider(final SearchKeysProvider searchKeysProvider) {
        this.searchKeysProvider = searchKeysProvider;
    }
}
