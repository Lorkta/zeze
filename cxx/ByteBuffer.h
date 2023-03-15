#pragma once

#include <string.h>
#include <string>
#include <stdexcept>
#include "Serializable.h"
#include "Vector.h"

namespace Zeze
{
	class Bean;
	class DynamicBean;

	class ByteBuffer
	{
		ByteBuffer() = delete;
		ByteBuffer(const ByteBuffer&) = delete;
		ByteBuffer& operator=(const ByteBuffer&) = delete;

	public:
		unsigned char* Bytes;
		int ReadIndex;
		int WriteIndex;
		int Capacity;
	private:
		const bool IsEncodeMode;

	public:
		explicit ByteBuffer(int capacity) : IsEncodeMode(true)
		{
			Capacity = ToPower2(capacity);
			Bytes = new unsigned char[(size_t)Capacity];
			ReadIndex = 0;
			WriteIndex = 0;
		}

		// 应该仅用于Decode。
		ByteBuffer(unsigned char* bytes, int offset, int length) : IsEncodeMode(false)
		{
			Bytes = bytes;
			ReadIndex = offset;
			WriteIndex = offset + length;
			Capacity = length; // XXX
		}

		int Size() const
		{
			return WriteIndex - ReadIndex;
		}

		~ByteBuffer()
		{
			if (IsEncodeMode)
				delete[] Bytes;
		}

		void Append(char b)
		{
			EnsureWrite(1);
			Bytes[WriteIndex++] = (unsigned char)b;
		}

		void Append(const char* src, int offset, int len)
		{
			EnsureWrite(len);
			memcpy(Bytes + WriteIndex, src + offset, (size_t)len);
			WriteIndex += len;
		}

		void Replace(int writeIndex, const char* src, int offset, int len)
		{
			if (writeIndex < ReadIndex || writeIndex + len > WriteIndex)
				throw std::exception();
			memcpy(Bytes + writeIndex, src + offset, (size_t)len);
		}

		void BeginWriteWithSize4(int& state)
		{
			state = Size();
			EnsureWrite(4);
			WriteIndex += 4;
		}

		void EndWriteWithSize4(int state)
		{
			int oldWriteIndex = state + ReadIndex;
			int size = WriteIndex - oldWriteIndex - 4;
			Replace(oldWriteIndex, (char*)&size, 0, 4); //NOTE: 注意大小端问题
		}

		void Campact()
		{
			int size = Size();
			if (size > 0)
			{
				if (ReadIndex > 0)
				{
					memmove(Bytes, Bytes + ReadIndex, (size_t)size);
					ReadIndex = 0;
					WriteIndex = size;
				}
			}
			else
				Reset();
		}

		/*
		char* Copy()
		{
			int size = Size();
			char* copy = new char[size];
			memcpy(copy, Bytes + ReadIndex, size);
			return copy;
		}
		*/

		void Reset()
		{
			ReadIndex = WriteIndex = 0;
		}

		static int ToPower2(int needSize)
		{
			int size = 1024;
			while (size < needSize)
				size <<= 1;
			return size;
		}

		void EnsureWrite(int size)
		{
			if (!IsEncodeMode)
				throw std::exception("not encode mode");

			int newSize = WriteIndex + size;
			if (newSize > Capacity)
			{
				Capacity = ToPower2(newSize);
				unsigned char* newBytes = new unsigned char[(size_t)Capacity];
				WriteIndex -= ReadIndex;
				memcpy(newBytes, Bytes + ReadIndex, (size_t)WriteIndex);
				ReadIndex = 0;
				delete[] Bytes;
				Bytes = newBytes;
			}
		}

		void EnsureRead(int size) const
		{
			if (IsEncodeMode)
				throw std::exception("not decode mode");
			if (ReadIndex + size > WriteIndex)
				throw std::exception("EnsureRead");
		}

		void WriteBool(bool b)
		{
			EnsureWrite(1);
			Bytes[WriteIndex++] = (unsigned char)(b ? 1 : 0);
		}

		bool ReadBool()
		{
			return ReadLong() != 0;
		}

		void WriteByte(char x)
		{
			EnsureWrite(1);
			Bytes[WriteIndex++] = (unsigned char)x;
		}

		void WriteByte(int x)
		{
			EnsureWrite(1);
			Bytes[WriteIndex++] = (unsigned char)x;
		}

		char ReadByte()
		{
			EnsureRead(1);
			return (char)Bytes[ReadIndex++];
		}

		void WriteInt4(int x)
		{
			Append((char*)&x, 0, 4); //NOTE: 注意大小端问题
		}

		int ReadInt4()
		{
			EnsureRead(4);
			int x = *(int*)(Bytes + ReadIndex); //NOTE: 注意大小端和对齐问题
			ReadIndex += 4;
			return x;
		}

		void WriteLong8(long long x)
		{
			char* bs = (char*)&x; //NOTE: 注意大小端问题
			//if (!BitConverter.IsLittleEndian)
			//	Array.Reverse(bs);
			Append(bs, 0, 8);
		}

		long long ReadLong8()
		{
			EnsureRead(8);
			long long x = *(long long*)(Bytes + ReadIndex); //NOTE: 注意大小端和对齐问题
			ReadIndex += 8;
			return x;
		}

		void WriteUInt(int x)
		{
			unsigned int u = (unsigned int)x;
			if (u < 0x80)
			{
				EnsureWrite(1); // 0xxx xxxx
				Bytes[WriteIndex++] = (unsigned char)u;
			}
			else if (u < 0x4000)
			{
				EnsureWrite(2); // 10xx xxxx +1B
				unsigned char* bytes = Bytes;
				int writeIndex = WriteIndex;
				bytes[writeIndex] = (unsigned char)((u >> 8) + 0x80);
				bytes[writeIndex + 1] = (unsigned char)u;
				WriteIndex = writeIndex + 2;
			}
			else if (u < 0x200000)
			{
				EnsureWrite(3); // 110x xxxx +2B
				unsigned char* bytes = Bytes;
				int writeIndex = WriteIndex;
				bytes[writeIndex] = (unsigned char)((u >> 16) + 0xc0);
				bytes[writeIndex + 1] = (unsigned char)(u >> 8);
				bytes[writeIndex + 2] = (unsigned char)u;
				WriteIndex = writeIndex + 3;
			}
			else if (u < 0x10000000)
			{
				EnsureWrite(4); // 1110 xxxx +3B
				unsigned char* bytes = Bytes;
				int writeIndex = WriteIndex;
				bytes[writeIndex] = (unsigned char)((u >> 24) + 0xe0);
				bytes[writeIndex + 1] = (unsigned char)(u >> 16);
				bytes[writeIndex + 2] = (unsigned char)(u >> 8);
				bytes[writeIndex + 3] = (unsigned char)u;
				WriteIndex = writeIndex + 4;
			}
			else
			{
				EnsureWrite(5); // 1111 0000 +4B
				unsigned char* bytes = Bytes;
				int writeIndex = WriteIndex;
				bytes[writeIndex] = (unsigned char)0xf0;
				bytes[writeIndex + 1] = (unsigned char)(u >> 24);
				bytes[writeIndex + 2] = (unsigned char)(u >> 16);
				bytes[writeIndex + 3] = (unsigned char)(u >> 8);
				bytes[writeIndex + 4] = (unsigned char)u;
				WriteIndex = writeIndex + 5;
			}
		}

		int ReadUInt()
		{
			EnsureRead(1);
			const unsigned char* bytes = Bytes;
			int readIndex = ReadIndex;
			int x = bytes[readIndex];
			if (x < 0x80)
				ReadIndex = readIndex + 1;
			else if (x < 0xc0)
			{
				EnsureRead(2);
				x = ((int)(x & 0x3f) << 8)
						+ bytes[readIndex + 1];
				ReadIndex = readIndex + 2;
			}
			else if (x < 0xe0)
			{
				EnsureRead(3);
				x = ((int)(x & 0x1f) << 16)
						+ ((int)bytes[readIndex + 1] << 8)
						+ bytes[readIndex + 2];
				ReadIndex = readIndex + 3;
			}
			else if (x < 0xf0)
			{
				EnsureRead(4);
				x = ((int)(x & 0xf) << 24)
						+ ((int)bytes[readIndex + 1] << 16)
						+ ((int)bytes[readIndex + 2] << 8)
						+ bytes[readIndex + 3];
				ReadIndex = readIndex + 4;
			}
			else
			{
				EnsureRead(5);
				x = ((int)bytes[readIndex + 1] << 24)
						+ ((int)bytes[readIndex + 2] << 16)
						+ ((int)bytes[readIndex + 3] << 8)
						+ bytes[readIndex + 4];
				ReadIndex = readIndex + 5;
			}
			return x;
		}

		void WriteLong(long long x)
		{
			if (x >= 0)
			{
				if (x < 0x40)
				{
					EnsureWrite(1); // 00xx xxxx
					Bytes[WriteIndex++] = (unsigned char)x;
				}
				else if (x < 0x2000)
				{
					EnsureWrite(2); // 010x xxxx +1B
					unsigned char* bytes = Bytes;
					int writeIndex = WriteIndex;
					bytes[writeIndex] = (unsigned char)((x >> 8) + 0x40);
					bytes[writeIndex + 1] = (unsigned char)x;
					WriteIndex = writeIndex + 2;
				}
				else if (x < 0x100000)
				{
					EnsureWrite(3); // 0110 xxxx +2B
					unsigned char* bytes = Bytes;
					int writeIndex = WriteIndex;
					bytes[writeIndex] = (unsigned char)((x >> 16) + 0x60);
					bytes[writeIndex + 1] = (unsigned char)(x >> 8);
					bytes[writeIndex + 2] = (unsigned char)x;
					WriteIndex = writeIndex + 3;
				}
				else if (x < 0x8000000)
				{
					EnsureWrite(4); // 0111 0xxx +3B
					unsigned char* bytes = Bytes;
					int writeIndex = WriteIndex;
					bytes[writeIndex] = (unsigned char)((x >> 24) + 0x70);
					bytes[writeIndex + 1] = (unsigned char)(x >> 16);
					bytes[writeIndex + 2] = (unsigned char)(x >> 8);
					bytes[writeIndex + 3] = (unsigned char)x;
					WriteIndex = writeIndex + 4;
				}
				else if (x < 0x400000000LL)
				{
					EnsureWrite(5); // 0111 10xx +4B
					unsigned char* bytes = Bytes;
					int writeIndex = WriteIndex;
					bytes[writeIndex] = (unsigned char)((x >> 32) + 0x78);
					bytes[writeIndex + 1] = (unsigned char)(x >> 24);
					bytes[writeIndex + 2] = (unsigned char)(x >> 16);
					bytes[writeIndex + 3] = (unsigned char)(x >> 8);
					bytes[writeIndex + 4] = (unsigned char)x;
					WriteIndex = writeIndex + 5;
				}
				else if (x < 0x20000000000LL)
				{
					EnsureWrite(6); // 0111 110x +5B
					unsigned char* bytes = Bytes;
					int writeIndex = WriteIndex;
					bytes[writeIndex] = (unsigned char)((x >> 40) + 0x7c);
					bytes[writeIndex + 1] = (unsigned char)(x >> 32);
					bytes[writeIndex + 2] = (unsigned char)(x >> 24);
					bytes[writeIndex + 3] = (unsigned char)(x >> 16);
					bytes[writeIndex + 4] = (unsigned char)(x >> 8);
					bytes[writeIndex + 5] = (unsigned char)x;
					WriteIndex = writeIndex + 6;
				}
				else if (x < 0x1000000000000LL)
				{
					EnsureWrite(7); // 0111 1110 +6B
					unsigned char* bytes = Bytes;
					int writeIndex = WriteIndex;
					bytes[writeIndex] = (unsigned char)0x7e;
					bytes[writeIndex + 1] = (unsigned char)(x >> 40);
					bytes[writeIndex + 2] = (unsigned char)(x >> 32);
					bytes[writeIndex + 3] = (unsigned char)(x >> 24);
					bytes[writeIndex + 4] = (unsigned char)(x >> 16);
					bytes[writeIndex + 5] = (unsigned char)(x >> 8);
					bytes[writeIndex + 6] = (unsigned char)x;
					WriteIndex = writeIndex + 7;
				}
				else if (x < 0x80000000000000LL)
				{
					EnsureWrite(8); // 0111 1111 0 +7B
					unsigned char* bytes = Bytes;
					int writeIndex = WriteIndex;
					bytes[writeIndex] = (unsigned char)0x7f;
					bytes[writeIndex + 1] = (unsigned char)(x >> 48);
					bytes[writeIndex + 2] = (unsigned char)(x >> 40);
					bytes[writeIndex + 3] = (unsigned char)(x >> 32);
					bytes[writeIndex + 4] = (unsigned char)(x >> 24);
					bytes[writeIndex + 5] = (unsigned char)(x >> 16);
					bytes[writeIndex + 6] = (unsigned char)(x >> 8);
					bytes[writeIndex + 7] = (unsigned char)x;
					WriteIndex = writeIndex + 8;
				}
				else
				{
					EnsureWrite(9); // 0111 1111 1 +8B
					unsigned char* bytes = Bytes;
					int writeIndex = WriteIndex;
					bytes[writeIndex] = (unsigned char)0x7f;
					bytes[writeIndex + 1] = (unsigned char)((x >> 56) + 0x80);
					bytes[writeIndex + 2] = (unsigned char)(x >> 48);
					bytes[writeIndex + 3] = (unsigned char)(x >> 40);
					bytes[writeIndex + 4] = (unsigned char)(x >> 32);
					bytes[writeIndex + 5] = (unsigned char)(x >> 24);
					bytes[writeIndex + 6] = (unsigned char)(x >> 16);
					bytes[writeIndex + 7] = (unsigned char)(x >> 8);
					bytes[writeIndex + 8] = (unsigned char)x;
					WriteIndex = writeIndex + 9;
				}
			}
			else
			{
				if (x >= -0x40)
				{
					EnsureWrite(1); // 11xx xxxx
					Bytes[WriteIndex++] = (unsigned char)x;
				}
				else if (x >= -0x2000)
				{
					EnsureWrite(2); // 101x xxxx +1B
					unsigned char* bytes = Bytes;
					int writeIndex = WriteIndex;
					bytes[writeIndex] = (unsigned char)((x >> 8) - 0x40);
					bytes[writeIndex + 1] = (unsigned char)x;
					WriteIndex = writeIndex + 2;
				}
				else if (x >= -0x100000)
				{
					EnsureWrite(3); // 1001 xxxx +2B
					unsigned char* bytes = Bytes;
					int writeIndex = WriteIndex;
					bytes[writeIndex] = (unsigned char)((x >> 16) - 0x60);
					bytes[writeIndex + 1] = (unsigned char)(x >> 8);
					bytes[writeIndex + 2] = (unsigned char)x;
					WriteIndex = writeIndex + 3;
				}
				else if (x >= -0x8000000)
				{
					EnsureWrite(4); // 1000 1xxx +3B
					unsigned char* bytes = Bytes;
					int writeIndex = WriteIndex;
					bytes[writeIndex] = (unsigned char)((x >> 24) - 0x70);
					bytes[writeIndex + 1] = (unsigned char)(x >> 16);
					bytes[writeIndex + 2] = (unsigned char)(x >> 8);
					bytes[writeIndex + 3] = (unsigned char)x;
					WriteIndex = writeIndex + 4;
				}
				else if (x >= -0x400000000LL)
				{
					EnsureWrite(5); // 1000 01xx +4B
					unsigned char* bytes = Bytes;
					int writeIndex = WriteIndex;
					bytes[writeIndex] = (unsigned char)((x >> 32) - 0x78);
					bytes[writeIndex + 1] = (unsigned char)(x >> 24);
					bytes[writeIndex + 2] = (unsigned char)(x >> 16);
					bytes[writeIndex + 3] = (unsigned char)(x >> 8);
					bytes[writeIndex + 4] = (unsigned char)x;
					WriteIndex = writeIndex + 5;
				}
				else if (x >= -0x20000000000LL)
				{
					EnsureWrite(6); // 1000 001x +5B
					unsigned char* bytes = Bytes;
					int writeIndex = WriteIndex;
					bytes[writeIndex] = (unsigned char)((x >> 40) - 0x7c);
					bytes[writeIndex + 1] = (unsigned char)(x >> 32);
					bytes[writeIndex + 2] = (unsigned char)(x >> 24);
					bytes[writeIndex + 3] = (unsigned char)(x >> 16);
					bytes[writeIndex + 4] = (unsigned char)(x >> 8);
					bytes[writeIndex + 5] = (unsigned char)x;
					WriteIndex = writeIndex + 6;
				}
				else if (x >= -0x1000000000000LL)
				{
					EnsureWrite(7); // 1000 0001 +6B
					unsigned char* bytes = Bytes;
					int writeIndex = WriteIndex;
					bytes[writeIndex] = (unsigned char)0x81;
					bytes[writeIndex + 1] = (unsigned char)(x >> 40);
					bytes[writeIndex + 2] = (unsigned char)(x >> 32);
					bytes[writeIndex + 3] = (unsigned char)(x >> 24);
					bytes[writeIndex + 4] = (unsigned char)(x >> 16);
					bytes[writeIndex + 5] = (unsigned char)(x >> 8);
					bytes[writeIndex + 6] = (unsigned char)x;
					WriteIndex = writeIndex + 7;
				}
				else if (x >= -0x80000000000000LL)
				{
					EnsureWrite(8); // 1000 0000 1 +7B
					unsigned char* bytes = Bytes;
					int writeIndex = WriteIndex;
					bytes[writeIndex] = (unsigned char)0x80;
					bytes[writeIndex + 1] = (unsigned char)(x >> 48);
					bytes[writeIndex + 2] = (unsigned char)(x >> 40);
					bytes[writeIndex + 3] = (unsigned char)(x >> 32);
					bytes[writeIndex + 4] = (unsigned char)(x >> 24);
					bytes[writeIndex + 5] = (unsigned char)(x >> 16);
					bytes[writeIndex + 6] = (unsigned char)(x >> 8);
					bytes[writeIndex + 7] = (unsigned char)x;
					WriteIndex = writeIndex + 8;
				}
				else
				{
					EnsureWrite(9); // 1000 0000 0 +8B
					unsigned char* bytes = Bytes;
					int writeIndex = WriteIndex;
					bytes[writeIndex] = (unsigned char)0x80;
					bytes[writeIndex + 1] = (unsigned char)((x >> 56) - 0x80);
					bytes[writeIndex + 2] = (unsigned char)(x >> 48);
					bytes[writeIndex + 3] = (unsigned char)(x >> 40);
					bytes[writeIndex + 4] = (unsigned char)(x >> 32);
					bytes[writeIndex + 5] = (unsigned char)(x >> 24);
					bytes[writeIndex + 6] = (unsigned char)(x >> 16);
					bytes[writeIndex + 7] = (unsigned char)(x >> 8);
					bytes[writeIndex + 8] = (unsigned char)x;
					WriteIndex = writeIndex + 9;
				}
			}
		}

		long long ReadLong1()
		{
			EnsureRead(1);
			return Bytes[ReadIndex++];
		}

		long long ReadLong2BE()
		{
			EnsureRead(2);
			const unsigned char* bytes = Bytes;
			int readIndex = ReadIndex;
			ReadIndex = readIndex + 2;
			return ((long long)bytes[readIndex] << 8) +
					bytes[readIndex + 1];
		}

		long long ReadLong3BE()
		{
			EnsureRead(3);
			const unsigned char* bytes = Bytes;
			int readIndex = ReadIndex;
			ReadIndex = readIndex + 3;
			return ((long long)bytes[readIndex] << 16) +
					((long long)bytes[readIndex + 1] << 8) +
					bytes[readIndex + 2];
		}

		long long ReadLong4BE()
		{
			EnsureRead(4);
			const unsigned char* bytes = Bytes;
			int readIndex = ReadIndex;
			ReadIndex = readIndex + 4;
			return ((long long)bytes[readIndex] << 24) +
					((long long)bytes[readIndex + 1] << 16) +
					((long long)bytes[readIndex + 2] << 8) +
					bytes[readIndex + 3];
		}

		long long ReadLong5BE()
		{
			EnsureRead(5);
			const unsigned char* bytes = Bytes;
			int readIndex = ReadIndex;
			ReadIndex = readIndex + 5;
			return ((long long)bytes[readIndex] << 32) +
					((long long)bytes[readIndex + 1] << 24) +
					((long long)bytes[readIndex + 2] << 16) +
					((long long)bytes[readIndex + 3] << 8) +
					bytes[readIndex + 4];
		}

		long long ReadLong6BE()
		{
			EnsureRead(6);
			const unsigned char* bytes = Bytes;
			int readIndex = ReadIndex;
			ReadIndex = readIndex + 6;
			return ((long long)bytes[readIndex] << 40) +
					((long long)bytes[readIndex + 1] << 32) +
					((long long)bytes[readIndex + 2] << 24) +
					((long long)bytes[readIndex + 3] << 16) +
					((long long)bytes[readIndex + 4] << 8) +
					bytes[readIndex + 5];
		}

		long long ReadLong7BE()
		{
			EnsureRead(7);
			const unsigned char* bytes = Bytes;
			int readIndex = ReadIndex;
			ReadIndex = readIndex + 7;
			return ((long long)bytes[readIndex] << 48) +
					((long long)bytes[readIndex + 1] << 40) +
					((long long)bytes[readIndex + 2] << 32) +
					((long long)bytes[readIndex + 3] << 24) +
					((long long)bytes[readIndex + 4] << 16) +
					((long long)bytes[readIndex + 5] << 8) +
					bytes[readIndex + 6];
		}

		long long ReadLong()
		{
			EnsureRead(1);
			long long b = (signed char)Bytes[ReadIndex++];
			switch ((b >> 3) & 0x1f)
			{
			case 0x00: case 0x01: case 0x02: case 0x03: case 0x04: case 0x05: case 0x06: case 0x07:
			case 0x18: case 0x19: case 0x1a: case 0x1b: case 0x1c: case 0x1d: case 0x1e: case 0x1f: return b;
			case 0x08: case 0x09: case 0x0a: case 0x0b: return ((b - 0x40) <<  8) + ReadLong1();
			case 0x14: case 0x15: case 0x16: case 0x17: return ((b + 0x40) <<  8) + ReadLong1();
			case 0x0c: case 0x0d:                       return ((b - 0x60) << 16) + ReadLong2BE();
			case 0x12: case 0x13:                       return ((b + 0x60) << 16) + ReadLong2BE();
			case 0x0e:                                  return ((b - 0x70) << 24) + ReadLong3BE();
			case 0x11:                                  return ((b + 0x70) << 24) + ReadLong3BE();
			case 0x0f:
				switch (b & 7)
				{
				case 0: case 1: case 2: case 3: return ((b - 0x78) << 32) + ReadLong4BE();
				case 4: case 5:                 return ((b - 0x7c) << 40) + ReadLong5BE();
				case 6:                         return ReadLong6BE();
				default: long long r = ReadLong7BE(); return r < 0x80000000000000LL ?
						r : ((r - 0x80000000000000LL) << 8) + ReadLong1();
				}
			default: // 0x10
				switch (b & 7)
				{
				case 4: case 5: case 6: case 7: return ((b + 0x78) << 32) + ReadLong4BE();
				case 2: case 3:                 return ((b + 0x7c) << 40) + ReadLong5BE();
				case 1:                         return 0xffff000000000000LL + ReadLong6BE();
				default: long long r = ReadLong7BE(); return r >= 0x80000000000000LL ?
						0xff00000000000000LL + r : ((r + 0x80000000000000LL) << 8) + ReadLong1();
				}
			}
		}

		void WriteInt(int x)
		{
			WriteLong(x);
		}

		int ReadInt()
		{
			return (int)ReadLong();
		}

		void WriteFloat(float x)
		{
			Append((char*)&x, 0, 4); //NOTE: 注意大小端问题
		}

		float ReadFloat()
		{
			EnsureRead(4);
			float x = *(float*)(Bytes + ReadIndex); //NOTE: 注意大小端和对齐问题
			ReadIndex += 4;
			return x;
		}

		void WriteDouble(double x)
		{
			Append((char*)&x, 0, 8); //NOTE: 注意大小端问题
		}

		double ReadDouble()
		{
			EnsureRead(8);
			double x = *(double*)(Bytes + ReadIndex); //NOTE: 注意大小端和对齐问题
			ReadIndex += 8;
			return x;
		}

		// string must be utf-8 encode
		void WriteString(const std::string& x)
		{
			WriteBytes(x);
		}

		std::string ReadString()
		{
			unsigned int n = (unsigned int)ReadUInt();
			EnsureRead((int)n);
			std::string x((const char*)(Bytes + ReadIndex), n);
			ReadIndex += n;
			return x;
		}

		const char* ReadStringNoCopy(int& outlength)
		{
			int n = ReadUInt();
			EnsureRead(n);
			const char* outstr = (const char*)(Bytes + ReadIndex);
			ReadIndex += n;
			outlength = n;
			return outstr;
		}

		void WriteBytes(const std::string& x)
		{
			unsigned int length = (unsigned int)x.length();
			WriteUInt((int)length);
			EnsureWrite(length);
			memcpy(Bytes + WriteIndex, x.data(), length);
			WriteIndex += length;
		}

		std::string ReadBytes()
		{
			return ReadString();
		}

		void SkipBytes()
		{
			int n = ReadUInt();
			EnsureRead(n);
			ReadIndex += n;
		}

		void SkipBytes4()
		{
			int n = ReadInt4();
			EnsureRead(n);
			ReadIndex += n;
		}

		// 只能增加新的类型定义，增加时记得同步 SkipUnknownField
		static const int
			INTEGER = 0, // byte,short,int,long,bool
			FLOAT = 1, // float
			DOUBLE = 2, // double
			BYTES = 3, // binary,string
			LIST = 4, // list,set
			MAP = 5, // map
			BEAN = 6, // bean
			DYNAMIC = 7, // dynamic
			VECTOR2 = 8, // float{x,y}
			VECTOR2INT = 9, // int{x,y}
			VECTOR3 = 10, // float{x,y,z}
			VECTOR3INT = 11, // int{x,y,z}
			VECTOR4 = 12; // float{x,y,z,w} Quaternion

		static const int TAG_SHIFT = 4;
		static const int TAG_MASK = (1 << TAG_SHIFT) - 1;
		static const int ID_MASK = 0xff - TAG_MASK;

		// 只用于lua描述特殊类型
		static const int
			LUA_BOOL = INTEGER + (1 << TAG_SHIFT),
			LUA_SET = LIST + (1 << TAG_SHIFT);

		int WriteTag(int lastVarId, int varId, int type)
		{
			int deltaId = varId - lastVarId;
			if (deltaId < 0xf)
				WriteByte((deltaId << TAG_SHIFT) + type);
			else
			{
				WriteByte(0xf0 + type);
				WriteUInt(deltaId - 0xf);
			}
			return varId;
		}

		void WriteListType(int listSize, int elemType)
		{
			if (listSize < 0xf)
				WriteByte((listSize << TAG_SHIFT) + elemType);
			else
			{
				WriteByte(0xf0 + elemType);
				WriteUInt(listSize - 0xf);
			}
		}

		void WriteMapType(int mapSize, int keyType, int valueType)
		{
			WriteByte((keyType << TAG_SHIFT) + valueType);
			WriteUInt(mapSize);
		}

		int ReadTagSize(int tagByte)
		{
			int deltaId = (tagByte & ID_MASK) >> TAG_SHIFT;
			return deltaId < 0xf ? deltaId : 0xf + ReadUInt();
		}

		bool ReadBool(int type)
		{
			type &= TAG_MASK;
			if (type == INTEGER)
				return ReadLong() != 0;
			if (type == FLOAT)
				return ReadFloat() != 0;
			if (type == DOUBLE)
				return ReadDouble() != 0;
			SkipUnknownField(type);
			return false;
		}

		char ReadByte(int type)
		{
			type &= TAG_MASK;
			if (type == INTEGER)
				return (char)ReadLong();
			if (type == FLOAT)
				return (char)ReadFloat();
			if (type == DOUBLE)
				return (char)ReadDouble();
			SkipUnknownField(type);
			return 0;
		}

		short ReadShort(int type)
		{
			type &= TAG_MASK;
			if (type == INTEGER)
				return (short)ReadLong();
			if (type == FLOAT)
				return (short)ReadFloat();
			if (type == DOUBLE)
				return (short)ReadDouble();
			SkipUnknownField(type);
			return 0;
		}

		int ReadInt(int type)
		{
			type &= TAG_MASK;
			if (type == INTEGER)
				return (int)ReadLong();
			if (type == FLOAT)
				return (int)ReadFloat();
			if (type == DOUBLE)
				return (int)ReadDouble();
			SkipUnknownField(type);
			return 0;
		}

		long long ReadLong(int type)
		{
			type &= TAG_MASK;
			if (type == INTEGER)
				return ReadLong();
			if (type == FLOAT)
				return (long long)ReadFloat();
			if (type == DOUBLE)
				return (long long)ReadDouble();
			SkipUnknownField(type);
			return 0;
		}

		float ReadFloat(int type)
		{
			type &= TAG_MASK;
			if (type == FLOAT)
				return ReadFloat();
			if (type == DOUBLE)
				return (float)ReadDouble();
			if (type == INTEGER)
				return (float)ReadLong();
			SkipUnknownField(type);
			return 0;
		}

		double ReadDouble(int type)
		{
			type &= TAG_MASK;
			if (type == DOUBLE)
				return ReadDouble();
			if (type == FLOAT)
				return ReadFloat();
			if (type == INTEGER)
				return (double)ReadLong();
			SkipUnknownField(type);
			return 0;
		}

		std::string ReadBytes(int type)
		{
			type &= TAG_MASK;
			if (type == BYTES)
				return ReadBytes();
			SkipUnknownField(type);
			return "";
		}

		std::string ReadString(int type)
		{
			type &= TAG_MASK;
			if (type == BYTES)
				return ReadString();
			SkipUnknownField(type);
			return "";
		}

		static float ToFloat(void* p)
		{
			return *reinterpret_cast<float*>(p);
		}

		Vector2 ReadVector2()
		{
			EnsureRead(8);
			int i = ReadIndex;
			float x = ToFloat(Bytes + i);
			float y = ToFloat(Bytes + i + 4);
			ReadIndex = i + 8;
			return Vector2(x, y);
		}

		Vector3 ReadVector3()
		{
			EnsureRead(12);
			int i = ReadIndex;
			float x = ToFloat(Bytes + i);
			float y = ToFloat(Bytes + i + 4);
			float z = ToFloat(Bytes + i + 8);
			ReadIndex = i + 12;
			return Vector3(x, y, z);
		};

		Vector4 ReadVector4()
		{
			EnsureRead(16);
			int i = ReadIndex;
			float x = ToFloat(Bytes + i);
			float y = ToFloat(Bytes + i + 4);
			float z = ToFloat(Bytes + i + 8);
			float w = ToFloat(Bytes + i + 12);
			ReadIndex = i + 16;
			return Vector4(x, y, z, w);
		}

		Quaternion ReadQuaternion()
		{
			EnsureRead(16);
			int i = ReadIndex;
			float x = ToFloat(Bytes + i);
			float y = ToFloat(Bytes + i + 4);
			float z = ToFloat(Bytes + i + 8);
			float w = ToFloat(Bytes + i + 12);
			ReadIndex = i + 16;
			return Quaternion(x, y, z, w);
		}

		Vector2Int ReadVector2Int()
		{
			int x = ReadInt();
			int y = ReadInt();
			return Vector2Int(x, y);
		}

		Vector3Int ReadVector3Int()
		{
			int x = ReadInt();
			int y = ReadInt();
			int z = ReadInt();
			return Vector3Int(x, y, z);
		}

		Vector2 ReadVector2(int type)
		{
			type &= TAG_MASK;
			if (type == VECTOR2)
				return ReadVector2();
			if (type == VECTOR3)
				return ReadVector3();
			if (type == VECTOR4)
				return ReadVector4();
			if (type == VECTOR2INT)
				return Vector2(ReadVector2Int());
			if (type == VECTOR3INT)
				return Vector3(ReadVector3Int());
			SkipUnknownField(type);
			return Vector2();
		}

		Vector3 ReadVector3(int type)
		{
			type &= TAG_MASK;
			if (type == VECTOR3)
				return ReadVector3();
			if (type == VECTOR2)
				return Vector3(ReadVector2());
			if (type == VECTOR4)
				return ReadVector4();
			if (type == VECTOR3INT)
				return Vector3(ReadVector3Int());
			if (type == VECTOR2INT)
				return Vector3(ReadVector2Int());
			SkipUnknownField(type);
			return Vector3();
		}

		Vector4 ReadVector4(int type)
		{
			type &= TAG_MASK;
			if (type == VECTOR4)
				return ReadVector4();
			if (type == VECTOR3)
				return Vector4(ReadVector3());
			if (type == VECTOR2)
				return Vector4(ReadVector2());
			if (type == VECTOR3INT)
				return Vector4(ReadVector3Int());
			if (type == VECTOR2INT)
				return Vector4(ReadVector2Int());
			SkipUnknownField(type);
			return Vector4();
		}

		Quaternion ReadQuaternion(int type)
		{
			type &= TAG_MASK;
			if (type == VECTOR4)
				return ReadQuaternion();
			if (type == VECTOR3)
				return Quaternion(ReadVector3());
			if (type == VECTOR2)
				return Quaternion(ReadVector2());
			if (type == VECTOR3INT)
				return Quaternion(ReadVector3Int());
			if (type == VECTOR2INT)
				return Quaternion(ReadVector2Int());
			SkipUnknownField(type);
			return Quaternion();
		}

		Vector2Int ReadVector2Int(int type)
		{
			type &= TAG_MASK;
			if (type == VECTOR2INT)
				return ReadVector2Int();
			if (type == VECTOR3INT)
				return ReadVector3Int();
			if (type == VECTOR2)
				return Vector2Int(ReadVector2());
			if (type == VECTOR3)
				return Vector2Int(ReadVector3());
			if (type == VECTOR4)
				return Vector2Int(ReadVector4());
			SkipUnknownField(type);
			return Vector2Int();
		}

		Vector3Int ReadVector3Int(int type)
		{
			type &= TAG_MASK;
			if (type == VECTOR3INT)
				return ReadVector3Int();
			if (type == VECTOR2INT)
				return Vector3Int(ReadVector2Int());
			if (type == VECTOR3)
				return Vector3Int(ReadVector3());
			if (type == VECTOR2)
				return Vector3Int(ReadVector2());
			if (type == VECTOR4)
				return Vector3Int(ReadVector4());
			SkipUnknownField(type);
			return Vector3Int();
		}

		void WriteVector2(const Vector2& v)
		{
			v.Encode(*this);
		}

		void WriteVector3(const Vector3& v)
		{
			v.Encode(*this);
		}

		void WriteVector4(const Vector4& v)
		{
			v.Encode(*this);
		}

		void WriteVector2Int(const Vector2Int& v)
		{
			v.Encode(*this);
		}

		void WriteVector3Int(const Vector3Int& v)
		{
			v.Encode(*this);
		}

		void WriteQuaternion(const Quaternion& v)
		{
			v.Encode(*this);
		}

		std::string ReadBinary(int type)
		{
			type &= TAG_MASK;
			if (type == BYTES)
				return ReadBinary();
			SkipUnknownField(type);
			return std::string();
		}

		std::string ReadBinary()
		{
			return ReadString();
		}

		void WriteBinary(const std::string& v)
		{
			WriteString(v);
		}

		template<class T> // extends Bean
		T& ReadBean(T& bean, int type)
		{
			type &= TAG_MASK;
			if (type == BEAN)
				bean.Decode(*this);
			else if (type == DYNAMIC)
			{
				ReadLong();
				bean.Decode(*this);
			}
			else
				SkipUnknownField(type);
			return bean;
		}

		DynamicBean& ReadDynamic(DynamicBean& dynBean, int type);

		void SkipUnknownFieldOrThrow(int type, const char* curType)
		{
//			if (IGNORE_INCOMPATIBLE_FIELD)
				SkipUnknownField(type);
//			else
//				throw new IllegalStateException("can not read " + curType + " for type=" + type);
		}

		void SkipUnknownField(int type, int count)
		{
			while (--count >= 0)
				SkipUnknownField(type);
		}

		void SkipUnknownField(int type1, int type2, int count)
		{
			while (--count >= 0)
			{
				SkipUnknownField(type1);
				SkipUnknownField(type2);
			}
		}

		void SkipUnknownField(int type)
		{
			int t;
			switch (type & TAG_MASK)
			{
			case INTEGER:
				ReadLong();
				return;
			case FLOAT:
				EnsureRead(4);
				ReadIndex += 4;
				return;
			case DOUBLE:
				EnsureRead(8);
				ReadIndex += 8;
				return;
			case BYTES:
				SkipBytes();
				return;
			case LIST:
				t = ReadByte();
				SkipUnknownField(t, ReadTagSize(t));
				return;
			case MAP:
				t = ReadByte();
				SkipUnknownField(t >> TAG_SHIFT, t, ReadUInt());
				return;
			case DYNAMIC:
				ReadLong();
			case BEAN:
				while ((t = ReadByte()) != 0)
				{
					if ((t & ID_MASK) == 0xf0)
						ReadUInt();
					SkipUnknownField(t);
				}
				return;
			default:
				throw std::exception("SkipUnknownField");
			}
		}
	};
} // namespace Zeze
