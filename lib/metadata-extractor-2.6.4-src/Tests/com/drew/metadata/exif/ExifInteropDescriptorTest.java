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

import org.junit.Assert;
import org.junit.Test;

/**
 * JUnit test case for class ExifInteropDescriptor.
 *
 * @author  Drew Noakes http://drewnoakes.com
 */
public class ExifInteropDescriptorTest
{
    @Test
    public void testGetInteropVersionDescription() throws Exception
    {
        ExifInteropDirectory directory = new ExifInteropDirectory();
        directory.setIntArray(ExifInteropDirectory.TAG_INTEROP_VERSION, new int[]{0, 1, 0, 0});
        ExifInteropDescriptor descriptor = new ExifInteropDescriptor(directory);
        Assert.assertEquals("1.00", descriptor.getDescription(ExifInteropDirectory.TAG_INTEROP_VERSION));
        Assert.assertEquals("1.00", descriptor.getInteropVersionDescription());
    }

    @Test
    public void testGetInteropIndexDescription() throws Exception
    {
        ExifInteropDirectory directory = new ExifInteropDirectory();
        directory.setString(ExifInteropDirectory.TAG_INTEROP_INDEX, "R98");
        ExifInteropDescriptor descriptor = new ExifInteropDescriptor(directory);
        Assert.assertEquals("Recommended Exif Interoperability Rules (ExifR98)", descriptor.getDescription(ExifInteropDirectory.TAG_INTEROP_INDEX));
        Assert.assertEquals("Recommended Exif Interoperability Rules (ExifR98)", descriptor.getInteropIndexDescription());
    }
}
