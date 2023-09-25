package Server.handler;

import Message.ChatRequestMessage;
import Message.ChatResponseMessage;
import Server.session.SessionFactory;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

@ChannelHandler.Sharable

public class ChatRequestHandler extends SimpleChannelInboundHandler<ChatRequestMessage> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ChatRequestMessage msg) throws Exception {
        String to = msg.getTo();
        Channel channel = SessionFactory.getSession().getChannel(to);
        //对方在线
        if(channel != null){
            channel.writeAndFlush(new ChatResponseMessage(msg.getFrom(),msg.getContent()));
        }
        //对方不在线
        else {
            ctx.writeAndFlush(new ChatResponseMessage(false,"对方用户不在线"));
        }
    }
}
