package org.eclipse.jetty.websocket.parser;

import java.nio.ByteBuffer;

public class PongPayloadParser extends PayloadParser
{
    private Parser baseParser;

    public PongPayloadParser(Parser parser)
    {
        this.baseParser = parser;
    }

    @Override
    public boolean parse(ByteBuffer buffer)
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void reset()
    {
        // TODO Auto-generated method stub

    }

}
