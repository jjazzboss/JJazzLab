package org.jjazz.util.api;

import org.netbeans.modules.openide.windows.GlobalActionContextImpl;
import org.openide.util.ContextGlobalProvider;
import org.openide.util.Lookup;
import org.openide.util.lookup.AbstractLookup;
import org.openide.util.lookup.InstanceContent;
import org.openide.util.lookup.ProxyLookup;
import org.openide.util.lookup.ServiceProvider;


/**
 * Supersedes the original Netbeans ContextGlobalProvider used by Utilities.actionsGlobalContext().
 * <p>
 * The new actions global context provided merges the Netbeans original context (which proxies the lookup of the active TopComponent)
 * and another lookup in which objects can be directly added or removed.
 * <p>
 * To use this class you must edit the Windows System API module dependency: change the dependency to an implementation version so
 * that the org.netbeans.modules.openide.windows package is on the classpath.
 */
@ServiceProvider(service = ContextGlobalProvider.class, supersedes = "org.netbeans.modules.openide.windows.GlobalActionContextImpl")
public class GlobalActionContextProxy implements ContextGlobalProvider
{

    private static GlobalActionContextProxy INSTANCE;
    /**
     * The native NetBeans global context Lookup provider
     */
    private final GlobalActionContextImpl globalContextProvider;
    /**
     * The primary lookup managed by the platform
     */
    private final Lookup globalContextLookup;
    /**
     * The project lookup managed by this class
     */
    private Lookup projectLookup;
    /**
     * The actual Lookup returned by this class
     */
    private Lookup proxyLookup;
    /**
     * The additional content for our proxy lookup
     */
    private final InstanceContent content;

    public GlobalActionContextProxy()
    {
        this.content = new InstanceContent();

        // Create the default GlobalContextProvider
        this.globalContextProvider = new GlobalActionContextImpl();
        this.globalContextLookup = this.globalContextProvider.createGlobalContext();
        
        INSTANCE = this;
    }

    static public GlobalActionContextProxy getInstance()
    {
        return INSTANCE;
    }

    /**
     * Returns a ProxyLookup that adds the application-wide content to the original lookup returned by
     * Utilities.actionsGlobalContext().
     *
     * @return a ProxyLookup that includes the default global context plus our own content
     */
    @Override
    public Lookup createGlobalContext()
    {
        if (this.proxyLookup == null)
        {
            // Merge the two lookups that make up the proxy
            this.projectLookup = new AbstractLookup(content);
            this.proxyLookup = new ProxyLookup(this.globalContextLookup, this.projectLookup);
        }
        return this.proxyLookup;
    }

    /**
     * Adds an Object to the application scope global selection.
     *
     * @param obj
     */
    public void add(Object obj)
    {
        this.content.add(obj);
    }

    /**
     * Removes an Object from the application scope global selection.
     *
     * @param obj
     */
    public void remove(Object obj)
    {
        this.content.remove(obj);
    }
}
