Haven and Hearth Client
=======================

Hi, ProgrammerDan here.

You've found an amazing project, Loftar's Haven and Hearth open source client. It has been modified by EnderWiggin, and now by ProgrammerDan.

The most significant change so far is that I've mavenized as much of the toolchain as I could. 

Some ANT-bits remain, and for posterity (or those who enjoy `ant`) I've left build.xml where it is.

However, this project can now be build in one go via maven, as follows:

```
> mvn clean package -f parent-pom.xml
```

This will build the haven client; then, this will build the haven local resources jar, and put everything into the `target` folder.

You can double-click the hafen.jar file to run it, or invoke from the `target` folder using:

```
> javaw -jar hafen.jar -U http://game.havenandhearth.com/res/ game.havenandhearth.com
```

Enjoy, and happy playing Haven and Hearth.
