# Jenkins Reservable Resources Plugin 

This plugin adds extensions to Jenkins that allow to define nodes as reservale resources (such as databases, 
other servers needed, etc.), which can be used by the build using manually defined labels. If there is no free
resource available, the build will wait a configurable amount of time to acquire one, after which the build
will be aborted.

The plugin will expose the node name to the build via a environment variable called <PREFIX>_NODE_NAME, where <PREFIX>
is the the prefix that user needs to define for each job. If you don't care about this you can just use the default prefix.
The reservable resources can also define additional settings that will exposed to the build using same mechanism.
  
  ![image](https://user-images.githubusercontent.com/5693250/117473382-e70e6e00-af27-11eb-9cde-55a1656e9de3.png)
  
In addition, the plugin provides a simple monitoring and management page accessible to all authenticated users.

![image](https://user-images.githubusercontent.com/5693250/117475277-d4953400-af29-11eb-935a-3ef8046ecfaf.png)

## How to use the plugin

### Adding reservable resources

1. Navigate to a node (computer) you want to make a resource
2. Click **Configure**
3. Under *Node Properties* check off **Reservable resource**

   ![image](https://user-images.githubusercontent.com/5693250/117472560-0658cb80-af27-11eb-838b-292d83db4525.png)
5. Optionally, click **Add** button to add static settings that resource should expose to build
6. Click **Save**

### Configuring build job

1. Navigate to a project (job) you want to modify
2. Click **Configure**
3. Under *Build Enviornment* check off **This build requires reservable resource(s)**
4. Enter/select *Resource label*

   ![image](https://user-images.githubusercontent.com/5693250/117477351-e7106d00-af2b-11eb-9afb-db5a013d41f3.png)
5. Optionally, click *Add** button to add more resources making sure each resource has a unique prefix.
6. Click **Save**

## Acknowledgements

<div>Icons made by <a href="https://www.flaticon.com/authors/phatplus" title="phatplus">phatplus</a> from <a href="https://www.flaticon.com/" title="Flaticon">www.flaticon.com</a></div>
<div>Icons made by <a href="https://www.freepik.com" title="Freepik">Freepik</a> from <a href="https://www.flaticon.com/" title="Flaticon">www.flaticon.com</a></div>

## License

Licensed under MIT, see [LICENSE](LICENSE)
