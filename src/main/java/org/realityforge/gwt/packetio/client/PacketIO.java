package org.realityforge.gwt.packetio.client;

import com.google.gwt.http.client.RequestBuilder;
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.HandlerRegistration;
import com.google.web.bindery.event.shared.SimpleEventBus;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import javax.annotation.Nonnull;
import org.realityforge.gwt.eventsource.client.EventSource;
import org.realityforge.gwt.packetio.client.event.ErrorEvent;
import org.realityforge.gwt.packetio.client.event.MessageEvent;
import org.realityforge.gwt.packetio.client.event.StartEvent;
import org.realityforge.gwt.packetio.client.event.StopEvent;
import org.realityforge.gwt.webpoller.client.HttpRequestFactory;
import org.realityforge.gwt.webpoller.client.WebPoller;
import org.realityforge.gwt.websockets.client.WebSocket;

public class PacketIO
{
  private final ArrayList<HandlerRegistration> _registrations = new ArrayList<HandlerRegistration>();
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

    switch ( _strategy )
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
          throw new IllegalStateException( _strategy + " strategy not supported" );
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
          throw new IllegalStateException( _strategy + " strategy not supported" );
        }
        break;
      case POLL:
      {
        final RequestBuilder requestBuilder = new RequestBuilder( RequestBuilder.GET, baseURL + "api/time/poll" );
        _webPoller = WebPoller.newWebPoller();
        _webPoller.setRequestFactory( new HttpRequestFactory( requestBuilder ) );
        registerListeners( _webPoller );
        // Configure long poll
        _webPoller.setLongPoll( false );
        _webPoller.start();
      }
      break;

      default:
      {
          throw new IllegalStateException( _strategy + " strategy not supported" );
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
    deregisterHandlers();
    onStop();
  }

  private void register( final HandlerRegistration handlerRegistration )
  {
    _registrations.add( handlerRegistration );
  }

  private void deregisterHandlers()
  {
    for ( final HandlerRegistration registration : _registrations )
    {
      registration.removeHandler();
    }
    _registrations.clear();
  }

  private void registerListeners( final WebSocket webSocket )
  {
    register( webSocket.addOpenHandler( new org.realityforge.gwt.websockets.client.event.OpenEvent.Handler()
    {
      @Override
      public void onOpenEvent( @Nonnull final org.realityforge.gwt.websockets.client.event.OpenEvent event )
      {
        onStart();
      }
    } ) );
    register( webSocket.addCloseHandler( new org.realityforge.gwt.websockets.client.event.CloseEvent.Handler()
    {
      @Override
      public void onCloseEvent( @Nonnull final org.realityforge.gwt.websockets.client.event.CloseEvent event )
      {
        onStop();
      }
    } ) );
    register( webSocket.addErrorHandler( new org.realityforge.gwt.websockets.client.event.ErrorEvent.Handler()
    {
      @Override
      public void onErrorEvent( @Nonnull final org.realityforge.gwt.websockets.client.event.ErrorEvent event )
      {
        onError( new Exception() );
      }
    } ) );
    register( webSocket.addMessageHandler( new org.realityforge.gwt.websockets.client.event.MessageEvent.Handler()
    {
      @Override
      public void onMessageEvent( @Nonnull final org.realityforge.gwt.websockets.client.event.MessageEvent event )
      {
        onMessage( event.getTextData() );
      }
    } ) );
  }

  private void registerListeners( final WebPoller webPoller )
  {
    register( webPoller.addStartHandler( new org.realityforge.gwt.webpoller.client.event.StartEvent.Handler()
    {
      @Override
      public void onStartEvent( @Nonnull final org.realityforge.gwt.webpoller.client.event.StartEvent event )
      {
        onStart();
      }
    } ) );
    register( webPoller.addStopHandler( new org.realityforge.gwt.webpoller.client.event.StopEvent.Handler()
    {
      @Override
      public void onStopEvent( @Nonnull final org.realityforge.gwt.webpoller.client.event.StopEvent event )
      {
        onStop();
      }
    } ) );
    register( webPoller.addErrorHandler( new org.realityforge.gwt.webpoller.client.event.ErrorEvent.Handler()
    {
      @Override
      public void onErrorEvent( @Nonnull final org.realityforge.gwt.webpoller.client.event.ErrorEvent event )
      {
        onError( event.getException() );
      }
    } ) );
    register( webPoller.addMessageHandler( new org.realityforge.gwt.webpoller.client.event.MessageEvent.Handler()
    {
      @Override
      public void onMessageEvent( @Nonnull final org.realityforge.gwt.webpoller.client.event.MessageEvent event )
      {
        onMessage( event.getContext(), event.getData() );
      }
    } ) );
  }

  private void registerListeners( final EventSource eventSource )
  {
    register( eventSource.addOpenHandler( new org.realityforge.gwt.eventsource.client.event.OpenEvent.Handler()
    {
      @Override
      public void onOpenEvent( @Nonnull final org.realityforge.gwt.eventsource.client.event.OpenEvent event )
      {
        onStart();
      }
    } ) );
    register( eventSource.addCloseHandler( new org.realityforge.gwt.eventsource.client.event.CloseEvent.Handler()
    {
      @Override
      public void onCloseEvent( @Nonnull final org.realityforge.gwt.eventsource.client.event.CloseEvent event )
      {
        onStop();
      }
    } ) );
    register( eventSource.addErrorHandler( new org.realityforge.gwt.eventsource.client.event.ErrorEvent.Handler()
    {
      @Override
      public void onErrorEvent( @Nonnull final org.realityforge.gwt.eventsource.client.event.ErrorEvent event )
      {
        onError( new Exception() );
      }
    } ) );
    register( eventSource.addMessageHandler( new org.realityforge.gwt.eventsource.client.event.MessageEvent.Handler()
    {
      @Override
      public void onMessageEvent( @Nonnull final org.realityforge.gwt.eventsource.client.event.MessageEvent event )
      {
        onMessage( event.getData() );
      }
    } ) );
  }

  private void onMessage( @Nonnull final String data )
  {
    @Nonnull final Map<String, String> context = Collections.emptyMap();
    onMessage( context, data );
  }
}
