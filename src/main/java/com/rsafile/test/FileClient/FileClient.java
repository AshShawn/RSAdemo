package com.rsafile.test.FileClient;/**
 * Create by sq598 on 2019/5/20
 */

import java.io.File;
import java.nio.channels.Pipe;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpRequestEncoder;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;

import com.rsafile.test.common.FileUploadFile;

import io.netty.handler.codec.serialization.ObjectEncoder;
import io.netty.handler.stream.ChunkedWriteHandler;

import com.rsafile.test.common.FileUploadFile;


/**
 * @program: rsafiledemo
 * @description: 文件传输客户端
 * @author: 沈琪
 * @create: 2019-05-20 10:17
 **/
public class FileClient {
    //端口号
    public static final int FILE_PORT = 6600;
    //被传输文件的地址 todo
    public static final String FILE_PATH = "C:\\Users\\sq598\\Desktop\\test.txt";
    //RSA公钥(切勿改动)
    public static final String PUBLIC_KEY = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCC/5F/6wLu18NTVFNe+chuupWQXAqARSXp6sg8eWS561f3eZO3+91mqq/DKMwnObEHUSR9U44LNPcHVAmc+UUR+oBmmLGfIkMqpjesDxuhBP+8MpmRMllbZgGWyJC6Pw5CBfZo/Q+fgKx1/h9uJLNTtb0B0T21p8Iqc/y7wAzjkQIDAQAB";

    public void connect(int port, String host, final FileUploadFile fileUploadFile) throws Exception {
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group).channel(NioSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .handler(new ChannelInitializer<Channel>() {

                        @Override
                        protected void initChannel(Channel ch) throws Exception {
                            ch.pipeline().addLast(new ObjectEncoder());
                            ch.pipeline().addLast(new ObjectDecoder(ClassResolvers.weakCachingConcurrentResolver(null)));
                            ch.pipeline().addLast(new FileClientHandler(PUBLIC_KEY,fileUploadFile));
                        }
                    });
            ChannelFuture f = b.connect(host, port).sync();
            f.channel().closeFuture().sync();
            System.out.println("FileUploadClient connect()结束");
        } finally {
            group.shutdownGracefully();
        }
    }


    public static void main(String[] args) {
        int port = FILE_PORT;
        if (args != null && args.length > 0) {
            try {
                port = Integer.valueOf(args[0]);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
        try {
            FileUploadFile uploadFile = new FileUploadFile();
            File file = new File(FILE_PATH);// d:/source.rar,D:/2014work/apache-maven-3.5.0-bin.tar.gz
            String fileMd5 = file.getName();// 文件名
            uploadFile.setFile(file);
            uploadFile.setFile_md5(fileMd5);
            uploadFile.setStarPos(0);// 文件开始位置
            new FileClient().connect(port, "180.168.141.103", uploadFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
