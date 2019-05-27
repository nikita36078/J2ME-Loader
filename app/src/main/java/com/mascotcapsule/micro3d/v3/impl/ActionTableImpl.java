package com.mascotcapsule.micro3d.v3.impl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteOrder;
import java.util.Arrays;

public class ActionTableImpl {
	private int numActions;
	private int[] keyframesArr;

	@SuppressWarnings("ResultOfMethodCallIgnored")
	public ActionTableImpl(InputStream inputStream) throws IOException {
		BitInputStream bis = new BitInputStream(inputStream, ByteOrder.LITTLE_ENDIAN);
		byte[] mtraMagic = new byte[2];
		byte[] mtraVersion = new byte[2];
		bis.read(mtraMagic);
		bis.read(mtraVersion);
		byte[] magic = {'M', 'T'};
		if (!Arrays.equals(mtraMagic, magic)) {
			throw new RuntimeException("Not a MTRA file");
		}
		byte[] version = {5, 0};
		if (!Arrays.equals(mtraVersion, version)) {
			throw new RuntimeException("Unsupported version");
		}

		numActions = bis.readUnsignedShort();
		int num_segments = bis.readUnsignedShort();
		short[] tra_unk3 = new short[8];
		for (int i = 0; i < 8; i++) {
			tra_unk3[i] = (short) bis.readUnsignedShort();
		}
		int tra_unk4 = bis.readInt();

		keyframesArr = new int[numActions];

		for (int l = 0; l < numActions; l++) {
			int keyframes = bis.readUnsignedShort() << 16;
			keyframesArr[l] = keyframes;

			for (int i = 0; i < num_segments; i++) {
				unpackSegment(bis);
			}
			int count = bis.readUnsignedShort();
			for (int j = 0; j < count; j++) {
				System.out.println("aux");
				bis.readUnsignedShort();
				bis.readUnsignedShort();
				bis.readUnsignedShort();
			}
		}

		unpackTrailer(bis);
		if (bis.read() != -1) {
			System.out.println("uninterpreted bytes in file");
		}

		bis.close();
	}

	private void unpackSegment(BitInputStream bis) throws IOException {
		int type = bis.readUnsignedByte();
		if (type == 0) {
			System.out.println("full matrix");
			for (int j = 0; j < 3; j++) {
				bis.readUnsignedShort();
				bis.readUnsignedShort();
				bis.readUnsignedShort();
				bis.readUnsignedShort();
			}
		} else if (type == 1) {
			System.out.println("identity");
		} else if (type == 2) {
			System.out.println("animation");
			int count = bis.readUnsignedShort();
			for (int j = 0; j < count; j++) {
				bis.readUnsignedShort();
				bis.readUnsignedShort();
				bis.readUnsignedShort();
				bis.readUnsignedShort();
			}
			count = bis.readUnsignedShort();
			for (int j = 0; j < count; j++) {
				bis.readUnsignedShort();
				bis.readUnsignedShort();
				bis.readUnsignedShort();
				bis.readUnsignedShort();
			}
			count = bis.readUnsignedShort();
			for (int j = 0; j < count; j++) {
				bis.readUnsignedShort();
				bis.readUnsignedShort();
				bis.readUnsignedShort();
				bis.readUnsignedShort();
			}
			count = bis.readUnsignedShort();
			for (int j = 0; j < count; j++) {
				bis.readUnsignedShort();
				bis.readUnsignedShort();
			}
		} else if (type == 3) {
			System.out.println("unknown3");
			bis.readUnsignedShort();
			bis.readUnsignedShort();
			bis.readUnsignedShort();
			int count = bis.readUnsignedShort();
			for (int j = 0; j < count; j++) {
				bis.readUnsignedShort();
				bis.readUnsignedShort();
				bis.readUnsignedShort();
				bis.readUnsignedShort();
			}
			bis.readUnsignedShort();
		} else if (type == 4) {
			System.out.println("unknown4");
			int count = bis.readUnsignedShort();
			for (int j = 0; j < count; j++) {
				bis.readUnsignedShort();
				bis.readUnsignedShort();
				bis.readUnsignedShort();
				bis.readUnsignedShort();
			}
			count = bis.readUnsignedShort();
			for (int j = 0; j < count; j++) {
				bis.readUnsignedShort();
				bis.readUnsignedShort();
			}
		} else if (type == 5) {
			System.out.println("unknown5");
			int count = bis.readUnsignedShort();
			for (int j = 0; j < count; j++) {
				bis.readUnsignedShort();
				bis.readUnsignedShort();
				bis.readUnsignedShort();
				bis.readUnsignedShort();
			}
		} else if (type == 6) {
			System.out.println("unknown6");
			int count = bis.readUnsignedShort();
			for (int j = 0; j < count; j++) {
				bis.readUnsignedShort();
				bis.readUnsignedShort();
				bis.readUnsignedShort();
				bis.readUnsignedShort();
			}
			count = bis.readUnsignedShort();
			for (int j = 0; j < count; j++) {
				bis.readUnsignedShort();
				bis.readUnsignedShort();
				bis.readUnsignedShort();
				bis.readUnsignedShort();
			}
			count = bis.readUnsignedShort();
			for (int j = 0; j < count; j++) {
				bis.readUnsignedShort();
				bis.readUnsignedShort();
			}
		} else {
			throw new RuntimeException("Animation type " + type + " is not supported");
		}
	}

	private void unpackTrailer(BitInputStream bis) throws IOException {
		StringBuilder wordStr = new StringBuilder();
		for (int i = 0; i < 2; i++) {
			byte[] key = new byte[2];
			bis.read(key);
			for (int j = 0; j < 4; j++) {
				byte[] word = new byte[2];

				for (int k = 0; k < 2; k++) {
					int encrypted_byte = bis.readUnsignedByte();
					word[k] = (byte) (((encrypted_byte ^ key[k]) + 127) & 0xff);
				}
				wordStr.append(new String(word));
			}
		}
		System.out.println("Trailer: " + wordStr);
	}

	public int getNumActions() {
		return numActions;
	}

	public int getNumFrames(int idx) {
		int result;
		if (numActions != 0) {
			result = keyframesArr[idx];
		} else {
			result = 0;
		}
		return result;
	}
}
