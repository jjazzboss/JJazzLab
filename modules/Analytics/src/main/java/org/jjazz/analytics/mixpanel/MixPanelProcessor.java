package org.jjazz.analytics.mixpanel;

import com.mixpanel.mixpanelapi.ClientDelivery;
import com.mixpanel.mixpanelapi.MessageBuilder;
import com.mixpanel.mixpanelapi.MixpanelAPI;
import java.io.IOException;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.analytics.api.Analytics;
import org.jjazz.analytics.spi.AnalyticsProcessor;
import org.json.JSONObject;
import org.openide.util.lookup.ServiceProvider;

/**
 * A processor using MixPanel.
 */
@ServiceProvider(service = AnalyticsProcessor.class)
public class MixPanelProcessor implements AnalyticsProcessor
{

    private final static long INITIAL_SECONDS_TO_WAIT = 10;
    private final static long SECONDS_TO_WAIT = 180;
    private final static int MAX_NB_FLUSHED_MESSAGES_BEFORE_SHUTDOWN = 40;
    Queue<JSONObject> msgQueue;
    DeliveryThread worker;
    private boolean enabled;

    private final MessageBuilder messageBuilder;

    private String distinctId = null;
    private static final Logger LOGGER = Logger.getLogger(MixPanelProcessor.class.getSimpleName());

    /**
     * It's a registered ServiceProvider, instance will be created by the Netbeans framework automatically.
     */
    public MixPanelProcessor()
    {
        // prepare the thread
        msgQueue = new ConcurrentLinkedQueue<>();
        worker = new DeliveryThread();
        worker.setPriority(Thread.NORM_PRIORITY - 1);
        worker.start();


        String token = System.getProperty("run.from.ide") == null ? "a67ed8aa58b306c1336dcf74fb99b2f69f1" : "0000";
        distinctId = token.equals("0000") ? "null" : Analytics.getJJazzLabComputerId();
        LOGGER.log(Level.INFO, "MixPanelProcessor() distinctId={0}", distinctId);


        // Prepare data to send message
        messageBuilder = new MessageBuilder(prep(token));

        setEnabled(token != null);
    }


    public boolean isEnabled()
    {
        return enabled;
    }


    public final void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    @Override
    public void logEvent(String eventName)
    {
        LOGGER.log(Level.FINE, "logEvent() eventName={0}", eventName);


        JSONObject msg = messageBuilder.event(distinctId, eventName, null);


        if (Analytics.EVENT_STOP_APPLICATION.equals(eventName))
        {
            stopApplication(msg);

        } else
        {
            postMessage(msg);
        }
    }

    @Override
    public void logEvent(String eventName, Map<String, ?> properties)
    {
        LOGGER.log(Level.FINE, "logEvent() eventName={0} properties={1}", new Object[]
        {
            eventName, properties
        });
        JSONObject msg = messageBuilder.event(distinctId, eventName, new JSONObject(properties));
        postMessage(msg);
    }

    @Override
    public void setProperties(Map<String, ?> properties)
    {
        LOGGER.log(Level.FINE, "setProperties() properties={0}", properties);
        JSONObject msg = messageBuilder.set(distinctId, new JSONObject(properties));
        postMessage(msg);
    }

    @Override
    public void setPropertiesOnce(Map<String, ?> properties)
    {
        LOGGER.log(Level.FINE, "setPropertiesOnce() properties={0}", properties);
        JSONObject msg = messageBuilder.setOnce(distinctId, new JSONObject(properties));
        postMessage(msg);
    }

    @Override
    public void incrementProperties(Map<String, Long> properties)
    {
        LOGGER.log(Level.FINE, "incrementProperties() properties={0}", properties);
        JSONObject msg = messageBuilder.increment(distinctId, properties);
        postMessage(msg);
    }


    // ============================================================================================
    // Private methods
    // ============================================================================================    
    /**
     * Post the message if processor is enabled.
     *
     * @param msg
     */
    private void postMessage(JSONObject msg)
    {
        if (isEnabled())
        {
            msgQueue.add(msg);
        }
    }


    /**
     * Flush the pending events and send them with lastMessage.
     *
     * @param lastMessage
     */
    private void stopApplication(JSONObject lastMessage)
    {

        worker.interrupt();


        // Get the remaining messages
        JSONObject[] messages = new JSONObject[Math.min(MAX_NB_FLUSHED_MESSAGES_BEFORE_SHUTDOWN, msgQueue.size() + 1)];

        int i = 0;
        for (var msg : msgQueue)
        {
            if (i >= MAX_NB_FLUSHED_MESSAGES_BEFORE_SHUTDOWN - 1)
            {
                break;
            }
            messages[i] = msg;
            i++;
        }


        messages[i] = lastMessage;


        logEventsImmediatly(2000, messages);
    }

    private String prep(String s)
    {
        return  s.substring(3);
    }

    /**
     * Log the specified messages immediatly, but don't block more that timeOut milliseconds.
     *
     * @param timeOut  Time out duration in milliseconds.
     * @param messages
     */
    private void logEventsImmediatly(int timeOut, JSONObject[] messages)
    {
        if (messages.length == 0)
        {
            return;
        }

        // Prepare data
        MixpanelAPI mixPanelAPI = new MixpanelAPI("https://api-eu.mixpanel.com/track",
                "https://api-eu.mixpanel.com/engage",
                "https://api-eu.mixpanel.com/groups");
        ClientDelivery delivery = new ClientDelivery();
        for (var imsg : messages)
        {
            delivery.addMessage(imsg);
        }


        // The sending task
        Runnable task = () -> 
        {
            try
            {
                // Send : can block (network access), possibly for a long time
                mixPanelAPI.deliver(delivery);      // Must be interruptible!
            } catch (IOException ex)
            {
                LOGGER.log(Level.WARNING, "logEventsImmediatly.task.run() IOException ex={0}", ex.getMessage());
            }
        };


        LOGGER.log(Level.FINE, "logEventsImmediatly() delivering {0} messages, lastMessage={1}", new Object[]
        {
            messages.length, messages[messages.length - 1]
        });


        // Run the task and wait no more than time out
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<?> future = executor.submit(task);

        try
        {
            future.get(timeOut, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException ex)
        {
            LOGGER.log(Level.WARNING, "logEventsImmediatly() ex={0}", ex.getMessage());
        } catch (TimeoutException ex)
        {
            future.cancel(true);
            LOGGER.log(Level.INFO, "logEventsImmediatly() timed out (delay={0}ms)", timeOut);
        }

        executor.shutdownNow();
    }


    // ============================================================================================
    // Private classes
    // ============================================================================================    
    /**
     * The thread which periodically sends the queued messages.
     */
    private class DeliveryThread extends Thread
    {

        private MixpanelAPI mixPanelAPI;
        private boolean first = true;
        private boolean enabled = true;

        public DeliveryThread()
        {
            // EU specific servers
            mixPanelAPI = new MixpanelAPI("https://api-eu.mixpanel.com/track",
                    "https://api-eu.mixpanel.com/engage",
                    "https://api-eu.mixpanel.com/groups");
        }

        public void disable()
        {
            enabled = false;
        }

        @Override
        public void run()
        {
            try
            {
                while (true)
                {
                    ClientDelivery delivery = new ClientDelivery();
                    JSONObject message = null;
                    JSONObject lastMessage = null;
                    int count = 0;
                    do
                    {

                        message = msgQueue.poll();
                        if (message != null)
                        {
                            delivery.addMessage(message);
                            count++;
                            lastMessage = message;
                        }

                    } while (message != null);

                    // Send : can block (network access), possibly for a long time
                    LOGGER.log(Level.FINE, "DeliveryThread.run() delivering {0} messages, lastMessage={1}", new Object[]
                    {
                        count, lastMessage
                    });
                    mixPanelAPI.deliver(delivery);

                    // Wait for next period
                    Thread.sleep((first ? INITIAL_SECONDS_TO_WAIT : SECONDS_TO_WAIT) * 1000);
                    first = false;
                }
            } catch (IOException e)
            {
                LOGGER.log(Level.WARNING, "DeliveryThread.run() e={0}", e.getMessage());
            } catch (InterruptedException e)
            {
                LOGGER.log(Level.FINE, "DeliveryThread.run() interruped e={0}", e.getMessage());
            }
        }
    }

}
