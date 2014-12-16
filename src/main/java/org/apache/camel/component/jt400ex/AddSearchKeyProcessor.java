package org.apache.camel.component.jt400ex;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A camel processor that adds keys to the SearchKeysProvider. Create it with something like
 *
 * <bean id="searchKeysProvider" class="com.apache.camel.component.jt400ex.SearchKeysProviderImpl"/>
 *
 * <bean id="addSearchKeyProcessor" class="org.apache.camel.component.jt400ex.AddSearchKeyProcessor">
 * <property name="searchKeysProvider" value="searchKeysProvider"/>
 * </bean>
 *
 * <camel:route>
 * <camel:from uri="activemq:queue:AddKeyQueue"/>
 * <camel:process ref="addSearchKeyProcessor"/>
 * </camel:route>
 */
public class AddSearchKeyProcessor implements Processor {
    private static final Logger LOGGER = Logger.getLogger(AddSearchKeyProcessor.class.getName());

    private String searchKeysProvider;

    @Override
    public void process(final Exchange exchange) throws Exception {
        final CamelContext camelContext = exchange.getContext();
        final SearchKeysProvider searchKeysProviderBean = (SearchKeysProvider)camelContext.getRegistry().lookup(searchKeysProvider);

        final String keyName = exchange.getIn().getBody().toString();
        searchKeysProviderBean.addKey(keyName);

        LOGGER.log(Level.INFO, "Added key with name " + keyName);
    }

    /**
     *
     * @return The name of the spring bean that holds the keys
     */
    public String getSearchKeysProvider() {
        return searchKeysProvider;
    }

    /**
     *
     * @param searchKeysProvider The name of the spring bean that holds the keys
     */
    public void setSearchKeysProvider(final String searchKeysProvider) {
        this.searchKeysProvider = searchKeysProvider;
    }
}
