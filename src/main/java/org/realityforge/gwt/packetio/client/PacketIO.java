package org.realityforge.gwt.packetio.client;

import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.HandlerRegistration;
import com.google.web.bindery.event.shared.SimpleEventBus;
import java.util.Map;
import javax.annotation.Nonnull;
import org.realityforge.gwt.packetio.client.event.ErrorEvent;
import org.realityforge.gwt.packetio.client.event.MessageEvent;
import org.realityforge.gwt.packetio.client.event.StartEvent;
import org.realityforge.gwt.packetio.client.event.StopEvent;

public class PacketIO
{
  private final EventBus _eventBus;
  private boolean _active;

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
    return _active;
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

  protected final EventBus getEventBus()
  {
    return _eventBus;
  }

  /**
   * Sub-classes should override this method to provide functionality.
   */
  protected void doStop()
  {
    _active = false;
    onStop();
  }

  /**
   * Sub-classes should override this method to provide functionality.
   */
  protected void doStart()
  {
    _active = true;
    onStart();
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
}
