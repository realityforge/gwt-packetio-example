package org.realityforge.gwt.packetio.client;

import com.google.gwt.http.client.RequestBuilder;
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.HandlerRegistration;
import com.google.web.bindery.event.shared.SimpleEventBus;
import java.util.Collections;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.realityforge.gwt.eventsource.client.EventSource;
import org.realityforge.gwt.eventsource.client.EventSourceListenerAdapter;
import org.realityforge.gwt.packetio.client.event.ErrorEvent;
import org.realityforge.gwt.packetio.client.event.MessageEvent;
import org.realityforge.gwt.packetio.client.event.StartEvent;
import org.realityforge.gwt.packetio.client.event.StopEvent;
import org.realityforge.gwt.webpoller.client.HttpRequestFactory;
import org.realityforge.gwt.webpoller.client.WebPoller;
import org.realityforge.gwt.webpoller.client.WebPollerListener;
import org.realityforge.gwt.websockets.client.WebSocket;
import org.realityforge.gwt.websockets.client.WebSocketListenerAdapter;

public class PacketIO
{
  private final EventBus _eventBus;
  private EventSource _eventSource;
  private WebSocket _webSocket;
  private WebPoller _webPoller;
  private String _baseURL;
  private Strategy _strategy;

  public static PacketIO newPacketIO()
  {
    return new PacketIO( new SimpleEventBus() );
  }

  protected PacketIO( @Nonnull final EventBus eventBus )
  {
    _eventBus = eventBus;
  }

  /**
   * @return true if the poller is active.
   */
  public boolean isActive()
  {
    return null != _webPoller || null != _eventSource || null != _webSocket;
  }

  /**
   * Start polling.
   *
   * @throws IllegalStateException if the connection is already active.
   */
  public final void start()
    throws IllegalStateException
  {
    if ( isActive() )
    {
      throw new IllegalStateException( "Start invoked on active packet io connection" );
    }
    doStart();
  }

  /**
   * Stop polling.
   *
   * @throws IllegalStateException if the connection is not active.
   */
  public final void stop()
    throws IllegalStateException
  {
    if ( !isActive() )
    {
      throw new IllegalStateException( "Stop invoked on inactive packet io connection" );
    }
    doStop();
  }

  public final String getBaseURL()
  {
    return _baseURL;
  }

  public Strategy getStrategy()
  {
    return _strategy;
  }

  public void setStrategy( @Nonnull final Strategy strategy )
  {
    if ( isActive() )
    {
      throw new IllegalStateException( "Attempt to invoke setStrategy when packet io is active" );
    }
    _strategy = strategy;
  }

  public final void setBaseURL( @Nonnull final String baseURL )
    throws IllegalStateException
  {
    if ( isActive() )
    {
      throw new IllegalStateException( "Attempt to invoke setBaseURL when packet io is active" );
    }
    _baseURL = baseURL;
  }

  protected final EventBus getEventBus()
  {
    return _eventBus;
  }

  @Nonnull
  public final HandlerRegistration addStartHandler( @Nonnull StartEvent.Handler handler )
  {
    return getEventBus().addHandler( StartEvent.getType(), handler );
  }

  @Nonnull
  public final HandlerRegistration addStopHandler( @Nonnull StopEvent.Handler handler )
  {
    return getEventBus().addHandler( StopEvent.getType(), handler );
  }

  @Nonnull
  public final HandlerRegistration addMessageHandler( @Nonnull MessageEvent.Handler handler )
  {
    return getEventBus().addHandler( MessageEvent.getType(), handler );
  }

  @Nonnull
  public final HandlerRegistration addErrorHandler( @Nonnull ErrorEvent.Handler handler )
  {
    return getEventBus().addHandler( ErrorEvent.getType(), handler );
  }

  /**
   * Fire a Start event.
   */
  protected final void onStart()
  {
    _eventBus.fireEventFromSource( new StartEvent( this ), this );
  }

  /**
   * Fire a Stop event.
   */
  protected final void onStop()
  {
    _eventBus.fireEventFromSource( new StopEvent( this ), this );
  }

  /**
   * Fire a Message event.
   */
  protected final void onMessage( @Nonnull final Map<String, String> context,
                                  @Nonnull final String data )
  {
    _eventBus.fireEventFromSource( new MessageEvent( this, context, data ), this );
  }

  /**
   * Fire an Error event.
   */
  protected final void onError( @Nonnull final Throwable exception )
  {
    _eventBus.fireEventFromSource( new ErrorEvent( this, exception ), this );
  }

  protected void doStart()
  {
    final String baseURL = getBaseURL();

    switch ( getStrategy() )
    {
      case EVENTSOURCE:
        if ( EventSource.isSupported() )
        {
          _eventSource = EventSource.newEventSourceIfSupported();
          _eventSource.open( baseURL + "api/time" );
          registerListeners( _eventSource );
        }
        else
        {
          throw new IllegalStateException( getStrategy() + " strategy not supported" );
        }
        break;
      case WEBSOCKET:
        if ( WebSocket.isSupported() )
        {
          _webSocket = WebSocket.newWebSocketIfSupported();
          _webSocket.connect( baseURL.replaceFirst( "^http\\:", "ws:" ) + "api/wstime" );
          registerListeners( _webSocket );
        }
        else
        {
          throw new IllegalStateException( getStrategy() + " strategy not supported" );
        }
        break;
      case POLL:
      {
        final RequestBuilder requestBuilder = new RequestBuilder( RequestBuilder.GET, baseURL + "api/time/poll" );
        _webPoller = WebPoller.newWebPoller();
        _webPoller.setRequestFactory( new HttpRequestFactory( requestBuilder ) );
        registerListeners( _webPoller );
        _webPoller.setInterRequestDuration( 0 );
        _webPoller.start();
      }
      break;

      default:
      {
        throw new IllegalStateException( getStrategy() + " strategy not supported" );
      }
    }
    if ( !isActive() )
    {
      throw new IllegalStateException( "Unable to initiate connection" );
    }
  }

  protected void doStop()
  {
    if ( null != _webPoller )
    {
      _webPoller.stop();
      _webPoller = null;
    }
    if ( null != _eventSource )
    {
      _eventSource.close();
      _eventSource = null;
    }
    if ( null != _webSocket )
    {
      _webSocket.close();
      _webSocket = null;
    }
    onStop();
  }

  private void registerListeners( final WebSocket webSocket )
  {
    webSocket.setListener( new WebSocketListenerAdapter()
    {
      @Override
      public void onOpen( @Nonnull final WebSocket webSocket )
      {
        onStart();
      }

      @Override
      public void onClose( @Nonnull final WebSocket webSocket,
                           final boolean wasClean,
                           final int code,
                           @Nullable final String reason )
      {
        onStop();
      }

      @Override
      public void onMessage( @Nonnull final WebSocket webSocket, @Nonnull final String data )
      {
        PacketIO.this.onMessage( data );
      }

      @Override
      public void onError( @Nonnull final WebSocket webSocket )
      {
        PacketIO.this.onError( new Throwable( "" ) );
      }
    } );
  }

  private void registerListeners( final WebPoller webPoller )
  {
    webPoller.setListener( new WebPollerListener()
    {
      @Override
      public void onStart( @Nonnull final WebPoller webPoller )
      {
        PacketIO.this.onStart();
      }

      @Override
      public void onStop( @Nonnull final WebPoller webPoller )
      {
        PacketIO.this.onStop();
      }

      @Override
      public void onMessage( @Nonnull final WebPoller webPoller,
                             @Nonnull final Map<String, String> context,
                             @Nonnull final String data )
      {
        PacketIO.this.onMessage( context, data );
      }

      @Override
      public void onError( @Nonnull final WebPoller webPoller, @Nonnull final Throwable exception )
      {
        PacketIO.this.onError( exception );
      }
    } );
  }

  private void registerListeners( final EventSource eventSource )
  {
    eventSource.setListener( new EventSourceListenerAdapter()
    {
      @Override
      public void onOpen( @Nonnull final EventSource eventSource )
      {
        onStart();
      }

      @Override
      public void onClose( @Nonnull final EventSource eventSource )
      {
        onStop();
      }

      @Override
      public void onMessage( @Nonnull final EventSource eventSource,
                             @Nullable final String lastEventId,
                             @Nonnull final String type,
                             @Nonnull final String data )
      {
        PacketIO.this.onMessage( data );
      }

      @Override
      public void onError( @Nonnull final EventSource eventSource )
      {
        PacketIO.this.onError( new Throwable() );
      }
    } );
  }

  private void onMessage( @Nonnull final String data )
  {
    @Nonnull final Map<String, String> context = Collections.emptyMap();
    onMessage( context, data );
  }
}
