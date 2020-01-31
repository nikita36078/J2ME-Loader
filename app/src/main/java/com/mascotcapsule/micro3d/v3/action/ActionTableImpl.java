package com.mascotcapsule.micro3d.v3.action;

import com.mascotcapsule.micro3d.v3.util.BitInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;

public class ActionTableImpl {
	private int numActions;
	private int[] numKeyframes;
	private ArrayList<Action> actions = new ArrayList<>();

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
		System.out.printf("num_actions=%d num_segments=%d tra_unk3=%d tra_unk4=%d\n",
				numActions, num_segments, tra_unk3[1], tra_unk4);

		numKeyframes = new int[numActions];

		for (int l = 0; l < numActions; l++) {
			int keyframes = bis.readUnsignedShort();
			numKeyframes[l] = keyframes << 16;

			Action action = new Action();
			for (int i = 0; i < num_segments; i++) {
				AnimatedBone bone = unpackSegment(bis);
				action.add(bone);
			}
			int count = bis.readUnsignedShort();
			for (int j = 0; j < count; j++) {
				// aux
				bis.readUnsignedShort();
				bis.readUnsignedShort();
				bis.readUnsignedShort();
			}
			actions.add(action);
		}

		unpackTrailer(bis);
		if (bis.read() != -1) {
			System.out.println("uninterpreted bytes in file");
		}

		bis.close();
	}

	private AnimatedBone unpackSegment(BitInputStream bis) throws IOException {
		AnimatedBone bone = new AnimatedBone();
		int type = bis.readUnsignedByte();
		switch (type) {
			case 0:
				//  full matrix
				readMatrixAnimation(bis, bone);
				break;
			case 1:
				// identity
				bone.add(new Animation(type, 0, 0, 0, 0));
				break;
			case 2: {
				// animation
				// translation
				readAnimation3(bis, bone, type);
				// scale
				readAnimation3(bis, bone, type);
				// rotation
				readAnimation3(bis, bone, type);
				// roll
				readAnimation1(bis, bone, type);
				break;
			}
			case 3: {
				bis.readUnsignedShort();
				bis.readUnsignedShort();
				bis.readUnsignedShort();
				readAnimation3(bis, bone, type);
				bis.readUnsignedShort();
				break;
			}
			case 4: {
				readAnimation3(bis, bone, type);
				readAnimation1(bis, bone, type);
				break;
			}
			case 5: {
				readAnimation3(bis, bone, type);
				break;
			}
			case 6: {
				readAnimation3(bis, bone, type);
				readAnimation3(bis, bone, type);
				readAnimation1(bis, bone, type);
				break;
			}
			default:
				throw new RuntimeException("Animation type " + type + " is not supported");
		}
		return bone;
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

	private void readAnimation1(BitInputStream bis, AnimatedBone bone, int type) throws IOException {
		int count = bis.readUnsignedShort();
		for (int j = 0; j < count; j++) {
			int start = bis.readUnsignedShort();
			int x = bis.readUnsignedShort();
			bone.add(new Animation(type, start, x, 0, 0));
		}
	}

	private void readAnimation3(BitInputStream bis, AnimatedBone bone, int type) throws IOException {
		int count = bis.readUnsignedShort();
		for (int j = 0; j < count; j++) {
			int start = bis.readUnsignedShort();
			int x = bis.readUnsignedShort();
			int y = bis.readUnsignedShort();
			int z = bis.readUnsignedShort();
			bone.add(new Animation(type, start, x, y, z));
		}
	}

	private void readMatrixAnimation(BitInputStream bis, AnimatedBone bone) throws IOException {
		int m00 = bis.readShort();
		int m01 = bis.readShort();
		int m02 = bis.readShort();
		int m03 = bis.readShort();
		int m10 = bis.readShort();
		int m11 = bis.readShort();
		int m12 = bis.readShort();
		int m13 = bis.readShort();
		int m20 = bis.readShort();
		int m21 = bis.readShort();
		int m22 = bis.readShort();
		int m23 = bis.readShort();
		bone.add(new MatrixAnimation(new int[]{m00, m01, m02, m03, m10, m11, m12, m13, m20, m21, m22, m23}));
	}

	public int getNumActions() {
		return numActions;
	}

	public int getNumFrames(int idx) {
		int result;
		if (numActions != 0) {
			result = numKeyframes[idx];
		} else {
			result = 0;
		}
		return result;
	}
}
