/**
 * Copyright (c) 2019 BITPlan GmbH
 *
 * http://www.bitplan.com
 *
 * This file is part of the Opensource project at:
 * https://github.com/BITPlan/com.bitplan.vcard
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bitplan.vcard;

import static org.junit.Assert.*;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

/**
 * Test the user settings
 * 
 * @author wf
 *
 */
public class TestUserSettings {

  @Test
  /**
   * @see <a href="https://github.com/BITPlan/com.bitplan.vcard/issues/1">Issue
   *      #1</a>
   */
  public void testJohnDoeSettings() throws Exception {
    String props = "# Properties for vcard\n" + "# JD 2019-05-04\n"
        + "username=johnd\n" + "password=uCtE0kklvnb$#9537\n"
        + "host=https://dav.bitplan.com\n"
        + "path=/remote.php/carddav/addressbooks/johndoe/default\n"
        + "backup=/Users/jd/backup/Contacts\n"
        + "title=John Doe's address book";
    File propertyFile = CardDavStore.getPropertyFile("johnd");
    FileUtils.writeStringToFile(propertyFile, props, "UTF-8");
    // check the properties
    String[] proplines = props.split("\n");
    for (String propline : proplines) {
      String namevalue[] = propline.split("=");
      if (!propline.startsWith("#")) {
        assertEquals(2, namevalue.length);
        String name=namevalue[0];
        String value=namevalue[1];
        assertEquals(value, CardDavStore.getProperty("johnd", name));
      }
    }
  }

}
