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
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;

import com.github.sardine.DavResource;
import com.github.sardine.Sardine;
import com.github.sardine.SardineFactory;

import ezvcard.Ezvcard;
import ezvcard.VCard;
import ezvcard.property.Note;
import ezvcard.property.RawProperty;

/**
 * manage vcards for a single user in as a store
 * 
 * @author wf
 * 
 */

public class CardDavStore {
  protected static Logger LOGGER = Logger.getLogger("com.bitplan.vcard");

  String host;
  String path;
  String username;
  String password;
  String title;

  /**
   * @return the title
   */
  public String getTitle() {
    return title;
  }

  /**
   * @param title
   *          the title to set
   */
  public void setTitle(String title) {
    this.title = title;
  }

  String backupPath;

  boolean debug = false;

  protected static Map<String, Properties> propMap = new HashMap<String, Properties>();
  Map<String, VCard> vcardByUid = new HashMap<String, VCard>();
  Map<String, VCard> vcardByName = new HashMap<String, VCard>();

  private Sardine sardine;

  private Map<String, DavResource> resourceMap = new HashMap<String, DavResource>();
  private Map<String, SyncRef> syncMap = new HashMap<String, SyncRef>();

  /**
   * @return the backupPath
   */
  public String getBackupPath() {
    return backupPath;
  }

  /**
   * @param backupPath
   *          the backupPath to set
   */
  public void getBackupPath(String backupPath) {
    this.backupPath = backupPath;
  }

  /**
   * @return the username
   */
  public String getUsername() {
    return username;
  }

  /**
   * @param username
   *          the username to set
   */
  public void setUsername(String username) {
    this.username = username;
  }

  /**
   * @return the password
   */
  public String getPassword() {
    return password;
  }

  /**
   * @param password
   *          the password to set
   */
  public void setPassword(String password) {
    this.password = password;
  }

  /**
   * @return the host
   */
  public String getHost() {
    return host;
  }

  /**
   * @param host
   *          the host to set
   */
  public void setHost(String host) {
    this.host = host;
  }

  /**
   * @return the path
   */
  public String getPath() {
    return path;
  }

  /**
   * @param path
   *          the path to set
   */
  public void setPath(String path) {
    this.path = path;
  }

  /**
   * @return the debug
   */
  public boolean isDebug() {
    return debug;
  }

  /**
   * @param debug
   *          the debug to set
   */
  public void setDebug(boolean debug) {
    this.debug = debug;
  }

  public Map<String, DavResource> getResourceMap() {
    return resourceMap;
  }

  public void setResourceMap(Map<String, DavResource> resourceMap) {
    this.resourceMap = resourceMap;
  }

  /**
   * get the property file for the given user
   * 
   * @param user
   * @return - the property file
   */
  public static File getPropertyFile(String user) {
    String propertyFileName = System.getProperty("user.home") + "/.vcard/"
        + user + ".ini";
    File propFile = new File(propertyFileName);
    return propFile;
  }

  /**
   * get the given Property for the given user
   * 
   * @param user
   * @param propName
   * @return the property
   * @throws FileNotFoundException
   * @throws IOException
   */
  public static String getProperty(String user, String propName)
      throws FileNotFoundException, IOException {
    Properties prop = propMap.get(user);
    if (prop == null) {
      prop = new Properties();
      File propFile = getPropertyFile(user);

      prop.load(new FileReader(propFile));
      propMap.put(user, prop);
      // properties.load(ClassLoader.getSystemResourceAsStream(testPropertiesFileName));
    }
    return prop.getProperty(propName);
  }

  /**
   * get the CardDav Store for the given user
   * 
   * @param user
   * @return the CardDavStore
   * @throws Exception
   */
  public static CardDavStore getCardDavStore(String user) throws Exception {
    String host = getProperty(user, "host");
    String path = getProperty(user, "path");
    CardDavStore cs = new CardDavStore(host, path);
    cs.setUsername(getProperty(user, "username"));
    cs.setPassword(getProperty(user, "password"));
    cs.getBackupPath(getProperty(user, "backup"));
    cs.setTitle(getProperty(user, "title"));
    cs.initSardine();
    return cs;
  }

  private void initSardine() {
    sardine = SardineFactory.begin(username, password);
  }

  /**
   * create a Card Dav Store
   * 
   * @param host
   * @param path
   */
  public CardDavStore(String host, String path) {
    super();
    this.host = host;
    this.path = path;
  }

  /**
   * add the given vcard
   * 
   * @param vcard
   * @param uidToUse
   */
  public void add(VCard vcard, String uidToUse) {
    String name = vcard.getFormattedName().getValue();
    String uid = this.getSyncUid(vcard);
    if (uidToUse != null) {
      if (uid == null) {
        uid = uidToUse;
        LOGGER.log(Level.WARNING,
            "VCard with no uid added: " + name + "(" + uid + ")");
      } else {
        if (!uidToUse.equals(uid)) {
          LOGGER.log(Level.WARNING, "VCard with different uid added: " + name
              + "(" + uid + "!=" + uidToUse + ")");
        }
      }
    }
    if (uid != null) {
      this.vcardByUid.put(uid, vcard);
    } else {
      LOGGER.log(Level.WARNING, "VCard with no uid found: " + name);
      // https://code.google.com/p/ez-vcard/wiki/WritingVCards
    }
    if (this.vcardByName.containsKey(name)) {
      LOGGER.log(Level.SEVERE, "Duplicate VCard for name found: " + name);
    }
    this.vcardByName.put(name, vcard);
  }

  /**
   * read this store from a single VCardFile (or directory)
   * 
   * @param vcardPath
   * @param title
   *          - the title for this Cardfile
   * @throws IOException
   */
  public CardDavStore(File vcardPath, String title) throws IOException {
    this.setTitle(title);
    read(vcardPath);
  }

  public void read(File vcardPath) throws IOException {
    if (vcardPath.isDirectory()) {
      String[] vcardFileNames = vcardPath.list(new FilenameFilter() {
        public boolean accept(File dir, String name) {
          return name.toLowerCase().endsWith(".vcf");
        }
      });
      for (String vcardFileName : vcardFileNames) {
        File vcardFile = new File(vcardPath, vcardFileName);
        VCard vcard = Ezvcard.parse(vcardFile).first();
        add(vcard, null);
      }
    } else {
      List<VCard> vcards = Ezvcard.parse(vcardPath).all();
      for (VCard vcard : vcards) {
        add(vcard, null);
      }
    }
  }

  /**
   * get the Synchronization uid
   * 
   * @param vcard
   * @return the uid
   */
  public String getSyncUid(VCard vcard) {
    /*
     * wf@jupiter:/Volumes/backup/digda/jupiter/wf>grep -i uid
     * ContactsMac2014-08-06.vcf | head -10
     * X-ABUID:7A59C9D8-1A65-46D9-9A43-956B4D150FC8
     * X-ABUID:D0C725CA-FA66-44EF-9CBE-DFCE87077FF8
     * X-ABUID:ACCC09F2-3B29-4582-A425-4DAF71065EEC
     * UID:d6bb8f2d-cc07-4a2a-bfe7-8810e8418650
     * X-ABUID:1EF19C6C-AB76-4522-A6D4-242375890AAC:ABPerson
     * UID:2a9df276-e72c-4ba6-be21-61b08acac9ac
     * X-ABUID:3786654B-EDF7-4551-A912-698E940A0336:ABPerson
     * UID:283281f3-2e54-4441-89d3-8332e15f5b4b
     * X-ABUID:283281F3-2E54-4441-89D3-8332E15F5B4B:ABPerson
     * UID:5d408420-33f8-4ad7-9afa-6b672d10306b
     */
    String uid = null;
    if (vcard.getUid() != null)
      uid = vcard.getUid().getValue();
    List<RawProperty> eprops = vcard.getExtendedProperties();
    for (RawProperty eprop : eprops) {
      String propName = eprop.getPropertyName();
      String value = eprop.getValue();
      LOGGER.log(Level.INFO, propName + "=" + value);
      if (propName.equals("X-ABUID")) {
        // uid = eprop.getValue().toLowerCase();
      }
    }
    return uid;
  }

  /**
   * get a map of VCard references from the server if the map is empty
   * 
   * @throws Exception
   */
  public void getVCardRefs() throws Exception {
    if (getResourceMap().size() == 0) {
      List<DavResource> resources = sardine.list(host + path);
      if (debug) {
        LOGGER.log(Level.INFO, " got " + resources.size() + " resources");
      }
      for (DavResource resource : resources) {
        String contentType = resource.getContentType();
        if (contentType.contains("text/vcard")) {
          String uid = this.getUid(resource);
          getResourceMap().put(uid, resource);
        }
      }
    }
  }

  /**
   * the synchronization state of a VCard
   */
  enum SyncState {
    untouchedCard, modifiedCard, newCard
  };

  /**
   * a synchronisation reference
   * 
   * @author wf
   *
   */
  public class SyncRef {
    SyncState syncState;
    DavResource ref;
    String uid;
    VCard vcard;
    File vCardFile;

    /**
     * construct me from the given VCard Reference
     * 
     * @param ref
     */
    public SyncRef(DavResource ref) {
      this.ref = ref;
      uid = CardDavStore.this.getUid(ref);
      syncState = SyncState.untouchedCard;
      // example href:
      // /davical/caldav.php/wf/addresses/e700df6d-c135-4b8b-855a-da5cf89ed90a.vcf
      vCardFile = getVCardFile(ref);
      boolean needSync = !vCardFile.exists();
      if (!needSync) {
        Date filemodified = new Date(vCardFile.lastModified());
        needSync = filemodified.before(ref.getModified());
        if (needSync)
          syncState = SyncState.modifiedCard;
      } else {
        syncState = SyncState.newCard;
      }
    }

    /**
     * synchronize me
     * 
     * @throws Exception
     */
    public void sync() throws Exception {
      switch (syncState) {
      case untouchedCard:
        vcard = Ezvcard.parse(vCardFile).first();
        break;
      case modifiedCard:
      case newCard:
        vcard = getVCardFromRef(ref);
        write(vcard, vCardFile, ref);
      }
      if (debug) {
        LOGGER.log(Level.INFO, this.toString());
      }
    }

    /**
     * get a human readable text representation of me
     */
    public String toString() {
      String text = uid + ":" + vcard.getFormattedName().getValue();
      for (Note note : vcard.getNotes()) {
        text += "\n\t" + note.getValue();
      } // for
      return text;
    }
  }

  /**
   * prepare the synchronization by creating a map of Synchronization References
   * 
   * @throws Exception
   */
  public void prepareSync() throws Exception {
    getVCardRefs();
    syncMap.clear();
    for (DavResource ref : getResourceMap().values()) {
      if (debug) {
        LOGGER.log(Level.INFO, ref.toString());
      }
      String uid = this.getUid(ref);
      syncMap.put(uid, new SyncRef(ref));
    }
  }

  /**
   * get the VCard for the given ref
   * 
   * @param ref
   * @return the VCard
   * @throws Exception
   */
  public VCard getVCardFromRef(DavResource ref) throws Exception {
    String url = this.getHost() + ref.getPath();
    InputStream is = sardine.get(url);
    String vcardText = IOUtils.toString(is, StandardCharsets.UTF_8);
    VCard vcard = Ezvcard.parse(vcardText).first();
    return vcard;
  }

  /**
   * write the given vcard to the given vCardFile using the given ref
   * 
   * @param vcard
   * @param vCardFile
   * @param ref
   * @throws Exception
   */
  public void write(VCard vcard, File vCardFile, DavResource ref)
      throws Exception {
    vcard.write(vCardFile);
    vCardFile.setLastModified(ref.getModified().getTime());
  }

  /**
   * get the Uid for the given VCard reference
   * 
   * @param ref
   * @return - the uid
   */
  public String getUid(DavResource ref) {
    String[] hrefparts = ref.getPath().split("/");
    String uid = hrefparts[hrefparts.length - 1];
    return uid.replace(".vcf", "");
  }

  /**
   * get the VCard file for the given reference
   * 
   * @param ref
   * @return the vcard file
   */
  private File getVCardFile(DavResource ref) {
    String cardfilename = getUid(ref) + ".vcf";
    File vCardFile = new File(this.backupPath, cardfilename);
    return vCardFile;
  }

  /**
   * Synchronize my local store with the server
   * 
   * @throws Exception
   */
  public void sync() throws Exception {
    prepareSync();

    // loop over all references
    for (SyncRef syncRef : syncMap.values()) {
      if (debug) {
        LOGGER.log(Level.INFO, syncRef.toString());
      }
      syncRef.sync();
    } // for
    this.synchronizationStatistics();
  } // sync

  public void synchronizationStatistics() {
    // prepare statistic
    Map<SyncState, AtomicInteger> stats = new HashMap<SyncState, AtomicInteger>();
    for (SyncState s : SyncState.values()) {
      stats.put(s, new AtomicInteger(0));
    }
    for (SyncRef syncRef : syncMap.values()) {
      // update statistics
      AtomicInteger count = stats.get(syncRef.syncState);
      count.set(count.get() + 1);
    }
    for (SyncState s : SyncState.values()) {
      LOGGER.log(Level.INFO, s.name() + ": " + stats.get(s));
    }
  }

  public void diff(Map<String, VCard> self, Map<String, VCard> other,
      String thisTitle, String otherTitle) {
    Map<String, VCard> bigger = other;
    Map<String, VCard> smaller = self;
    String biggerTitle = otherTitle;
    String smallerTitle = thisTitle;
    if (self.size() > other.size()) {
      bigger = self;
      biggerTitle = thisTitle;
      smaller = other;
      smallerTitle = otherTitle;
    }
    System.out.println(bigger.size() + " (" + biggerTitle + ") <-> "
        + smaller.size() + "(" + smallerTitle + ")");
    // check the uids
    int index = 0;
    for (String key : bigger.keySet()) {
      if (smaller.get(key) == null) {
        VCard vcard = bigger.get(key);
        index++;
        System.out.println("" + index + ":vcard " + key + " '"
            + vcard.getFormattedName().getValue() + "' "
            + vcard.getUid().getValue() + " is missing in " + smallerTitle);
      }
    }
  }

  /**
   * check the difference between two stores
   * 
   * @param other
   */
  public void diff(CardDavStore other) {
    diff(this.vcardByUid, other.vcardByUid, this.getTitle(), other.getTitle());
    diff(this.vcardByName, other.vcardByName, this.getTitle(),
        other.getTitle());
  }

  /**
   * get the VCards for this store
   * 
   * @param limit - the maximum number of cards to get
   * @return - the vcards
   * @throws Exception
   */
  public List<VCard> getVCards(int limit) throws Exception {
    List<VCard> result = new ArrayList<VCard>();
    this.getVCardRefs();
    for (DavResource ref : getResourceMap().values()) {
      VCard vcard = this.getVCardFromRef(ref);
      result.add(vcard);
      if (result.size() >= limit) {
        break;
      }
    }
    return result;
  }

  /**
   * backup all my vcards
   * 
   * @throws Exception
   */
  public List<VCard> backup() throws Exception {
    File vcardPath = new File(backupPath);
    vcardPath.mkdirs();
    List<VCard> result = new ArrayList<VCard>();
    this.getVCardRefs();
    for (DavResource ref : getResourceMap().values()) {
      VCard vcard = this.getVCardFromRef(ref);
      write(vcard, getVCardFile(ref), ref);
      if (debug)
        System.out.println(vcard.getFormattedName().getValue());
      result.add(vcard);
    }
    return result;
  }

  /**
   * read me from the backup directory
   * 
   * @throws IOException
   */
  public void readFromBackup() throws IOException {
    read(new File(backupPath));
  }

  /**
   * remove the given vcard
   * 
   * @param uid
   * @throws Exception
   */
  public void remove(String uid) throws Exception {
    getVCardRefs();
    DavResource ref = getResourceMap().get(uid);
    if (ref != null) {
      System.out.println("removing " + uid);
      sardine.delete(host + ref.getPath());
      File vcardFile = this.getVCardFile(ref);
      vcardFile.delete();
    } else {
      System.err.println("not found " + uid);
    }
  }

} // CardDavStore
