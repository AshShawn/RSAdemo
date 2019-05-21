package com.rsafile.test.FileClient;/**
 * Create by sq598 on 2019/5/20
 */

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.SocketAddress;
import java.util.UUID;

import io.netty.buffer.ByteBuf;
import io.netty.channel.*;

import com.alibaba.fastjson.JSON;
import com.rsafile.test.common.AESEncrept;
import com.rsafile.test.common.FileUploadFile;
import com.rsafile.test.common.RSAEncrypt;

/**
 * @program: rsafiledemo
 * @description:
 * @author: 沈琪
 * @create: 2019-05-20 10:21
 **/
public class FileClientHandler extends ChannelInboundHandlerAdapter {
    private int byteRead;
    private volatile int start = 0;
    private volatile int lastLength = 0;
    public RandomAccessFile randomAccessFile;
    private FileUploadFile fileUploadFile;
    private String publicKey;
    private String AESkey;

    public FileClientHandler(String publicKey, FileUploadFile ef) {
        if (ef.getFile().exists()) {
            if (!ef.getFile().isFile()) {
                System.out.println("Not a file :" + ef.getFile());
                return;
            }
        }
        this.fileUploadFile = ef;
        this.publicKey = publicKey;
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        System.out.println("客户端结束传递文件channelInactive()");
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("客户端channelactive");
        AESkey = UUID.randomUUID().toString();
        String encryptAESKey = RSAEncrypt.encrypt(AESkey, publicKey);
        ctx.writeAndFlush(encryptAESKey);
        System.out.println("客户端发送AES密钥秘文(RSA加密):" + encryptAESKey);
        System.out.println("对应明文AES密钥为:" + AESkey);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        System.out.println("客户端收到消息:" + JSON.toJSONString(msg));
        if (msg instanceof Integer) {
            start = (Integer) msg;
            if (start != -1) {
                randomAccessFile = new RandomAccessFile(
                        fileUploadFile.getFile(), "r");
                randomAccessFile.seek(start);
                System.out.println("长度：" + (randomAccessFile.length() - start));
                int a = (int) (randomAccessFile.length() - start);
                int b = (int) (randomAccessFile.length() / 1024 * 2);
                if (a < lastLength) {
                    lastLength = a;
                }
                System.out.println("文件长度：" + (randomAccessFile.length()) + ",start:" + start + ",a:" + a + ",b:" + b + ",lastLength:" + lastLength);
                byte[] bytes = new byte[lastLength];
                if ((byteRead = randomAccessFile.read(bytes)) != -1
                        && (randomAccessFile.length() - start) > 0) {
                    // log.info("byte 长度：" + bytes.length);
                    fileUploadFile.setEndPos(byteRead);
                    fileUploadFile.setBytes(bytes);
                    try {
                        ctx.writeAndFlush(AESEncrept.encrypt(JSON.toJSONString(fileUploadFile), AESkey));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    randomAccessFile.close();
                    ctx.close();
                    System.out.println("文件已经读完channelRead()--------" + byteRead);
                }
            }
        } else if (msg instanceof String) {
            try {
                randomAccessFile = new RandomAccessFile(fileUploadFile.getFile(),
                        "r");
                randomAccessFile.seek(fileUploadFile.getStarPos());
                // lastLength = (int) randomAccessFile.length() / 10;
                lastLength = 1024;
                byte[] bytes = new byte[lastLength];
                if ((byteRead = randomAccessFile.read(bytes)) != -1) {
                    fileUploadFile.setEndPos(byteRead);
                    fileUploadFile.setBytes(bytes);
                    ctx.writeAndFlush(AESEncrept.encrypt(JSON.toJSONString(fileUploadFile), AESkey));
                } else {
                    randomAccessFile.close();
                    ctx.close();
                    System.out.println("文件已经读完channelRead()--------" + byteRead);
                }
                System.out.println("channelActive()文件已经读完 " + byteRead);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
