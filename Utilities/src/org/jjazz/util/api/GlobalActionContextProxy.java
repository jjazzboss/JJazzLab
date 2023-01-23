package org.jjazz.util.api;

import org.netbeans.modules.openide.windows.GlobalActionContextImpl;
import org.openide.util.ContextGlobalProvider;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;
import org.openide.util.lookup.ProxyLookup;
import org.openide.util.lookup.ServiceProvider;


/**
 * Supersedes the original Netbeans ContextGlobalProvider used by Utilities.actionsGlobalContext().
 * <p>
 * The context created by this class merges the Netbeans original context (which proxies the lookup of the active TopComponent)
 * with another user-defined lookup (which can be changed).
 * <p>
 * NOTE: in order to get access GlobalActionContextImpl, change the Windows System API module dependency to an implementation
 * version so that the org.netbeans.modules.openide.windows package is on the classpath.
 */
@ServiceProvider(service = ContextGlobalProvider.class, supersedes = "org.netbeans.modules.openide.windows.GlobalActionContextImpl")
public class GlobalActionContextProxy implements ContextGlobalProvider, Lookup.Provider
{

    private static GlobalActionContextProxy INSTANCE;


    /**
     * The actual Lookup returned by this class
     */
    private Lookup lookup;
    /**
     * The original Netbeans lookup returned by GlobalActionContextImpl
     */
    private Lookup originalLookup;
    /**
     * Our proxy lookup to the used-defined lookup.
     */
    private Lookup proxy2UserLookup;
    /**
     * The user lookup. Can be null.
     */
    private Lookup userLookup;


    public GlobalActionContextProxy()
    {
    }

    static public GlobalActionContextProxy getInstance()
    {
        if (INSTANCE == null)
        {
            INSTANCE = Lookup.getDefault().lookup(GlobalActionContextProxy.class);
        }
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
        if (this.lookup == null)
        {
            // Create the default GlobalContextProvider
            originalLookup = new GlobalActionContextImpl().createGlobalContext();

            // Merge with a proxy lookup
            proxy2UserLookup = Lookups.proxy(this);
            this.lookup = new ProxyLookup(originalLookup, proxy2UserLookup);
        }
        return this.lookup;
    }


    /**
     * The current user lookup.
     *
     * @return Null if not set.
     */
    public synchronized Lookup getUserLookup()
    {
        return userLookup;
    }

    /**
     * Set the user lookup to be merged into the lookup returned by Utilities.actionsGlobalContext().
     *
     * @param userLookup Can be null
     */
    public synchronized void setUserLookup(Lookup userLookup)
    {
        this.userLookup = userLookup;

        // Force proxy lookup refresh, see Lookups.proxy() API.
        proxy2UserLookup.lookup(Object.class);

    }

    // =========================================================================================
    // Lookup.Provider interface
    // =========================================================================================
    /**
     * Return the lookup proxied by proxy2UserLookup.
     *
     * @return
     */
    @Override
    public synchronized Lookup getLookup()
    {
        return userLookup != null ? userLookup: Lookup.EMPTY;
    }
}
