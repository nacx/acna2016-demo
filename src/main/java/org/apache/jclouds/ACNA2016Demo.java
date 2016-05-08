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

import static org.jclouds.compute.options.TemplateOptions.Builder.runScript;

import org.jclouds.ContextBuilder;
import org.jclouds.chef.ChefApi;
import org.jclouds.chef.domain.BootstrapConfig;
import org.jclouds.chef.domain.BootstrapConfig.SSLVerifyMode;
import org.jclouds.chef.util.RunListBuilder;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.domain.OsFamily;
import org.jclouds.compute.domain.Template;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;
import org.jclouds.scriptbuilder.domain.Statement;
import org.jclouds.scriptbuilder.domain.StatementList;
import org.jclouds.scriptbuilder.statements.login.AdminAccess;
import org.jclouds.sshj.config.SshjSshClientModule;

import com.google.common.collect.ImmutableSet;

public class ACNA2016Demo {

   public static void main(final String[] args) throws Exception {

      // Create the connection to the compute provider

      String provider = "google-compute-engine";
      ProviderConfig config = ProviderConfig.load(provider);

      ComputeServiceContext ctx = ContextBuilder.newBuilder(provider)
            .credentials(config.identity(), config.credential())
            .overrides(config.overrides())
            .modules(ImmutableSet.of(new SshjSshClientModule(), new SLF4JLoggingModule()))
            .buildView(ComputeServiceContext.class);

      // Create the connection to the Chef Server

      ProviderConfig chefConfig = ProviderConfig.load("chef");
      ChefApi chef = ContextBuilder.newBuilder("chef")
            .endpoint("https://api.chef.io/organizations/apache-jclouds")
            .credentials(chefConfig.identity(), chefConfig.credential())
            .overrides(chefConfig.overrides())
            .modules(ImmutableSet.of(new SLF4JLoggingModule()))
            .buildApi(ChefApi.class);

      try {
         
         String chefRole = "load-balancer";
         String group = "acna2016-" + chefRole;
         int[] inPorts = { 22, 80, 22002 };
         int numInstances = 1;

         // Create the scripts to be executed on the nodes when they are started

         Statement userAccess = AdminAccess.standard();
         Statement chefBootstrap = generateChefBootstrap(chef, group, chefRole);

         Statement bootstrap = new StatementList(userAccess, chefBootstrap);

         // Select and configure the image and hardware profile to be deployed

         ComputeService compute = ctx.getComputeService();
         
         Template template = compute.templateBuilder()
               .smallest()
               .osFamily(OsFamily.DEBIAN)
               .os64Bit(true)
               .options(runScript(bootstrap)
                     .inboundPorts(inPorts))
               .build();

         // Create the nodes and bootstrap them

         compute.createNodesInGroup(group, numInstances, template);

         System.out.println("Done!");

      } finally {
         // Close the connections and free resources
         ctx.close();
         chef.close();
      }
   }

   private static Statement generateChefBootstrap(final ChefApi chef, final String group, final String role) {
      BootstrapConfig bootstrapConfig = BootstrapConfig.builder()
            .environment("jclouds-demo")
            .runList(new RunListBuilder().addRole(role).build())
            .sslVerifyMode(SSLVerifyMode.NONE)
            .build();

      System.out.println("Generating Chef configuration...");

      chef.chefService().updateBootstrapConfigForGroup(group, bootstrapConfig);
      return chef.chefService().createBootstrapScriptForGroup(group);
   }
}
