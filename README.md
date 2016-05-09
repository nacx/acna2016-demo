ApacheCon NA 2016 jclouds demo
==============================

The ACNA 2016 demo is an application that deploys nodes to a given cloud provider, and uses 
[Chef](https://www.chef.io) for configuration management and service discovery. If you don't have a Chef Server, you can try this demo for free with a [Hosted Chef](https://www.chef.io/chef/get-chef/) account.

## Configuring the Chef Server

First of all, the Chef Server must have the cookbooks and roles that will be used in this example
application. In order to do that, you need to have the `knife CLI` installed on route machine. If you don't have it, you can download and install Chef from [here](https://downloads.chef.io/chef-client/).

Once the Chef CLI is installed, you can upload everything to the Chef Server as follows:

```bash
gem install Berkshelf    # Cookbook manager used to manage cookbook dependencies
cd chef
berks install            # Download the cookbooks and their dependencies
berks upload             # Upload everything to the Chef Server
```

Once the cookbooks are installed, the roles and environments need to be uploaded:

```bash
knife environment from file environments/*
knife role from file roles/*
```

Now the Chef Server has all it needs for the demo.

## Configuring the connection to the cloud providers

To configure the connection to a cloud provider, create a file in the format `<provider-id>.properties` in the `src/main/resources` directory. The file must contain two keys:

* <provider-id>.identity = <identity used to connect to the provider>
* <provider-id>.credential = <credential used to connect to the provider>

Values can also be path to files that contain the credential. This makes it easier to provide
private keys, and avoids exposing the credentials if the file is uploaded to a version control
system. 

You will also have to change the `chef.properties` file to point to your Chef Server. 