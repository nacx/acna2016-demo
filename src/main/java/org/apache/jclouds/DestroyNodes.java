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

import static org.jclouds.compute.predicates.NodePredicates.inGroup;

import java.util.Set;
import java.util.stream.Collectors;

import org.jclouds.ContextBuilder;
import org.jclouds.chef.ChefApi;
import org.jclouds.chef.ChefService;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;
import org.jclouds.sshj.config.SshjSshClientModule;

import com.google.common.collect.ImmutableSet;

public class DestroyNodes {

   public static void main(final String[] args) throws Exception {

      String provider = "google-compute-engine";
      ProviderConfig config = ProviderConfig.load(provider);

      // Create the connection to the compute provider

      ComputeServiceContext ctx = ContextBuilder.newBuilder(provider)
            .credentials(config.identity(), config.credential())
            .overrides(config.overrides())
            .modules(ImmutableSet.of(new SshjSshClientModule(), new SLF4JLoggingModule()))
            .buildView(ComputeServiceContext.class);

      // Create the connection to the Chef server

      ProviderConfig chefConfig = ProviderConfig.load("chef");
      ChefApi chef = ContextBuilder.newBuilder("chef")
            .endpoint("https://api.chef.io/organizations/apache-jclouds")
            .credentials(chefConfig.identity(), chefConfig.credential())
            .overrides(chefConfig.overrides())
            .modules(ImmutableSet.of(new SLF4JLoggingModule()))
            .buildApi(ChefApi.class);

      try {
         
         // Destroy all nodes that belong to the used groups

         ComputeService compute = ctx.getComputeService();
         
         compute.destroyNodesMatching(inGroup("acna2016-webserver"));
         compute.destroyNodesMatching(inGroup("acna2016-load-balancer"));

         // Delete the information from the Chef server using the Chef API

         ChefService chefService = chef.chefService();
         
         chefService.deleteAllClientsInList(withPrefix(chef.listClients(), "acna2016"));
         chefService.deleteAllNodesInList(withPrefix(chef.listNodes(), "acna2016"));

         chef.deleteDatabag("bootstrap");
         
      } finally {
         // Close the connections and free resources
         ctx.close();
         chef.close();
      }
   }

   private static Set<String> withPrefix(Set<String> names, String prefix) {
      return names.stream().filter(n -> n.startsWith(prefix)).collect(Collectors.toSet());
   }
}
