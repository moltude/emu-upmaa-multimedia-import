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

package com.drew.metadata.exif;

import com.drew.imaging.ImageMetadataReader;
import com.drew.lang.Rational;
import com.drew.metadata.Metadata;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;

/**
 * JUnit test case for class ExifIFD0Descriptor.
 *
 * @author  Drew Noakes http://drewnoakes.com
 */
public class ExifIFD0DescriptorTest
{
    @Test
    public void testXResolutionDescription() throws Exception
    {
        ExifIFD0Directory directory = new ExifIFD0Directory();
        directory.setRational(ExifIFD0Directory.TAG_X_RESOLUTION, new Rational(72, 1));
        // 2 is for 'Inch'
        directory.setInt(ExifIFD0Directory.TAG_RESOLUTION_UNIT, 2);
        ExifIFD0Descriptor descriptor = new ExifIFD0Descriptor(directory);
        Assert.assertEquals("72 dots per inch", descriptor.getDescription(ExifIFD0Directory.TAG_X_RESOLUTION));
    }

    @Test
    public void testYResolutionDescription() throws Exception
    {
        ExifIFD0Directory directory = new ExifIFD0Directory();
        directory.setRational(ExifIFD0Directory.TAG_Y_RESOLUTION, new Rational(50, 1));
        // 3 is for 'cm'
        directory.setInt(ExifIFD0Directory.TAG_RESOLUTION_UNIT, 3);
        ExifIFD0Descriptor descriptor = new ExifIFD0Descriptor(directory);
        Assert.assertEquals("50 dots per cm", descriptor.getDescription(ExifIFD0Directory.TAG_Y_RESOLUTION));
    }

    @Test
    public void testWindowsXpFields() throws Exception
    {
        String fileName = "Tests/com/drew/metadata/exif/windowsXpFields.jpg";
        Metadata metadata = ImageMetadataReader.readMetadata(new File(fileName));
//        Metadata metadata = new Metadata();
//        final byte[] data = new JpegSegmentReader(new File(fileName)).readSegment(JpegSegmentReader.SEGMENT_APP1);
//        Assert.assertNotNull(data);
//        new ExifReader().extract(data, metadata);
        ExifIFD0Directory directory = metadata.getDirectory(ExifIFD0Directory.class);
        Assert.assertNotNull(directory);

        Assert.assertEquals("Testing artist\0", directory.getString(ExifIFD0Directory.TAG_WIN_AUTHOR, "UTF-16LE"));
        Assert.assertEquals("Testing comments\0", directory.getString(ExifIFD0Directory.TAG_WIN_COMMENT, "UTF-16LE"));
        Assert.assertEquals("Testing keywords\0", directory.getString(ExifIFD0Directory.TAG_WIN_KEYWORDS, "UTF-16LE"));
        Assert.assertEquals("Testing subject\0", directory.getString(ExifIFD0Directory.TAG_WIN_SUBJECT, "UTF-16LE"));
        Assert.assertEquals("Testing title\0", directory.getString(ExifIFD0Directory.TAG_WIN_TITLE, "UTF-16LE"));

        ExifIFD0Descriptor descriptor = new ExifIFD0Descriptor(directory);
        Assert.assertEquals("Testing artist", descriptor.getDescription(ExifIFD0Directory.TAG_WIN_AUTHOR));
        Assert.assertEquals("Testing comments", descriptor.getDescription(ExifIFD0Directory.TAG_WIN_COMMENT));
        Assert.assertEquals("Testing keywords", descriptor.getDescription(ExifIFD0Directory.TAG_WIN_KEYWORDS));
        Assert.assertEquals("Testing subject", descriptor.getDescription(ExifIFD0Directory.TAG_WIN_SUBJECT));
        Assert.assertEquals("Testing title", descriptor.getDescription(ExifIFD0Directory.TAG_WIN_TITLE));
    }
}
