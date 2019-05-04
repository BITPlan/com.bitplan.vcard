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

import java.io.File;

import org.junit.Test;

/**
 * test the synchronization
 * @author wf
 *
 */
public class TestSync {

  @Test
  public void testSynchronisation() throws Exception {
    String user="wf"; // modify for testing in your enviroment
    File propertyFile = CardDavStore.getPropertyFile(user);
    if (propertyFile.exists()) {
      CardDavStore cs = CardDavStore.getCardDavStore(user);
      cs.debug=true;
      cs.prepareSync();
      cs.synchronizationStatistics();
    } else {
      System.err.println(String.format("user %s not configured propertyfile %s is missing\nYou might want to adapt the JUnit test with your user settings", user,propertyFile.getPath()));
    }
  }

}
