package org.realityforge.gwt.packetio.example.server;

import java.util.Date;
import javax.ws.rs.core.MediaType;
import org.glassfish.jersey.media.sse.OutboundEvent;
import org.glassfish.jersey.media.sse.SseBroadcaster;

final class TimeGenerator
  implements Runnable
{
  private final SseBroadcaster _broadcaster;

  TimeGenerator( final SseBroadcaster broadcaster )
  {
    _broadcaster = broadcaster;
  }

  @Override
  public void run()
  {
    final OutboundEvent.Builder b = new OutboundEvent.Builder();
    b.mediaType( MediaType.TEXT_PLAIN_TYPE );
    b.data( String.class, "The time is " + new Date() );

    _broadcaster.broadcast( b.build() );
  }
}
