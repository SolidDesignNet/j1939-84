/**
 * Copyright 2019 Equipment & Tool Institute
 */
package org.etools.j1939_84.resources;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

/**
 * Unit tests for the {@link J193984Resources} class
 *
 * @author Matt Gumbel (matt@soliddesign.net)
 *
 */
public class ResourcesTest {

    @Test
    public void testGetLogoImage() {
        assertNotNull(J193984Resources.getLogoImages());
    }

}
