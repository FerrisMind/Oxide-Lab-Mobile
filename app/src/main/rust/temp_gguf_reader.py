#!/usr/bin/env python3

import struct
import sys

def read_gguf_header(f):
    magic = f.read(4)
    if magic != b'GGUF':
        raise ValueError("Not a GGUF file")

    version = struct.unpack('<I', f.read(4))[0]
    print(f"GGUF version: {version}")

    tensor_count = struct.unpack('<Q', f.read(8))[0]
    print(f"Tensor count: {tensor_count}")

    metadata_count = struct.unpack('<Q', f.read(8))[0]
    print(f"Metadata count: {metadata_count}")

    # For version 3, there is an additional alignment field (uint32)
    if version >= 3:
        alignment = struct.unpack('<I', f.read(4))[0]
        print(f"Alignment: {alignment}")

    print(f"File position after header: {f.tell()}")
    return version, tensor_count, metadata_count

def read_gguf_string_with_length(f):
    # Read string with length prefix (uint32)
    length = struct.unpack('<I', f.read(4))[0]
    if length > 1000:
        raise ValueError(f"String too long: {length} bytes")
    data = f.read(length)
    return data.decode('utf-8', errors='ignore')

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
        if array_len > 0 and array_len <= 10:  # Read up to 10 elements
            elements = []
            for i in range(array_len):
                try:
                    elements.append(read_gguf_value(f, array_type))
                except Exception as e:
                    elements.append(f"Error: {e}")
                    break
            return f"Array[{array_len}] type={array_type}: {elements}"
        elif array_len > 10:
            # Skip large arrays
            for _ in range(array_len):
                try:
                    read_gguf_value(f, array_type)
                except:
                    break
            return f"Array[{array_len}] type={array_type} (skipped)"
        return f"Array[{array_len}] empty"
    else:
        raise ValueError(f"Unknown value type: {value_type}")

def read_metadata(f, metadata_count):
    print(f"\nReading {metadata_count} metadata entries from position: {f.tell()}")

    # Debug: read first 64 bytes as hex
    current_pos = f.tell()
    debug_data = f.read(64)
    f.seek(current_pos)  # rewind
    print("First 64 bytes of metadata (hex):")
    print(' '.join(f'{b:02x}' for b in debug_data))
    print("First 64 bytes of metadata (repr):")
    print(repr(debug_data[:64]))

    print("\nAll metadata:")
    tokenizer_metadata = []

    for i in range(min(metadata_count, 5)):  # Read only first 5 entries for debugging
        try:
            print(f"\nTrying to read metadata {i} at position {f.tell()}")
            key = read_gguf_string_with_length(f)
            print(f"Metadata {i}: key='{key}'")
            value_type = struct.unpack('<I', f.read(4))[0]
            print(f"  value_type={value_type}")
            value = read_gguf_value(f, value_type)

            print(f"  {key}: {value}")

            if 'token' in key.lower() or 'vocab' in key.lower():
                tokenizer_metadata.append((key, value))

        except Exception as e:
            print(f"Error reading metadata {i}: {e}")
            import traceback
            traceback.print_exc()
            break

    print("\nTokenizer-related metadata:")
    for key, value in tokenizer_metadata:
        print(f"  {key}: {value}")

def main():
    if len(sys.argv) != 2:
        print("Usage: python gguf_reader.py <gguf_file>")
        sys.exit(1)

    file_path = sys.argv[1]
    print(f"Reading GGUF file: {file_path}")

    try:
        with open(file_path, 'rb') as f:
            version, tensor_count, metadata_count = read_gguf_header(f)
            read_metadata(f, metadata_count)

    except Exception as e:
        print(f"Error: {e}")

if __name__ == "__main__":
    main()
