package com.unascribed.thebappystick;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

public class RenameBappyStickMessage implements IMessage {

	public int windowId;
	public String name;

	@Override
	public void fromBytes(ByteBuf buf) {
		windowId = buf.readInt();
		name = ByteBufUtils.readUTF8String(buf);
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeInt(windowId);
		ByteBufUtils.writeUTF8String(buf, name);
	}

}
