package Client;

import Message.*;
import Protocol.MessageCodecSharable;
import Protocol.ProcotolFrameDecoder;
import Server.session.Group;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class ChatClient {
    public static void main(String[] args) {
        NioEventLoopGroup group = new NioEventLoopGroup();
        LoggingHandler LOGGING_HANDLER = new LoggingHandler(LogLevel.DEBUG);
        MessageCodecSharable MESSAGE_CODEC = new MessageCodecSharable();

        //用于线程交互
        CountDownLatch WAIT_FOR_LOGIN = new CountDownLatch(1);
        AtomicBoolean LOGIN = new AtomicBoolean(false);
        AtomicBoolean EXIT = new AtomicBoolean(false);

        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.channel(NioSocketChannel.class);
            bootstrap.group(group);
            bootstrap.handler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) throws Exception {

                    ch.pipeline().addLast(new ProcotolFrameDecoder());
                    //ch.pipeline().addLast(LOGGING_HANDLER);
                    ch.pipeline().addLast(MESSAGE_CODEC);

                    //空闲检测 避免出现连接假死 3秒内若未向服务器写数据，则会触发事件IdleState.WRITE_IDLE
                    ch.pipeline().addLast(new IdleStateHandler(0,3,0));
                    //ChannelDuplexHandler可同时作为入站和出站处理器
                    ch.pipeline().addLast(new ChannelDuplexHandler(){
                        //用来触发特殊事件
                        @Override
                        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
                            IdleStateEvent event = (IdleStateEvent) evt;
                            //触发了WRITE_IDLE事件
                            if(event.state() == IdleState.WRITER_IDLE){
                                log.debug("3s没有写数据了，发送一个心跳包");
                                ctx.writeAndFlush(new PingMessage());
                            }
                            super.userEventTriggered(ctx, evt);
                        }
                    });
                    ch.pipeline().addLast("Client Handler",new ChannelInboundHandlerAdapter(){

                        @Override
                        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                            log.debug("msg: {}",msg);
                            if(msg instanceof LoginResponseMessage){
                                LoginResponseMessage response = (LoginResponseMessage) msg;
                                if(response.isSuccess()){
                                    //如果登录成功
                                    LOGIN.set(true);
                                }
                            }
                            //唤醒线程1
                            WAIT_FOR_LOGIN.countDown();
                        }

                        @Override
                        public void channelActive(ChannelHandlerContext ctx) throws Exception {
                            //线程1：负责接收用户在控制台的输入
                            new Thread(()->{
                                Scanner scanner = new Scanner(System.in);
                                System.out.println("请输入用户名:");
                                String username = scanner.nextLine();
                                if(EXIT.get()){
                                    return;
                                }
                                System.out.println("请输入密码:");
                                String passwd = scanner.nextLine();
                                if(EXIT.get()){
                                    return;
                                }
                                //构造消息对象
                                LoginRequestMessage message = new LoginRequestMessage(username,passwd);
                                //发送消息
                                ctx.writeAndFlush(message);

                                System.out.println("等待···");
                                try {
                                    WAIT_FOR_LOGIN.await();
                                } catch (InterruptedException e) {
                                    throw new RuntimeException(e);
                                }
                                //如果登录失败
                                if(!LOGIN.get()){
                                    ctx.channel().close();
                                    return;
                                }
                                //如果登录成功
                                while(true){
                                    System.out.println("==================================");
                                    System.out.println("send [username] [content]");
                                    System.out.println("gsend [group name] [content]");
                                    System.out.println("gcreate [group name] [m1,m2,m3...]");
                                    System.out.println("gmembers [group name]");
                                    System.out.println("gjoin [group name]");
                                    System.out.println("gquit [group name]");
                                    System.out.println("quit");
                                    System.out.println("==================================");

                                    String command = scanner.nextLine();

                                    if(EXIT.get()){
                                        return;
                                    }

                                    String[] s = command.split(" ");
                                    switch (s[0]){
                                        //发送单人聊天消息
                                        case "send":
                                            ctx.writeAndFlush(new ChatRequestMessage(username,s[1],s[2]));
                                            break;
                                        //发送群聊天消息
                                        case "gsend":
                                            ctx.writeAndFlush(new GroupChatRequestMessage(username,s[1],s[2]));
                                            break;
                                        //创建群聊
                                        case "gcreate":
                                            Set<String> set = new HashSet<>(Arrays.asList(s[2].split(",")));
                                            set.add(username);
                                            ctx.writeAndFlush(new GroupCreateRequestMessage(s[1],set));
                                            break;
                                        //获取群内成员
                                        case "gmembers":
                                            ctx.writeAndFlush(new GroupMembersRequestMessage(s[1]));
                                            break;
                                        //加入群聊
                                        case "gjoin":
                                            ctx.writeAndFlush(new GroupJoinRequestMessage(username,s[1]));
                                            break;
                                        //退出群聊
                                        case "gquit":
                                            ctx.writeAndFlush(new GroupQuitRequestMessage(username,s[1]));
                                            break;
                                        //退出控制台
                                        case "quit":
                                            ctx.channel().close();
                                            return;

                                    }
                                }

                            },"system in").start();
                        }

                        // 在连接断开时触发
                        @Override
                        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                            log.debug("连接已经断开，按任意键退出..");
                            EXIT.set(true);
                        }

                        // 在出现异常时触发
                        @Override
                        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                            log.debug("连接已经断开，按任意键退出..{}", cause.getMessage());
                            EXIT.set(true);
                        }
                    });

                }
            });
            Channel channel = bootstrap.connect("localhost", 8080).sync().channel();
            channel.closeFuture().sync();
        } catch (Exception e) {
            log.error("client error", e);
        } finally {
            group.shutdownGracefully();
        }
    }
}
