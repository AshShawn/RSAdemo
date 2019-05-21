package com.rsafile.test.FileServer;/**
 * Create by sq598 on 2019/5/20
 */

import java.io.File;
import java.io.RandomAccessFile;
import java.security.PublicKey;
import java.util.UUID;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOutboundHandlerAdapter;

import com.alibaba.fastjson.JSON;
import com.rsafile.test.common.AESEncrept;
import com.rsafile.test.common.FileUploadFile;
import com.rsafile.test.common.RSAEncrypt;

/**
 * @program: rsafiledemo
 * @description:
 * @author: 沈琪
 * @create: 2019-05-20 13:38
 **/
public class FileServerHandler extends ChannelInboundHandlerAdapter {
    private int byteRead;
    private volatile int start = 0;
    //接收到文件保存的地址 todo
    private String file_dir = "C:\\develop\\code-repo\\workspace\\RSAFileDemo\\src\\main\\resources\\fileReceive";
    //RSA私钥(切勿改动)
    private String SECRET_KEY = "MIICdgIBADANBgkqhkiG9w0BAQEFAASCAmAwggJcAgEAAoGBAIL/kX/rAu7Xw1NUU175yG66lZBcCoBFJenqyDx5ZLnrV/d5k7f73Waqr8MozCc5sQdRJH1Tjgs09wdUCZz5RRH6gGaYsZ8iQyqmN6wPG6EE/7wymZEyWVtmAZbIkLo/DkIF9mj9D5+ArHX+H24ks1O1vQHRPbWnwipz/LvADOORAgMBAAECgYBcHmgs8hCf8K50fMob8b7WzvK6D197ECU3N1kT3bHNZAf6CU6thLoOplzu+lOGCfXJVXA5iXZLvzUvvBL+hfPhSjXyZ/T4vA547qt231skxxMGSK1qjPzehrYssEDbu/wY/QGv+OAy/1uYv1f+UTuIHV1gjTde+0c9Hxyv7FFGoQJBANTLD8rQg+254hngLOaR4MWO/DaALiDwxIs0G2oQdmZ6voPT7ebp1jeC/mPFICVS81PhQ8QaodYGavSDJZmXrMUCQQCdmNQIa2rx1pcNyhWW+qs6JGhrh+B7ljsAmAaJm+rDl0v0MaEDBAwDTRWOD7SFl9nFyBFQoPANvEdx6T53mKBdAkAN+zGUb9LbQcVbUeFhXOBZ2qUzp4RgYbFoPAo5E8/Tt+jgnIIbE+4hQ5gXUhJkoWifcEOVlPJhL5bDbgKAbvjJAkEAkl8wh8Vk5creLyODW9/jirPn+/+OYprMoCeS4tpaeGEcXWh+2DM5CRPeMjyp+O5piEXitxVTAB7f6I+uwhuSzQJACLt73OOEqmUAGO8VkYenQcxKVhjgbgKZG8/UCpSUqtyKNps4kSUW1jiKiM5UzTABUfGmYwgr4RJi1K4i2eKwAg==";

    private String AESKEY;
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        System.out.println("服务端channelactive");
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        ctx.flush();
        ctx.close();
    }


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        System.out.println("服务端收到消息:" + JSON.toJSONString(msg));
        if (msg instanceof byte[]) {
            String decryptMsg = new String(AESEncrept.decrypt(((byte[]) msg), AESKEY));
            FileUploadFile ef = JSON.parseObject(decryptMsg, FileUploadFile.class);
            byte[] bytes = ef.getBytes();
            byteRead = ef.getEndPos();
            String md5 = ef.getFile_md5();//文件名
            String path = file_dir + File.separator + md5;
            File file = new File(path);
            RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");
            randomAccessFile.seek(start);
            randomAccessFile.write(bytes);
            start = start + byteRead;
            System.out.println("path:" + path + "," + byteRead);
            if (byteRead > 0) {
                ctx.writeAndFlush(new Integer(start));
                randomAccessFile.close();
                if (byteRead != 1024) {
                    Thread.sleep(1000);
                    channelInactive(ctx);
                }
            } else {
                //System.out.println("文件接收完成");
                //ctx.flush();
                ctx.close();
            }
        } else if (msg instanceof String) {
            String encryptKey = (String) msg;
            AESKEY = RSAEncrypt.decrypt(encryptKey, SECRET_KEY);
            System.out.println("AES密钥明文:"+AESKEY);
            ctx.writeAndFlush("请开始上传加密文件");
        }

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
        System.out.println("FileUploadServerHandler--exceptionCaught()" + cause.getMessage());
    }
}
