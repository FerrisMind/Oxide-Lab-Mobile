#!/usr/bin/env python3

import struct
import sys
from pathlib import Path

def read_gguf_header(f):
    # GGUF magic: 'GGUF' (0x47, 0x47, 0x55, 0x46)
    magic = f.read(4)
    if magic != b'GGUF':
        raise ValueError("Not a GGUF file")

    # Version (uint32)
    version = struct.unpack('<I', f.read(4))[0]
    print(f"GGUF version: {version}")

    # Number of tensors (uint64)
    tensor_count = struct.unpack('<Q', f.read(8))[0]
    print(f"Tensor count: {tensor_count}")

    # Number of metadata key-value pairs (uint64)
    metadata_count = struct.unpack('<Q', f.read(8))[0]
    print(f"Metadata count: {metadata_count}")

    return version, tensor_count, metadata_count

def read_gguf_string(f):
    length = struct.unpack('<Q', f.read(8))[0]
    data = f.read(length)
    return data.decode('utf-8')

def read_gguf_value(f, value_type):
    if value_type == 1:  # uint8
        return struct.unpack('<B', f.read(1))[0]
    elif value_type == 2:  # int8
        return struct.unpack('<b', f.read(1))[0]
    elif value_type == 3:  # uint16
        return struct.unpack('<H', f.read(2))[0]
    elif value_type == 4:  # int16
        return struct.unpack('<h', f.read(2))[0]
    elif value_type == 5:  # uint32
        return struct.unpack('<I', f.read(4))[0]
    elif value_type == 6:  # int32
        return struct.unpack('<i', f.read(4))[0]
    elif value_type == 7:  # uint64
        return struct.unpack('<Q', f.read(8))[0]
    elif value_type == 8:  # int64
        return struct.unpack('<q', f.read(8))[0]
    elif value_type == 9:  # float32
        return struct.unpack('<f', f.read(4))[0]
    elif value_type == 10:  # float64
        return struct.unpack('<d', f.read(8))[0]
    elif value_type == 11:  # bool
        return bool(struct.unpack('<B', f.read(1))[0])
    elif value_type == 12:  # string
        return read_gguf_string(f)
    elif value_type == 13:  # array
        array_type = struct.unpack('<I', f.read(4))[0]
        array_len = struct.unpack('<Q', f.read(8))[0]
        array_data = []
        for i in range(min(array_len, 10)):  # Read first 10 elements
            array_data.append(read_gguf_value(f, array_type))
        if array_len > 10:
            # Skip remaining elements
            for _ in range(array_len - 10):
                read_gguf_value(f, array_type)
        return f"Array[{array_len}] of type {array_type}: {array_data[:5]}{'...' if array_len > 5 else ''}"
    else:
        raise ValueError(f"Unknown value type: {value_type}")

def read_metadata(f, metadata_count):
    metadata = {}
    for i in range(metadata_count):
        key = read_gguf_string(f)
        value_type = struct.unpack('<I', f.read(4))[0]
        value = read_gguf_value(f, value_type)
        metadata[key] = value
        print(f"  {key}: {value}")

    return metadata

def main():
    if len(sys.argv) != 2:
        print("Usage: python gguf_reader.py <gguf_file>")
        sys.exit(1)

    file_path = sys.argv[1]
    print(f"Reading GGUF file: {file_path}")

    try:
        with open(file_path, 'rb') as f:
            version, tensor_count, metadata_count = read_gguf_header(f)
            print("\nMetadata:")
            metadata = read_metadata(f, metadata_count)

            # Print tokenizer-related keys
            print("\nTokenizer-related metadata:")
            for key, value in metadata.items():
                if 'token' in key.lower() or 'vocab' in key.lower():
                    print(f"  {key}: {value}")

    except Exception as e:
        print(f"Error reading GGUF file: {e}")
        import traceback
        traceback.print_exc()

if __name__ == "__main__":
    main()


