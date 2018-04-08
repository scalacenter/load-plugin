# Load sbt plugins programmatically 

This is a module that helps load sbt plugins programmatically or directly from the sbt shell.

This is not an sbt plugin, nor a runnable application. This is a module that is expected to be
loaded inside sbt using the `apply` command.

This module injects a command called `load-plugin` which downloads and enables plugins inside your
build.

## Usage

First, use the `apply` command to get the `load-plugin` command inside sbt. The syntax of the
`apply` command is `apply -cp <jars> <fqcn>`, where `<fqcn>` is the fully qualified class name of an
object extending `State => State`:

```
$ sbt
(...)
sbt:empty> apply -cp (...)/load-plugin_2.12-0.1.0.jar ch.epfl.scala.LoadPlugin
[info] Applying State transformations ch.epfl.scala.LoadPlugin from (...)/load-plugin_2.12-0.1.0-SNAPSHOT.jar
[success] Injected `load-plugin`
```

The `load-plugin` command can be used to add plugins to the build. Here, [sbt-assembly] is
added:

```
sbt:empty> load-plugin com.eed3si9n:sbt-assembly:0.14.6 sbtassembly.AssemblyPlugin
[info] Resolving and loading com.eed3si9n:sbt-assembly:0.14.6
[info] Reapplying settings...
[info] Set current project to empty (in build file:/Users/martin/Desktop/empty/)
[success] Loaded plugin `sbtassembly.AssemblyPlugin`.

sbt:empty> assembly
(...)
[info] Done packaging.
[success] Total time: 4 s, completed Apr 6, 2018 6:53:54 PM
```

The syntax of `load-plugin` is `load-plugin <organization>:<name>:<version> <fqcn>`, where `<fqcn>`
is the fully qualified class name of the object extending `AutoPlugin` in this plugin.

## Why?

We have several competing implementations of language servers for Scala, but currently they all
require a setup that is complicated and drives newcomers away.

<table>
  <tr>
    <th>Language server</th>
    <th>Uses under the hood...</th>
    <th>Assumptions</th>
    <th>How does it start?</th>
  </tr>
  <tr>
    <td><a href="https://github.com/dragos/dragos-vscode-scala">dragos/dragos-vscode-scala</a></td>
    <td>
      <ul>
        <li><a href="https://github.com/sbt/sbt">sbt</a></li>
        <li><a href="https://github.com/ensime/ensime">ensime</a></li>
      </ul>
    </td>
    <td>
      <ul>
        <li>a JDK is installed</li>
        <li>sbt is installed</li>
        <li>ensime is configured</li>
        <li>user has a build.sbt</li>
      </ul>
    </td>
    <td>
      <ul>
        <li>starts sbt process, assuming it's on <code>$PATH</code></li>
      </ul>
    </td>
  </tr>
  <tr>
    <td><a href="https://github.com/scalameta/metals">scalameta/metals</a></td>
    <td>
      <ul>
        <li><a href="https://github.com/sbt/sbt">sbt</a></li>
        <li>semanticdb</li>
      </ul>
    </td>
    <td>
      <ul>
        <li>a JDK is installed</li>
        <li>sbt is running</li>
        <li>an sbt plugin is installed</li>
        <li>user has a build.sbt</li>
      </ul>
    </td>
    <td>
      <ul>
        <li>starts a new Java process<li>
      </ul>
    </td>
  </tr>
  <tr>
    <td><a href="https://github.com/lampepfl/dotty">lampepfl/dotty</a></td>
    <td>
      <ul>
        <li><a href="https://github.com/sbt/sbt">sbt</a></li>
        <li><a href="https://github.com/lampepfl/dotty">lampepfl/dotty</a></li>
      </ul>
    </td>
    <td>
      <ul>
        <li>a JDK is installed</li>
        <li>an sbt plugin is configured</li>
        <li>user has a build.sbt</li>
      </ul>
    </td>
    <td>
      <ul>
        <li>start the server and your editor directly from sbt</li>
      </ul>
    </td>
  </tr>
  <tr>
    <td><a href="https://github.com/redhat-developer/vscode-java">redhat-developer/vscode-java</a></td>
    <td>
      <ul>
        <li><a href="https://github.com/eclipse/eclipse.jdt.ls">eclipse/eclipse.jdt.ls</a></li>
      </ul>
    </td>
    <td>
      <ul>
        <li>a JDK is installed</li>
      <ul>
    </td>
    <td>
      <ul>
        <li>just open up a java file</li>
      </ul>
    </td>
  </tr>
</table>

This table shows that all the Scala language server implementations here depend on sbt being
installed on the user machine, or being able to start it. Moreover, the user has to perform several
configuration steps before getting any help from her IDE.

In comparison, in the Java world, a newcomer can simply install VSCode, install the first extension
that shows up when looking for `java`, and create a file `HelloWorld.java`. Immediately, red
squiggles, completion, documentation, etc. will work. Even from an empty directory!

This is more complicated for newcomers to Scala, because they are expected to know that they need to
install sbt, write a build definition and configure plugins before they can get a satisfactory IDE
experience.

I believe that one reason for that is because of how hard it is to programmatically start sbt and
use it to host a language server.

This is exactly the problem that this module wants to solve.

## Isn't that supposed to be solved by BSP?

This problem is orthogonal. In the case of an empty workspace, there will be no BSP server to
communicate with, and the LSP server will not know how to start a BSP server.

Finally, this module may also be useful for starting up a BSP server that an LSP server would want
to communicate with.

## Experimentation

I have been experimenting with this module, trying to get a better user experience for newcomers.
While it is not clear which language server to try it with for Scala 2.x, I have been trying with
the Dotty language server. Once the LSP-WG reaches a consensus on which LSP for Scala 2.x efforts
should be focused, I'd like to continue the experiment with it.

**My experimentations have shown that it is possible for newcomers to get a good IDE experience by
installing only VSCode and an extension.** Of course, while nothing of what I'm proposing is
currently published the setup seems complicated:

 1. Clone and `publishLocal` this branch: https://github.com/Duhemm/dotty/tree/topic/sbt1-no-sources
 1. Clone and `package` this repository.
 1. Create a new, empty directory on your machine. `cd` into that directory.
 1. Put the following in `launch.sh`: \\
    ```sh
    #!/bin/env sh
    
    coursier launch org.scala-sbt:sbt-launch:1.1.2 -- \
        "apply -cp <path-to-this-package>.jar ch.epfl.scala.LoadPlugin" \
        "set every scalaVersion := \"0.8.0-bin-SNAPSHOT\"" \
        "load-plugin ch.epfl.lamp:sbt-dotty:0.2.0-SNAPSHOT dotty.tools.sbtplugin.DottyPlugin" \
        "load-plugin ch.epfl.lamp:sbt-dotty:0.2.0-SNAPSHOT dotty.tools.sbtplugin.DottyIDEPlugin" \
        "launchIDE" \
        "shell"
    ```
 1. Run `launch.sh`.
 1. VSCode starts up, with all the features of Dotty IDE.

Obviously, the above script looks big and scary: this is only a prototype. In the real world, this
code would be part of the extension loading.

## What's next?

There's no reason why this approach couldn't work with other LSP server depending on sbt. I'd like
to work to support a welcoming and helpful IDE experience for newcomers to the Scala language. Once
it is decided what LSP server should be pushed forward for Scala 2.x, I'll use this module to add a
hassle-free experience for that LSP server.

[sbt-assembly]: https://github.com/sbt/sbt-assembly
