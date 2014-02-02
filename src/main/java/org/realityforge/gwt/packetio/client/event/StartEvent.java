package org.realityforge.gwt.packetio.client.event;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import javax.annotation.Nonnull;
import org.realityforge.gwt.packetio.client.PacketIO;

public class StartEvent
  extends PacketIOEvent<StartEvent.Handler>
{
  public interface Handler
    extends EventHandler
  {
    void onStartEvent( @Nonnull StartEvent event );
  }

  private static final GwtEvent.Type<Handler> TYPE = new Type<>();

  public static GwtEvent.Type<Handler> getType()
  {
    return TYPE;
  }

  public StartEvent( @Nonnull final PacketIO packetIO )
  {
    super( packetIO );
  }

  @Override
  public GwtEvent.Type<Handler> getAssociatedType()
  {
    return StartEvent.getType();
  }

  @Override
  protected void dispatch( @Nonnull final Handler handler )
  {
    handler.onStartEvent( this );
  }
}
