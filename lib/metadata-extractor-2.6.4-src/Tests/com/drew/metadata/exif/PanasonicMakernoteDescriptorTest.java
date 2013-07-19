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

import com.drew.imaging.jpeg.JpegMetadataReader;
import com.drew.metadata.Age;
import com.drew.metadata.Face;
import com.drew.metadata.Metadata;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

/**
 * @author psandhaus, Drew Noakes
 */
public class PanasonicMakernoteDescriptorTest
{
    private PanasonicMakernoteDirectory _panasonicDirectory;

    @Before
    public void setUp() throws Exception
    {
        File file = new File("Tests/com/drew/metadata/exif/withPanasonicFaces.jpg");
        Metadata metadata = JpegMetadataReader.readMetadata(file);
        _panasonicDirectory = metadata.getDirectory(PanasonicMakernoteDirectory.class);
    }

    @Test
    public void testGetDetectedFaces() throws Exception
    {
        Face expResult = new Face(142, 120, 76, 76, null, null);
        Face[] result = _panasonicDirectory.getDetectedFaces();
        Assert.assertNotNull(result);
        Assert.assertEquals(expResult, result[0]);
    }

    @Test
    public void testGetRecognizedFaces() throws Exception
    {
        Face expResult = new Face(142, 120, 76, 76, "NIELS", new Age(31, 7, 15, 0, 0, 0));
        Face[] result = _panasonicDirectory.getRecognizedFaces();
        Assert.assertNotNull(result);
        Assert.assertEquals(expResult, result[0]);
    }
}
