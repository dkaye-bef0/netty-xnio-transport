name := "netty-xnio-transport"

val nettyVersion = "4.0.17.Final"
val xnioVersion = "3.2.0.Beta4"

libraryDependencies ++= Seq(
  "io.netty" % "netty-transport" % nettyVersion,
  "io.netty" % "netty-buffer" % nettyVersion,
  "org.jboss.xnio" % "xnio-api" % xnioVersion,
  "org.jboss.xnio" % "xnio-nio" % xnioVersion)