package org.realityforge.gwt.packetio.example.server;

import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.MediaType;
import org.glassfish.jersey.media.sse.OutboundEvent;
import org.glassfish.jersey.media.sse.SseBroadcaster;

final class TimeGenerator
  implements Runnable
{
  private final SseBroadcaster _broadcaster;
  private final Collection<AsyncResponse> _waiters;

  TimeGenerator( final SseBroadcaster broadcaster,
                 final Collection<AsyncResponse> waiters )
  {
    _broadcaster = broadcaster;
    _waiters = waiters;
  }

  @Override
  public void run()
  {
    final String message = "The time is " + new Date();
    final OutboundEvent.Builder b = new OutboundEvent.Builder();
    b.mediaType( MediaType.TEXT_PLAIN_TYPE );
    b.data( String.class, message );

    _broadcaster.broadcast( b.build() );

    synchronized ( _waiters )
    {
      final Iterator<AsyncResponse> iterator = _waiters.iterator();
      while( iterator.hasNext() )
      {
        final AsyncResponse response = iterator.next();
        iterator.remove();
        if( !response.isCancelled() )
        {
          response.resume( message );
        }
      }
    }
  }
}
