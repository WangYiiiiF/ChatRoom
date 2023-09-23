<<<<<<< HEAD
package cn.itcast.advance.c2;

import cn.itcast.message.LoginRequestMessage;
import cn.itcast.protocol.MessageCodec;
=======
package Protocol;

import Message.LoginRequestMessage;
>>>>>>> b11a9fe (Initial commit)
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.logging.LoggingHandler;

public class TestMessageCodec {
<<<<<<< HEAD
    public static void main(String[] args) throws Exception {
        EmbeddedChannel channel = new EmbeddedChannel(
                new LoggingHandler(),
                new LengthFieldBasedFrameDecoder(
                        1024, 12, 4, 0, 0),
                new MessageCodec()
        );
        // encode
        LoginRequestMessage message = new LoginRequestMessage("zhangsan", "123");
//        channel.writeOutbound(message);
        // decode
        ByteBuf buf = ByteBufAllocator.DEFAULT.buffer();
        new MessageCodec().encode(null, message, buf);

        ByteBuf s1 = buf.slice(0, 100);
        ByteBuf s2 = buf.slice(100, buf.readableBytes() - 100);
        s1.retain(); // 引用计数 2
        channel.writeInbound(s1); // release 1
        channel.writeInbound(s2);
=======
    public static void main(String[] args) throws Exception{
        EmbeddedChannel channel = new EmbeddedChannel(
                new LengthFieldBasedFrameDecoder(1024,12,4,0,0),
                new LoggingHandler(),
                new MessageCodec());

        //encode
        LoginRequestMessage message = new LoginRequestMessage("zhangsan","123");
        //出战方向
        channel.writeOutbound(message);

        //decode
        ByteBuf out = ByteBufAllocator.DEFAULT.buffer();
        new MessageCodec().encode(null,message,out);

        //入站方向
        channel.writeInbound(out);
>>>>>>> b11a9fe (Initial commit)
    }
}
