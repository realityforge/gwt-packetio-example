package org.realityforge.gwt.packetio.client.event;

import com.google.gwt.event.shared.EventHandler;
import javax.annotation.Nonnull;
import org.realityforge.gwt.packetio.client.PacketIO;

public class StopEvent
  extends PacketIOEvent<StopEvent.Handler>
{
  public interface Handler
    extends EventHandler
  {
    void onStopEvent( @Nonnull StopEvent event );
  }

  private static final Type<Handler> TYPE = new Type<>();

  public static Type<Handler> getType()
  {
    return TYPE;
  }

  public StopEvent( @Nonnull final PacketIO packetIO )
  {
    super( packetIO );
  }

  @Override
  public Type<Handler> getAssociatedType()
  {
    return StopEvent.getType();
  }

  @Override
  protected void dispatch( @Nonnull final Handler handler )
  {
    handler.onStopEvent( this );
  }
}
