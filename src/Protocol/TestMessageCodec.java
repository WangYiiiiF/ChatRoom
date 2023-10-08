package Protocol;

import Message.LoginRequestMessage;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.logging.LoggingHandler;

public class TestMessageCodec {
    public static void main(String[] args) throws Exception{
        EmbeddedChannel channel = new EmbeddedChannel(
                new LengthFieldBasedFrameDecoder(1024,12,4,0,0),
                new LoggingHandler(),
                new MessageCodec());

        //encode
        LoginRequestMessage message = new LoginRequestMessage("zhangsan","123");
        //出站方向
        channel.writeOutbound(message);

        //decode
        ByteBuf out = ByteBufAllocator.DEFAULT.buffer();
        new MessageCodec().encode(null,message,out);

        //入站方向
        channel.writeInbound(out);
    }
}
