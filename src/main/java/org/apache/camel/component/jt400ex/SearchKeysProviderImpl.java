package org.apache.camel.component.jt400ex;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

/**
 * An implementation of SearchKeysProvider.
 *
 * Include a reference to this class in the beans.xml file with the entry
 *
 * <bean id="searchKeysProvider" class="org.apache.camel.component.jt400ex.SearchKeysProviderImpl"/>
 *
 * Then use the same bean id when defining the endpoint like
 *
 * <camel:route>
 * <camel:from uri="jt400ex://user:password@server/QUEUENAME?keyed=true&amp;searchKeysProvider=searchKeysProvider"/>
 * <camel:to uri="activemq:topic:JMSOutput"/>
 * </camel:route>
 */
public class SearchKeysProviderImpl implements SearchKeysProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(SearchKeysProviderImpl.class);

    final List<String> keys = new ArrayList<String>();

    public SearchKeysProviderImpl() {
        LOGGER.info("Constructed SearchKeysProvider");
    }

    @Override
    synchronized public void addKey(final String key) {
        if (keys.indexOf(key) == -1) {
            keys.add(key);
        }
    }

    @Override
    synchronized public void removeKey(final String key) {
        keys.remove(key);
    }

    @Override
    synchronized public List<String> getKeys() {
        return Collections.unmodifiableList(keys);
    }
}
