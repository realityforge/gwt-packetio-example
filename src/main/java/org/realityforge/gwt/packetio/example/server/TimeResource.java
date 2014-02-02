package org.realityforge.gwt.packetio.example.server;

import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import org.glassfish.jersey.media.sse.EventOutput;
import org.glassfish.jersey.media.sse.SseBroadcaster;

@Path("/time")
@Singleton
public class TimeResource
{
  private final SseBroadcaster _broadcaster = new SseBroadcaster();
  private final Collection<AsyncResponse> _waiters = new ConcurrentLinkedQueue<>();
  private final ScheduledExecutorService _scheduledExecutor = Executors.newSingleThreadScheduledExecutor();

  @PostConstruct
  public void postConstruct()
  {
    _scheduledExecutor.scheduleWithFixedDelay( new TimeGenerator( _broadcaster, _waiters ), 0, 1, TimeUnit.SECONDS );
  }

  @GET
  @Produces("text/event-stream")
  public EventOutput getMessages()
  {
    final EventOutput eventOutput = new EventOutput();
    _broadcaster.add( eventOutput );
    return eventOutput;
  }

  @Path("long_poll")
  @GET
  @Produces("text/plain")
  public void hangUp( @Suspended AsyncResponse response )
  {
    _waiters.add( response );
  }
}
