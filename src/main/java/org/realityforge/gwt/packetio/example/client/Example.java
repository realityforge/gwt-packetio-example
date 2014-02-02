package org.realityforge.gwt.packetio.example.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.web.bindery.event.shared.HandlerRegistration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import javax.annotation.Nonnull;
import org.realityforge.gwt.eventsource.client.EventSource;
import org.realityforge.gwt.webpoller.client.HttpRequestFactory;
import org.realityforge.gwt.webpoller.client.WebPoller;
import org.realityforge.gwt.websockets.client.WebSocket;

public final class Example
  implements EntryPoint
{
  private final ArrayList<HandlerRegistration> _registrations = new ArrayList<>();
  private HTML _messages;
  private ScrollPanel _scrollPanel;
  private Button _disconnect;
  private Button _connect;
  private ListBox _commChannel;
  private EventSource _eventSource;
  private WebSocket _webSocket;
  private WebPoller _webPoller;

  public void onModuleLoad()
  {
    _commChannel = new ListBox();
    if ( WebSocket.isSupported() )
    {
      _commChannel.addItem( ConnectionType.WEBSOCKET.getLabel(), ConnectionType.WEBSOCKET.name() );
    }

    if ( EventSource.isSupported() )
    {
      _commChannel.addItem( ConnectionType.EVENTSOURCE.getLabel(), ConnectionType.EVENTSOURCE.name() );
    }
    _commChannel.addItem( ConnectionType.LONG_POLL.getLabel(), ConnectionType.LONG_POLL.name() );
    _commChannel.addItem( ConnectionType.POLL.getLabel(), ConnectionType.POLL.name() );

    _connect = new Button( "Connect", new ClickHandler()
    {
      @Override
      public void onClick( final ClickEvent event )
      {
        doConnect();
      }
    } );
    _disconnect = new Button( "Disconnect", new ClickHandler()
    {
      @Override
      public void onClick( ClickEvent event )
      {
        doDisconnect();
      }
    } );
    _disconnect.setEnabled( false );

    _messages = new HTML();
    _scrollPanel = new ScrollPanel();
    _scrollPanel.setHeight( "250px" );
    _scrollPanel.add( _messages );
    RootPanel.get().add( _scrollPanel );

    {
      final FlowPanel controls = new FlowPanel();
      final HorizontalPanel connectionTypePanel = new HorizontalPanel();
      connectionTypePanel.add( new Label( "Connection Type:" ) );
      connectionTypePanel.add( _commChannel );
      controls.add( connectionTypePanel );
      controls.add( _connect );
      controls.add( _disconnect );
      RootPanel.get().add( controls );
    }
  }

  private void doConnect()
  {
    final String moduleBaseURL = GWT.getModuleBaseURL();
    final String moduleName = GWT.getModuleName();
    final String baseURL = moduleBaseURL.substring( 0, moduleBaseURL.length() - moduleName.length() - 1 );

    final int selectedIndex = _commChannel.getSelectedIndex();
    final ConnectionType connectionType = ConnectionType.valueOf( _commChannel.getValue( selectedIndex ) );
    switch ( connectionType )
    {
      case EVENTSOURCE:
        _eventSource = EventSource.newEventSourceIfSupported();
        _eventSource.connect( baseURL + "api/time" );
        registerListeners( _eventSource );
        break;
      case WEBSOCKET:
        _webSocket = WebSocket.newWebSocketIfSupported();
        _webSocket.connect( baseURL.replaceFirst( "^http\\:", "ws:" ) + "api/wstime" );
        registerListeners( _webSocket );
        break;
      case LONG_POLL:
      {
        final RequestBuilder requestBuilder = new RequestBuilder( RequestBuilder.GET, baseURL + "api/time/long_poll" );
        _webPoller = WebPoller.newWebPoller( new HttpRequestFactory( requestBuilder ) );
        registerListeners( _webPoller );
        _webPoller.setLongPoll( true );
        _webPoller.start();
      }
      break;
      case POLL:
      {
        final RequestBuilder requestBuilder = new RequestBuilder( RequestBuilder.GET, baseURL + "api/time/poll" );
        _webPoller = WebPoller.newWebPoller( new HttpRequestFactory( requestBuilder ) );
        registerListeners( _webPoller );
        _webPoller.setLongPoll( false );
        _webPoller.start();
      }
      break;

      default:
        Window.alert( "Connection Type unsupported" );
        return;
    }
    _connect.setEnabled( false );
    _commChannel.setEnabled( false );
  }

  private void doDisconnect()
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
    _disconnect.setEnabled( false );
  }

  private void registerListeners( final WebSocket webSocket )
  {
    register( webSocket.addOpenHandler( new org.realityforge.gwt.websockets.client.event.OpenEvent.Handler()
    {
      @Override
      public void onOpenEvent( @Nonnull final org.realityforge.gwt.websockets.client.event.OpenEvent event )
      {
        onOpen();
      }
    } ) );
    register( webSocket.addCloseHandler( new org.realityforge.gwt.websockets.client.event.CloseEvent.Handler()
    {
      @Override
      public void onCloseEvent( @Nonnull final org.realityforge.gwt.websockets.client.event.CloseEvent event )
      {
        onClose();
      }
    } ) );
    register( webSocket.addErrorHandler( new org.realityforge.gwt.websockets.client.event.ErrorEvent.Handler()
    {
      @Override
      public void onErrorEvent( @Nonnull final org.realityforge.gwt.websockets.client.event.ErrorEvent event )
      {
        onError();
      }
    } ) );
    register( webSocket.addMessageHandler( new org.realityforge.gwt.websockets.client.event.MessageEvent.Handler()
    {
      @Override
      public void onMessageEvent( @Nonnull final org.realityforge.gwt.websockets.client.event.MessageEvent event )
      {
        onMessage( event.getData() );
      }
    } ) );
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


  private void registerListeners( final WebPoller eventSource )
  {
    register( eventSource.addStartHandler( new org.realityforge.gwt.webpoller.client.event.StartEvent.Handler()
    {
      @Override
      public void onStartEvent( @Nonnull final org.realityforge.gwt.webpoller.client.event.StartEvent event )
      {
        onOpen();
      }
    } ) );
    register( eventSource.addStopHandler( new org.realityforge.gwt.webpoller.client.event.StopEvent.Handler()
    {
      @Override
      public void onStopEvent( @Nonnull final org.realityforge.gwt.webpoller.client.event.StopEvent event )
      {
        onClose();
      }
    } ) );
    register( eventSource.addErrorHandler( new org.realityforge.gwt.webpoller.client.event.ErrorEvent.Handler()
    {
      @Override
      public void onErrorEvent( @Nonnull final org.realityforge.gwt.webpoller.client.event.ErrorEvent event )
      {
        onError();
      }
    } ) );
    register( eventSource.addMessageHandler( new org.realityforge.gwt.webpoller.client.event.MessageEvent.Handler()
    {
      @Override
      public void onMessageEvent( @Nonnull final org.realityforge.gwt.webpoller.client.event.MessageEvent event )
      {
        onMessage( event.getData() );
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
        onOpen();
      }
    } ) );
    register( eventSource.addCloseHandler( new org.realityforge.gwt.eventsource.client.event.CloseEvent.Handler()
    {
      @Override
      public void onCloseEvent( @Nonnull final org.realityforge.gwt.eventsource.client.event.CloseEvent event )
      {
        onClose();
      }
    } ) );
    register( eventSource.addErrorHandler( new org.realityforge.gwt.eventsource.client.event.ErrorEvent.Handler()
    {
      @Override
      public void onErrorEvent( @Nonnull final org.realityforge.gwt.eventsource.client.event.ErrorEvent event )
      {
        onError();
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

  private void onMessage( final String data )
  {
    appendText( "message: " + data, "black" );
  }

  private void onOpen()
  {
    appendText( "open", "silver" );
    _disconnect.setEnabled( true );
  }

  private void onClose()
  {
    appendText( "close", "silver" );
    _connect.setEnabled( true );
    _commChannel.setEnabled( true );
    _disconnect.setEnabled( false );
  }

  private void onError()
  {
    appendText( "error", "red" );
    _connect.setEnabled( false );
    _commChannel.setEnabled( false );
    _disconnect.setEnabled( false );
  }

  private void appendText( final String text, final String color )
  {
    final DivElement div = Document.get().createDivElement();
    div.setInnerText( text );
    div.setAttribute( "style", "color:" + color );
    _messages.getElement().appendChild( div );
    _scrollPanel.scrollToBottom();
  }
}
