package org.realityforge.gwt.packetio.client.event;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import javax.annotation.Nonnull;
import org.realityforge.gwt.packetio.client.PacketIO;

/**
 * Base class of all events originating from PacketIO.
 */
public abstract class PacketIOEvent<H extends EventHandler>
  extends GwtEvent<H>
{
  private final PacketIO _packetIO;

  protected PacketIOEvent( @Nonnull final PacketIO packetIO )
  {
    _packetIO = packetIO;
  }

  @Nonnull
  public final PacketIO getPacketIO()
  {
    return _packetIO;
  }
}
