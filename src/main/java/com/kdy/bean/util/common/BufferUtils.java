package com.kdy.bean.util.common;

public class BufferUtils {

	public static int byteArrayToInt(byte[] paramArrayOfByte) {
		return paramArrayOfByte[0] << 24 | (paramArrayOfByte[1] & 0xFF) << 16 | (paramArrayOfByte[2] & 0xFF) << 8 | paramArrayOfByte[3] & 0xFF;
	}

	public static int byteArrayToInt(byte[] paramArrayOfByte, int paramInt) {
		return paramArrayOfByte[(paramInt + 0)] << 24 | (paramArrayOfByte[(paramInt + 1)] & 0xFF) << 16 | (paramArrayOfByte[(paramInt + 2)] & 0xFF) << 8 | paramArrayOfByte[(paramInt + 3)] & 0xFF;
	}

	public static int byteArrayToInt(byte[] paramArrayOfByte, int paramInt1, int paramInt2) {
		return byteArrayToInt(paramArrayOfByte, paramInt1, paramInt2, false);
	}

	public static int byteArrayToInt(byte[] paramArrayOfByte, int paramInt1, int paramInt2, boolean paramBoolean) {
		int i = 0;
		for (int j = 0; j < paramInt2; j++) {
			if (j > 0) {
				i <<= 8;
			}
			if (paramBoolean) {
				i |= paramArrayOfByte[(paramInt1 + (paramInt2 - (j + 1)))] & 0xFF;
			} else {
				i |= paramArrayOfByte[(paramInt1 + j)] & 0xFF;
			}
		}
		return i;
	}

	public static byte[] longToByteArray(long paramLong) {
		return longToByteArray(paramLong, 8);
	}

	public static void longToByteArray(long paramLong, byte[] paramArrayOfByte, int paramInt1, int paramInt2) {
		for (int i = 0; i < Math.min(paramInt2, 8); i++) {
			int j = paramInt2 - i - 1;
			paramArrayOfByte[(paramInt1 + j)] = (byte) (int) (paramLong & 0xFF);
			paramLong >>= 8;
		}
	}

	public static byte[] longToByteArray(long paramLong, int paramInt) {
		byte[] arrayOfByte = new byte[paramInt];
		for (int i = 0; i < Math.min(paramInt, 8); i++) {
			int j = paramInt - i - 1;
			arrayOfByte[j] = (byte) (int) (paramLong & 0xFF);
			paramLong >>= 8;
			if (paramLong == 0L) {
				break;
			}
		}
		return arrayOfByte;
	}

	public static void intToByteArray(int paramInt1, byte[] paramArrayOfByte, int paramInt2, int paramInt3) {
		intToByteArray(paramInt1, paramArrayOfByte, paramInt2, paramInt3, false);
	}

	public static void intToByteArray(int paramInt1, byte[] paramArrayOfByte, int paramInt2, int paramInt3, boolean paramBoolean) {
		for (int i = 0; i < Math.min(paramInt3, 4); i++) {
			int j = paramBoolean ? i : paramInt3 - (i + 1);
			paramArrayOfByte[(paramInt2 + j)] = (byte) (paramInt1 & 0xFF);
			paramInt1 >>= 8;
		}
	}
}
