/**
 * Copyright (C) 2016 Ignasi Barrera
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
package org.apache.jclouds;

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

/**
 * Configures a connection to a cloud provider.
 */
public class ProviderConfig {

   private final String identity;
   private final String credential;
   private final Properties overrides;

   /**
    * Loads the provider configuration from the properties file for the
    * provider.
    */
   public static ProviderConfig load(final String provider) throws IOException {
      Properties props = new Properties();
      props.load(ProviderConfig.class.getResourceAsStream("/" + provider + ".properties"));

      String identity = (String) props.remove(provider + ".identity");
      String credential = fileContents((String) props.remove(provider + ".credential"));

      Properties overrides = new Properties();
      for (Object key : props.keySet()) {
         overrides.put(key, readFileOrValue(props, (String) key));
      }

      return new ProviderConfig(identity, credential, overrides);
   }

   /**
    * Reads the value of a property, and if it is a file, reads its contents.
    */
   private static String readFileOrValue(final Properties props, final String key) throws IOException {
      String value = props.getProperty(key);
      return new File(value).canRead() ? fileContents(value) : value;
   }
   
   /**
    * Reads a given file and returns a String with the contents.
    */
   private static String fileContents(final String path) throws IOException {
      return Files.toString(new File(path), Charsets.UTF_8);
   }

   private ProviderConfig(final String identity, final String credential, final Properties overrides) {
      this.identity = requireNonNull(identity, "identity cannot be null");
      this.credential = requireNonNull(credential, "credential cannot be null");
      this.overrides = requireNonNull(overrides, "overrides cannot be null");
   }

   public String identity() {
      return identity;
   }

   public String credential() {
      return credential;
   }

   public Properties overrides() {
      return overrides;
   }

}
