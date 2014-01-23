package org.realityforge.gwt.packetio.example.client;

public enum ConnectionType
{
  WEBSOCKET( "WebSockets" ),
  EVENTSOURCE( "Event Source/Server Sent Events" ),
  LONG_POLL( "Long Poll" ),
  POLL( "Poll" );

  private final String _label;

  private ConnectionType( final String label )
  {
    _label = label;
  }

  public String getLabel()
  {
    return _label;
  }
}
