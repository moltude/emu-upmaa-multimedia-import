/*
 * Copyright 2002-2012 Drew Noakes
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 * More information about this project is available at:
 *
 *    http://drewnoakes.com/code/exif/
 *    http://code.google.com/p/metadata-extractor/
 */
package com.drew.imaging.tiff;

import com.drew.lang.ByteArrayReader;
import com.drew.lang.RandomAccessFileReader;
import com.drew.lang.annotations.NotNull;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifReader;

import java.io.*;

/**
 * Obtains all available metadata from TIFF formatted files.  Note that TIFF files include many digital camera RAW
 * formats, including Canon (CRW, CR2) and Nikon (NEF).
 *
 * @author Darren Salomons, Drew Noakes http://drewnoakes.com
 */
public class TiffMetadataReader
{
    @NotNull
    public static Metadata readMetadata(@NotNull File file) throws IOException
    {
        Metadata metadata = new Metadata();

        RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r");

        try {
        new ExifReader().extractTiff(new RandomAccessFileReader(randomAccessFile), metadata);
        } finally {
            randomAccessFile.close();
        }

        return metadata;
    }

    @Deprecated
    @NotNull
    public static Metadata readMetadata(@NotNull InputStream inputStream, boolean waitForBytes) throws IOException
    {
        // NOTE this method is very inefficient, as it attempts to read the entire TIFF file into a byte[]
        // TIFF processing requires random access, as directories can be scattered throughout the byte sequence.
        // InputStream does not support seeking backwards, and so is not a viable option for TIFF processing

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int b;
        // TODO do this in chunks rather than byte-by-byte, and honour 'waitForBytes'
        while((b = inputStream.read()) != -1) {
            out.write(b);
        }
        Metadata metadata = new Metadata();
        new ExifReader().extractTiff(new ByteArrayReader(out.toByteArray()), metadata);
        return metadata;
    }
}
