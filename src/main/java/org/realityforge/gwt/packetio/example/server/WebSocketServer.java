package org.realityforge.gwt.packetio.example.server;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

@ServerEndpoint("/api/wstime")
@Singleton
public class WebSocketServer
{
  private static final Logger LOG = Logger.getLogger( WebSocketServer.class.getName() );

  private static final Set<Session> _sessions = Collections.synchronizedSet( new HashSet<Session>() );
  private final ScheduledExecutorService _scheduledExecutor = Executors.newSingleThreadScheduledExecutor();

  @PostConstruct
  public void postConstruct()
  {
    _scheduledExecutor.scheduleWithFixedDelay( new WsTimeGenerator( _sessions ), 0, 1, TimeUnit.SECONDS );
  }

  @OnOpen
  public void onOpen( final Session session )
  {
    LOG.info( "onOpen(" + session.getId() + ")" );
    _sessions.add( session );
  }

  @OnClose
  public void onClose( final Session session )
  {
    LOG.info( "onClose(" + session.getId() + ")" );
    _sessions.remove( session );
  }

  @OnMessage
  public void onMessage( final String message, final Session session )
  {
    LOG.info( "onMessage(" + message + "," + session.getId() + ")" );
  }
}
