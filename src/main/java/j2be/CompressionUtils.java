package j2be;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class CompressionUtils {
	
	public static byte[] compress(byte[] inputPacket) {
		 // will compress inputPacket
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DeflaterOutputStream dos = new DeflaterOutputStream(baos);
        try {
        dos.write(inputPacket);
        dos.flush();
        dos.close();
        } catch(IOException e) {
        	System.out.println("byte array cannot be compressed");
        }
        return baos.toByteArray();
	}
	
	public static ByteBuf compress(ByteBuf inputPacket) {
		byte[] bytes = new byte[inputPacket.readableBytes()];
		inputPacket.readBytes(bytes);
		return Unpooled.wrappedBuffer(compress(bytes));
	}

	public static void compress(ByteBuf inputPacket, ByteBuf output) {
	    byte[] bytes = new byte[inputPacket.readableBytes()];
	    inputPacket.readBytes(bytes);
	    output.writeBytes(compress(bytes));
    }
	
	public static byte[] decompress(byte[] compressedPacket) {
		// will decompress compressedPacket
		ByteArrayInputStream bais = new ByteArrayInputStream(compressedPacket);
		InflaterInputStream iis = new InflaterInputStream(bais);
		
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[1000];
        int rlen;
        try {
        while ((rlen = iis.read(buf)) != -1) {
            baos.write(buf, 0, rlen);
        } 
        } catch(IOException e) {
        	System.out.println("Bad compression on compressedPacket");
        }
        return baos.toByteArray();

	}
	
	public static ByteBuf decompress(ByteBuf compressedPacket) {
		byte[] bytes = new byte[compressedPacket.readableBytes()];
		compressedPacket.readBytes(bytes);
		return Unpooled.wrappedBuffer(decompress(bytes));
	}

	public static void decompress(ByteBuf compressedPacket, ByteBuf output) {
	    byte[] bytes = new byte[compressedPacket.readableBytes()];
	    compressedPacket.readBytes(bytes);
	    output.writeBytes(decompress(bytes));
    }
	

}


