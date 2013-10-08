kevoree-classloading-framework
==============================

Kevoree Class loading framwork (Aka KCL)

Minimal and efficient classloading library for the JVM.

### Fill the gap between compile and runtime environement !

Application are build using severals dependencies. So far so good! Project description tools like Maven describe application like a graph of dependencies. So we need to transpose such mecanism to the runtime world to fill the gap between compile and deploy.

### Why isolated classloader?

Because application can use conflicting libraries, every JAR must be isolated into a separated classloader not to have side effect. A simple log example is enough to illustrate this problem, if a jar named Core depends on SLF4J API in version 1.6 and a module Mod depend on SLF4J version 1.7, one can face a compatilibty issue. Perhaps you can decide to only keep at runtime the version 1.7 but then you can break your code if for instance one method has been removed in the new SLF4J 1.7.

So flattening libraries at runtime can lead to serious link issues and is in fact a lost of type checking because it hides the real dependency at compile time.

In conclusion, each JAR can be deployed several times, in several versions, and have different relationships between them, depending of the ones expressed at compile time.

### Wait, why not using a framework like OSGi?

For many reasons, first of all for the sake of simplicity. 

One main reason is that for us the deploy mechanism must not impact the deployment mechanism, so if my JAR is resolvable by Maven I see no reason to forbid to deploy this JAR into my classpath…

Next, performance is critical. Because the classloading is the backbone of your application any slow down will impact all your application. So taking care about link speed and classloader resolution is very important…

In addition, frameworks like OSGi have a lot of interesting features like events managements and URL resolution and so on. Most of them are far from a bootstrap service but their high number is a strong issue when porting such framework on embedded environement like RaspberryPI and Android devices.

### Ok how to use it?

The simplest way is to use the maven dependency system to build your own platform.
Then include this dependency in your project:

        <dependency>
            <groupId>org.kevoree.kcl</groupId>
            <artifactId>org.kevoree.kcl</artifactId>
            <version>REPLACE_KCL_VERSION</version>
        </dependency>
        
Current KCL version: 2 (depends on Kotlin 0.6.602)

Then in Java code you have two important class to now:
- Klassloader: KCL API
- KevoreeJarClassLoader: Default JAR ClassLoader Scope

Through an example:


##### 1) Create an isolated scope: 
	
		Klassloader kclScope1 = new KevoreeJarClassLoader();
		
##### 2) Fill scope with classes:

		//from plain Jar file
		kclScope1.addJarFromURL("file://slf4j.jar");
		
		//from direct stream must by .class or .jar or zip fie
		kclScope1.addJarFromStream("http://...");
		
##### 3) Isolate or not from system classloader:

		//optional
		//isolate the kclScope1 from system already loaded classes
		//i.e. if you already have loading SLF4J in your bootclasspath
		//and you want to avoid conficts.
		kclScope1.isolateFromSystem();
		
##### 4) Call classes from your scope:

		Class cl1 = kclScope1.loadClass("org.slf4j.Logger");
		Object objOfCl1 = cl1.newInstance();
		
##### 5) Create another scope:

		Klassloader kclScope2 = new KevoreeJarClassLoader();
		kclScope2.addJarFromURL("file://yourCoreApp.jar");
		
##### 6) Link your main app with the needed SLF4J library:

		//can be call dynamicallly at anytime
		//removeChild also available
		kclScope2.addChild(kclScope1);	
			
		
##### 7) Run your app:
		
		Class clMain = kclScope2.loadClass("yourMain");
		//slf4j classes will be resolved transitively
		clMain.getMethod("main").invoke();


That's all folks :-)



