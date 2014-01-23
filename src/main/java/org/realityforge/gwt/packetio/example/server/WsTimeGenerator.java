package org.realityforge.gwt.packetio.example.server;

import java.util.Date;
import java.util.Set;
import javax.websocket.Session;

final class WsTimeGenerator
  implements Runnable
{
  private final Set<Session> _sessions;

  WsTimeGenerator( final Set<Session> sessions )
  {
    _sessions = sessions;
  }

  @Override
  public void run()
  {
    synchronized ( _sessions )
    {
      for ( final Session session : _sessions )
      {
        session.getAsyncRemote().sendText( "WS: The time is " + new Date() );
      }
    }
  }
}
