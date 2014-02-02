package org.realityforge.gwt.packetio.client.event;

import com.google.gwt.event.shared.EventHandler;
import javax.annotation.Nonnull;
import org.realityforge.gwt.packetio.client.PacketIO;

/**
 * Event fired when there is an error with the packet io system.
 */
public class ErrorEvent
  extends PacketIOEvent<ErrorEvent.Handler>
{
  public interface Handler
    extends EventHandler
  {
    void onErrorEvent( @Nonnull ErrorEvent event );
  }

  private static final Type<Handler> TYPE = new Type<>();

  public static Type<Handler> getType()
  {
    return TYPE;
  }

  private final Throwable _exception;

  public ErrorEvent( @Nonnull final PacketIO packetIO, @Nonnull final Throwable exception )
  {
    super( packetIO );
    _exception = exception;
  }

  @Nonnull
  public Throwable getException()
  {
    return _exception;
  }

  @Override
  public Type<Handler> getAssociatedType()
  {
    return ErrorEvent.getType();
  }

  @Override
  protected void dispatch( @Nonnull final Handler handler )
  {
    handler.onErrorEvent( this );
  }
}
