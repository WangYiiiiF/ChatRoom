package Server.handler;

import Message.LoginRequestMessage;
import Message.LoginResponseMessage;
import Server.service.UserServiceFactory;
import Server.session.SessionFactory;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

@ChannelHandler.Sharable

public class LoginRequestHandler extends SimpleChannelInboundHandler<LoginRequestMessage> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, LoginRequestMessage msg) throws Exception {
        String username = msg.getUsername();
        String passwd = msg.getPassword();
        boolean login = UserServiceFactory.getUserService().login(username, passwd);
        LoginResponseMessage message;
        if (login) {
            SessionFactory.getSession().bind(ctx.channel(), username);
            message = new LoginResponseMessage(true, "登录成功");
        } else {
            message = new LoginResponseMessage(false, "登录失败");
        }
        ctx.writeAndFlush(message);
    }
}
