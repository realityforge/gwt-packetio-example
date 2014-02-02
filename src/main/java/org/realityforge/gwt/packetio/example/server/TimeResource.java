package org.realityforge.gwt.packetio.example.server;

import java.util.Collection;
import java.util.Date;
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
  private String _message;

  @PostConstruct
  public void postConstruct()
  {
    final TimeGenerator command = new TimeGenerator( _broadcaster, _waiters );
    _scheduledExecutor.scheduleWithFixedDelay( new Runnable()
    {
      @Override
      public void run()
      {
        command.run();
        _message = "p: The time is " + new Date();
      }
    }, 0, 1, TimeUnit.SECONDS );
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

  @Path("poll")
  @GET
  @Produces("text/plain")
  public String poll()
  {
    final String message = _message;
    _message = null;
    return message;
  }
}
